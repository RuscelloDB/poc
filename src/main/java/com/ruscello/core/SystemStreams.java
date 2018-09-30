package com.ruscello.core;

import com.google.common.base.Strings;

public class SystemStreams {

    public static final String PERSISTENT_SUBSCRIPTION_CONFIG = "$persistentSubscriptionConfig";
    public static final String ALL_STREAM = "$all";
    public static final String STREAMS_STREAM = "$streams"; // huh?? seems like a weird name
    public static final String SETTINGSS_STREAM = "$settings";
    public static final String STATS_STREAM_PREFIX = "$stats";
    public static final String SCAVENGE_STREAM = "$scavenges";

    private SystemStreams() {
        // statics only
    }

    public static boolean isSystemStream(String streamId) {
        return !Strings.isNullOrEmpty(streamId) && streamId.charAt(0) == '$';
    }

    public static String metastreamOf(String streamId) {
        return "$$" + streamId;
    }

    public static boolean isMetastream(String streamId) {
        return !Strings.isNullOrEmpty(streamId)
                && streamId.length() >= 2
                && streamId.charAt(0) == '$'
                && streamId.charAt(1) == '$';
    }

    public static String originalStreamOf(String metastreamId) {
        return metastreamId.substring(2);
    }

}
