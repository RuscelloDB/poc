package com.ruscello.exceptions;

/**
 * The base class of all Ruscello exceptions
 */
public class RuscelloException extends RuntimeException {

    private final static long serialVersionUID = 1L;

    public RuscelloException(String message, Throwable cause) {
        super(message, cause);
    }

    public RuscelloException(String message) {
        super(message);
    }

    public RuscelloException(Throwable cause) {
        super(cause);
    }

    public RuscelloException() {
        super();
    }

}