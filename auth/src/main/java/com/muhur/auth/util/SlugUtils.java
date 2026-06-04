package com.muhur.auth.util;

import java.util.Locale;

public class SlugUtils {

    private SlugUtils() {
    }

    public static String toSlug(String input) {
        return input.toLowerCase(Locale.ROOT)
                .trim()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-");
    }
}
