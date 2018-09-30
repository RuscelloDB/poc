package com.ruscello.storage;

import java.util.Arrays;
import java.util.List;

// TODO: should this be an eum?

/**
 * Contains list of all key spaces.
 * We use the term "key space" as different storage engines use different terminology and key space seems the most
 * generically accurate for our use case.
 * Key spaces represent indexes.
 */
public class KeySpaces {

    private KeySpaces() {
        // statics only
    }

    // TODO: what should the name of the main lexiographically ordered keyspace be?
    /**
     * default (hash of stream name + delimiter (@ like eventstore) + sequence number of stream)
     *
     */
    public static String MAIN = "main";


    // TODO: should this be in a separate keyspace part of the default main?
    // like with any system related key/values we can prefix/encode it
    public static final String STREAM_METADATA = "stream_metadata";

    /**
     * total ordered based on (global) storage sequence number.
     * The name sucks. global sequence number may be better?
     */
    public static final String TOTAL_ORDERED = "total_ordered";

    /**
     * TODO: should this also includes ngrams?
     * My initial thought is yes keep them in the same keyspace but with a special prefix/suffix with the values
     * pointing to the actual stream name key
     * What about streams that will have the same ngrams? What is the key design? Also what if we delete the stream?
     * how to find all the keys to delete.
     * if we just want to support prefix searching then Ngrams would not be necessary
     */
    public static final String STREAM_NAMES = "stream_names";

    public static final String PERSISTENT_SUBSCRIPTIONS = "persistent_subscriptions";

    public static final String USERS = "users";

    public static final List<String> ALL_STREAM_NAMES = Arrays.asList(MAIN, TOTAL_ORDERED, STREAM_NAMES, PERSISTENT_SUBSCRIPTIONS);

}
