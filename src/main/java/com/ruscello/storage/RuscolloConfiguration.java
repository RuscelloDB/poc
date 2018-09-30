package com.ruscello.storage;

import com.ruscello.storage.lmdb.LmdbConfiguration;
import com.ruscello.storage.rocksdb.RocksdbConfiguration;

public interface RuscolloConfiguration extends LmdbConfiguration, RocksdbConfiguration {

    // /var/lib/mysql
    // /var/lib/postgresql/9.5/main
    // /var/lib/elasticsearch/data - ubuntu
    // /var/lib/elasticsearch - centos
    // Data: /var/lib/eventstore
    // Application Logs: /var/log/eventstore
    // /var/lib/mongodb/
    //  "/var/lib/influxdb/data" - https://docs.influxdata.com/influxdb/v1.6/administration/config/

    // where to put configurtion, log files, etc

    /**
     *
     * @return path to the database
     */
    String dataDir();

    /**
     *
     * @return path to where logs should be written to
     */
    String loggingDir();

    /**
     *
     * @return The storage engine to use
     */
    String storageEngine();


}
