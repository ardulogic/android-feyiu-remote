package com.feyiuremote.libs.Cameras.Panasonic.Values;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class PanasonicApertures {
    private static final Map<String, String> apertures = new HashMap<>();

    static {
        apertures.put("1", "0/256");
        apertures.put("1.1", "85/256");
        apertures.put("1.2", "171/256");
        apertures.put("1.4", "256/256");
        apertures.put("1.6", "341/256");
        apertures.put("1.8", "427/256");
        apertures.put("2", "512/256");
        apertures.put("2.2", "597/256");
        apertures.put("2.4", "640/256");
        apertures.put("2.8", "768/256");
        apertures.put("3.2", "853/256");
        apertures.put("3.5", "939/256");
        apertures.put("4", "1024/256");
        apertures.put("4.5", "1110/256");
        apertures.put("5", "1195/256");
        apertures.put("5.6", "1280/256");
        apertures.put("6.3", "1364/256");
        apertures.put("7.1", "1451/256");
        apertures.put("8", "1536/256");
        apertures.put("9", "1621/256");
        apertures.put("10", "1707/256");
        apertures.put("11", "1792/256");
        apertures.put("13", "1877/256");
        apertures.put("14", "1963/256");
        apertures.put("16", "2048/256");
        apertures.put("18", "2133/256");
        apertures.put("20", "2219/256");
        apertures.put("22", "2304/256");
    }

    public static String getControlValue(String aperture) {
        return apertures.get(aperture);
    }

    public static BigDecimal calculateApertureValue(String input) {
        // Extract the numerator from the input string (e.g., "X/256")
        String numeratorString = input.substring(0, input.indexOf('/'));
        BigDecimal numerator = new BigDecimal(numeratorString);
        BigDecimal denominator = new BigDecimal("256");

        // Calculate the aperture value using the formula
        BigDecimal aperture = numerator.divide(denominator, 10, BigDecimal.ROUND_HALF_UP);

        return aperture;
    }
}
