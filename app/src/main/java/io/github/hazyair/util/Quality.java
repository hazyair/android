package io.github.hazyair.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Quality {
    private static final Map<String, Double> limits = Collections.unmodifiableMap(
            new HashMap<String, Double>() {{
                put("C₆H₆", 5.0);
                put("CO", 23000.0);
                put("NO₂", 40.0);
                put("O₃", 120.0);
                put("PM10", 40.0);
                put("PM2.5", 25.0);
                put("SO₂", 125.0);
            }});

    public static int normalize(String parameter, double value) {
        return (int)(100 * value / limits.get(parameter));
    }
}
