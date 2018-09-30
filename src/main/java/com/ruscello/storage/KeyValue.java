package com.ruscello.storage;

import java.util.Objects;

/**
 * A key-value pair defined for a single Kafka Streams record.
 * If the record comes directly from a Kafka topic then its key/value are defined as the message key/value.
 *
 * @param <K> Key type
 * @param <V> Value type
 */
public class KeyValue<K, V> {

    /** The key of the key-value pair. */
    public final K key;
    /** The value of the key-value pair. */
    public final V value;

    /**
     * Create a new key-value pair.
     *
     * @param key   the key
     * @param value the value
     */
    public KeyValue(final K key, final V value) {
        this.key = key;
        this.value = value;
    }

    /**
     * Create a new key-value pair.
     *
     * @param key   the key
     * @param value the value
     * @param <K>   the type of the key
     * @param <V>   the type of the value
     * @return a new key-value pair
     */
    public static <K, V> KeyValue<K, V> pair(final K key, final V value) {
        return new KeyValue<>(key, value);
    }

    @Override
    public String toString() {
        return "KeyValue(" + key + ", " + value + ")";
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj)
            return true;

        if (!(obj instanceof KeyValue)) {
            return false;
        }

        final KeyValue other = (KeyValue) obj;
        return (key == null ? other.key == null : key.equals(other.key))
                && (value == null ? other.value == null : value.equals(other.value));
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, value);
    }

}