package com.ruscello.storage.rocksdb;

import com.google.common.hash.HashCode;
import com.ruscello.storage.KeySpaces;
import com.ruscello.storage.KeyValue;
import com.ruscello.storage.StorageEngine;
import com.ruscello.storage.StoreIterator;
import org.lmdbjava.Dbi;
import org.rocksdb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.jvm.hotspot.runtime.Bytes;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.util.*;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.rocksdb.Env.COMPACTION_POOL;
import static org.rocksdb.Env.FLUSH_POOL;

// https://github.com/apache/kafka/blob/a1352f8c5a96c8e861c17c8105557428a47f4334/streams/src/main/java/org/apache/kafka/streams/state/internals/RocksDBStore.java
// https://github.com/Instagram/cassandra/blob/rocks_3.0/src/java/org/apache/cassandra/rocksdb/RocksDBIteratorAdapter.java
// https://github.com/Instagram/cassandra/blob/rocks_3.0/src/java/org/apache/cassandra/rocksdb/RocksDBEngine.java

// https://github.com/facebook/mysql-5.6/wiki/MyRocks-data-dictionary-format
// https://www.slideshare.net/matsunobu/myrocks-deep-dive
// https://www.percona.com/blog/2018/02/01/myrocks-engine-things-know-start/

// A persistent key-value store based on RocksDB.
// Note that the use of array-typed keys is discouraged because they result in incorrect caching behavior.
// If you intend to work on byte arrays as key, for example, you may want to wrap them with the {@code Bytes} class,
// i.e. use {@code RocksDBStore<Bytes, ...>} rather than {@code RocksDBStore<byte[], ...>}.
public class RocksdbStorageEngine implements StorageEngine {

    private static final Logger logger = LoggerFactory.getLogger(RocksdbStorageEngine.class);

    private static final String DB_FILE_DIR = "rocksdb";

    private final Set<StoreIterator> openIterators = Collections.synchronizedSet(new HashSet<>());


    private final String name;
    private final String parentDir;
    File dbDir;
    private RocksDB db;

    // the following option objects will be created in the constructor and closed in the close() method
    // private DBOptions dbOptions
    // private ColumnFamilyOptions cfOptions
    private Options options;
    private WriteOptions wOptions;
    private FlushOptions fOptions;
    private Map<String, ColumnFamilyHandle> cfHandles;

    protected volatile boolean open = false;



//    protected int compactionthroughputMbPerSec = RocksDBConfigs.RATE_MBYTES_PER_SECOND;
//    public final RateLimiter rateLimiter = new RateLimiter(1024L * 1024L * compactionthroughputMbPerSec);
//    public final Cache cache = new LRUCache(RocksDBConfigs.BLOCK_CACHE_SIZE_MBYTES * 1024 * 1024L);


    // TODO: Do we need to do this?
    static {
        RocksDB.loadLibrary();
    }

    // TODO: not sure I want to keep this
    public RocksdbStorageEngine(String name) {
        this(name, DB_FILE_DIR);
    }

    // TODO: not sure I want to keep this
    public RocksdbStorageEngine(String name, String parentDir) {
        this.name = name;
        this.parentDir = parentDir;
    }

//    // DBOptions options
//    // TODO: Someone needs to dispose of options. in this instance caller needs to dispose of options
//    public RocksdbStorageEngine(final Options options, String path) throws RocksDBException {
//
//        // TODO: move this out of constructor into factory.
//        // TODO: where to create db?
//        // need to create handles
//
////        final List<ColumnFamilyDescriptor> cfDescriptors = Arrays.asList(
////                new ColumnFamilyDescriptor(RocksDB.DEFAULT_COLUMN_FAMILY, cfOpts),
////                new ColumnFamilyDescriptor("my-first-columnfamily".getBytes(), cfOpts)
////        );
//
//
//
//
//    }

    public void open(final Options options) {

//        From kafka streams. Looks like rocksdb options = DBOptions + ColumnFamilyOptions
//        final BlockBasedTableConfig tableConfig = new BlockBasedTableConfig();
//        tableConfig.setBlockCacheSize(BLOCK_CACHE_SIZE);
//        tableConfig.setBlockSize(BLOCK_SIZE);
//        options = new Options();
//        options.setTableFormatConfig(tableConfig);
//        options.setWriteBufferSize(WRITE_BUFFER_SIZE);
//        options.setCompressionType(COMPRESSION_TYPE);
//        options.setCompactionStyle(COMPACTION_STYLE);
//        options.setMaxWriteBufferNumber(MAX_WRITE_BUFFERS);
//        options.setCreateIfMissing(true);
//        options.setErrorIfExists(false);
//        options.setInfoLogLevel(InfoLogLevel.ERROR_LEVEL);
//        // this is the recommended way to increase parallelism in RocksDb
//        // note that the current implementation of setIncreaseParallelism affects the number
//        // of compaction threads but not flush threads (the latter remains one). Also
//        // the parallelism value needs to be at least two because of the code in
//        // https://github.com/facebook/rocksdb/blob/62ad0a9b19f0be4cefa70b6b32876e764b7f3c11/util/options.cc#L580
//        // subtracts one from the value passed to determine the number of compaction threads
//        // (this could be a bug in the RocksDB code and their devs have been contacted).
//        options.setIncreaseParallelism(Math.max(Runtime.getRuntime().availableProcessors(), 2));
//
//        if (prepareForBulkload) {
//            options.prepareForBulkLoad();
//        }
//
//        wOptions = new WriteOptions();
//        wOptions.setDisableWAL(true);
//
//        fOptions = new FlushOptions();
//        fOptions.setWaitForFlush(true);

        // OptionsUtil

        // Does rocksjava have a DBWithTTL


        EnvOptions envOptions = new EnvOptions();


        // needs to be disposed of
        Env env = RocksEnv.getDefault();
        // https://github.com/facebook/rocksdb/wiki/RocksDB-Tuning-Guide#parallelism-options
        // env.setBackgroundThreads(1, FLUSH_POOL);
        // env.setBackgroundThreads(1, COMPACTION_POOL);

        BlockBasedTableConfig blockBasedTableConfig = new BlockBasedTableConfig();
        // blockBasedTableConfig.setBlockCache()
        // blockBasedTableConfig.setBlockCacheSize()
        // blockBasedTableConfig.setBlockSize()

        // https://github.com/facebook/rocksdb/wiki/RocksDB-Tuning-Guide#prefix-databases
        // blockBasedTableConfig.setFilter()
        // blockBasedTableConfig.setIndexType()


        DBOptions dbOptions = new DBOptions();
        // dbOptions.setBaseBackgroundCompactions();
        // dbOptions.setMaxBackgroundCompactions()
        // dbOptions.setMaxBackgroundFlushes()
        // dbOptions.setIncreaseParallelism()
        // dbOptions.setEnv()

        ReadOptions rOptions = new ReadOptions();
        // rOptions.setBackgroundPurgeOnIteratorCleanup()
        // rOptions.setFillCache()
        // rOptions.setIterateUpperBound()
        // rOptions.setPrefixSameAsStart()
        // rOptions.setTailing()
        // rOptions.setTotalOrderSeek()


        WriteOptions wOptions = new WriteOptions();
        // wOptions.setDisableWAL()
        // wOptions.setSync()
        // wOptions.setNoSlowdown()

        FlushOptions fOptions = new FlushOptions();
        // fOptions.setWaitForFlush()

        ColumnFamilyOptions cfOptions = new ColumnFamilyOptions();
        // cfOptions.setCompressionType()
        // cfOptions.setCompactionStyle()
        // cfOptions.setMaxWriteBufferNumber()

//        this.dbDir = new File(new File(context.stateDir(), parentDir), this.name);

        List<ColumnFamilyDescriptor> cfDescriptors = new ArrayList<>(KeySpaces.ALL_STREAM_NAMES.size());
        for (String dbName : KeySpaces.ALL_STREAM_NAMES) {
            cfDescriptors.add(new ColumnFamilyDescriptor(dbName.getBytes(UTF_8)));
        }
        List<ColumnFamilyHandle> cfHandles = new ArrayList<>();

//        https://github.com/facebook/rocksdb/wiki/RocksJava-Basics

//        Files.createDirectories(dir.getParentFile().toPath());
//        return RocksDB.open(options, dir.getAbsolutePath());

//        db = RocksDB.open(options, dbDir, cfDescriptors, cfHandles);
    }

    public void close() {
        if (!open) {
            return;
        }

        open = false;
        //closeOpenIterators();
        options.close();
        wOptions.close();
        fOptions.close();

        for (ColumnFamilyHandle cfHandle : cfHandles.values()) {
            cfHandle.close();
        }

        db.close();

        options = null;
        wOptions = null;
        fOptions = null;
        cfHandles = null;
        db = null;
    }

    private boolean isOverflowing(final long value) {
        // RocksDB returns an unsigned 8-byte integer, which could overflow long
        // and manifest as a negative value.
        return value < 0;
    }

//    private void validateStoreOpen() {
//        if (!open) {
//            throw new InvalidStateStoreException("Store " + this.name + " is currently closed");
//        }
//    }
//
//    @Override
//    public boolean isOpen() {
//        return open;
//    }

//    @Override
//    public synchronized KeyValueIterator<Bytes, byte[]> all() {
//        validateStoreOpen();
//        // query rocksdb
//        final RocksIterator innerIter = db.newIterator();
//        innerIter.seekToFirst();
//        final RocksDbIterator rocksDbIterator = new RocksDbIterator(name, innerIter);
//        openIterators.add(rocksDbIterator);
//        return rocksDbIterator;
//    }


//    public synchronized KeyValue<Bytes, byte[]> first() {
//        validateStoreOpen();
//
//        final RocksIterator innerIter = db.newIterator();
//        innerIter.seekToFirst();
//        final KeyValue<Bytes, byte[]> pair = new KeyValue<>(new Bytes(innerIter.key()), innerIter.value());
//        innerIter.close();
//
//        return pair;
//    }

//    public synchronized KeyValue<Bytes, byte[]> last() {
//        validateStoreOpen();
//
//        final RocksIterator innerIter = db.newIterator();
//        innerIter.seekToLast();
//        final KeyValue<Bytes, byte[]> pair = new KeyValue<>(new Bytes(innerIter.key()), innerIter.value());
//        innerIter.close();
//
//        return pair;
//    }

//    /**
//     * Return an approximate count of key-value mappings in this store.
//     *
//     * <code>RocksDB</code> cannot return an exact entry count without doing a
//     * full scan, so this method relies on the <code>rocksdb.estimate-num-keys</code>
//     * property to get an approximate count. The returned size also includes
//     * a count of dirty keys in the store's in-memory cache, which may lead to some
//     * double-counting of entries and inflate the estimate.
//     *
//     * @return an approximate count of key-value mappings in the store.
//     */
//    @Override
//    public long approximateNumEntries() {
//        validateStoreOpen();
//        final long value;
//        try {
//            value = this.db.getLongProperty("rocksdb.estimate-num-keys");
//        } catch (final RocksDBException e) {
//            throw new ProcessorStateException("Error fetching property from store " + this.name, e);
//        }
//        if (isOverflowing(value)) {
//            return Long.MAX_VALUE;
//        }
//        return value;
//    }

//    @Override
//    public synchronized void flush() {
//        if (db == null) {
//            return;
//        }
//        // flush RocksDB
//        flushInternal();
//    }


//    /**
//     * @throws ProcessorStateException if flushing failed because of any internal store exceptions
//     */
//    private void flushInternal() {
//        try {
//            db.flush(fOptions);
//        } catch (final RocksDBException e) {
//            throw new ProcessorStateException("Error while executing flush from store " + this.name, e);
//        }
//    }

//    private void closeOpenIterators() {
//        final HashSet<KeyValueIterator> iterators;
//        synchronized (openIterators) {
//            iterators = new HashSet<>(openIterators);
//        }
//        for (final KeyValueIterator iterator : iterators) {
//            iterator.close();
//        }
//    }


//    /**
//     * constructs a Rocksdb backend aka storage engine
//     * @param options
//     * @param path
//     */
//    public static StorageEngine RocksdbStorageEngineFactory(Options options, String path) {
//
//    }

    // TODO: this doesn't need to be an explicit method?
    // whatever object uses the storage engine might be able to do the necessary things
    public void createStream(String name, String hashedName, Object metadata) throws RocksDBException {


    }

//    public void append(HashCode hashedName, Object event, Long expectedVersion) throws RocksDBException {
//        db.put(cfHandle, (hashedName.toString() + "@" + expectedVersion + 1).getBytes(), event.toString().getBytes());
//
//    }

//    public void get(HashCode hashedName, String keyspace, int fromVersionInclusive, int maxCount, String direction) {
//        RocksIterator sit = db.newIterator(cfHandle, ro);
//        for (sit.seekToFirst(); sit.isValid(); sit.next()) {
//            System.out.println(new String(sit.key(), UTF_8) + ":" + new String(sit.value(), UTF_8));
//        }
//    }
//
//    public void getBackwards(HashCode hashedName, String keyspace, int fromVersionInclusive, int maxCount) {
//        RocksIterator sit = db.newIterator(cfHandle, ro);
//        // TODO: need to add check for limit
//        for (sit.seekForPrev(hashedName.asBytes()); sit.isValid(); sit.prev()) {
//            System.out.println(new String(sit.key(), UTF_8) + ":" + new String(sit.value(), UTF_8));
//        }
//    }



    // should append a tombstone
    public void deleteStream(String hashedName) {

    }

    // for scavenging
    public void deleteStreamEvent(String hashedName) {

    }

    @Override
    public byte[] get(byte[] key) {
        return new byte[0];
    }

    @Override
    public StoreIterator<byte[], byte[]> range(byte[] start, byte[] end) {
        return null;
    }

    @Override
    public StoreIterator<byte[], byte[]> range(byte[] start, int maxCount) {
        return null;
    }

    @Override
    public StoreIterator<byte[], byte[]> all() {
        return null;
    }

    @Override
    public long approximateNumEntries() {
        return 0;
    }


    // do we really need a custom iterator that wraps RocksIterator?
//    private class RocksDbIterator implements StoreIterator<Bytes, byte[]> {
//        private final String storeName;
//        private final RocksIterator iter;
//
//        private volatile boolean open = true;
//
//        RocksDbIterator(final String storeName,
//                        final RocksIterator iter) {
//            this.iter = iter;
//            this.storeName = storeName;
//        }
//
//        byte[] peekRawKey() {
//            return iter.key();
//        }
//
//        private KeyValue<Bytes, byte[]> getKeyValue() {
//            return new KeyValue<>(new Bytes(iter.key()), iter.value());
//        }
//
//        @Override
//        public synchronized boolean hasNext() {
//            if (!open) {
//                throw new InvalidStateStoreException(String.format("RocksDB store %s has closed", storeName));
//            }
//
//            return iter.isValid();
//        }
//
//        /**
//         * @throws NoSuchElementException if no next element exist
//         */
//        @Override
//        public synchronized KeyValue<Bytes, byte[]> next() {
//            if (!hasNext())
//                throw new NoSuchElementException();
//
//            final KeyValue<Bytes, byte[]> entry = this.getKeyValue();
//            iter.next();
//            return entry;
//        }
//
//        @Override
//        public void remove() {
//            throw new UnsupportedOperationException("RocksDB iterator does not support remove()");
//        }
//
//        @Override
//        public synchronized void close() {
//            openIterators.remove(this);
//            iter.close();
//            open = false;
//        }
//
//        @Override
//        public Bytes peekNextKey() {
//            if (!hasNext()) {
//                throw new NoSuchElementException();
//            }
//            return new Bytes(iter.key());
//        }
//    }
//
//    private class RocksDBRangeIterator extends RocksDbIterator {
//        // RocksDB's JNI interface does not expose getters/setters that allow the
//        // comparator to be pluggable, and the default is lexicographic, so it's
//        // safe to just force lexicographic comparator here for now.
//        private final Comparator<byte[]> comparator = Bytes.BYTES_LEXICO_COMPARATOR;
//        private final byte[] rawToKey;
//
//        RocksDBRangeIterator(final String storeName,
//                             final RocksIterator iter,
//                             final Bytes from,
//                             final Bytes to) {
//            super(storeName, iter);
//            iter.seek(from.get());
//            this.rawToKey = to.get();
//            if (this.rawToKey == null) {
//                throw new NullPointerException("RocksDBRangeIterator: RawToKey is null for key " + to);
//            }
//        }
//
//        @Override
//        public synchronized boolean hasNext() {
//            return super.hasNext() && comparator.compare(super.peekRawKey(), this.rawToKey) <= 0;
//        }
//    }




//    SeekToLast - reverse
//    Range - [start, limit)
//    for (it->Seek(start);
//    it->Valid() && it->key().ToString() < limit;
//    it->Next()) {
//    ...
//    }

//    range - (limit, start] in reverse order
//            for (it->SeekForPrev(start);
//    it->Valid() && it->key().ToString() > limit;
//    it->Prev()) {
//    ...
//    }
//  assert(it->status().ok()); // Check for any errors found during the scan


//    public void close(ColumnFamilyStore cfs)
//    {
//        RocksDBCF rocksDBCF = getRocksDBCF(cfs);
//        if (rocksDBCF != null)
//        {
//            rocksDBCF.close();
//        }
//        else
//            logger.info("Can not find rocksdb table: " + cfs.name);
//    }

}
