package com.hotel.portal.dao;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hotel.util.DatabaseConnection;

/** Simple DTO for a hotel-configured service. */
class AvailableService {
    public int serviceId;
    public String serviceName;
    public String serviceType;
    public String description;
    public String icon;
    public boolean active;
}

/**
 * DAO for AVAILABLE_SERVICES (hotel-configured) and PHONE_CALLS tables.
 */
public class AvailableServicesDAO {

    private static final Logger logger = LoggerFactory.getLogger(AvailableServicesDAO.class);

    // ── Available Services ────────────────────────────────────────────────

    public List<AvailableService> findAll(boolean activeOnly) {
        List<AvailableService> list = new ArrayList<>();
        String sql = activeOnly
                ? "SELECT * FROM AVAILABLE_SERVICES WHERE ACTIVE=1 ORDER BY SERVICE_NAME"
                : "SELECT * FROM AVAILABLE_SERVICES ORDER BY SERVICE_NAME";
        try {
            try (Statement st = DatabaseConnection.getConnection().createStatement();
                    ResultSet rs = st.executeQuery(sql)) {
                while (rs.next())
                    list.add(mapService(rs));
            }
        } catch (SQLException e) {
            logger.error("Error fetching services: {}", e.getMessage());
        } finally {
            DatabaseConnection.closeConnection();
        }
        return list;
    }

    public int addService(String name, String type, String description, String icon) {
        String sql = "INSERT INTO AVAILABLE_SERVICES (SERVICE_NAME,SERVICE_TYPE,DESCRIPTION,ICON) VALUES (?,?,?,?)";
        try {
            try (PreparedStatement ps = DatabaseConnection.getConnection()
                    .prepareStatement(sql, new String[] { "SERVICE_ID" })) {
                ps.setString(1, name);
                ps.setString(2, type);
                ps.setString(3, description);
                ps.setString(4, icon != null ? icon : "Tools");
                ps.executeUpdate();
                DatabaseConnection.commit();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next())
                        return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            DatabaseConnection.rollback();
            logger.error("Error adding service: {}", e.getMessage());
        } finally {
            DatabaseConnection.closeConnection();
        }
        return -1;
    }

    public boolean toggleActive(int serviceId, boolean active) {
        String sql = "UPDATE AVAILABLE_SERVICES SET ACTIVE=? WHERE SERVICE_ID=?";
        try {
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
                ps.setInt(1, active ? 1 : 0);
                ps.setInt(2, serviceId);
                ps.executeUpdate();
                DatabaseConnection.commit();
                return true;
            }
        } catch (SQLException e) {
            DatabaseConnection.rollback();
            logger.error("Error toggling service: {}", e.getMessage());
            return false;
        } finally {
            DatabaseConnection.closeConnection();
        }
    }

    public boolean deleteService(int serviceId) {
        String sql = "DELETE FROM AVAILABLE_SERVICES WHERE SERVICE_ID=?";
        try {
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
                ps.setInt(1, serviceId);
                ps.executeUpdate();
                DatabaseConnection.commit();
                return true;
            }
        } catch (SQLException e) {
            DatabaseConnection.rollback();
            logger.error("Error deleting service: {}", e.getMessage());
            return false;
        } finally {
            DatabaseConnection.closeConnection();
        }
    }

    // ── Phone Calls ───────────────────────────────────────────────────────

    /** Customer places a call — creates a RINGING record. */
    public int placeCall(int customerId, String callerName, String roomNumber) {
        String sql = "INSERT INTO PHONE_CALLS (CUSTOMER_ID,CALLER_NAME,ROOM_NUMBER,CALL_STATUS) VALUES (?,?,?,'RINGING')";
        try {
            try (PreparedStatement ps = DatabaseConnection.getConnection()
                    .prepareStatement(sql, new String[] { "CALL_ID" })) {
                ps.setInt(1, customerId);
                ps.setString(2, callerName);
                ps.setString(3, roomNumber);
                ps.executeUpdate();
                DatabaseConnection.commit();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next())
                        return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            DatabaseConnection.rollback();
            logger.error("Error placing call: {}", e.getMessage());
        } finally {
            DatabaseConnection.closeConnection();
        }
        return -1;
    }

    public String getCallStatus(int callId) {
        String sql = "SELECT CALL_STATUS FROM PHONE_CALLS WHERE CALL_ID=?";
        try {
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
                ps.setInt(1, callId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next())
                        return rs.getString("CALL_STATUS");
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting call status: {}", e.getMessage());
        } finally {
            DatabaseConnection.closeConnection();
        }
        return "MISSED";
    }

    /** Receptionist answers a call. */
    public boolean answerCall(int callId, String answeredBy) {
        String sql = "UPDATE PHONE_CALLS SET CALL_STATUS='ANSWERED', ANSWERED_AT=SYSDATE, ANSWERED_BY=? WHERE CALL_ID=?";
        try {
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
                ps.setString(1, answeredBy);
                ps.setInt(2, callId);
                ps.executeUpdate();
                DatabaseConnection.commit();
                return true;
            }
        } catch (SQLException e) {
            DatabaseConnection.rollback();
            return false;
        } finally {
            DatabaseConnection.closeConnection();
        }
    }

    public boolean endCall(int callId) {
        String sql = "UPDATE PHONE_CALLS SET CALL_STATUS='ENDED' WHERE CALL_ID=?";
        try {
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
                ps.setInt(1, callId);
                ps.executeUpdate();
                DatabaseConnection.commit();
                return true;
            }
        } catch (SQLException e) {
            DatabaseConnection.rollback();
            return false;
        } finally {
            DatabaseConnection.closeConnection();
        }
    }

    public boolean missCall(int callId) {
        String sql = "UPDATE PHONE_CALLS SET CALL_STATUS='MISSED' WHERE CALL_ID=? AND CALL_STATUS='RINGING'";
        try {
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
                ps.setInt(1, callId);
                ps.executeUpdate();
                DatabaseConnection.commit();
                return true;
            }
        } catch (SQLException e) {
            DatabaseConnection.rollback();
            return false;
        } finally {
            DatabaseConnection.closeConnection();
        }
    }

    /** Get all RINGING calls (for receptionist notification). */
    public List<int[]> getRingingCalls() {
        List<int[]> list = new ArrayList<>();
        String sql = "SELECT pc.CALL_ID, pc.CUSTOMER_ID, pc.CALLER_NAME, pc.ROOM_NUMBER " +
                "FROM PHONE_CALLS pc WHERE pc.CALL_STATUS='RINGING' ORDER BY pc.CALLED_AT";
        try {
            try (Statement st = DatabaseConnection.getConnection().createStatement();
                    ResultSet rs = st.executeQuery(sql)) {
                while (rs.next()) {
                    list.add(new int[] { rs.getInt("CALL_ID"), rs.getInt("CUSTOMER_ID") });
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting ringing calls: {}", e.getMessage());
        } finally {
            DatabaseConnection.closeConnection();
        }
        return list;
    }

    /** Full ringing call info for display */
    public List<String[]> getRingingCallsInfo() {
        List<String[]> list = new ArrayList<>();
        String sql = "SELECT pc.CALL_ID, pc.CALLER_NAME, pc.ROOM_NUMBER, " +
                "TO_CHAR(pc.CALLED_AT,'HH24:MI:SS') AS CALLED_TIME " +
                "FROM PHONE_CALLS pc WHERE pc.CALL_STATUS='RINGING' ORDER BY pc.CALLED_AT";
        try {
            try (Statement st = DatabaseConnection.getConnection().createStatement();
                    ResultSet rs = st.executeQuery(sql)) {
                while (rs.next()) {
                    list.add(new String[] {
                            rs.getString("CALL_ID"),
                            rs.getString("CALLER_NAME"),
                            rs.getString("ROOM_NUMBER"),
                            rs.getString("CALLED_TIME")
                    });
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting call info: {}", e.getMessage());
        } finally {
            DatabaseConnection.closeConnection();
        }
        return list;
    }

    public int countRingingCalls() {
        String sql = "SELECT COUNT(*) FROM PHONE_CALLS WHERE CALL_STATUS='RINGING'";
        try {
            try (Statement st = DatabaseConnection.getConnection().createStatement();
                    ResultSet rs = st.executeQuery(sql)) {
                if (rs.next())
                    return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.error("Error counting calls: {}", e.getMessage());
        } finally {
            DatabaseConnection.closeConnection();
        }
        return 0;
    }

    private String mapTypeToIcon(String type) {
        if (type == null)
            return "GENERAL";
        return switch (type) {
            case "HOUSEKEEPING" -> "CLEAN";
            case "PHONE_CALL" -> "PHONE";
            case "EXTRA_TOWELS" -> "TOWEL";
            case "EXTRA_PILLOW" -> "PILLOW";
            case "WAKE_UP_CALL" -> "CLOCK";
            case "LAUNDRY" -> "LAUNDRY";
            case "TAXI" -> "TAXI";
            case "MAINTENANCE" -> "TOOLS";
            default -> "GENERAL";
        };
    }

    private AvailableService mapService(ResultSet rs) throws SQLException {
        AvailableService s = new AvailableService();
        s.serviceId = rs.getInt("SERVICE_ID");
        s.serviceName = rs.getString("SERVICE_NAME");
        s.serviceType = rs.getString("SERVICE_TYPE");
        s.description = rs.getString("DESCRIPTION");
        String dbIcon = rs.getString("ICON");

        // sanitize corrupted emoji
        if (dbIcon == null || dbIcon.contains("ð") || dbIcon.contains("?")) {
            dbIcon = mapTypeToIcon(s.serviceType);
        }

        s.icon = dbIcon;
        s.active = rs.getInt("ACTIVE") == 1;
        return s;
    }
}
