package com.ruscello.storage.lmdb;

import com.ruscello.storage.KeySpaces;
import com.ruscello.storage.KeyValue;
import com.ruscello.storage.StorageEngine;
import com.ruscello.storage.StoreIterator;
import org.lmdbjava.*;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;
import sun.jvm.hotspot.runtime.Bytes;

import java.io.File;
import java.nio.ByteBuffer;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static java.nio.ByteBuffer.allocateDirect;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.lmdbjava.DbiFlags.MDB_CREATE;
import static org.lmdbjava.Env.create;


public class LmbdStorageEngine implements StorageEngine {

    /**
     * LMBD doesn't have the concept of column families or keyspaces so the number of dbs is based on the number
     * of keyspaces we need which includes
     * Main stream based on based on hash
     * stream names
     */
    private static final int DBS = KeySpaces.ALL_STREAM_NAMES.size();
    private static final long MAX_SIZE = 10_485_760; // TB 1_099_511_627_776 // one influxdb lib used 100g as default


    private final Env<ByteBuffer> env;

    // should we consider having individual dbi variables? Might be worth it so we arent required to always pull the
    // dbi handle out of the map. This is likely a pre-optimization that I should benchmark
    private Map<String, Dbi<ByteBuffer>> dbHandles;

    // TODO: pass in RuscolloConfiguration for these
    // - db size
    // - max dbs is something I control and is
    // https://lmdb.readthedocs.io/en/release/#environment-class
    // from docs "On 64-bit there is no penalty for making this huge (say 1TB)"
    // do we want to enforce 64 bit machine? my initial thought is yes
    // max key size - I thinking (?) we control all the key sizes, even stream name we can enforce a max of 256 characters (?)
    public LmbdStorageEngine(File path, Long maxSize) {
        // TODO: should we move this to an init method or move to open?
        env = create()
                // LMDB also needs to know how large our DB might be. Over-estimating is OK.
                .setMapSize(MAX_SIZE)
                // LMDB also needs to know how many DBs (Dbi) we want to store in this Env.
                .setMaxDbs(DBS)
                // Now let's open the Env. The same path can be concurrently opened and
                // used in different processes, but do not open the same path twice in
                // the same process at the same time.
                .open(path);

    }

    public void open() {
        for (String dbName : KeySpaces.ALL_STREAM_NAMES) {
            // TODO: flags should be based on RuscelloConfiguration
            dbHandles.put(dbName, env.openDbi(dbName, MDB_CREATE));
        }
    }

    public void close() {
//        if (!open) {
//            return;
//        }
//
//        open = false;
//        closeOpenIterators();

        for (Dbi<ByteBuffer> db : dbHandles.values()) {
            db.close();
        }

    }

    @Override
    public byte[] get(byte[] key) {

        Dbi<ByteBuffer> dbi = dbHandles.get(KeySpaces.MAIN);
        try (Txn<ByteBuffer> txn = env.txnRead()) {
            // TODO: we need to encapsulate the conversion of String key to ByteBuffer somewhere in an above layer
            final ByteBuffer bbKey = allocateDirect(env.getMaxKeySize());
            bbKey.put(key).flip();
            // return dbi.get(txn, bbKey);
        }

        return new byte[0];
    }

    // TODO: support different key range types (open / closed)
    // technically we dont need to support all variations as we really only need to support a prefix search
    // which I think would just be closed/closed ????
    // TODO: support direction
    // TODO: we need to encapsulate the conversion of String key to ByteBuffer somewhere in an above layer
    @Override
    public StoreIterator<byte[], byte[]> range(byte[] start, byte[] end) {

        Dbi<ByteBuffer> dbi = dbHandles.get(KeySpaces.MAIN);
        try (Txn<ByteBuffer> txn = env.txnRead()) {

            CursorIterator<ByteBuffer> ckr = dbi.iterate(txn, KeyRange.closed(start, end));
            for (final CursorIterator.KeyVal<ByteBuffer> kv : ckr.iterable()) {
                System.out.println(UTF_8.decode(kv.key()).toString() + ":" + UTF_8.decode(kv.val()).toString());
            }
        } catch (Exception e) {
            System.out.println(e);
        }


        return null;
    }

    // This is a prefix search with a count
    @Override
    public StoreIterator<byte[], byte[]> range(byte[] start, int maxCount) {
        return null;
    }

    // TODO: is this something we would actually need? Initial thought is no as we'll always want a slice however
    // there may be streaming scenarios we'd want to consider?
    // TODO: I'm sure we need to at least contrain this to just the user space keys (non-system keys)
    @Override
    public StoreIterator<byte[], byte[]> all() {
        Dbi<ByteBuffer> dbi = dbHandles.get(KeySpaces.MAIN);
        try (Txn<ByteBuffer> txn = env.txnRead()) {

            CursorIterator<ByteBuffer> ckr = dbi.iterate(txn);

        }


        return null;
    }

    @Override
    public long approximateNumEntries() {
        return 0;
    }

//    // env stats vs individual db stats?
//    @Override
//    public long approximateNumEntries() {
//        validateStoreOpen();
//        final long value;
//        try {
//            value = this.env.stat().entries;
//        } catch (final RocksDBException e) {
//            throw new ProcessorStateException("Error fetching property from store " + this.name, e);
//        }
//        if (isOverflowing(value)) {
//            return Long.MAX_VALUE;
//        }
//        return value;
//    }
//
//    @Override
//    public ReadAllPage readAllForwards(long fromPositionInclusive, int maxCount, boolean prefetch) throws SQLException {
//        return null;
//    }

//    @Override
//    public ReadAllPage readAllBackwards(long fromPositionInclusive, int maxCount, boolean prefetch) throws SQLException {
//        return null;
//    }
//
//    @Override
//    public ReadStreamPage readStreamForwards(String streamId, int fromVersionInclusive, int maxCount, boolean prefetch) throws SQLException {
//        return null;
//    }
//
//    @Override
//    public ReadStreamPage readStreamBackwards(String streamId, int fromVersionInclusive, int maxCount, boolean prefetch) throws SQLException {
//        return null;
//    }
//
//    @Override
//    public Long readHeadPosition() {
//        return null;
//    }
//
//    @Override
//    public StreamMetadataResult getStreamMetadata(String streamId) throws SQLException {
//        return null;
//    }


//    // do we really need a custom iterator that wraps CursorIterator?
//    private class LmdbIterator implements StoreIterator<Bytes, byte[]> {
//        private final String storeName;
//        private final CursorIterator<ByteBuffer> iter;
//
//        private volatile boolean open = true;
//
//        LmdbIterator(final String storeName,
//                        final CursorIterator<ByteBuffer> iter) {
//            this.iter = iter;
//            this.storeName = storeName;
//        }
//
//        byte[] peekRawKey() {
//
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
//            return iter.hasNext();
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

}
