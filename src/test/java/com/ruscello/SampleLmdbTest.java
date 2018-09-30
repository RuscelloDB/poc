package com.ruscello;

import com.google.common.base.Stopwatch;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junitpioneer.jupiter.TempDirectory;
import org.lmdbjava.*;
import org.lmdbjava.CursorIterator.KeyVal;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.nio.ByteBuffer.allocateDirect;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.lmdbjava.DbiFlags.MDB_CREATE;
import static org.lmdbjava.DbiFlags.MDB_DUPSORT;
import static org.lmdbjava.DirectBufferProxy.PROXY_DB;
import static org.lmdbjava.Env.create;
import static org.lmdbjava.Env.open;
import static org.lmdbjava.SeekOp.MDB_FIRST;
import static org.lmdbjava.SeekOp.MDB_LAST;

//import sun.nio.ch.DirectBuffer;


// https://hackerfall.com/story/atlasdb-transactions-for-distributed-key-value-sto
// https://db.cs.cmu.edu/seminar2015/

// Instead of calling mdb_dbi_open each time, keep your own map with database names to database handles returned from mdb_dbi_open.
// Reuse these handles for the lifetime of your program.
// This will allow you to have multiple databases within an environment and prevent the overhead with mdb_dbi_open.

// https://media.readthedocs.org/pdf/lmdb/stable/lmdb.pdf

// https://www.openldap.org/lists/openldap-technical/201504/msg00195.html
// https://symas.com/getting-down-and-dirty-with-lmdb-qa-with-symas-corporations-howard-chu-about-symass-lightning-memory-mapped-database/
// https://www.openldap.org/doc/admin24/backends.html
// https://www.openldap.org/faq/data/cache/1166.html
// https://www.openldap.org/faq/data/cache/756.html
// https://www.google.com/search?q=back-hdb+backends&oq=back-hdb+backends&aqs=chrome..69i57.359j0j4&sourceid=chrome&ie=UTF-8
// http://www.openldap.org/conf/odd-sfo-2003/proceedings.html
// https://www.openldap.org/lists/openldap-technical/201303/msg00145.html
@ExtendWith(TempDirectory.class)
public class SampleLmdbTest {

    private static final String DB_NAME = "my DB";

    // @Rule
    // public final TemporaryFolder tmp = new TemporaryFolder();

    // no such thing as column families in lmdb but transactions can span databases
    // and is common for secondary indexes
    // so I think we use databases as key spaces. We could try to pack multiple key spaces in on db
    // https://docs.oracle.com/cd/E17275_01/html/programmer_reference/am_second.html
    // https://docs.oracle.com/cd/E17275_01/html/programmer_reference/second.javas

    // Python lib
    // https://lmdb.readthedocs.io/en/release/
    // Multiple named databases may be created with transactions covering all named databases.

    // http://www.lmdb.tech/doc/
    // Note: a single transaction can open multiple databases. Generally databases should only be opened once, by the first transaction in the process.
    // After the first transaction completes, the database handles can freely be used by all subsequent transactions.

    @Test
    public void test(@TempDirectory.TempDir Path tmpDir) throws IOException {

        // We need a storage directory first.
        // The path cannot be on a remote file system.
        // final File path = tmp.newFolder();


        // We always need an Env. An Env owns a physical on-disk storage file. One
        // Env can store many different databases (ie sorted maps).
        final Env<ByteBuffer> env = create()
                // LMDB also needs to know how large our DB might be. Over-estimating is OK.
                .setMapSize(10_485_760)
                // LMDB also needs to know how many DBs (Dbi) we want to store in this Env.
                .setMaxDbs(1)
                // Now let's open the Env. The same path can be concurrently opened and
                // used in different processes, but do not open the same path twice in
                // the same process at the same time.
                .open(tmpDir.toFile());

        // We need a Dbi for each DB. A Dbi roughly equates to a sorted map. The
        // MDB_CREATE flag causes the DB to be created if it doesn't already exist.
        final Dbi<ByteBuffer> db = env.openDbi(DB_NAME, MDB_CREATE);
        System.out.println("Last Transaction id:" + env.info().lastTransactionId);

        // We want to store some data, so we will need a direct ByteBuffer.
        // Note that LMDB keys cannot exceed maxKeySize bytes (511 bytes by default).
        // Values can be larger.
        final ByteBuffer key = allocateDirect(env.getMaxKeySize());
        final ByteBuffer val = allocateDirect(700);
        key.put("greeting".getBytes(UTF_8)).flip();
        val.put("Hello world".getBytes(UTF_8)).flip();
        final int valSize = val.remaining();

        // Now store it. Dbi.put() internally begins and commits a transaction (Txn).
        db.put(key, val);
        System.out.println("Last Transaction id:" + env.info().lastTransactionId);

        final ByteBuffer key2 = allocateDirect(env.getMaxKeySize());
        final ByteBuffer val2 = allocateDirect(700);
        key2.put("abc".getBytes(UTF_8)).flip();
        val2.put("abc".getBytes(UTF_8)).flip();
        final int val2Size = val2.remaining();


        // Now store it. Dbi.put() internally begins and commits a transaction (Txn).
        db.put(key2, val2);

        // this looks to be similar to rocksdb sequence and I think we can use it
        // for our totally ordered key space db
        System.out.println("Last Transaction id:" + env.info().lastTransactionId);


        // To fetch any data from LMDB we need a Txn. A Txn is very important in
        // LmdbJava because it offers ACID characteristics and internally holds a
        // read-only key buffer and read-only value buffer. These read-only buffers
        // are always the same two Java objects, but point to different LMDB-managed
        // memory as we use Dbi (and Cursor) methods. These read-only buffers remain
        // valid only until the Txn is released or the next Dbi or Cursor call. If
        // you need data afterwards, you should copy the bytes to your own buffer.
        try (Txn<ByteBuffer> txn = env.txnRead()) {

            final ByteBuffer found = db.get(txn, key);
            assertNotNull(found);

            // The fetchedVal is read-only and points to LMDB memory
            final ByteBuffer fetchedVal = txn.val();
            assertThat(fetchedVal.remaining(), is(valSize));

            // Let's double-check the fetched value is correct
            assertThat(UTF_8.decode(fetchedVal).toString(), is("Hello world"));


            CursorIterator<ByteBuffer> c = db.iterate(txn);
            for (final CursorIterator.KeyVal<ByteBuffer> kv : c.iterable()) {
                System.out.println(UTF_8.decode(kv.key()).toString() + ":" + UTF_8.decode(kv.val()).toString());
            }

            CursorIterator<ByteBuffer> call = db.iterate(txn,KeyRange.all());
            for (final CursorIterator.KeyVal<ByteBuffer> kv : call.iterable()) {
                System.out.println(UTF_8.decode(kv.key()).toString() + ":" + UTF_8.decode(kv.val()).toString());
            }

            System.out.println("--- Range Search ---");
            ByteBuffer start = allocateDirect(env.getMaxKeySize());
            start.put("a".getBytes(UTF_8)); //.flip();

            ByteBuffer end = allocateDirect(env.getMaxKeySize());
            end.put("d".getBytes(UTF_8));

            CursorIterator<ByteBuffer> ckr = db.iterate(txn,KeyRange.open(start, end));
            for (final CursorIterator.KeyVal<ByteBuffer> kv : ckr.iterable()) {
                System.out.println(UTF_8.decode(kv.key()).toString() + ":" + UTF_8.decode(kv.val()).toString());
            }
        }

        // We can also delete. The simplest way is to let Dbi allocate a new Txn...
        db.delete(key);
        db.delete(key2);

        // Now if we try to fetch the deleted row, it won't be present
        try (Txn<ByteBuffer> txn = env.txnRead()) {
            assertNull(db.get(txn, key));
        }

    }

    @Test
    public void hash128StreamTest(@TempDirectory.TempDir Path tmpDir) throws IOException {
        // We need a storage directory first.
        // The path cannot be on a remote file system.
        //final File path = tmp.newFolder();

        // We always need an Env. An Env owns a physical on-disk storage file. One
        // Env can store many different databases (ie sorted maps).
        final Env<ByteBuffer> env = create()
                // LMDB also needs to know how large our DB might be. Over-estimating is OK.
                .setMapSize(10_485_760)
                // LMDB also needs to know how many DBs (Dbi) we want to store in this Env.
                .setMaxDbs(1)
                // Now let's open the Env. The same path can be concurrently opened and
                // used in different processes, but do not open the same path twice in
                // the same process at the same time.
                .open(tmpDir.toFile());

        // We need a Dbi for each DB. A Dbi roughly equates to a sorted map. The
        // MDB_CREATE flag causes the DB to be created if it doesn't already exist.
        final Dbi<ByteBuffer> db = env.openDbi(DB_NAME, MDB_CREATE);

        String streamSean = "stream-sean";
        HashCode hash = Hashing.murmur3_128().hashString(streamSean, UTF_8);
        System.out.println("hash toString: " + hash.toString());
        System.out.println("hash has bits " + hash.bits());
        System.out.println("number of bytes in hash byte array: " + hash.asBytes().length);
        // TODO: probably shouldnt use the paramerless getBytes and instead use UTF_8
        System.out.println("hash byte array " + Arrays.toString(hash.toString().getBytes()));
        System.out.println("hash asBytes array " + Arrays.toString(hash.asBytes()));

        // We want to store some data, so we will need a direct ByteBuffer.
        // Note that LMDB keys cannot exceed maxKeySize bytes (511 bytes by default).
        // Values can be larger.
        final ByteBuffer key = allocateDirect(env.getMaxKeySize());
        final ByteBuffer val = allocateDirect(700);
        key.put((hash.toString() + "@1").getBytes()).flip();
        val.put("first".getBytes(UTF_8)).flip();
        final int valSize = val.remaining();

        db.put(key, val);


        final ByteBuffer key2 = allocateDirect(env.getMaxKeySize());
        final ByteBuffer val2 = allocateDirect(700);
        key2.put((hash.toString() + "@2").getBytes()).flip();
        val2.put("second".getBytes(UTF_8)).flip();
        final int val2Size = val2.remaining();

        // Now store it. Dbi.put() internally begins and commits a transaction (Txn).
        db.put(key2, val2);


        // To fetch any data from LMDB we need a Txn. A Txn is very important in
        // LmdbJava because it offers ACID characteristics and internally holds a
        // read-only key buffer and read-only value buffer. These read-only buffers
        // are always the same two Java objects, but point to different LMDB-managed
        // memory as we use Dbi (and Cursor) methods. These read-only buffers remain
        // valid only until the Txn is released or the next Dbi or Cursor call. If
        // you need data afterwards, you should copy the bytes to your own buffer.
        try (Txn<ByteBuffer> txn = env.txnRead()) {

            CursorIterator<ByteBuffer> c = db.iterate(txn);
            // same as CursorIterator<ByteBuffer> call = db.iterate(txn,KeyRange.all());
            for (final CursorIterator.KeyVal<ByteBuffer> kv : c.iterable()) {
                // TODO: should have a helper method that checks hasArray returning array() if true otherwise below
                byte[] keyBytes = new byte[kv.key().remaining()];
                kv.key().get(keyBytes);
                //System.out.println(bytesToInt(arr, hashbytes.length, sequenceTwo.length) + ":" + UTF_8.decode(kv.val()).toString());


                System.out.println("number of bytes in key byte array: " + keyBytes.length);
                System.out.println(new String(keyBytes, "UTF-8"));
                //System.out.println(kv.key());
                System.out.println(new String(Arrays.copyOfRange(keyBytes, 0, 32)));
                // could we instead use bytebuffer? then we can use get method?
                // 128 bits = 16 bytes. java char size is 2 bytes plus 1 byte for "@"
                System.out.println(new String(Arrays.copyOfRange(keyBytes, 32 + 1, keyBytes.length)));

                byte[] valBytes = new byte[kv.val().remaining()];
                kv.val().get(valBytes);
                System.out.println(new String(valBytes, UTF_8));
                //System.out.println(kv.val());
            }
        }

    }

    // search keys need to be the at same or greater size
    @Test
    public void prefixSeek(@TempDirectory.TempDir Path tmpDir) throws IOException {
        // We need a storage directory first.
        // The path cannot be on a remote file system.
        //final File path = tmp.newFolder();

        // We always need an Env. An Env owns a physical on-disk storage file. One
        // Env can store many different databases (ie sorted maps).
        final Env<ByteBuffer> env = create()
                // LMDB also needs to know how large our DB might be. Over-estimating is OK.
                .setMapSize(10_485_760)
                // LMDB also needs to know how many DBs (Dbi) we want to store in this Env.
                .setMaxDbs(1)
                // Now let's open the Env. The same path can be concurrently opened and
                // used in different processes, but do not open the same path twice in
                // the same process at the same time.
                .open(tmpDir.toFile());

        // We need a Dbi for each DB. A Dbi roughly equates to a sorted map. The
        // MDB_CREATE flag causes the DB to be created if it doesn't already exist.
        final Dbi<ByteBuffer> db = env.openDbi(DB_NAME, MDB_CREATE);

        String streamSean = "stream-sean";
        HashCode streamSeanHash = Hashing.murmur3_128().hashString(streamSean, UTF_8);
        final ByteBuffer key = allocateDirect(env.getMaxKeySize());
        final ByteBuffer val = allocateDirect(env.getMaxKeySize());
        //System.out.println(Arrays.toString(streamSeanHash.toString().getBytes(UTF_8)));
        //System.out.println(Arrays.toString((streamSeanHash.toString() + "@1").getBytes(UTF_8)));
        key.put((streamSeanHash.toString() + "@1").getBytes(UTF_8)).flip();
        val.put("first".getBytes(UTF_8)).flip();

        // Insert some data. Note that ByteBuffer order defaults to Big Endian.
        // LMDB does not persist the byte order, but it's critical to sort keys.
        // If your numeric keys don't sort as expected, review buffer byte order.

        db.put(key, val);

        final ByteBuffer key2 = allocateDirect(env.getMaxKeySize());
        final ByteBuffer val2 = allocateDirect(env.getMaxKeySize());
        key2.put((streamSeanHash.toString() + "@2").getBytes(UTF_8)).flip();
        val2.put("second".getBytes(UTF_8)).flip();

        // Now store it. Dbi.put() internally begins and commits a transaction (Txn).
        db.put(key2, val2);


        String streamSuzanne = "stream-suzanne";
        HashCode streamSuzanneHash = Hashing.murmur3_128().hashString(streamSuzanne, UTF_8);
        final ByteBuffer suzanneKey1 = allocateDirect(env.getMaxKeySize());
        final ByteBuffer suzanneKey1Val = allocateDirect(env.getMaxKeySize());
        suzanneKey1.put((streamSuzanneHash.toString() + "@" + Integer.MAX_VALUE).getBytes(UTF_8)).flip();
        suzanneKey1Val.put("sugie".getBytes(UTF_8)).flip();

        db.put(suzanneKey1, suzanneKey1Val);

        // To fetch any data from LMDB we need a Txn. A Txn is very important in
        // LmdbJava because it offers ACID characteristics and internally holds a
        // read-only key buffer and read-only value buffer. These read-only buffers
        // are always the same two Java objects, but point to different LMDB-managed
        // memory as we use Dbi (and Cursor) methods. These read-only buffers remain
        // valid only until the Txn is released or the next Dbi or Cursor call. If
        // you need data afterwards, you should copy the bytes to your own buffer.
        try (Txn<ByteBuffer> txn = env.txnRead()) {

            // TODO: Fix. why you no work?
            System.out.println("--- Range Search ---");

            ByteBuffer start = allocateDirect(env.getMaxKeySize());
            start.put((streamSuzanneHash.toString() + "@0").getBytes(UTF_8)).flip();

            ByteBuffer end = allocateDirect(env.getMaxKeySize());
            end.put((streamSuzanneHash.toString() + "@" + Integer.MAX_VALUE).getBytes(UTF_8)).flip();

            CursorIterator<ByteBuffer> ckr = db.iterate(txn, KeyRange.closed(start, end));
            for (final CursorIterator.KeyVal<ByteBuffer> kv : ckr.iterable()) {
                System.out.println(UTF_8.decode(kv.key()).toString() + ":" + UTF_8.decode(kv.val()).toString());
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    // why does this work but not above with @ -- issue is key size.
    // https://github.com/lmdbjava/lmdbjava/issues/98
    @Test
    public void simpleHashPrefix(@TempDirectory.TempDir Path tmpDir) throws IOException {
        //final File path = tmp.newFolder();

        // We always need an Env. An Env owns a physical on-disk storage file. One
        // Env can store many different databases (ie sorted maps).
        final Env<ByteBuffer> env = create()
                // LMDB also needs to know how large our DB might be. Over-estimating is OK.
                .setMapSize(10_485_760)
                // LMDB also needs to know how many DBs (Dbi) we want to store in this Env.
                .setMaxDbs(1)
                // Now let's open the Env. The same path can be concurrently opened and
                // used in different processes, but do not open the same path twice in
                // the same process at the same time.
                .open(tmpDir.toFile());

        // We need a Dbi for each DB. A Dbi roughly equates to a sorted map. The
        // MDB_CREATE flag causes the DB to be created if it doesn't already exist.
        final Dbi<ByteBuffer> db = env.openDbi(DB_NAME, MDB_CREATE);

        String streamSean = "stream-sean";
        HashCode streamSeanHash = Hashing.murmur3_128().hashString(streamSean, UTF_8);
        final ByteBuffer seanKey = allocateDirect(env.getMaxKeySize());
        final ByteBuffer seanVal = allocateDirect(env.getMaxKeySize());
        seanKey.put(streamSeanHash.toString().getBytes(UTF_8)).flip();
        seanVal.put("first".getBytes(UTF_8)).flip();
        db.put(seanKey, seanVal);

        String streamSuzanne = "stream-suzanne";
        HashCode streamSuzanneHash = Hashing.murmur3_128().hashString(streamSuzanne, UTF_8);
        final ByteBuffer suzanneKey1 = allocateDirect(env.getMaxKeySize());
        final ByteBuffer suzanneKey1Val = allocateDirect(env.getMaxKeySize());
        suzanneKey1.put(streamSuzanneHash.toString().getBytes(UTF_8)).flip();
        suzanneKey1Val.put("sugie".getBytes(UTF_8)).flip();

        db.put(suzanneKey1, suzanneKey1Val);

        // To fetch any data from LMDB we need a Txn. A Txn is very important in
        // LmdbJava because it offers ACID characteristics and internally holds a
        // read-only key buffer and read-only value buffer. These read-only buffers
        // are always the same two Java objects, but point to different LMDB-managed
        // memory as we use Dbi (and Cursor) methods. These read-only buffers remain
        // valid only until the Txn is released or the next Dbi or Cursor call. If
        // you need data afterwards, you should copy the bytes to your own buffer.
        try (Txn<ByteBuffer> txn = env.txnRead()) {
            System.out.println("--- Range Search ---");

            ByteBuffer start = allocateDirect(env.getMaxKeySize());
            start.put(streamSeanHash.toString().getBytes(UTF_8)).flip();

            ByteBuffer end = allocateDirect(env.getMaxKeySize());
            end.put(streamSeanHash.toString().getBytes(UTF_8)).flip();

            CursorIterator<ByteBuffer> ckr = db.iterate(txn, KeyRange.closed(start, end));
            for (final CursorIterator.KeyVal<ByteBuffer> kv : ckr.iterable()) {
                kv.key().mark();
                kv.val().mark();
                assertEquals("e01c4cd414ccc6eefb37b7735e570d78", UTF_8.decode(kv.key()).toString());
                assertEquals("first", UTF_8.decode(kv.val()).toString());
                kv.key().reset();
                kv.val().reset();
                System.out.println(UTF_8.decode(kv.key()).toString() + ":" + UTF_8.decode(kv.val()).toString());
            }

            start.clear();
            start.put(streamSuzanneHash.toString().getBytes(UTF_8)).flip();

            end.clear();
            end.put(streamSuzanneHash.toString().getBytes(UTF_8)).flip();
            ckr = db.iterate(txn, KeyRange.closed(start, end));
            for (final CursorIterator.KeyVal<ByteBuffer> kv : ckr.iterable()) {
                kv.key().mark();
                kv.val().mark();
                assertEquals("f6165eadb71be20e1a6ea44e00b35eba", UTF_8.decode(kv.key()).toString());
                assertEquals("sugie", UTF_8.decode(kv.val()).toString());
                kv.key().reset();
                kv.val().reset();
                System.out.println(UTF_8.decode(kv.key()).toString() + ":" + UTF_8.decode(kv.val()).toString());
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    @Test
    public void prefixWithArgonaDirectBuffer(@TempDirectory.TempDir Path tmpDir) throws IOException {
        // The critical difference is we pass the PROXY_DB field to Env.create().
        // There's also a PROXY_SAFE if you want to stop ByteBuffer's Unsafe use.
        // Aside from that and a different type argument, it's the same as usual...
        // final File path = tmp.newFolder();

        final Env<org.agrona.DirectBuffer> env = create(PROXY_DB)
                .setMapSize(10_485_760)
                .setMaxDbs(1)
                .open(tmpDir.toFile());

        final Dbi<org.agrona.DirectBuffer> db = env.openDbi(DB_NAME, MDB_CREATE);

        final ByteBuffer keyBb = allocateDirect(env.getMaxKeySize());
        final ByteBuffer valueBb = allocateDirect(env.getMaxKeySize());
        final MutableDirectBuffer key = new UnsafeBuffer(keyBb);
        final MutableDirectBuffer val = new UnsafeBuffer(valueBb);

        // Agrona is not only faster than ByteBuffer, but its methods are nicer...
        //key.putStringWithoutLengthUtf8(0, "stream-sean");
        key.putStringUtf8(0, "stream-sean");
        //val.putStringWithoutLengthUtf8(0, "sean1");
        val.putStringUtf8(0, "sean1");
        db.put(key, val);

        //key.putStringWithoutLengthUtf8(0, "stream-suzanne");
        key.putStringUtf8(0, "stream-suzanne");
        //val.putStringWithoutLengthUtf8(0, "suzanne1");
        val.putStringUtf8(0, "suzanne1");
        db.put(key, val);

        try (Txn<org.agrona.DirectBuffer> txn = env.txnRead()) {
            final MutableDirectBuffer start = new UnsafeBuffer(allocateDirect(env.getMaxKeySize())); //new UnsafeBuffer(allocateDirect(34));
            //start.putStringWithoutLengthUtf8(0, "a");
            start.putStringUtf8(0, "stream-suzanne");

            final MutableDirectBuffer end = new UnsafeBuffer(allocateDirect(env.getMaxKeySize()));// new UnsafeBuffer(allocateDirect(34));
            //end.putStringWithoutLengthUtf8(0, "z");
            end.putStringUtf8(0, "stream-suzanne");


            CursorIterator<org.agrona.DirectBuffer> ckr = db.iterate(txn, KeyRange.closed(start, end));
            for (final CursorIterator.KeyVal<org.agrona.DirectBuffer> kv : ckr.iterable()) {
                //System.out.println(kv.key().getStringWithoutLengthUtf8(0, kv.key().capacity()) + ":" + kv.val().getStringWithoutLengthUtf8(0, kv.val().capacity()));
                System.out.println(kv.key().getStringUtf8(0) + ":" + kv.val().getStringUtf8(0));
            }

            // c.close();
            // txn.commit();
        }
    }


    @Test
    public void simplePrefixseek(@TempDirectory.TempDir Path tmpDir) throws IOException {
        // The critical difference is we pass the PROXY_DB field to Env.create().
        // There's also a PROXY_SAFE if you want to stop ByteBuffer's Unsafe use.
        // Aside from that and a different type argument, it's the same as usual...
        // final File path = tmp.newFolder();

        final Env<org.agrona.DirectBuffer> env = create(PROXY_DB)
                .setMapSize(10_485_760)
                .setMaxDbs(1)
                .open(tmpDir.toFile());

        final Dbi<org.agrona.DirectBuffer> db = env.openDbi(DB_NAME, MDB_CREATE);

        final ByteBuffer keyBb = allocateDirect(env.getMaxKeySize());
        //final MutableDirectBuffer key = new UnsafeBuffer(keyBb);
        final MutableDirectBuffer key = new UnsafeBuffer(allocateDirect(4));//UnsafeBuffer(new byte[4]);
        final MutableDirectBuffer val = new UnsafeBuffer(keyBb);

        // Agrona is not only faster than ByteBuffer, but its methods are nicer...
        key.putInt(0, 1);
        //val.putStringWithoutLengthUtf8(0, "1");
        val.putStringUtf8(0, "1");
        db.put(key, val);

        key.putInt(0, 100);
        val.putStringUtf8(0, "100");
        db.put(key, val);

        key.putInt(0, 10);
        val.putStringUtf8(0, "10");
        db.put(key, val);


        try (Txn<org.agrona.DirectBuffer> txn = env.txnRead()) {
            final org.agrona.DirectBuffer found = db.get(txn, key);
            assertNotNull(found);

            //final MutableDirectBuffer start = new UnsafeBuffer(keyBb);
            final MutableDirectBuffer start = new UnsafeBuffer(allocateDirect(4));
            start.putInt(0, 5);

            //final MutableDirectBuffer end = new UnsafeBuffer(keyBb);
            final MutableDirectBuffer end = new UnsafeBuffer(allocateDirect(4));
            end.putInt(0, 1000);

            CursorIterator<org.agrona.DirectBuffer> ckr = db.iterate(txn, KeyRange.open(start, end));
            for (final CursorIterator.KeyVal<org.agrona.DirectBuffer> kv : ckr.iterable()) {
                System.out.println(kv.key().getInt(0) + ":" + kv.val().getStringUtf8(0));
            }

            // c.close();
            // txn.commit();
        }
    }



    @Test
    public void simplePrefixseek2(@TempDirectory.TempDir Path tmpDir) throws IOException {
        // The critical difference is we pass the PROXY_DB field to Env.create().
        // There's also a PROXY_SAFE if you want to stop ByteBuffer's Unsafe use.
        // Aside from that and a different type argument, it's the same as usual...
        // final File path = tmp.newFolder();

        final Env<org.agrona.DirectBuffer> env = create(PROXY_DB)
                .setMapSize(10_485_760)
                .setMaxDbs(1)
                .open(tmpDir.toFile());

        final Dbi<org.agrona.DirectBuffer> db = env.openDbi(DB_NAME, MDB_CREATE);

        final ByteBuffer keyBb = allocateDirect(env.getMaxKeySize());
        final ByteBuffer valueBb = allocateDirect(env.getMaxKeySize());
        //final MutableDirectBuffer key = new UnsafeBuffer(keyBb);
        final MutableDirectBuffer key = new UnsafeBuffer(keyBb);
        final MutableDirectBuffer val = new UnsafeBuffer(valueBb);

        // Agrona is not only faster than ByteBuffer, but its methods are nicer...
        key.putStringUtf8(0, "apple");
        val.putStringUtf8(0, "1");
        db.put(key, val);

        key.putStringUtf8(0, "raspberry");
        val.putStringUtf8(0, "100");
        db.put(key, val);

        key.putStringUtf8(0, "pineapple");
        val.putStringUtf8(0, "10");
        db.put(key, val);


        try (Txn<org.agrona.DirectBuffer> txn = env.txnRead()) {
            final org.agrona.DirectBuffer found = db.get(txn, key);
            assertNotNull(found);

            final MutableDirectBuffer start = new UnsafeBuffer(keyBb);
            start.putStringUtf8(0, "a");

            final MutableDirectBuffer end = new UnsafeBuffer(valueBb);
            end.putStringUtf8(0, "z");

            CursorIterator<org.agrona.DirectBuffer> ckr = db.iterate(txn, KeyRange.closed(start, end));
            for (final CursorIterator.KeyVal<org.agrona.DirectBuffer> kv : ckr.iterable()) {
                System.out.println(kv.key().getInt(0) + ":" + kv.val().getStringUtf8(0));
            }

        }
    }



    // TODO: Add a performance benchmark comparing original to Agrona
    @Test
    public void testArgonaDirectBuffer(@TempDirectory.TempDir Path tmpDir) throws IOException {
        // The critical difference is we pass the PROXY_DB field to Env.create().
        // There's also a PROXY_SAFE if you want to stop ByteBuffer's Unsafe use.
        // Aside from that and a different type argument, it's the same as usual...
        // final File path = tmp.newFolder();

        final Env<org.agrona.DirectBuffer> env = create(PROXY_DB)
                .setMapSize(10_485_760)
                .setMaxDbs(1)
                .open(tmpDir.toFile());

        final Dbi<org.agrona.DirectBuffer> db = env.openDbi(DB_NAME, MDB_CREATE);

        final ByteBuffer keyBb = allocateDirect(env.getMaxKeySize());
        final MutableDirectBuffer key = new UnsafeBuffer(keyBb);
        final MutableDirectBuffer val = new UnsafeBuffer(allocateDirect(700));

        try (Txn<org.agrona.DirectBuffer> txn = env.txnWrite()) {
            final Cursor<org.agrona.DirectBuffer> c = db.openCursor(txn);

            // Agrona is not only faster than ByteBuffer, but its methods are nicer...
            val.putStringWithoutLengthUtf8(0, "The Value");
            key.putStringWithoutLengthUtf8(0, "yyy");
            c.put(key, val);

            key.putStringWithoutLengthUtf8(0, "ggg");
            c.put(key, val);

            c.seek(MDB_FIRST);
            assertThat(c.key().getStringWithoutLengthUtf8(0, env.getMaxKeySize()), startsWith("ggg"));

            c.seek(MDB_LAST);
            assertThat(c.key().getStringWithoutLengthUtf8(0, env.getMaxKeySize()), startsWith("yyy"));

            // DirectBuffer has no notion of a position. Often you don't want to store
            // the unnecessary bytes of a varying-size buffer. Let's have a look...
            final int keyLen = key.putStringWithoutLengthUtf8(0, "12characters");
            assertThat(keyLen, is(12));
            assertThat(key.capacity(), is(env.getMaxKeySize()));

            // To only store the 12 characters, we simply call wrap:
            key.wrap(key, 0, keyLen);
            assertThat(key.capacity(), is(keyLen));
            c.put(key, val);
            c.seek(MDB_FIRST);
            assertThat(c.key().capacity(), is(keyLen));
            assertThat(c.key().getStringWithoutLengthUtf8(0, c.key().capacity()), is("12characters"));

            // If we want to store bigger values again, just wrap our original buffer.
            key.wrap(keyBb);
            assertThat(key.capacity(), is(env.getMaxKeySize()));

            c.close();
            txn.commit();
        }
    }


//    private void run_app(DbEnv dbenv)
//            throws DbException, FileNotFoundException
//    {
//        Db dbp, sdbp;
//        Dbt key, pkey, skey, data;
//        StudentRecord srec;
//
//		/* Open/create primary */
//        dbp = new Db(dbenv, 0);
//        dbp.open(null, "students.db", null, Db.DB_BTREE, Db.DB_CREATE,
//                0600);
//
//		/*
//		 * Open/create secondary.  Note that it supports duplicate data
//		 * items, since last names might not be unique.
//		 */
//        sdbp = new Db(dbenv, 0);
//        sdbp.set_flags(Db.DB_DUP | Db.DB_DUPSORT);
//        sdbp.open(null, "lastname.db", null, Db.DB_BTREE, Db.DB_CREATE,
//                0600);
//
//        try {
//			/* Associate the secondary with the primary. */
//            dbp.associate(sdbp, new GetName(), 0);
//
//			/* Add a new record */
//            key = new Dbt();
//            key.set_data("WC42".getBytes());
//            key.set_size(4);
//            srec = new StudentRecord();
//            srec.student_id = "WC42";
//            srec.last_name = "Churchill      ";
//            srec.first_name = "Winston        ";
//            data = new Dbt();
//            srec.encode(data);
//
//            System.out.println("Adding a record with primary key " +
//                    new String(key.get_data()) + " and secondary key " +
//                    srec.last_name);
//            dbp.put(null, key, data, 0);
//
//			/* Now do a lookup */
//            skey = new Dbt();
//            pkey = new Dbt();
//            data = new Dbt();
//            skey.set_data("Churchill      ".getBytes());
//            skey.set_size(15);
//            System.out.println("Searching with secondary key " +
//                    new String(skey.get_data()));
//            sdbp.pget(null, skey, pkey, data, 0);
//
//            System.out.println("Found a record with primary key " +
//                    new String(pkey.get_data()));
//        } finally {
//            dbp.close(0);
//            sdbp.close(0);
//        }
//    }




    @Test
    public void tutorial4(@TempDirectory.TempDir Path tmpDir) throws IOException {
        // As per tutorial1...
        // final File path = tmp.newFolder();

        final Env<ByteBuffer> env = open(tmpDir.toFile(), 10);
        final Dbi<ByteBuffer> db = env.openDbi(DB_NAME, MDB_CREATE);


        // ByteBuffer with allocateDirect needs to flip. ByteBuffer order defaults to Big Endian. flip to read data
        // MutableDirectBuffer does not

        try (Txn<ByteBuffer> txn = env.txnWrite()) {
            final ByteBuffer key = allocateDirect(env.getMaxKeySize());
            final ByteBuffer val = allocateDirect(700);

            // Insert some data. Note that ByteBuffer order defaults to Big Endian.
            // LMDB does not persist the byte order, but it's critical to sort keys.
            // If your numeric keys don't sort as expected, review buffer byte order.
            val.putInt(100).flip();
            key.putInt(1).flip();
            db.put(txn, key, val);
            key.clear();
            key.putInt(50).flip();
            db.put(txn, key, val);
            key.clear();
            key.putInt(100).flip();
            db.put(txn, key, val);
            key.clear();

            // Each iterator uses a cursor and must be closed when finished.
            // Iterate forward in terms of key ordering starting with the first key.
            try (CursorIterator<ByteBuffer> it = db.iterate(txn, KeyRange.all())) {
                for (final KeyVal<ByteBuffer> kv : it.iterable()) {
                    //System.out.println(UTF_8.decode(kv.key()).toString() + ":" + UTF_8.decode(kv.val()).toString());
                    System.out.println(kv.key().getInt() + ":" + kv.val().getInt());
                    assertThat(kv.key(), notNullValue());
                    assertThat(kv.val(), notNullValue());
                }
            }

            // Iterate backward in terms of key ordering starting with the last key.
            try (CursorIterator<ByteBuffer> it = db.iterate(txn, KeyRange.allBackward())) {
                for (final KeyVal<ByteBuffer> kv : it.iterable()) {
                    //System.out.println(UTF_8.decode(kv.key()).toString() + ":" + UTF_8.decode(kv.val()).toString());
                    System.out.println(kv.key().getInt() + ":" + kv.val().getInt());
                    assertThat(kv.key(), notNullValue());
                    assertThat(kv.val(), notNullValue());
                }
            }

            // There are many ways to control the desired key range via KeyRange, such
            // as arbitrary start and stop values, direction etc. We've adopted Guava's
            // terminology for our range classes (see KeyRangeType for further details).
            key.putInt(75);
            final ByteBuffer end = allocateDirect(env.getMaxKeySize());
            end.putInt(750);
            final KeyRange<ByteBuffer> range = KeyRange.open(key, end);
            // TODO: why dont we have to flip the range keys
            try (CursorIterator<ByteBuffer> it = db.iterate(txn, range)) {
                for (final KeyVal<ByteBuffer> kv : it.iterable()) {
                    //System.out.println(UTF_8.decode(kv.key()).toString() + ":" + UTF_8.decode(kv.val()).toString());
                    System.out.println(kv.key().getInt() + ":" + kv.val().getInt());
                    assertThat(kv.key(), notNullValue());
                    assertThat(kv.val(), notNullValue());
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
    public void lexiographicComparison() {
        String hello = "hello@1";
        String world = "world@500000";

        String start = "a";
        String end = "z";

        List<String> foundStrings = new ArrayList<>();
        for (String word : Arrays.asList(hello, world)) {
            if (word.compareTo(start) >= 0 && word.compareTo(end) <= 0) {
                foundStrings.add(word);
            }
        }

        assertEquals(2, foundStrings.size());

        List<ByteBuffer> foundByteBuffers = new ArrayList<>();
        for (ByteBuffer bb : Arrays.asList(ByteBuffer.wrap(hello.getBytes()), ByteBuffer.wrap(world.getBytes()))) {
            if (bb.compareTo(ByteBuffer.wrap(start.getBytes())) >= 0 && bb.compareTo(ByteBuffer.wrap(end.getBytes())) <= 0) {
                foundByteBuffers.add(bb);
            }
        }

        assertEquals(2, foundByteBuffers.size());


    }

}
