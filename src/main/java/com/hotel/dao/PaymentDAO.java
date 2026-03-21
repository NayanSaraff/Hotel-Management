package com.hotel.dao;

import com.hotel.model.Payment;
import com.hotel.util.DatabaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO for Payment CRUD operations against the PAYMENTS table.
 */
public class PaymentDAO implements GenericDAO<Payment, Integer> {

    private static final Logger logger = LoggerFactory.getLogger(PaymentDAO.class);

    @Override
    public int save(Payment p) {
        String sql = "INSERT INTO PAYMENTS (BOOKING_ID,AMOUNT,PAYMENT_MODE,PAYMENT_TYPE," +
                     "TRANSACTION_ID,PAYMENT_DATE,REMARKS) VALUES (?,?,?,?,?,?,?)";
        try (PreparedStatement ps = DatabaseConnection.getConnection()
                .prepareStatement(sql, new String[]{"PAYMENT_ID"})) {
            ps.setInt(1, p.getBookingId());
            ps.setDouble(2, p.getAmount());
            ps.setString(3, p.getPaymentMode().name());
            ps.setString(4, p.getPaymentType().name());
            ps.setString(5, p.getTransactionId());
            ps.setTimestamp(6, p.getPaymentDate() != null ?
                    Timestamp.valueOf(p.getPaymentDate()) : new Timestamp(System.currentTimeMillis()));
            ps.setString(7, p.getRemarks());
            ps.executeUpdate();
            DatabaseConnection.commit();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            DatabaseConnection.rollback();
            logger.error("Error saving payment: {}", e.getMessage());
        }
        return -1;
    }

    @Override
    public boolean update(Payment p) {
        String sql = "UPDATE PAYMENTS SET AMOUNT=?,PAYMENT_MODE=?,REMARKS=? WHERE PAYMENT_ID=?";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setDouble(1, p.getAmount());
            ps.setString(2, p.getPaymentMode().name());
            ps.setString(3, p.getRemarks());
            ps.setInt(4, p.getPaymentId());
            int rows = ps.executeUpdate();
            DatabaseConnection.commit();
            return rows > 0;
        } catch (SQLException e) {
            DatabaseConnection.rollback();
            logger.error("Error updating payment: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean delete(Integer id) {
        String sql = "DELETE FROM PAYMENTS WHERE PAYMENT_ID=?";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            int rows = ps.executeUpdate();
            DatabaseConnection.commit();
            return rows > 0;
        } catch (SQLException e) {
            DatabaseConnection.rollback();
            logger.error("Error deleting payment: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public Optional<Payment> findById(Integer id) {
        String sql = "SELECT p.*, b.BOOKING_REFERENCE FROM PAYMENTS p " +
                     "JOIN BOOKINGS b ON p.BOOKING_ID=b.BOOKING_ID WHERE p.PAYMENT_ID=?";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding payment: {}", e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public List<Payment> findAll() {
        List<Payment> list = new ArrayList<>();
        String sql = "SELECT p.*, b.BOOKING_REFERENCE FROM PAYMENTS p " +
                     "JOIN BOOKINGS b ON p.BOOKING_ID=b.BOOKING_ID ORDER BY p.PAYMENT_DATE DESC";
        try (Statement st = DatabaseConnection.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            logger.error("Error fetching payments: {}", e.getMessage());
        }
        return list;
    }

    /** Return all payments for a specific booking. */
    public List<Payment> findByBookingId(int bookingId) {
        List<Payment> list = new ArrayList<>();
        String sql = "SELECT p.*, b.BOOKING_REFERENCE FROM PAYMENTS p " +
                     "JOIN BOOKINGS b ON p.BOOKING_ID=b.BOOKING_ID " +
                     "WHERE p.BOOKING_ID=? ORDER BY p.PAYMENT_DATE";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            logger.error("Error fetching payments for booking: {}", e.getMessage());
        }
        return list;
    }

    /** Sum of all payments received for a booking. */
    public double getTotalPaidForBooking(int bookingId) {
        String sql = "SELECT NVL(SUM(AMOUNT),0) FROM PAYMENTS " +
                     "WHERE BOOKING_ID=? AND PAYMENT_TYPE != 'REFUND'";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble(1);
            }
        } catch (SQLException e) {
            logger.error("Error summing payments: {}", e.getMessage());
        }
        return 0;
    }

    private Payment mapRow(ResultSet rs) throws SQLException {
        Payment p = new Payment();
        p.setPaymentId(rs.getInt("PAYMENT_ID"));
        p.setBookingId(rs.getInt("BOOKING_ID"));
        p.setAmount(rs.getDouble("AMOUNT"));
        p.setPaymentMode(Payment.Mode.valueOf(rs.getString("PAYMENT_MODE")));
        p.setPaymentType(Payment.Type.valueOf(rs.getString("PAYMENT_TYPE")));
        p.setTransactionId(rs.getString("TRANSACTION_ID"));
        Timestamp ts = rs.getTimestamp("PAYMENT_DATE");
        if (ts != null) p.setPaymentDate(ts.toLocalDateTime());
        p.setRemarks(rs.getString("REMARKS"));
        p.setBookingReference(rs.getString("BOOKING_REFERENCE"));
        return p;
    }
}
