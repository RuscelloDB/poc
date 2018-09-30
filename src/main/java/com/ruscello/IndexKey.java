package com.ruscello;

// From EventStore Architecture doc
// Each record in the index is 16 bytes:
// 4 bytes for the hash of the stream ID --> uint / uint32
// 4 bytes for the sequence number --> Int
// 8 bytes for the original position the record had in the log. --> Long

// Given we have multiple indices this likely doesnt make sense to keep in current form
public class IndexKey {
    private String streamId;
    private long version;
    // public long Position; --> we dont have this at the time we insert
    // public ulong Hash;
    private long hash;

//    public IndexKey(String streamId, long version, long position) : this(streamId, version, position, 0) { }
//    public IndexKey(string streamId, long version, long position, ulong hash)
//    {
//        StreamId = streamId;
//        Version = version;
//        Position = position;
//
//        Hash = hash;
//    }

    public IndexKey(String streamId, long version) {
        this(streamId, version, 0);
    }

    public IndexKey(String streamId, long version, long hash) {
        this.streamId = streamId;
        this.version = version;
        this.hash = hash;
    }

    public String getStreamId() {
        return streamId;
    }

    public void setStreamId(String streamId) {
        this.streamId = streamId;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public long getHash() {
        return hash;
    }

    public void setHash(long hash) {
        this.hash = hash;
    }
}
