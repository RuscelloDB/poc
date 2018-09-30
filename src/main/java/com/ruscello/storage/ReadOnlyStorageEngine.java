package com.ruscello.storage;



// TDOO: this is the wrong bytes. kafka stream was using
// https://github.com/apache/kafka/blob/021d8a8e9698dce454e0e801092460b98f0a8a4d/clients/src/main/java/org/apache/kafka/common/utils/Bytes.java
// not sure what makes sense for us
// also see RocksDBStore which talks about why and how it relates to their caching

// I think these methods will require keyspace to be passed in
public interface ReadOnlyStorageEngine /*<Bytes, byte[]>*/ {

    /**
     * Get the value corresponding to this key.
     *
     * @param key The key to fetch
     * @return The value or null if no value is found.
     * @throws NullPointerException If null is used for key.
     * @throws InvalidStateStoreException if the store is not initialized
     */
    byte[] get(byte[] key);

    /**
     * Get an iterator over a given range of keys. This iterator must be closed after use.
     * The returned iterator must be safe from {@link java.util.ConcurrentModificationException}s
     * and must not return null values. No ordering guarantees are provided.
     * @param from The first key that could be in the range
     * @param to The last key that could be in the range
     * @return The iterator for this range.
     * @throws NullPointerException If null is used for from or to.
     * @throws InvalidStateStoreException if the store is not initialized
     */
    StoreIterator<byte[], byte[]> range(byte[] start, byte[] end);


    StoreIterator<byte[], byte[]> range(byte[] start, int maxCount);

    /**
     * Return an iterator over all keys in this store. This iterator must be closed after use.
     * The returned iterator must be safe from {@link java.util.ConcurrentModificationException}s
     * and must not return null values. No ordering guarantees are provided.
     * @return An iterator of all key/value pairs in the store.
     * @throws InvalidStateStoreException if the store is not initialized
     */
    StoreIterator<byte[], byte[]> all();

    /**
     * Return an approximate count of key-value mappings in this store.
     *
     * The count is not guaranteed to be exact in order to accommodate stores
     * where an exact count is expensive to calculate.
     *
     * @return an approximate count of key-value mappings in the store.
     * @throws InvalidStateStoreException if the store is not initialized
     */
    long approximateNumEntries();




//    /**
//     *
//     * @param fromPositionInclusive position to start reading from. Use Position.START to start from the beginning
//     * @param maxCount maximum number of events to read
//     * @param prefetch Prefetches the message data as part of the page read. This means a single request to the server but a higher payload size.
//     * @return An @{link ReadAllPage} presenting the result of the read. If all messages read have expired then the message collection MAY be empty.
//     */
//    ReadAllPage readAllForwards(long fromPositionInclusive, int maxCount, boolean prefetch) throws SQLException;
//
//    /**
//     *
//     * @param fromPositionInclusive The position to start reading from. Use Position.END to start from the end.
//     * @param maxCount maximum number of events to read
//     * @param prefetch Prefetches the message data as part of the page read. This means a single request to the server but a higher payload size.
//     * @return An @{link ReadAllPage} presenting the result of the read. If all messages read have expired then the message collection MAY be empty.
//     */
//    ReadAllPage readAllBackwards(long fromPositionInclusive, int maxCount, boolean prefetch) throws SQLException;
//
//    /**
//     *
//     * @param streamId the stream id to read
//     * @param fromVersionInclusive The version of the stream to start reading from. Use StreamVersion.Start to read from the start.
//     * @param maxCount maximum number of events to read
//     * @param prefetch Prefetches the message data as part of the page read. This means a single request to the server but a higher payload size.
//     * @return An @{link ReadAllPage} presenting the result of the read. If all messages read have expired then the message collection MAY be empty.
//     */
//    ReadStreamPage readStreamForwards(
//            String streamId,
//            int fromVersionInclusive,
//            int maxCount,
//            boolean prefetch) throws SQLException;
//
//    /**
//     *
//     * @param streamId the stream id to read
//     * @param fromVersionInclusive The version of the stream to start reading from. Use StreamVersion.End to read from the end
//     * @param maxCount maximum number of events to read
//     * @param prefetch Prefetches the message data as part of the page read. This means a single request to the server but a higher payload size.
//     * @return An @{link ReadAllPage} presenting the result of the read. If all messages read have expired then the message collection MAY be empty.
//     */
//    ReadStreamPage readStreamBackwards(String streamId,
//                                       int fromVersionInclusive,
//                                       int maxCount,
//                                       boolean prefetch) throws SQLException;
//
//
//    /**
//     * Reads the head position (the position of the very latest message).
//     * @return the head position
//     */
//    Long readHeadPosition();
//
//    /**
//     * Gets the stream metadata
//     * @param streamId The stream ID whose metadata is to be read.
//     */
//    StreamMetadataResult getStreamMetadata(String streamId) throws SQLException;


}
