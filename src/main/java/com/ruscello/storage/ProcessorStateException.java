package com.ruscello.storage;

import com.ruscello.exceptions.RuscelloException;

// TODO: Needs a more appropriate name for ruscello
/**
 * Indicates a processor state operation (e.g. put, get) has failed.
 *
 * @see org.apache.kafka.streams.processor.StateStore
 */
public class ProcessorStateException extends RuscelloException {

    private final static long serialVersionUID = 1L;

    public ProcessorStateException(final String message) {
        super(message);
    }

    public ProcessorStateException(final String message, final Throwable throwable) {
        super(message, throwable);
    }

    public ProcessorStateException(final Throwable throwable) {
        super(throwable);
    }
}