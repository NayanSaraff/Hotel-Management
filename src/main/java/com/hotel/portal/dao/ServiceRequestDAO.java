package com.hotel.portal.dao;

import com.hotel.portal.model.ServiceRequest;
import com.hotel.util.DatabaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for SERVICE_REQUESTS table.
 */
public class ServiceRequestDAO {

    private static final Logger logger = LoggerFactory.getLogger(ServiceRequestDAO.class);

    public int save(ServiceRequest req) {
        String sql = "INSERT INTO SERVICE_REQUESTS " +
                     "(CUSTOMER_ID, BOOKING_ID, REQUEST_TYPE, DESCRIPTION, PHONE_NUMBER, PRIORITY, STATUS) " +
                     "VALUES (?, ?, ?, ?, ?, ?, 'PENDING')";
        try {
            try (PreparedStatement ps = DatabaseConnection.getConnection()
                    .prepareStatement(sql, new String[]{"REQUEST_ID"})) {
                ps.setInt(1, req.getCustomerId());
                if (req.getBookingId() > 0) ps.setInt(2, req.getBookingId());
                else ps.setNull(2, Types.INTEGER);
                ps.setString(3, req.getRequestType().name());
                ps.setString(4, req.getDescription());
                ps.setString(5, req.getPhoneNumber());
                ps.setString(6, req.getPriority() != null ? req.getPriority().name() : "NORMAL");
                ps.executeUpdate();
                DatabaseConnection.commit();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            DatabaseConnection.rollback();
            logger.error("Error saving service request: {}", e.getMessage());
        } finally {
            DatabaseConnection.closeConnection();
        }
        return -1;
    }

    public List<ServiceRequest> findByCustomer(int customerId) {
        List<ServiceRequest> list = new ArrayList<>();
        String sql = "SELECT sr.*, c.FIRST_NAME || ' ' || c.LAST_NAME AS CUSTOMER_NAME " +
                     "FROM SERVICE_REQUESTS sr " +
                     "JOIN CUSTOMERS c ON sr.CUSTOMER_ID = c.CUSTOMER_ID " +
                     "WHERE sr.CUSTOMER_ID = ? " +
                     "ORDER BY sr.REQUESTED_AT DESC";
        try {
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
                ps.setInt(1, customerId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error fetching service requests: {}", e.getMessage());
        } finally {
            DatabaseConnection.closeConnection();
        }
        return list;
    }

    /** All requests – for hotel management view */
    public List<ServiceRequest> findAll() {
        List<ServiceRequest> list = new ArrayList<>();
        String sql = "SELECT sr.*, c.FIRST_NAME || ' ' || c.LAST_NAME AS CUSTOMER_NAME " +
                     "FROM SERVICE_REQUESTS sr " +
                     "JOIN CUSTOMERS c ON sr.CUSTOMER_ID = c.CUSTOMER_ID " +
                     "ORDER BY sr.SEEN_BY_HOTEL ASC, sr.REQUESTED_AT DESC";
        try {
            try (Statement st = DatabaseConnection.getConnection().createStatement();
                 ResultSet rs = st.executeQuery(sql)) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            logger.error("Error fetching all service requests: {}", e.getMessage());
        } finally {
            DatabaseConnection.closeConnection();
        }
        return list;
    }

    public boolean updateStatus(int requestId, ServiceRequest.Status status, String notes) {
        String sql = "UPDATE SERVICE_REQUESTS SET STATUS = ?, NOTES = ?, " +
                     "COMPLETED_AT = CASE WHEN ? = 'COMPLETED' THEN SYSDATE ELSE COMPLETED_AT END, " +
                     "SEEN_BY_HOTEL = 1 " +
                     "WHERE REQUEST_ID = ?";
        try {
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
                ps.setString(1, status.name());
                ps.setString(2, notes);
                ps.setString(3, status.name());
                ps.setInt(4, requestId);
                int rows = ps.executeUpdate();
                DatabaseConnection.commit();
                return rows > 0;
            }
        } catch (SQLException e) {
            DatabaseConnection.rollback();
            logger.error("Error updating service request status: {}", e.getMessage());
            return false;
        } finally {
            DatabaseConnection.closeConnection();
        }
    }

    /** Mark all unseen requests as seen (after hotel clicks refresh) */
    public void markAllSeen() {
        String sql = "UPDATE SERVICE_REQUESTS SET SEEN_BY_HOTEL = 1 WHERE SEEN_BY_HOTEL = 0";
        try {
            try (Statement st = DatabaseConnection.getConnection().createStatement()) {
                st.executeUpdate(sql);
                DatabaseConnection.commit();
            }
        } catch (SQLException e) {
            logger.error("Error marking requests seen: {}", e.getMessage());
        } finally {
            DatabaseConnection.closeConnection();
        }
    }

    public int countUnseen() {
        String sql = "SELECT COUNT(*) FROM SERVICE_REQUESTS WHERE SEEN_BY_HOTEL = 0";
        try {
            try (Statement st = DatabaseConnection.getConnection().createStatement();
                 ResultSet rs = st.executeQuery(sql)) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.error("Error counting unseen requests: {}", e.getMessage());
        } finally {
            DatabaseConnection.closeConnection();
        }
        return 0;
    }

    private ServiceRequest mapRow(ResultSet rs) throws SQLException {
        ServiceRequest r = new ServiceRequest();
        r.setRequestId(rs.getInt("REQUEST_ID"));
        r.setCustomerId(rs.getInt("CUSTOMER_ID"));
        r.setBookingId(rs.getInt("BOOKING_ID"));
        try { r.setRequestType(ServiceRequest.Type.valueOf(rs.getString("REQUEST_TYPE"))); } catch (Exception ignored) {}
        r.setDescription(rs.getString("DESCRIPTION"));
        r.setPhoneNumber(rs.getString("PHONE_NUMBER"));
        try { r.setStatus(ServiceRequest.Status.valueOf(rs.getString("STATUS"))); } catch (Exception ignored) {}
        try { r.setPriority(ServiceRequest.Priority.valueOf(rs.getString("PRIORITY"))); } catch (Exception ignored) {}
        Timestamp reqAt = rs.getDate("REQUESTED_AT") != null ? new java.sql.Timestamp(rs.getDate("REQUESTED_AT").getTime()) : null;
        if (reqAt != null) r.setRequestedAt(reqAt.toLocalDateTime());
        Timestamp compAt = rs.getDate("COMPLETED_AT") != null ? new java.sql.Timestamp(rs.getDate("COMPLETED_AT").getTime()) : null;
        if (compAt != null) r.setCompletedAt(compAt.toLocalDateTime());
        r.setNotes(rs.getString("NOTES"));
        r.setSeenByHotel(rs.getInt("SEEN_BY_HOTEL") == 1);
        try { r.setCustomerName(rs.getString("CUSTOMER_NAME")); } catch (SQLException ignored) {}
        return r;
    }
}
