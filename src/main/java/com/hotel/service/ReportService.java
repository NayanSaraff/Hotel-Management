package com.hotel.service;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hotel.dao.BookingDAO;
import com.hotel.dao.PaymentDAO;
import com.hotel.dao.RoomDAO;
import com.hotel.model.Room;
import com.hotel.util.DatabaseConnection;

/**
 * Service for generating statistical reports and analytics.
 */
public class ReportService {

    private static final Logger logger = LoggerFactory.getLogger(ReportService.class);

    private final BookingDAO bookingDAO = new BookingDAO();
    private final RoomDAO    roomDAO    = new RoomDAO();
    private final PaymentDAO paymentDAO = new PaymentDAO();

    // ── Revenue ───────────────────────────────────────────────────────────

    /** Monthly revenue for the past N months as a LinkedHashMap<Month, Amount>. */
    public Map<String, Double> getMonthlyRevenue(int months) {
        Map<String, Double> data = new LinkedHashMap<>();
        String sql = "SELECT TO_CHAR(PAYMENT_DATE,'Mon-YYYY') AS MONTH, " +
                     "SUM(AMOUNT) AS REVENUE " +
                     "FROM PAYMENTS " +
                     "WHERE PAYMENT_TYPE <> 'REFUND' " +
                     "AND PAYMENT_DATE >= TRUNC(ADD_MONTHS(TRUNC(SYSDATE,'MM'), ?), 'MM') " +
                     "GROUP BY TO_CHAR(PAYMENT_DATE,'Mon-YYYY'), TRUNC(PAYMENT_DATE,'MM') " +
                     "ORDER BY TRUNC(PAYMENT_DATE,'MM')";
        try {
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
                ps.setInt(1, -(months - 1));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        data.put(rs.getString("MONTH"), rs.getDouble("REVENUE"));
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Error fetching monthly revenue: {}", e.getMessage());
        } finally {
            DatabaseConnection.closeConnection();
        }
        return data;
    }

    /** Revenue breakdown by room category. */
    public Map<String, Double> getRevenueByCategory() {
        Map<String, Double> data = new LinkedHashMap<>();
        String sql = "SELECT r.CATEGORY, SUM(b.TOTAL_AMOUNT) AS REVENUE " +
                     "FROM BOOKINGS b JOIN ROOMS r ON b.ROOM_ID=r.ROOM_ID " +
                     "WHERE b.STATUS IN ('CHECKED_OUT','CHECKED_IN') " +
                     "GROUP BY r.CATEGORY ORDER BY REVENUE DESC";
        try {
            try (Statement st = DatabaseConnection.getConnection().createStatement();
                 ResultSet rs = st.executeQuery(sql)) {
                while (rs.next()) {
                    data.put(rs.getString("CATEGORY"), rs.getDouble("REVENUE"));
                }
            }
        } catch (SQLException e) {
            logger.error("Error fetching revenue by category: {}", e.getMessage());
        } finally {
            DatabaseConnection.closeConnection();
        }
        return data;
    }

    // ── Occupancy ─────────────────────────────────────────────────────────

    /** Occupancy rate as a percentage (0–100). */
    public double getOccupancyRate() {
        int total    = roomDAO.findAll().size();
        int occupied = roomDAO.countByStatus(Room.Status.OCCUPIED);
        if (total == 0) return 0;
        return (occupied * 100.0) / total;
    }

    /** Daily occupancy for a date range. */
    public Map<LocalDate, Integer> getDailyOccupancy(LocalDate from, LocalDate to) {
        Map<LocalDate, Integer> data = new LinkedHashMap<>();
        String sql = "SELECT TRUNC(b.ACTUAL_CHECK_IN) AS DAY, COUNT(*) AS CNT " +
                     "FROM BOOKINGS b WHERE b.STATUS IN ('CHECKED_IN','CHECKED_OUT') " +
                     "AND TRUNC(b.ACTUAL_CHECK_IN) BETWEEN ? AND ? " +
                     "GROUP BY TRUNC(b.ACTUAL_CHECK_IN) ORDER BY DAY";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setDate(1, java.sql.Date.valueOf(from));
            ps.setDate(2, java.sql.Date.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    data.put(rs.getDate("DAY").toLocalDate(), rs.getInt("CNT"));
                }
            }
        } catch (SQLException e) {
            logger.error("Error fetching daily occupancy: {}", e.getMessage());
        }
        return data;
    }

    // ── Booking Stats ─────────────────────────────────────────────────────

    public Map<String, Integer> getBookingsByStatus() {
        Map<String, Integer> data = new LinkedHashMap<>();
        String sql = "SELECT STATUS, COUNT(*) AS CNT FROM BOOKINGS GROUP BY STATUS";
        try {
            try (Statement st = DatabaseConnection.getConnection().createStatement();
                 ResultSet rs = st.executeQuery(sql)) {
                while (rs.next()) {
                    data.put(rs.getString("STATUS"), rs.getInt("CNT"));
                }
            }
        } catch (SQLException e) {
            logger.error("Error fetching bookings by status: {}", e.getMessage());
        } finally {
            DatabaseConnection.closeConnection();
        }
        return data;
    }

    /** Top 5 most-booked rooms. */
    public Map<String, Integer> getTopBookedRooms() {
        Map<String, Integer> data = new LinkedHashMap<>();
        String sql = "SELECT * FROM ( " +
                     "  SELECT r.ROOM_NUMBER, COUNT(b.BOOKING_ID) AS BOOKINGS " +
                     "  FROM ROOMS r JOIN BOOKINGS b ON r.ROOM_ID=b.ROOM_ID " +
                     "  GROUP BY r.ROOM_NUMBER " +
                     "  ORDER BY BOOKINGS DESC " +
                     ") WHERE ROWNUM <= 5";
        try {
            try (Statement st = DatabaseConnection.getConnection().createStatement();
                 ResultSet rs = st.executeQuery(sql)) {
                while (rs.next()) {
                    data.put(rs.getString("ROOM_NUMBER"), rs.getInt("BOOKINGS"));
                }
            }
        } catch (SQLException e) {
            logger.error("Error fetching top booked rooms: {}", e.getMessage());
        } finally {
            DatabaseConnection.closeConnection();
        }
        return data;
    }

    // ── Summary Card Values ───────────────────────────────────────────────

    public double getTodayRevenue() {
        String sql = "SELECT NVL(SUM(p.AMOUNT),0) FROM PAYMENTS p " +
                     "WHERE TRUNC(p.PAYMENT_DATE) = TRUNC(SYSDATE) AND p.PAYMENT_TYPE != 'REFUND'";
        try {
            try (Statement st = DatabaseConnection.getConnection().createStatement();
                 ResultSet rs = st.executeQuery(sql)) {
                if (rs.next()) return rs.getDouble(1);
            }
        } catch (SQLException e) {
            logger.error("Error fetching today revenue: {}", e.getMessage());
        } finally {
            DatabaseConnection.closeConnection();
        }
        return 0;
    }

    public int getTodayCheckIns() {
        String sql = "SELECT COUNT(*) FROM BOOKINGS WHERE TRUNC(ACTUAL_CHECK_IN)=TRUNC(SYSDATE)";
        try {
            try (Statement st = DatabaseConnection.getConnection().createStatement();
                 ResultSet rs = st.executeQuery(sql)) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.error("Error fetching today check-ins: {}", e.getMessage());
        } finally {
            DatabaseConnection.closeConnection();
        }
        return 0;
    }

    public int getTodayCheckOuts() {
        String sql = "SELECT COUNT(*) FROM BOOKINGS WHERE TRUNC(ACTUAL_CHECK_OUT)=TRUNC(SYSDATE)";
        try {
            try (Statement st = DatabaseConnection.getConnection().createStatement();
                 ResultSet rs = st.executeQuery(sql)) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.error("Error fetching today check-outs: {}", e.getMessage());
        } finally {
            DatabaseConnection.closeConnection();
        }
        return 0;
    }
}
