package com.ruscello.storage;

import java.io.Closeable;
import java.util.Iterator;

/**
 * Iterator interface of {@link KeyValue}.
 *
 * Users must call its {@code close} method explicitly upon completeness to release resources,
 * or use try-with-resources statement (available since JDK7) for this {@link Closeable} class.
 *
 * @param <K> Type of keys
 * @param <V> Type of values
 */
public interface StoreIterator<K, V> extends Iterator<KeyValue<K, V>>, Closeable {

    @Override
    void close();

    /**
     * Peek at the next key without advancing the iterator
     * @return the key of the next value that would be returned from the next call to next
     */
    K peekNextKey();
}