package com.hotel.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Utility for GST (Goods and Services Tax) calculations in India.
 * Room tariff GST slabs:
 *   < 1000/night  → 0%
 *   1000–7499     → 12%
 *   >= 7500       → 18%
 */
public class GSTCalculator {

    public static final double GST_RATE_ZERO  = 0.00;
    public static final double GST_RATE_12    = 0.12;
    public static final double GST_RATE_18    = 0.18;

    /**
     * Determine applicable GST rate based on room tariff per night.
     */
    public static double getGSTRate(double tariffPerNight) {
        if (tariffPerNight < 1000) return GST_RATE_ZERO;
        if (tariffPerNight < 7500) return GST_RATE_12;
        return GST_RATE_18;
    }

    /**
     * Calculate GST amount.
     */
    public static double calculateGST(double baseAmount, double tariffPerNight) {
        double rate = getGSTRate(tariffPerNight);
        return round(baseAmount * rate);
    }

    /**
     * Calculate total amount including GST.
     */
    public static double calculateTotal(double baseAmount, double tariffPerNight) {
        double gst = calculateGST(baseAmount, tariffPerNight);
        return round(baseAmount + gst);
    }

    private static double round(double value) {
        return BigDecimal.valueOf(value)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
