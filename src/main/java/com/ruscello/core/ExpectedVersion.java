package com.ruscello.core;

/**
 * ExpectedVersion.Any	This disables the optimistic concurrency check.
 * ExpectedVersion.NoStream	This specifies the expectation that target stream does not yet exist.
 * ExpectedVersion.EmptyStream	This specifies the expectation that the target stream has been explicitly created, but does not yet have any user events written in it.
 * ExpectedVersion.StreamExists	This specifies the expectation that the target stream or its metadata stream has been created, but does not expect the stream to be at a specific event number.
 * Any other integer value	The event number that you expect the stream to currently be at.
 */
public class ExpectedVersion {

    public static final long ANY = -2;
    public static final long NO_STRREAM = -1;
    //public const int EMPTY_STREAM = 0;
    public static final long INVALID = -3;
    public static final long STREAM_EXISTS = -4;


    private ExpectedVersion() {
        // statics only
    }

}