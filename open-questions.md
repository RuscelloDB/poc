how to handle configuration between all the different storage engines?
should be have a configuration class that contains common stuff and then
nested classes for each storage engine?
I dont think there is a common interface/class that can be used?


http://www.dataengconf.com/influxdb-storage-engine-internals

// open source single node
// commercial adds clustering?
// what does eventstore do oss vs paid commercial?