package com.ruscello.storage.lmdb;

// this might not be a good idea
public interface LmdbConfiguration {

    String path();
    int maxKeySize();
    // do we want to expore max readers?
    // int maxReaders();
}
