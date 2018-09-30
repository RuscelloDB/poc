package com.ruscello;

public class ApplicationInitializationException extends Exception {
    public ApplicationInitializationException(String message) {
        super(message);
    }
    public ApplicationInitializationException(String message, Exception innerException) {
        super(message, innerException);
    }

}
