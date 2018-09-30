package com.ruscello.storage.rocksdb;

// https://github.com/Instagram/cassandra/blob/rocks_3.0/src/java/org/apache/cassandra/rocksdb/RocksDBConfigs.java
// https://github.com/influxdata/influxdb/blob/v0.8.8/datastore/storage/engine.go
public interface RocksdbConfiguration {

    // TODO: what else should we make accessible to users?

    String path();

    // useFixedLengthPrefixExtractor
// create if missing
//    private MemTableConfig memTableConfig_;
//    private TableFormatConfig tableFormatConfig_;
//    private RateLimiter rateLimiter_;
//    private AbstractComparator<? extends AbstractSlice<?>> comparator_;
//    private CompactionOptionsUniversal compactionOptionsUniversal_;
//    private CompactionOptionsFIFO compactionOptionsFIFO_;
//    private CompressionOptions compressionOptions_;
//    private Cache rowCache_;
    // max open files
    // wal dir
    // total wal size
    // setIncreaseParallelism
    // max file opening threads
    // use fsync
    // setDeleteObsoleteFilesPeriodMicros
    // setMaxBackgroundCompactions
    // maxBackgroundJobs
    // setMaxLogFileSize
    // setLogFileTimeToRoll
    // walTtlSeconds
    // walSizeLimitMB
    // setUseDirectReads


    // flush options
    //  db.flush(new FlushOptions().setWaitForFlush(true));
}
