package io.github.hazyair.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

// http://ec.europa.eu/environment/air/quality/standards.htm
// http://powietrze.gios.gov.pl/pjp/content/health_informations
// http://apps.who.int/iris/bitstream/handle/10665/69477/WHO_SDE_PHE_OEH_06.02_eng.pdf
// http://www.euro.who.int/__data/assets/pdf_file/0009/128169/e94535.pdf
// http://www.euro.who.int/__data/assets/pdf_file/0005/74732/E71922.pdf

// PM2.5:
// WHO:    10 µg/m³ annual mean, 25 µg/m³ 24-hour mean
// EU:     25 µg/m³ annual mean, 20 µg/m³ 3-year mean, 18 µg/m³ reduction target
// Poland: 25 µg/m³ annual mean, 25 µg/m³ mean of 'good' in quality index

// PM10:
// WHO:    20 µg/m³ annual mean, 50 µg/m³ 24-hour mean
// EU:     40 µg/m³ annual mean, 50 µg/m³ 24-hour mean
// Poland: 40 µg/m³ annual mean, 50 µg/m³ 24-hour mean, 40 µg/m³ mean of 'good' in quality index

// C₆H₆ (benzene):
// WHO:    Undefined due to no safe level exposure
// EU:     5 µg/m³ annual mean
// Poland: 5 µg/m³ annual mean, 9 µg/m³ mean of 'good' in quality index

// O₃ (ozone):
// WHO:    100 µg/m³ 8-hour mean
// EU:     120 µg/m³ 8-hour mean
// Poland: 120 µg/m³ 8-hour mean, 100 µg/m³ mean of 'good' in quality index

// NO₂ (nitrogen dioxide):
// WHO:    40 µg/m³ annual mean, 200 µg/m³ 1-hour mean
// EU:     40 µg/m³ annual mean, 200 µg/m³ 1-hour mean
// Poland: 40 µg/m³ annual mean, 200 µg/m³ 1-hour mean,  70 µg/m³ mean of 'good' in quality index

// SO₂ (sulphur dioxide):
// WHO:    20 µg/m³ 24-hour mean, 500 µg/m³ 10-minute mean
// EU:     125 µg/m³ 24-hour mean, 350 µg/m³ 1-hour mean
// Poland: 125 µg/m³ 24-hour mean, 350 µg/m³ 1-hour mean, 75 µg/m³ mean of 'good' in quality index

// CO (carbon monoxide):
// WHO:    7 mg/m³ 24-hour mean, 10 mg/m³ 8-hour mean, 35 mg/m³ 1-hour mean, 100 mg/m³ 15-min mean
// EU:     10 mg/m³ 8-hour mean
// Poland: 5 mg/m³ mean of 'good' in quality index

public final class Quality {
    private static final Map<String, Double> limits = Collections.unmodifiableMap(
            new HashMap<String, Double>() {{
                put("C₆H₆", 5.0);
                put("CO", 7000.0);
                put("NO₂", 40.0);
                put("O₃", 100.0);
                put("PM10", 40.0);
                put("PM2.5", 25.0);
                put("SO₂", 75.0);
            }});

    public static int normalize(String parameter, double value) {
        return (int)Math.round(100 * value / limits.get(parameter));
    }
}
