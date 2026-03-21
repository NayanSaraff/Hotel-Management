package com.hotel.dao;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hotel.model.Booking;
import com.hotel.util.DatabaseConnection;

public class BookingDAO implements GenericDAO<Booking, Integer> {

    private static final Logger logger = LoggerFactory.getLogger(BookingDAO.class);

    private static final String SELECT_WITH_JOINS =
        "SELECT b.*, c.FIRST_NAME || ' ' || c.LAST_NAME AS CUSTOMER_NAME, " +
        "       r.ROOM_NUMBER, r.CATEGORY AS ROOM_CATEGORY " +
        "FROM BOOKINGS b " +
        "JOIN CUSTOMERS c ON b.CUSTOMER_ID = c.CUSTOMER_ID " +
        "JOIN ROOMS r     ON b.ROOM_ID = r.ROOM_ID ";

    @Override
    public int save(Booking booking) {
        String sql = "INSERT INTO BOOKINGS (BOOKING_REFERENCE,CUSTOMER_ID,ROOM_ID,USER_ID," +
                     "CHECK_IN_DATE,CHECK_OUT_DATE,NUMBER_OF_GUESTS,ROOM_CHARGES," +
                     "SERVICE_CHARGES,GST_AMOUNT,TOTAL_AMOUNT,ADVANCE_PAID,BALANCE_DUE," +
                     "STATUS,SPECIAL_REQUESTS,BOOKING_DATE) " +
                     "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = DatabaseConnection.getConnection()
                .prepareStatement(sql, new String[]{"BOOKING_ID"})) {

            ps.setString(1, booking.getBookingReference());
            ps.setInt(2, booking.getCustomerId());
            ps.setInt(3, booking.getRoomId());
            ps.setInt(4, booking.getUserId());
            ps.setDate(5, Date.valueOf(booking.getCheckInDate()));
            ps.setDate(6, Date.valueOf(booking.getCheckOutDate()));
            ps.setInt(7, booking.getNumberOfGuests());
            ps.setDouble(8, booking.getRoomCharges());
            ps.setDouble(9, booking.getServiceCharges());
            ps.setDouble(10, booking.getGstAmount());
            ps.setDouble(11, booking.getTotalAmount());
            ps.setDouble(12, booking.getAdvancePaid());
            ps.setDouble(13, booking.getBalanceDue());
            ps.setString(14, booking.getStatus().name());
            ps.setString(15, booking.getSpecialRequests());
            ps.setTimestamp(16, Timestamp.valueOf(LocalDateTime.now()));

            ps.executeUpdate();
            DatabaseConnection.commit();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            DatabaseConnection.rollback();
            logger.error("Error saving booking: {}", e.getMessage());
        }
        return -1;
    }

    @Override
    public boolean update(Booking booking) {
        String sql = "UPDATE BOOKINGS SET STATUS=?,ACTUAL_CHECK_IN=?,ACTUAL_CHECK_OUT=?," +
                     "BALANCE_DUE=?,SPECIAL_REQUESTS=? WHERE BOOKING_ID=?";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, booking.getStatus().name());
            ps.setTimestamp(2, booking.getActualCheckIn() != null ?
                    Timestamp.valueOf(booking.getActualCheckIn()) : null);
            ps.setTimestamp(3, booking.getActualCheckOut() != null ?
                    Timestamp.valueOf(booking.getActualCheckOut()) : null);
            ps.setDouble(4, booking.getBalanceDue());
            ps.setString(5, booking.getSpecialRequests());
            ps.setInt(6, booking.getBookingId());

            int rows = ps.executeUpdate();
            DatabaseConnection.commit();
            return rows > 0;
        } catch (SQLException e) {
            DatabaseConnection.rollback();
            logger.error("Error updating booking: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean delete(Integer id) {
        String sql = "UPDATE BOOKINGS SET STATUS='CANCELLED' WHERE BOOKING_ID=?";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            int rows = ps.executeUpdate();
            DatabaseConnection.commit();
            return rows > 0;
        } catch (SQLException e) {
            DatabaseConnection.rollback();
            logger.error("Error cancelling booking: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public Optional<Booking> findById(Integer id) {
        String sql = SELECT_WITH_JOINS + "WHERE b.BOOKING_ID=?";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding booking: {}", e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public List<Booking> findAll() {
        List<Booking> list = new ArrayList<>();
        String sql = SELECT_WITH_JOINS + "ORDER BY b.BOOKING_DATE DESC";
        try (Statement st = DatabaseConnection.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            logger.error("Error fetching bookings: {}", e.getMessage());
        }
        return list;
    }

    public List<Booking> findConfirmed() {
        return findByStatus(Booking.Status.CONFIRMED);
    }

    public List<Booking> findCheckedIn() {
        return findByStatus(Booking.Status.CHECKED_IN);
    }

    public List<Booking> findByStatus(Booking.Status status) {
        List<Booking> list = new ArrayList<>();
        String sql = SELECT_WITH_JOINS + "WHERE b.STATUS=? ORDER BY b.CHECK_IN_DATE";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            logger.error("Error fetching bookings by status: {}", e.getMessage());
        }
        return list;
    }

    public int countByStatus(Booking.Status status) {
        String sql = "SELECT COUNT(*) FROM BOOKINGS WHERE STATUS=?";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.error("Error counting bookings: {}", e.getMessage());
        }
        return 0;
    }

    public double getMonthlyRevenue() {
        String sql = "SELECT NVL(SUM(TOTAL_AMOUNT), 0) FROM BOOKINGS " +
                     "WHERE STATUS IN ('CHECKED_OUT','CHECKED_IN') " +
                     "AND TRUNC(BOOKING_DATE,'MM') = TRUNC(SYSDATE,'MM')";
        try (Statement st = DatabaseConnection.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) {
            logger.error("Error calculating monthly revenue: {}", e.getMessage());
        }
        return 0;
    }

    public String generateBookingReference() {
        String sql = "SELECT 'HMS-' || TO_CHAR(SYSDATE,'YYYY') || '-' || " +
                     "LPAD(NVL(MAX(BOOKING_ID),0)+1,4,'0') FROM BOOKINGS";
        try (Statement st = DatabaseConnection.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getString(1);
        } catch (SQLException e) {
            logger.error("Error generating booking reference: {}", e.getMessage());
        }
        return "HMS-" + java.time.Year.now().getValue() + "-0001";
    }

    // ── Alert Service Methods ─────────────────────────────────────────────

    public List<Booking> findTodayCheckouts() {
        List<Booking> list = new ArrayList<>();
        String sql = SELECT_WITH_JOINS +
                "WHERE TRUNC(b.CHECK_OUT_DATE)=TRUNC(SYSDATE) " +
                "AND b.STATUS='CHECKED_IN' ORDER BY b.CHECK_OUT_DATE";
        try (Statement st = DatabaseConnection.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            logger.error("Error finding today checkouts: {}", e.getMessage());
        }
        return list;
    }

    public List<Booking> findTodayCheckIns() {
        List<Booking> list = new ArrayList<>();
        String sql = SELECT_WITH_JOINS +
                "WHERE TRUNC(b.CHECK_IN_DATE)=TRUNC(SYSDATE) " +
                "AND b.STATUS='CONFIRMED' ORDER BY b.CHECK_IN_DATE";
        try (Statement st = DatabaseConnection.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            logger.error("Error finding today check-ins: {}", e.getMessage());
        }
        return list;
    }

    private Booking mapRow(ResultSet rs) throws SQLException {
        Booking b = new Booking();
        b.setBookingId(rs.getInt("BOOKING_ID"));
        b.setBookingReference(rs.getString("BOOKING_REFERENCE"));
        b.setCustomerId(rs.getInt("CUSTOMER_ID"));
        b.setRoomId(rs.getInt("ROOM_ID"));
        b.setUserId(rs.getInt("USER_ID"));
        b.setCheckInDate(rs.getDate("CHECK_IN_DATE").toLocalDate());
        b.setCheckOutDate(rs.getDate("CHECK_OUT_DATE").toLocalDate());
        b.setNumberOfGuests(rs.getInt("NUMBER_OF_GUESTS"));
        b.setRoomCharges(rs.getDouble("ROOM_CHARGES"));
        b.setServiceCharges(rs.getDouble("SERVICE_CHARGES"));
        b.setGstAmount(rs.getDouble("GST_AMOUNT"));
        b.setTotalAmount(rs.getDouble("TOTAL_AMOUNT"));
        b.setAdvancePaid(rs.getDouble("ADVANCE_PAID"));
        b.setBalanceDue(rs.getDouble("BALANCE_DUE"));
        b.setStatus(Booking.Status.valueOf(rs.getString("STATUS")));
        b.setSpecialRequests(rs.getString("SPECIAL_REQUESTS"));
        Timestamp ts = rs.getTimestamp("BOOKING_DATE");
        if (ts != null) b.setBookingDate(ts.toLocalDateTime());
        Timestamp ci = rs.getTimestamp("ACTUAL_CHECK_IN");
        if (ci != null) b.setActualCheckIn(ci.toLocalDateTime());
        Timestamp co = rs.getTimestamp("ACTUAL_CHECK_OUT");
        if (co != null) b.setActualCheckOut(co.toLocalDateTime());
        b.setCustomerName(rs.getString("CUSTOMER_NAME"));
        b.setRoomNumber(rs.getString("ROOM_NUMBER"));
        b.setRoomCategory(rs.getString("ROOM_CATEGORY"));
        return b;
    }
}