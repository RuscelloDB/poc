package com.ruscello;

import com.google.common.base.Stopwatch;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.ruscello.hashing.MurmurHash3;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junitpioneer.jupiter.TempDirectory;
import org.rocksdb.*;
import org.rocksdb.util.SizeUnit;
import org.springframework.util.StopWatch;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.util.*;

import static com.google.common.io.BaseEncoding.base16;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

// Eventstore index layout
// Each record in the index is 16 bytes:
// 4 bytes for the hash of the stream ID --> uint / uint32
// 4 bytes for the sequence number --> Int
// 8 bytes for the original position the record had in the log. --> Long

// we currently use sequence number in two different contexts
// 1. sequence number within a specific stream
// 2. rocksdb global sequence number
// need to use either different terminology or some other way to differentiate the two

// key spaces
// 1. default (hash of stream id + sequence number of stream)
// 2. total ordered (rocksdb sequence number)
// 3. stream names (stream names as keys
//  - TODO: probably want another column family / key space for hash -> stream name / stream id
//  - somewhat related to https://github.com/EventStore/EventStore.UI/issues/87
//  - how to support like searches (both stream-* and *-...)
//  - how does myrocks do like queries? what data structures do they use
//  - does eventstore do anything similiar to like queries?
//  - would something like ngrams work here?
//  - initial thought that likely is not a good idea. Have one key space with stream names and then another key space
//    for stream name search which could be ngrams that point to the stream name
//    I like the idea of elasticsearch edge-ngram but can make the special characters specific to what we allow for stream names
//    Or like ES come up with some sane default but let users override by setting some RuscolloConfiguration property
//    I also like the idea of doing reverse ngrams. From ES, which essentially reverses the token, builds front EdgeNGrams and reverses the ngram again.
//    Which I think would look for like statements such as *...
//    if stream name was seanCarroll with ngram of min: 1 and max:4 with reverse also configured splitting on upper case
//    we would get: s, se, sea, sean, C, Ca, Car, Carr, n, an, ean, l, ll, oll, roll --> verify this is correct
//    Perhaps check out CJKBigramFilter in lucene regarding searching on whole words that are smaller than min ngram
//    another option is simply to index field twice: once with a standard analyzer and once with ngrams (from elasticsearch)
//    https://potmat.wordpress.com/2015/02/06/camelcase-tokenizer-for-cloudant-search-indexes/
// 4. Persistent subscriptions - this would be where we store consumer offsets. Look at eventstore maybe event kafka
// 5. Categories similar to EventStore? Review how they work.

// Projections
// What ways do we want to allow users to write projections
// eventstore just does javascript
// I'm thinking I would like to support javascript, java, groovy and then maybe scala and closure.
// Would be nice to support C#/F# via .NET core
// I suppose this requires a DSL. Review in-depth how eventstore does this.

// https://www.slideshare.net/matsunobu/myrocks-deep-dive/
@ExtendWith(TempDirectory.class)
public class SampleRocksdbTest {

    @Test
    public void test(@TempDirectory.TempDir Path tmpDir) {

        // a static method that loads the RocksDB C++ library.
        RocksDB.loadLibrary();

        // the Options class contains a set of configurable DB options
        // that determines the behaviour of the database.
        try (final Options options = new Options().setCreateIfMissing(true)) {

            // a factory method that returns a RocksDB instance
            try (final RocksDB db = RocksDB.open(options, tmpDir.toFile().getAbsolutePath())) {

                final int numberOfPuts = 5;
                for (int i = 0; i < numberOfPuts; i++) {
                    db.put(String.valueOf(i).getBytes(), String.valueOf(i).getBytes());
                }
                db.flush(new FlushOptions().setWaitForFlush(true));
                // insert 5 writes into a cf
                try (final ColumnFamilyHandle cfHandle = db.createColumnFamily(
                        new ColumnFamilyDescriptor("secondary_index".getBytes()))) {
                    for (int i = 0; i < numberOfPuts; i++) {
                        db.put(cfHandle, String.valueOf(i).getBytes(), String.valueOf(i).getBytes());
                    }
                    
                    // the latest sequence number is 10 because
                    // (5 + 5) puts were written beforehand
                    assertEquals(numberOfPuts + numberOfPuts, db.getLatestSequenceNumber());

                    // Get updates since the beginning
                    try (final TransactionLogIterator transactionLogIterator = db.getUpdatesSince(0)) {
                        assertTrue(transactionLogIterator.isValid());
                        transactionLogIterator.status();

                        // The first sequence number is 1
                        final TransactionLogIterator.BatchResult batchResult = transactionLogIterator.getBatch();
                        assertEquals(1, batchResult.sequenceNumber());
                    }
                }
            }
        } catch (RocksDBException e) {
            System.out.println(e);
        }

    }


    @Test
    public void testUpdatesSince(@TempDirectory.TempDir Path tmpDir) throws RocksDBException {
        // a static method that loads the RocksDB C++ library.
        RocksDB.loadLibrary();


        // the Options class contains a set of configurable DB options
        // that determines the behaviour of the database.
        try (final Options options = new Options().setCreateIfMissing(true)) {

            String path = tmpDir.toFile().getAbsolutePath();
            try (final RocksDB db = RocksDB.open(options, path)) {
                db.put("hello".getBytes(), "world".getBytes());

                try (final TransactionLogIterator transactionLogIterator = db.getUpdatesSince(0)) {
                    assertTrue(transactionLogIterator.isValid());
                    transactionLogIterator.status();

                    // The first sequence number is 1
                    final TransactionLogIterator.BatchResult batchResult = transactionLogIterator.getBatch();
                    assertEquals(1, batchResult.sequenceNumber());
                }
            }

            try (final RocksDB db = RocksDB.open(options, path)) {
                assertNotNull(db.get("hello".getBytes()));
                try (final TransactionLogIterator transactionLogIterator = db.getUpdatesSince(0)) {
                    transactionLogIterator.status();

                    // TODO: determine why this for some reason this returns false.
                    assertFalse(transactionLogIterator.isValid());

                }
            }
        }
    }

    // keyspace test
    @Test
    public void testColumnFamily(@TempDirectory.TempDir Path tmpDir) {

        // a static method that loads the RocksDB C++ library.
        RocksDB.loadLibrary();

        // the Options class contains a set of configurable DB options
        // that determines the behaviour of the database.
        try (final Options options = new Options().setCreateIfMissing(true)) {

            // a factory method that returns a RocksDB instance
            try (final RocksDB db = RocksDB.open(options, tmpDir.toFile().getAbsolutePath())) {

                try (final ColumnFamilyHandle cfHandle = db.createColumnFamily(new ColumnFamilyDescriptor("secondary_index".getBytes()))) {

                    final int numberOfPuts = 5;
                    for (int i = 0; i < numberOfPuts; i++) {
//                        char c = (char)i;
                        Character c = Character.forDigit(i, 10);
                        db.put(String.valueOf(i).getBytes(), String.valueOf(i).getBytes());
                        db.put(cfHandle, String.valueOf(db.getLatestSequenceNumber()).getBytes(), String.valueOf(i).getBytes());
                    }
                    db.flush(new FlushOptions().setWaitForFlush(true));


                    ReadOptions ro = new ReadOptions();
                    RocksIterator it = db.newIterator(ro);
                    for (it.seekToFirst(); it.isValid(); it.next()) {
                        System.out.println(new String(it.key(), UTF_8) + ":" + new String(it.value(), UTF_8));
                    }

                    System.out.println("Testing column family");
                    RocksIterator sit = db.newIterator(cfHandle, ro);
                    for (sit.seekToFirst(); sit.isValid(); sit.next()) {
                        System.out.println(new String(sit.key(), UTF_8) + ":" + new String(sit.value(), UTF_8));
                    }
                }

            }
        } catch (RocksDBException e) {
            System.out.println(e);
        }

    }



    @Test
    public void prefixSeek(@TempDirectory.TempDir Path tmpDir) throws RocksDBException {

        // a static method that loads the RocksDB C++ library.
        RocksDB.loadLibrary();

        // the Options class contains a set of configurable DB options
        // that determines the behaviour of the database.
        try (final Options options = new Options().setCreateIfMissing(true)) {
            // generally we'd want to hash so that we have a fixed length for for this test the prefix is "stream-"
            options.useFixedLengthPrefixExtractor(10);

            // a factory method that returns a RocksDB instance
            try (final RocksDB db = RocksDB.open(options, tmpDir.toFile().getAbsolutePath())) {

                String streamSean = "stream-sean";
                String streamSuzanne = "stream-suzanne";

                db.put((streamSean + "2").getBytes(), "second".getBytes());
                db.put((streamSuzanne + "1").getBytes(), "first".getBytes());
                db.put((streamSuzanne + "2").getBytes(), "second".getBytes());
                db.put((streamSean + "1").getBytes(), "first".getBytes());

                db.flush(new FlushOptions().setWaitForFlush(true));

                // not sure if java version follows new usage pattern
                // https://github.com/facebook/rocksdb/wiki/Prefix-Seek-API-Changes#transition-to-the-new-usage
                // seek to last is not supported with prefix iterating
                ReadOptions ro = new ReadOptions();
                ro.setPrefixSameAsStart(true);
//                ro.setTotalOrderSeek(true);
                RocksIterator it = db.newIterator(ro);
                for (it.seek(streamSean.getBytes()); it.isValid(); it.next()) {
                    System.out.println(new String(it.key(), UTF_8) + ":" + new String(it.value(), UTF_8));
                }
            }
        }
    }


//    @Test
//    public void murmurhash364BitTest() {
//        MurmurHash3.LongPair result = new MurmurHash3.LongPair();
//        byte[] data = "stream-sean".getBytes();
//        MurmurHash3.murmurhash3_x64_128(data, 0, data.length, 0x9e3779b1, result);
//        System.out.println("val 1: " + result.val1);
//        System.out.println("val 1 bytes: " + Arrays.toString(longToBytes(result.val1)));
//        System.out.println("val 2: " + result.val2);
//        System.out.println("val 2 bytes: " + Arrays.toString(longToBytes(result.val2)));
//
//        // [-102, -114, 47, 121, 66, 87, 28, 48, -113, -110, 67, 122, 41, -92, -35, -36]
//        byte[] bytes = ByteBuffer.wrap(new byte[16])
//                        .order(ByteOrder.LITTLE_ENDIAN)
//                        .putLong(result.val1)
//                        .putLong(result.val2)
//                        .array();
//        System.out.println(Arrays.toString(bytes));
//    }

//    @Test
//    public void murmurHash128() {
//        String streamSean = "stream-sean";
//        HashCode hash = Hashing.murmur3_128().hashString(streamSean, UTF_8);
//        System.out.println("hash toString: " + hash.toString());
//        // [-32, 28, 76, -44, 20, -52, -58, -18, -5, 55, -73, 115, 94, 87, 13, 120]
//        System.out.println("hash asBytes: " + Arrays.toString(hash.asBytes()));
//        System.out.println("hash asBytes length: " + hash.asBytes().length);
//        System.out.println("hash asLong: " + hash.asLong());
//        System.out.println(Arrays.toString(longToBytes(hash.asLong())));
//        System.out.println(Arrays.toString(longToBytes(hash.padToLong())));
//        byte[] firstEight = new byte[8];
//        byte[] lastEight = new byte[8];
//        System.arraycopy(hash.asBytes(),0, firstEight,0, 8);
//        System.arraycopy(hash.asBytes(),8, lastEight, 0, 8);
////        System.out.println(hash.writeBytesTo(firstEight, 0, 8));
////        System.out.println(hash.writeBytesTo(lastEight, 8, 8));
//
//        // [-32, 28, 76, -44, 20, -52, -58, -18]
//        System.out.println(Arrays.toString(firstEight));
//        System.out.println(bytesToLong(firstEight));
//        // -5, 55, -73, 115, 94, 87, 13, 120]
//        System.out.println(Arrays.toString(lastEight));
//        System.out.println(bytesToLong(lastEight));
//    }

//    @Test
//    public void hash32bitStreamTest(@TempDirectory.TempDir Path tmpDir) throws RocksDBException {
//        // a static method that loads the RocksDB C++ library.
//        RocksDB.loadLibrary();
//
//        // the Options class contains a set of configurable DB options that determines the behaviour of the database.
//        try (final Options options = new Options().setCreateIfMissing(true)) {
//            options.useFixedLengthPrefixExtractor(4);
//
//            // a factory method that returns a RocksDB instance
//            try (final RocksDB db = RocksDB.open(options, tmpDir.toFile().getAbsolutePath())) {
//
//                // TODO: lets use 64 bit hash.  https://groups.google.com/forum/#!topic/event-store/hKkEjXB1xVE
//                // there aren't a ton of 64 bit variations of murmur3
//                // apache hive has one
//                // http://yonik.com/murmurhash3-for-java/
//                // If you need 64 bit hashes:
//                // int – use MurmurHash3.fmix64((long)val)
//                // long – use MurmurHash3.fmix64(val)
//                // float – use MurmurHash3.fmix64((long)Float.floatToRawIntBits(value))
//                // double – use MurmurHash3.fmix64(Double.doubleToRawLongBits(value))
//                // bytes – use MurmurHash3.murmurhash3_x64_128(value) and then just use one half (one long) of the 128bit result
//                // Hashing.murmur3_128().hashString("", StandardCharsets.UTF_8).
//                // http://facebook.github.io/jcommon/util/jacoco/com.facebook.util.digest/MurmurHash.java.html
//                // http://mvnrepository.com/artifact/com.facebook.jcommon
//                // https://github.com/facebook/jcommon/blob/master/util/src/main/java/com/facebook/util/digest/MurmurHash.java
//                // MurmurHash murmurHash = MurmurHash.createRepeatableHasher();
//                String streamSean = "stream-sean";
//                HashCode hash = Hashing.murmur3_32().hashString(streamSean, UTF_8);
//                System.out.println("hash toString: " + hash.toString());
//                byte[] hashbytes = hash.asBytes();
//                System.out.println("Hash bytes: " + hashbytes);
//                byte[] sequenceOne = intToBytes(1);
//                System.out.println("sequenceOne bytes: " + sequenceOne);
//
//                // cant do this because hashing will change order
//                System.out.println("Hash stream with sequenceOne " + Hashing.murmur3_32().hashString(streamSean + "1", UTF_8));
//                System.out.println("Hash stream with sequenceTwo " + Hashing.murmur3_32().hashString(streamSean + "2", UTF_8));
//
//                byte[] combinedOne = new byte[hashbytes.length + sequenceOne.length];
//                System.arraycopy(hashbytes,0, combinedOne,0, hashbytes.length);
//                System.arraycopy(sequenceOne,0, combinedOne, hashbytes.length, sequenceOne.length);
//                System.out.println("Adding hash with sequenceOne " + combinedOne);
//
//
//                byte[] sequenceTwo = intToBytes(2);
//                System.out.println("sequenceTwo bytes: " + sequenceTwo);
//
//                byte[] combinedTwo = new byte[hashbytes.length + sequenceTwo.length];
//                System.arraycopy(hashbytes,0,combinedTwo,0,hashbytes.length);
//                System.arraycopy(sequenceTwo,0,combinedTwo,hashbytes.length,sequenceTwo.length);
//                System.out.println("Adding hash with sequenceTwo " + combinedTwo);
//
//                db.put(combinedOne, "first".getBytes());
//                db.put(combinedTwo, "second".getBytes());
//
//                db.flush(new FlushOptions().setWaitForFlush(true));
//
//                // not sure if java version follows new usage pattern
//                // https://github.com/facebook/rocksdb/wiki/Prefix-Seek-API-Changes#transition-to-the-new-usage
//                // seek to last is not supported with prefix iterating
//                ReadOptions ro = new ReadOptions();
//                ro.setPrefixSameAsStart(true);
////                ro.setTotalOrderSeek(true);
//                RocksIterator it = db.newIterator(ro);
//                for (it.seek(hashbytes); it.isValid(); it.next()) {
//                    byte[] key = it.key();
//
//                    //System.out.println(new String(it.key(), 0, hashbytes.length, UTF_8));
//                    System.out.println(bytesToInt(it.key(), hashbytes.length, sequenceTwo.length));
//                    System.out.println(new String(it.value(), UTF_8));
//                }
//            }
//        }
//    }

    // TODO: fix
//    @Test
//    public void hash64bitStreamTest(@TempDirectory.TempDir Path tmpDir) throws RocksDBException {
//        // a static method that loads the RocksDB C++ library.
//        RocksDB.loadLibrary();
//
//        // the Options class contains a set of configurable DB options that determines the behaviour of the database.
//        try (final Options options = new Options().setCreateIfMissing(true)) {
//            options.useFixedLengthPrefixExtractor(8);
//
//            // a factory method that returns a RocksDB instance
//            try (final RocksDB db = RocksDB.open(options, tmpDir.toFile().getAbsolutePath())) {
//
//                // TODO: lets use 64 bit hash.  https://groups.google.com/forum/#!topic/event-store/hKkEjXB1xVE
//                // there aren't a ton of 64 bit variations of murmur3
//                // apache hive has one
//                // http://yonik.com/murmurhash3-for-java/
//                // If you need 64 bit hashes:
//                // int – use MurmurHash3.fmix64((long)val)
//                // long – use MurmurHash3.fmix64(val)
//                // float – use MurmurHash3.fmix64((long)Float.floatToRawIntBits(value))
//                // double – use MurmurHash3.fmix64(Double.doubleToRawLongBits(value))
//                // bytes – use MurmurHash3.murmurhash3_x64_128(value) and then just use one half (one long) of the 128bit result
//                // Hashing.murmur3_128().hashString("", StandardCharsets.UTF_8).
//                // http://facebook.github.io/jcommon/util/jacoco/com.facebook.util.digest/MurmurHash.java.html
//                // http://mvnrepository.com/artifact/com.facebook.jcommon
//                // https://github.com/facebook/jcommon/blob/master/util/src/main/java/com/facebook/util/digest/MurmurHash.java
//                // MurmurHash murmurHash = MurmurHash.createRepeatableHasher();
//                String streamSean = "stream-sean";
//                HashCode hash = Hashing.murmur3_32().hashString(streamSean, UTF_8);
//                System.out.println("hash toString: " + hash.toString());
//                byte[] hashbytes = hash.asBytes();
//                System.out.println("Hash bytes: " + hashbytes);
//                byte[] sequenceOne = intToBytes(1);
//                System.out.println("sequenceOne bytes: " + sequenceOne);
//
//                // cant do this because hashing will change order
//                System.out.println("Hash stream with sequenceOne " + Hashing.murmur3_32().hashString(streamSean + "1", UTF_8));
//                System.out.println("Hash stream with sequenceTwo " + Hashing.murmur3_32().hashString(streamSean + "2", UTF_8));
//
//                byte[] combinedOne = new byte[hashbytes.length + sequenceOne.length];
//                System.arraycopy(hashbytes,0, combinedOne,0, hashbytes.length);
//                System.arraycopy(sequenceOne,0, combinedOne, hashbytes.length, sequenceOne.length);
//                System.out.println("Adding hash with sequenceOne " + combinedOne);
//
//
//                byte[] sequenceTwo = intToBytes(2);
//                System.out.println("sequenceTwo bytes: " + sequenceTwo);
//
//                byte[] combinedTwo = new byte[hashbytes.length + sequenceTwo.length];
//                System.arraycopy(hashbytes,0,combinedTwo,0,hashbytes.length);
//                System.arraycopy(sequenceTwo,0,combinedTwo,hashbytes.length,sequenceTwo.length);
//                System.out.println("Adding hash with sequenceTwo " + combinedTwo);
//
//                db.put(combinedOne, "first".getBytes());
//                db.put(combinedTwo, "second".getBytes());
//
//                db.flush(new FlushOptions().setWaitForFlush(true));
//
//                // not sure if java version follows new usage pattern
//                // https://github.com/facebook/rocksdb/wiki/Prefix-Seek-API-Changes#transition-to-the-new-usage
//                // seek to last is not supported with prefix iterating
//                ReadOptions ro = new ReadOptions();
//                ro.setPrefixSameAsStart(true);
////                ro.setTotalOrderSeek(true);
//                RocksIterator it = db.newIterator(ro);
//                for (it.seek(hashbytes); it.isValid(); it.next()) {
//                    byte[] key = it.key();
//
//                    //System.out.println(new String(it.key(), 0, hashbytes.length, UTF_8));
//                    System.out.println(bytesToInt(it.key(), hashbytes.length, sequenceTwo.length));
//                    System.out.println(new String(it.value(), UTF_8));
//                }
//            }
//        }
//    }

    @Test
    public void hash128StreamTest(@TempDirectory.TempDir Path tmpDir) throws RocksDBException, UnsupportedEncodingException {
        // a static method that loads the RocksDB C++ library.
        RocksDB.loadLibrary();

        // the Options class contains a set of configurable DB options that determines the behaviour of the database.
        try (final Options options = new Options().setCreateIfMissing(true)) {
            options.useFixedLengthPrefixExtractor(16);

            // a factory method that returns a RocksDB instance
            try (final RocksDB db = RocksDB.open(options, tmpDir.toFile().getAbsolutePath())) {

                String streamSean = "stream-sean";
                HashCode hash = Hashing.murmur3_128().hashString(streamSean, UTF_8);
                System.out.println("hash toString: " + hash.toString());

                // TODO: why doesn't this work with hash.asBytes()???
                byte[] hashbytes = hash.asBytes();
                System.out.println("Hash bytes: " + hashbytes);

                System.out.println(HashCode.fromBytes(hashbytes).toString());
                System.out.println(base16().lowerCase().encode(hashbytes));

                byte[] hashbytes2 = hash.toString().getBytes();
                System.out.println("Hash bytes2: " + hashbytes2);
                byte[] sequenceOne = intToBytes(1);
                System.out.println("sequenceOne bytes: " + sequenceOne);

                // TODO: not sure I need to do all this copy stuff unless I _really_ think that being able to grab
                // the sequence number via splitting the byte array is work it
                byte[] combinedOne = new byte[hashbytes2.length + sequenceOne.length];
                System.arraycopy(hashbytes2,0, combinedOne,0, hashbytes2.length);
                System.arraycopy(sequenceOne,0, combinedOne, hashbytes2.length, sequenceOne.length);
                System.out.println("Adding hash with sequenceOne " + combinedOne);

                byte[] sequenceTwo = intToBytes(2);
                System.out.println("sequenceTwo bytes: " + sequenceTwo);

                byte[] combinedTwo = new byte[hashbytes2.length + sequenceTwo.length];
                System.arraycopy(hashbytes2,0,combinedTwo,0,hashbytes2.length);
                System.arraycopy(sequenceTwo,0,combinedTwo,hashbytes2.length,sequenceTwo.length);
                System.out.println("Adding hash with sequenceTwo " + combinedTwo);

                db.put(combinedOne, "first".getBytes());
                db.put(combinedTwo, "second".getBytes());

                db.flush(new FlushOptions().setWaitForFlush(true));

                // not sure if java version follows new usage pattern
                // https://github.com/facebook/rocksdb/wiki/Prefix-Seek-API-Changes#transition-to-the-new-usage
                // seek to last is not supported with prefix iterating
                ReadOptions ro = new ReadOptions();
                ro.setPrefixSameAsStart(true);
//                ro.setTotalOrderSeek(true);
                RocksIterator it = db.newIterator(ro);
                for (it.seek(hashbytes2); it.isValid(); it.next()) {
                    System.out.println(bytesToInt(it.key(), hashbytes2.length, sequenceTwo.length));
                    System.out.println(new String(it.value(), UTF_8));
                }

                ReadOptions ro2 = new ReadOptions();
                ro2.setTotalOrderSeek(true);
                RocksIterator it2 = db.newIterator(ro2);
                for (it2.seekToFirst(); it2.isValid(); it2.next()) {
                    // System.out.println(new String(it2.key(), "UTF-8"));
                    // System.out.println("hashbyte length: " + hashbytes2.length);
                    // System.out.println("key length: " + it2.key().length);

                    // ByteBuffer.wrap doesn't appear to work as I would expect. Getting back full array instead of
                    // one with the offset and length
                    // System.out.println(new String(ByteBuffer.wrap(it2.key(), 0, hashbytes2.length).array(), "UTF-8"));
                    // https://stackoverflow.com/questions/35032252/bytebuffer-wrap-came-out-unexpectedly
                    // need to do the following

                    System.out.println(new String(it2.key(), 0, hashbytes2.length));
                    System.out.println(bytesToInt(Arrays.copyOfRange(it2.key(), hashbytes2.length, it2.key().length)));

                    //System.out.println(new String(Arrays.copyOfRange(it2.key(), 0, hashbytes2.length), "UTF-8"));
                    //System.out.println(bytesToInt(it2.key(), hashbytes.length, sequenceTwo.length));
                    System.out.println(new String(it2.value(), UTF_8));
                }

            }
        }
    }



    @Test
    public void hashTestWithoutArrayCombineCopy(@TempDirectory.TempDir Path tmpDir) throws RocksDBException, UnsupportedEncodingException {
        // a static method that loads the RocksDB C++ library.
        RocksDB.loadLibrary();

        // the Options class contains a set of configurable DB options that determines the behaviour of the database.
        try (final Options options = new Options().setCreateIfMissing(true)) {
            options.useFixedLengthPrefixExtractor(16);

            // a factory method that returns a RocksDB instance
            try (final RocksDB db = RocksDB.open(options, tmpDir.toFile().getAbsolutePath())) {

                String streamSean = "stream-sean";
                HashCode hash = Hashing.murmur3_128().hashString(streamSean, UTF_8);
                System.out.println("hash toString: " + hash.toString());
                System.out.println("hash has bits " + hash.bits());
                System.out.println("number of bytes in hash byte array: " + hash.asBytes().length);
                // TODO: probably shouldnt use the paramerless getBytes and instead use UTF_8
                System.out.println("hash byte array " + Arrays.toString(hash.toString().getBytes()));
                System.out.println("hash asBytes array " + Arrays.toString(hash.asBytes()));

                //System.out.println("murmur32 hash byte array" + Arrays.toString(Hashing.murmur3_32().hashString(streamSean, UTF_8).toString().getBytes()));

//                System.out.println(Arrays.toString("@".getBytes()));
//                System.out.println(Arrays.toString("1".getBytes()));
//                System.out.println(Arrays.toString("@1".getBytes()));
//                System.out.println(Arrays.toString(String.valueOf(Integer.MAX_VALUE).getBytes()));
//                System.out.println(Arrays.toString(("@"+Integer.MAX_VALUE).getBytes()));

                db.put((hash.toString() + "@1").getBytes(), "first".getBytes());
                db.put((hash.toString() + "@2").getBytes(), "second".getBytes());

                db.flush(new FlushOptions().setWaitForFlush(true));

                ReadOptions ro2 = new ReadOptions();
                ro2.setTotalOrderSeek(true);
                RocksIterator it2 = db.newIterator(ro2);
                for (it2.seekToFirst(); it2.isValid(); it2.next()) {
                    System.out.println("number of bytes in key byte array: " + it2.key().length);
                    System.out.println(new String(it2.key(), "UTF-8"));
                    System.out.println(new String(Arrays.copyOfRange(it2.key(), 0, 32)));
                    // could we instead use bytebuffer? then we can use get method?
                    // 128 bits = 16 bytes. java char size is 2 bytes plus 1 byte for "@"
                    System.out.println(new String(Arrays.copyOfRange(it2.key(), 32 + 1, it2.key().length)));
                    System.out.println(new String(it2.value(), UTF_8));
                }

            }
        }
    }


    // TODO: total order vs prefix


    // Event Store supports a special type of event called Link Events.
    // These events can be thought of as pointers to an event in another stream.
    // SubscriptionsService ResolveLinkToEvent
    // var res = _readIndex.ReadEvent(streamId, eventNumber);
    // EmittedLinkToWithRecategorization
    @Test
    public void categoryProjectionsTest() throws RocksDBException {

        // a static method that loads the RocksDB C++ library.
        RocksDB.loadLibrary();

        // the Options class contains a set of configurable DB options
        // that determines the behaviour of the database.
        try (final Options options = new Options().setCreateIfMissing(true)) {

        }
    }

    @Test
    public void eventTypeProjectionsTest(@TempDirectory.TempDir Path tmpDir) throws RocksDBException {

        // a static method that loads the RocksDB C++ library.
        RocksDB.loadLibrary();

        // the Options class contains a set of configurable DB options
        // that determines the behaviour of the database.
        try (final Options options = new Options().setCreateIfMissing(true)) {
            options.useFixedLengthPrefixExtractor(4);

            // a factory method that returns a RocksDB instance
            try (final RocksDB db = RocksDB.open(options, tmpDir.toFile().getAbsolutePath())) {



            }
        }
    }

    @Test
    public void correlationIdProjectionTest(@TempDirectory.TempDir Path tmpDir) throws RocksDBException {

        // a static method that loads the RocksDB C++ library.
        RocksDB.loadLibrary();

        // the Options class contains a set of configurable DB options
        // that determines the behaviour of the database.
        try (final Options options = new Options().setCreateIfMissing(true)) {
            options.useFixedLengthPrefixExtractor(4);

            // a factory method that returns a RocksDB instance
            try (final RocksDB db = RocksDB.open(options, tmpDir.toFile().getAbsolutePath())) {
                options.useFixedLengthPrefixExtractor(4);

                db.put("hello".getBytes(), "world".getBytes());
                db.put("hello".getBytes(), "sean".getBytes());

                try (final TransactionLogIterator transactionLogIterator = db.getUpdatesSince(0)) {
                    assertTrue(transactionLogIterator.isValid());
                    transactionLogIterator.status();

                    // The first sequence number is 1
                    final TransactionLogIterator.BatchResult batchResult = transactionLogIterator.getBatch();
                    assertEquals(1, batchResult.sequenceNumber());

                    byte[] value = db.get("hello".getBytes());
                }

            }
        }
    }


    public static byte[] intToBytes(final int i) {
        return ByteBuffer.allocate(4).putInt(i).array();
    }

    public static int bytesToInt(final byte[] b) {
        return ByteBuffer.wrap(b).getInt();
    }

    public static int bytesToInt(final byte[] b, int offset, int length) {
        return ByteBuffer.wrap(b, offset, length).getInt();
    }

    public static byte[] longToBytes(final long l) {
        return ByteBuffer.allocate(Long.BYTES).putLong(l).array();
    }

    public long bytesToLong(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.put(bytes);
        buffer.flip();//need flip
        return buffer.getLong();
    }


    @Test
    public void sample(@TempDirectory.TempDir Path tmpDir) {
        // a static method that loads the RocksDB C++ library.
        RocksDB.loadLibrary();

        final String db_path = tmpDir.toFile().getAbsolutePath();
        final String db_path_not_found = db_path + "_not_found";

        System.out.println("RocksDBSample");
        try (final Options options = new Options();
             final Filter bloomFilter = new BloomFilter(10);
             final ReadOptions readOptions = new ReadOptions().setFillCache(false);
             final Statistics stats = new Statistics();
             final RateLimiter rateLimiter = new RateLimiter(10000000,10000, 10)) {

            try (final RocksDB db = RocksDB.open(options, db_path_not_found)) {
                assert (false);
            } catch (final RocksDBException e) {
                System.out.format("Caught the expected exception -- %s\n", e);
            }

            try {
                options.setCreateIfMissing(true)
                        .setStatistics(stats)
                        .setWriteBufferSize(8 * SizeUnit.KB)
                        .setMaxWriteBufferNumber(3)
                        .setMaxBackgroundCompactions(10)
                        .setCompressionType(CompressionType.SNAPPY_COMPRESSION)
                        .setCompactionStyle(CompactionStyle.UNIVERSAL);
            } catch (final IllegalArgumentException e) {
                assert (false);
            }

            assert (options.createIfMissing() == true);
            assert (options.writeBufferSize() == 8 * SizeUnit.KB);
            assert (options.maxWriteBufferNumber() == 3);
            assert (options.maxBackgroundCompactions() == 10);
            assert (options.compressionType() == CompressionType.SNAPPY_COMPRESSION);
            assert (options.compactionStyle() == CompactionStyle.UNIVERSAL);

            assert (options.memTableFactoryName().equals("SkipListFactory"));
            options.setMemTableConfig(
                    new HashSkipListMemTableConfig()
                            .setHeight(4)
                            .setBranchingFactor(4)
                            .setBucketCount(2000000));
            assert (options.memTableFactoryName().equals("HashSkipListRepFactory"));

            options.setMemTableConfig(
                    new HashLinkedListMemTableConfig()
                            .setBucketCount(100000));
            assert (options.memTableFactoryName().equals("HashLinkedListRepFactory"));

            options.setMemTableConfig(
                    new VectorMemTableConfig().setReservedSize(10000));
            assert (options.memTableFactoryName().equals("VectorRepFactory"));

            options.setMemTableConfig(new SkipListMemTableConfig());
            assert (options.memTableFactoryName().equals("SkipListFactory"));

            options.setTableFormatConfig(new PlainTableConfig());
            // Plain-Table requires mmap read
            options.setAllowMmapReads(true);
            assert (options.tableFactoryName().equals("PlainTable"));

            options.setRateLimiter(rateLimiter);

            final BlockBasedTableConfig table_options = new BlockBasedTableConfig();
            table_options.setBlockCacheSize(64 * SizeUnit.KB)
                    .setFilter(bloomFilter)
                    .setCacheNumShardBits(6)
                    .setBlockSizeDeviation(5)
                    .setBlockRestartInterval(10)
                    .setCacheIndexAndFilterBlocks(true)
                    .setHashIndexAllowCollision(false)
                    .setBlockCacheCompressedSize(64 * SizeUnit.KB)
                    .setBlockCacheCompressedNumShardBits(10);

            assert (table_options.blockCacheSize() == 64 * SizeUnit.KB);
            assert (table_options.cacheNumShardBits() == 6);
            assert (table_options.blockSizeDeviation() == 5);
            assert (table_options.blockRestartInterval() == 10);
            assert (table_options.cacheIndexAndFilterBlocks() == true);
            assert (table_options.hashIndexAllowCollision() == false);
            assert (table_options.blockCacheCompressedSize() == 64 * SizeUnit.KB);
            assert (table_options.blockCacheCompressedNumShardBits() == 10);

            options.setTableFormatConfig(table_options);
            assert (options.tableFactoryName().equals("BlockBasedTable"));

            try (final RocksDB db = RocksDB.open(options, db_path)) {
                db.put("hello".getBytes(), "world".getBytes());

                final byte[] value = db.get("hello".getBytes());
                assert ("world".equals(new String(value)));

                final String str = db.getProperty("rocksdb.stats");
                System.out.println(str);
                // options.statistics().
                assert (str != null && !str.equals(""));
            } catch (final RocksDBException e) {
                System.out.format("[ERROR] caught the unexpected exception -- %s\n", e);
                assert (false);
            }

            try (final RocksDB db = RocksDB.open(options, db_path)) {
                db.put("hello".getBytes(), "world".getBytes());
                byte[] value = db.get("hello".getBytes());
                System.out.format("Get('hello') = %s\n", new String(value));

                for (int i = 1; i <= 9; ++i) {
                    for (int j = 1; j <= 9; ++j) {
                        db.put(String.format("%dx%d", i, j).getBytes(), String.format("%d", i * j).getBytes());
                    }
                }

                for (int i = 1; i <= 9; ++i) {
                    for (int j = 1; j <= 9; ++j) {
                        System.out.format("%s ", new String(db.get(String.format("%dx%d", i, j).getBytes())));
                    }
                    System.out.println("");
                }

                // write batch test
                try (final WriteOptions writeOpt = new WriteOptions()) {
                    for (int i = 10; i <= 19; ++i) {
                        try (final WriteBatch batch = new WriteBatch()) {
                            for (int j = 10; j <= 19; ++j) {
                                batch.put(String.format("%dx%d", i, j).getBytes(), String.format("%d", i * j).getBytes());
                            }
                            db.write(writeOpt, batch);
                        }
                    }
                }
                for (int i = 10; i <= 19; ++i) {
                    for (int j = 10; j <= 19; ++j) {
                        assert (new String(
                                db.get(String.format("%dx%d", i, j).getBytes())).equals(
                                String.format("%d", i * j)));
                        System.out.format("%s ", new String(db.get(
                                String.format("%dx%d", i, j).getBytes())));
                    }
                    System.out.println("");
                }

                value = db.get("1x1".getBytes());
                assert (value != null);
                value = db.get("world".getBytes());
                assert (value == null);
                value = db.get(readOptions, "world".getBytes());
                assert (value == null);

                final byte[] testKey = "asdf".getBytes();
                final byte[] testValue =
                        "asdfghjkl;'?><MNBVCXZQWERTYUIOP{+_)(*&^%$#@".getBytes();
                db.put(testKey, testValue);
                byte[] testResult = db.get(testKey);
                assert (testResult != null);
                assert (Arrays.equals(testValue, testResult));
                assert (new String(testValue).equals(new String(testResult)));
                testResult = db.get(readOptions, testKey);
                assert (testResult != null);
                assert (Arrays.equals(testValue, testResult));
                assert (new String(testValue).equals(new String(testResult)));

                final byte[] insufficientArray = new byte[10];
                final byte[] enoughArray = new byte[50];
                int len;
                len = db.get(testKey, insufficientArray);
                assert (len > insufficientArray.length);
                len = db.get("asdfjkl;".getBytes(), enoughArray);
                assert (len == RocksDB.NOT_FOUND);
                len = db.get(testKey, enoughArray);
                assert (len == testValue.length);

                len = db.get(readOptions, testKey, insufficientArray);
                assert (len > insufficientArray.length);
                len = db.get(readOptions, "asdfjkl;".getBytes(), enoughArray);
                assert (len == RocksDB.NOT_FOUND);
                len = db.get(readOptions, testKey, enoughArray);
                assert (len == testValue.length);

                db.remove(testKey);
                len = db.get(testKey, enoughArray);
                assert (len == RocksDB.NOT_FOUND);

                // repeat the test with WriteOptions
                try (final WriteOptions writeOpts = new WriteOptions()) {
                    writeOpts.setSync(true);
                    writeOpts.setDisableWAL(true);
                    db.put(writeOpts, testKey, testValue);
                    len = db.get(testKey, enoughArray);
                    assert (len == testValue.length);
                    assert (new String(testValue).equals(
                            new String(enoughArray, 0, len)));
                }

                try {
                    for (final TickerType statsType : TickerType.values()) {
                        if (statsType != TickerType.TICKER_ENUM_MAX) {
                            stats.getTickerCount(statsType);
                        }
                    }
                    System.out.println("getTickerCount() passed.");
                } catch (final Exception e) {
                    System.out.println("Failed in call to getTickerCount()");
                    assert (false); //Should never reach here.
                }

                try {
                    for (final HistogramType histogramType : HistogramType.values()) {
                        if (histogramType != HistogramType.HISTOGRAM_ENUM_MAX) {
                            HistogramData data = stats.getHistogramData(histogramType);
                        }
                    }
                    System.out.println("getHistogramData() passed.");
                } catch (final Exception e) {
                    System.out.println("Failed in call to getHistogramData()");
                    assert (false); //Should never reach here.
                }

                try (final RocksIterator iterator = db.newIterator()) {

                    boolean seekToFirstPassed = false;
                    for (iterator.seekToFirst(); iterator.isValid(); iterator.next()) {
                        iterator.status();
                        assert (iterator.key() != null);
                        assert (iterator.value() != null);
                        seekToFirstPassed = true;
                    }
                    if (seekToFirstPassed) {
                        System.out.println("iterator seekToFirst tests passed.");
                    }

                    boolean seekToLastPassed = false;
                    for (iterator.seekToLast(); iterator.isValid(); iterator.prev()) {
                        iterator.status();
                        assert (iterator.key() != null);
                        assert (iterator.value() != null);
                        seekToLastPassed = true;
                    }

                    if (seekToLastPassed) {
                        System.out.println("iterator seekToLastPassed tests passed.");
                    }

                    iterator.seekToFirst();
                    iterator.seek(iterator.key());
                    assert (iterator.key() != null);
                    assert (iterator.value() != null);

                    System.out.println("iterator seek test passed.");

                }
                System.out.println("iterator tests passed.");

                final List<byte[]> keys = new ArrayList<>();
                try (final RocksIterator iterator = db.newIterator()) {
                    for (iterator.seekToLast(); iterator.isValid(); iterator.prev()) {
                        keys.add(iterator.key());
                    }
                }

                Map<byte[], byte[]> values = db.multiGet(keys);
                assert (values.size() == keys.size());
                for (final byte[] value1 : values.values()) {
                    assert (value1 != null);
                }

                values = db.multiGet(new ReadOptions(), keys);
                assert (values.size() == keys.size());
                for (final byte[] value1 : values.values()) {
                    assert (value1 != null);
                }
            } catch (final RocksDBException e) {
                System.err.println(e);
            }
        }

    }
}