package io.github.hazyair.util;

import org.apache.commons.lang3.StringUtils;

import java.text.Normalizer;

public class Text {
    private final static String ELLIPSIS = "...";

    public static String truncateSting(String string, int length) {
        if (string == null) return null;
        return string.length() < length ? string : string.substring(0, length - 3) + ELLIPSIS;
    }

    public static boolean contains(String string, String query) {
        return StringUtils.containsIgnoreCase(
                Normalizer.normalize(string, Normalizer.Form.NFD)
                        .replaceAll("\\p{InCombiningDiacriticalMarks}+",
                                "").replaceAll("ł", "l")
                        .replaceAll("Ł", "L"), query);
    }
}
