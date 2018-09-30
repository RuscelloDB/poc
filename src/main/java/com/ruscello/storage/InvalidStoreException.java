package com.ruscello.storage;

import com.ruscello.exceptions.RuscelloException;

/**
 * Indicates that there was a problem when trying to access a
 * {@link org.apache.kafka.streams.processor.StateStore StateStore}, i.e, the Store is no longer valid because it is
 * closed or doesn't exist any more due to a rebalance.
 * <p>
 * These exceptions may be transient, i.e., during a rebalance it won't be possible to query the stores as they are
 * being (re)-initialized. Once the rebalance has completed the stores will be available again. Hence, it is valid
 * to backoff and retry when handling this exception.
 */
public class InvalidStoreException extends RuscelloException {

    private final static long serialVersionUID = 1L;

    public InvalidStoreException(final String message) {
        super(message);
    }

    public InvalidStoreException(final String message, final Throwable throwable) {
        super(message, throwable);
    }

    public InvalidStoreException(final Throwable throwable) {
        super(throwable);
    }

}
