package com.hotel.service;

import com.hotel.util.DatabaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;
import java.util.Random;

/**
 * Service for generating scratch coupons after checkout
 * and validating/applying them on future bookings.
 */
public class CouponService {

    private static final Logger logger = LoggerFactory.getLogger(CouponService.class);
    private static final String[] PREFIXES = {"SAVE", "GRAND", "HOTEL", "VIP", "LUCKY"};
    private static final int[] DISCOUNT_VALUES = {5, 10, 15, 20, 25}; // percent

    // ── Generate Coupon after Checkout ────────────────────────────────────

    /**
     * Generate a scratch coupon for a guest after checkout.
     * Returns the coupon code string.
     */
    public String generateCheckoutCoupon(int customerId, String bookingRef) {
        Random random = new Random();
        String prefix   = PREFIXES[random.nextInt(PREFIXES.length)];
        int discount    = DISCOUNT_VALUES[random.nextInt(DISCOUNT_VALUES.length)];
        String code     = prefix + discount + "-" + generateRandomCode(6);
        LocalDate expiry = LocalDate.now().plusDays(90); // valid 90 days

        String sql = "INSERT INTO COUPONS (COUPON_CODE, DISCOUNT_PERCENT, CUSTOMER_ID, " +
                     "BOOKING_REF, EXPIRY_DATE, IS_USED) VALUES (?,?,?,?,?,0)";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, code);
            ps.setInt(2, discount);
            ps.setInt(3, customerId);
            ps.setString(4, bookingRef);
            ps.setDate(5, Date.valueOf(expiry));
            ps.executeUpdate();
            DatabaseConnection.commit();
            logger.info("Coupon {} ({}% off) generated for customer {}", code, discount, customerId);
            return code;
        } catch (SQLException e) {
            DatabaseConnection.rollback();
            logger.error("Error generating coupon: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Validate a coupon code. Returns discount percent, or 0 if invalid.
     */
    public int validateCoupon(String couponCode) {
        String sql = "SELECT DISCOUNT_PERCENT, EXPIRY_DATE, IS_USED FROM COUPONS WHERE COUPON_CODE=?";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, couponCode.toUpperCase().trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int isUsed  = rs.getInt("IS_USED");
                    Date expiry = rs.getDate("EXPIRY_DATE");
                    if (isUsed == 1) return -1;   // already used
                    if (expiry.toLocalDate().isBefore(LocalDate.now())) return -2; // expired
                    return rs.getInt("DISCOUNT_PERCENT");
                }
            }
        } catch (SQLException e) {
            logger.error("Error validating coupon: {}", e.getMessage());
        }
        return 0; // not found
    }

    /**
     * Apply coupon — mark as used and return discounted amount.
     */
    public double applyCoupon(String couponCode, double originalAmount) {
        int discount = validateCoupon(couponCode);
        if (discount <= 0) return originalAmount;

        double discountAmt = originalAmount * discount / 100.0;
        double finalAmount = originalAmount - discountAmt;

        // Mark coupon as used
        String sql = "UPDATE COUPONS SET IS_USED=1, USED_DATE=SYSDATE WHERE COUPON_CODE=?";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, couponCode.toUpperCase().trim());
            ps.executeUpdate();
            DatabaseConnection.commit();
            logger.info("Coupon {} applied: {}% off on ₹{} → ₹{}",
                    couponCode, discount, originalAmount, finalAmount);
        } catch (SQLException e) {
            DatabaseConnection.rollback();
            logger.error("Error applying coupon: {}", e.getMessage());
        }
        return finalAmount;
    }

    /**
     * Get coupon details as string for display.
     */
    public String getCouponDetails(String couponCode) {
        String sql = "SELECT DISCOUNT_PERCENT, EXPIRY_DATE, IS_USED FROM COUPONS WHERE COUPON_CODE=?";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, couponCode.toUpperCase().trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int pct    = rs.getInt("DISCOUNT_PERCENT");
                    Date exp   = rs.getDate("EXPIRY_DATE");
                    int isUsed = rs.getInt("IS_USED");
                    return String.format("%d%% discount | Expiry: %s | Status: %s",
                            pct, exp.toLocalDate(), isUsed == 1 ? "Used" : "Valid");
                }
            }
        } catch (SQLException e) {
            logger.error("Error fetching coupon: {}", e.getMessage());
        }
        return "Coupon not found";
    }

    private String generateRandomCode(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < length; i++) sb.append(chars.charAt(random.nextInt(chars.length())));
        return sb.toString();
    }
}
