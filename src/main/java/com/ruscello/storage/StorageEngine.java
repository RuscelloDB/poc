package com.ruscello.storage;

// Whats the appropriate API abstraction here? Should it match our higher level operations?

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

/**
 * LMBD doesn't have the concept of column families or keyspaces so the number of dbs is based on the number
 * of keyspaces we need which includes
 * Main stream based on based on hash
 * stream names - where to put ngrams for allow for stream name search?
 */

// TODO: StreamId should be either core domain object or hash
// TODO: fix javadoc
// TODO: the names dont need to be domain oriented as the storage engine is internal. The ruscello client will be the
// TODO: exposing data base / storage engines stats - check what MyRocks, Influxdb, MongoDB, cockroachDB or others with pluggable storage engines do
// the think that exposes these specific method names
// I think these methods will require keyspace to be passed in
// Food for thought...what if we had a storage engine that didnt support multiple keyspaces or if we wanted to change one
// our existing engines to only have one keyspace?
// The one keyspace is easy...always pass the one default keyspace in.
// is there a way to separate/split keyspaced storage engines from non-keyspaced?
// This is YAGNI but worth thinking about.
public interface StorageEngine extends ReadOnlyStorageEngine {


    // TODO: do we perhaps need to separate this into another interface?
    void appendToStream(String streamId,
                        int expectedVersion,
                        byte[] message);

    // TODO: maybe change to byte[][]
    void appendToStream(String streamId,
                        int expectedVersion,
                        List<byte[]> message);


    // TODO: add metadata methods

    // TODO: do we want to support deletes?
    /**
     * Hard deletes a stream and all of its messages. Deleting a stream will result in a '$stream-deleted'
     * message being appended to the '$deleted' stream. See Deleted.StreamDeleted for the
     * message structure.
     * @param streamId The stream Id to delete.
     * @param expectedVersion The stream expected version. See ExpectedVersion for const values.
     */
    void deleteStream(String streamId, int expectedVersion) throws SQLException;


    /**
     * Hard deletes a message from the stream. Deleting a message will result in a '$message-deleted'
     * message being appended to the '$deleted' stream. See Deleted.MessageDeleted for the
     * message structure.
     * @param streamId stream to delete from
     * @param messageId The message to delete. If the message doesn't exist then nothing happens.
     */
    void deleteMessage(String streamId, UUID messageId) throws SQLException;

}
