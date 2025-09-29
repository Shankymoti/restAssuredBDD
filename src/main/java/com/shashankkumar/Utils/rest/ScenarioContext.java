package com.shashankkumar.Utils.rest;

import lombok.Data;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class ScenarioContext {

    // Thread-local RestHelper to ensure each thread has its own instance
    private static ThreadLocal<RestHelper> restHelper = new ThreadLocal<>();

    // API name
    private static String apiName;

    // Thread-safe context variable storage
    private static Map<String, Object> contextVariables = new ConcurrentHashMap<>();

    // Get a value from context
    public static Object getContextVariable(String key) {
        return contextVariables.get(key);
    }

    // Set a value in context
    public static void setContextVariable(String key, Object value) {
        contextVariables.put(key, value);
    }

    // Thread-safe update of a Map in context
    public static void updateContextVariable(String key, Map<String, String> newValue) {
        contextVariables.compute(key, (k, existingValue) -> {
            if (existingValue == null) {
                return newValue; // Add new map if key doesn't exist
            }
            if (existingValue instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, String> existingMap = (Map<String, String>) existingValue;
                existingMap.putAll(newValue); // Merge maps
                return existingMap;
            }
            throw new IllegalArgumentException(
                    "Existing value for key " + key + " is not a Map."
            );
        });
    }

    // Getter and Setter for RestHelper
    public static RestHelper getRestHelper() {
        return restHelper.get();
    }

    public static void setRestHelper(RestHelper helper) {
        restHelper.set(helper);
    }

    public static void removeRestHelper() {
        restHelper.remove(); // Prevent memory leaks
    }
}
