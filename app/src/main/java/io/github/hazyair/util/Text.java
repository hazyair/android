package io.github.hazyair.util;

public class Text {
    private final static String ELLIPSIS = "...";

    public static String truncateSting(String string, int length) {
        if (string == null) return null;
        return string.length() < length ? string : string.substring(0, length - 3) + ELLIPSIS;
    }
}
