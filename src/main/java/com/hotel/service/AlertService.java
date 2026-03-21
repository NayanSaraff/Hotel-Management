package com.hotel.service;

import com.hotel.dao.BookingDAO;
import com.hotel.dao.InventoryDAO;
import com.hotel.model.Booking;
import com.hotel.model.InventoryItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * AlertService — provides real-time alerts for:
 * 1. Low inventory stock items
 * 2. Bookings due for checkout today
 * 3. Check-ins due today
 */
public class AlertService {

    private static final Logger logger = LoggerFactory.getLogger(AlertService.class);
    private final InventoryDAO inventoryDAO = new InventoryDAO();
    private final BookingDAO   bookingDAO   = new BookingDAO();

    // ── Low Stock Alerts ──────────────────────────────────────────────────

    public List<InventoryItem> getLowStockItems() {
        return inventoryDAO.findLowStock();
    }

    public boolean hasLowStock() {
        return !inventoryDAO.findLowStock().isEmpty();
    }

    public String getLowStockSummary() {
        List<InventoryItem> items = getLowStockItems();
        if (items.isEmpty()) return null;
        StringBuilder sb = new StringBuilder("⚠️ Low Stock Alert!\n\n");
        for (InventoryItem item : items) {
            sb.append("• ").append(item.getItemName())
              .append(": ").append(item.getQuantityAvailable())
              .append(" ").append(item.getUnit())
              .append(" (min: ").append(item.getMinimumThreshold()).append(")\n");
        }
        return sb.toString();
    }

    // ── Today's Checkout Alerts ───────────────────────────────────────────

    public List<Booking> getTodayCheckouts() {
        return bookingDAO.findTodayCheckouts();
    }

    public boolean hasTodayCheckouts() {
        return !bookingDAO.findTodayCheckouts().isEmpty();
    }

    public String getTodayCheckoutSummary() {
        List<Booking> checkouts = getTodayCheckouts();
        if (checkouts.isEmpty()) return null;
        StringBuilder sb = new StringBuilder("🚪 Checkouts Due Today!\n\n");
        for (Booking b : checkouts) {
            sb.append("• ").append(b.getBookingReference())
              .append(" | ").append(b.getCustomerName())
              .append(" | Room ").append(b.getRoomNumber()).append("\n");
        }
        return sb.toString();
    }

    // ── Today's Check-in Alerts ───────────────────────────────────────────

    public List<Booking> getTodayCheckIns() {
        return bookingDAO.findTodayCheckIns();
    }

    public String getTodayCheckInSummary() {
        List<Booking> checkIns = getTodayCheckIns();
        if (checkIns.isEmpty()) return null;
        StringBuilder sb = new StringBuilder("✅ Check-Ins Expected Today!\n\n");
        for (Booking b : checkIns) {
            sb.append("• ").append(b.getBookingReference())
              .append(" | ").append(b.getCustomerName())
              .append(" | Room ").append(b.getRoomNumber()).append("\n");
        }
        return sb.toString();
    }

    // ── Combined Alert Summary ────────────────────────────────────────────

    /**
     * Returns combined alert message, or null if no alerts.
     */
    public String getAllAlertsSummary() {
        StringBuilder sb = new StringBuilder();
        String stock    = getLowStockSummary();
        String checkOut = getTodayCheckoutSummary();
        String checkIn  = getTodayCheckInSummary();

        if (stock    != null) sb.append(stock).append("\n");
        if (checkOut != null) sb.append(checkOut).append("\n");
        if (checkIn  != null) sb.append(checkIn);

        return sb.length() > 0 ? sb.toString().trim() : null;
    }

    public int getTotalAlertCount() {
        return getLowStockItems().size() +
               getTodayCheckouts().size() +
               getTodayCheckIns().size();
    }
}
