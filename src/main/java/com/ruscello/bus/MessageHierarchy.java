package com.ruscello.bus;

import java.util.HashMap;
import java.util.Map;

public class MessageHierarchy {

    public static final Map<Class<?>, int[]> DescendantsByType;
    static {
        DescendantsByType = new HashMap<>();
    }
}
