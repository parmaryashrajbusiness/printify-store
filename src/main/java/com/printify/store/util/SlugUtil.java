package com.printify.store.util;

public class SlugUtil {

    private SlugUtil() {}

    public static String toSlug(String value) {
        return value == null ? "" :
                value.toLowerCase()
                        .trim()
                        .replaceAll("[^a-z0-9\\s-]", "")
                        .replaceAll("\\s+", "-")
                        .replaceAll("-+", "-");
    }
}