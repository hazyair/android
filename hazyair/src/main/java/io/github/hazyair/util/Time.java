package io.github.hazyair.util;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

public final class Time {
    public static long getTimestamp(long timestamp) {
        return new DateTime(timestamp, DateTimeZone.forID("UTC"))
                .withZone(DateTimeZone.getDefault()).getMillis();
    }
}
