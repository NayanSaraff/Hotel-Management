package com.hotel.dao;

import com.hotel.model.Room;
import com.hotel.util.DatabaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO for Room CRUD and availability operations.
 */
public class RoomDAO implements GenericDAO<Room, Integer> {

    private static final Logger logger = LoggerFactory.getLogger(RoomDAO.class);

    @Override
    public int save(Room room) {
        String sql = "INSERT INTO ROOMS (ROOM_NUMBER,CATEGORY,FLOOR,CAPACITY,PRICE_PER_NIGHT," +
                     "STATUS,DESCRIPTION,AMENITIES,BED_TYPE,HAS_AC,HAS_WIFI,HAS_TV) " +
                     "VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
        try {
            try (PreparedStatement ps = DatabaseConnection.getConnection()
                    .prepareStatement(sql, new String[]{"ROOM_ID"})) {

                ps.setString(1, room.getRoomNumber());
                ps.setString(2, room.getCategory().name());
                ps.setInt(3, room.getFloor());
                ps.setInt(4, room.getCapacity());
                ps.setDouble(5, room.getPricePerNight());
                ps.setString(6, room.getStatus().name());
                ps.setString(7, room.getDescription());
                ps.setString(8, room.getAmenities());
                ps.setString(9, room.getBedType());
                ps.setInt(10, room.isHasAC() ? 1 : 0);
                ps.setInt(11, room.isHasWifi() ? 1 : 0);
                ps.setInt(12, room.isHasTV() ? 1 : 0);

                ps.executeUpdate();
                DatabaseConnection.commit();

                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            DatabaseConnection.rollback();
            logger.error("Error saving room: {}", e.getMessage());
        } finally {
            DatabaseConnection.closeConnection();
        }
        return -1;
    }

    @Override
    public boolean update(Room room) {
        String sql = "UPDATE ROOMS SET ROOM_NUMBER=?,CATEGORY=?,FLOOR=?,CAPACITY=?," +
                     "PRICE_PER_NIGHT=?,STATUS=?,DESCRIPTION=?,AMENITIES=?,BED_TYPE=?," +
                     "HAS_AC=?,HAS_WIFI=?,HAS_TV=? WHERE ROOM_ID=?";
        try {
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
                ps.setString(1, room.getRoomNumber());
                ps.setString(2, room.getCategory().name());
                ps.setInt(3, room.getFloor());
                ps.setInt(4, room.getCapacity());
                ps.setDouble(5, room.getPricePerNight());
                ps.setString(6, room.getStatus().name());
                ps.setString(7, room.getDescription());
                ps.setString(8, room.getAmenities());
                ps.setString(9, room.getBedType());
                ps.setInt(10, room.isHasAC() ? 1 : 0);
                ps.setInt(11, room.isHasWifi() ? 1 : 0);
                ps.setInt(12, room.isHasTV() ? 1 : 0);
                ps.setInt(13, room.getRoomId());

                int rows = ps.executeUpdate();
                DatabaseConnection.commit();
                return rows > 0;
            }
        } catch (SQLException e) {
            DatabaseConnection.rollback();
            logger.error("Error updating room: {}", e.getMessage());
            return false;
        } finally {
            DatabaseConnection.closeConnection();
        }
    }

    @Override
    public boolean delete(Integer id) {
        String sql = "DELETE FROM ROOMS WHERE ROOM_ID=?";
        try {
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
                ps.setInt(1, id);
                int rows = ps.executeUpdate();
                DatabaseConnection.commit();
                return rows > 0;
            }
        } catch (SQLException e) {
            DatabaseConnection.rollback();
            logger.error("Error deleting room: {}", e.getMessage());
            return false;
        } finally {
            DatabaseConnection.closeConnection();
        }
    }

    @Override
    public Optional<Room> findById(Integer id) {
        String sql = "SELECT * FROM ROOMS WHERE ROOM_ID=?";
        try {
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding room: {}", e.getMessage());
        } finally {
            DatabaseConnection.closeConnection();
        }
        return Optional.empty();
    }

    @Override
    public List<Room> findAll() {
        List<Room> list = new ArrayList<>();
        String sql = "SELECT * FROM ROOMS ORDER BY ROOM_NUMBER";
        try {
            try (Statement st = DatabaseConnection.getConnection().createStatement();
                 ResultSet rs = st.executeQuery(sql)) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            logger.error("Error fetching rooms: {}", e.getMessage());
        } finally {
            DatabaseConnection.closeConnection();
        }
        return list;
    }

    /**
     * Find rooms available between two dates.
     */
    public List<Room> findAvailableRooms(LocalDate checkIn, LocalDate checkOut) {
        List<Room> list = new ArrayList<>();
        String sql = "SELECT r.* FROM ROOMS r " +
                     "WHERE r.STATUS = 'AVAILABLE' " +
                     "AND r.ROOM_ID NOT IN (" +
                     "  SELECT b.ROOM_ID FROM BOOKINGS b " +
                     "  WHERE b.STATUS NOT IN ('CANCELLED','CHECKED_OUT') " +
                     "  AND NOT (b.CHECK_OUT_DATE <= ? OR b.CHECK_IN_DATE >= ?)" +
                     ") ORDER BY r.CATEGORY, r.ROOM_NUMBER";
        try {
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
                ps.setDate(1, Date.valueOf(checkIn));
                ps.setDate(2, Date.valueOf(checkOut));
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding available rooms: {}", e.getMessage());
        } finally {
            DatabaseConnection.closeConnection();
        }
        return list;
    }

    /**
     * Update only the status of a room.
     */
    public boolean updateStatus(int roomId, Room.Status status) {
        String sql = "UPDATE ROOMS SET STATUS=? WHERE ROOM_ID=?";
        try {
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
                ps.setString(1, status.name());
                ps.setInt(2, roomId);
                int rows = ps.executeUpdate();
                DatabaseConnection.commit();
                return rows > 0;
            }
        } catch (SQLException e) {
            DatabaseConnection.rollback();
            logger.error("Error updating room status: {}", e.getMessage());
            return false;
        } finally {
            DatabaseConnection.closeConnection();
        }
    }

    /**
     * Count rooms by status.
     */
    public int countByStatus(Room.Status status) {
        String sql = "SELECT COUNT(*) FROM ROOMS WHERE STATUS=?";
        try {
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
                ps.setString(1, status.name());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            logger.error("Error counting rooms: {}", e.getMessage());
        } finally {
            DatabaseConnection.closeConnection();
        }
        return 0;
    }

    /**
     * Atomically lock and check if room is available, preventing race conditions.
     * Uses SELECT FOR UPDATE to lock the row until transaction ends.
     * Returns true if room was AVAILABLE and has been locked for the booking.
     */
    public boolean lockRoomForBooking(int roomId) {
        String sql = "SELECT ROOM_ID FROM ROOMS WHERE ROOM_ID=? AND STATUS=? FOR UPDATE";
        try {
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
                ps.setInt(1, roomId);
                ps.setString(2, Room.Status.AVAILABLE.name());
                try (ResultSet rs = ps.executeQuery()) {
                    boolean found = rs.next();
                    if (found) {
                        logger.debug("Room {} locked for booking", roomId);
                    } else {
                        logger.warn("Room {} is not available (race condition avoided)", roomId);
                    }
                    return found;
                }
            }
        } catch (SQLException e) {
            logger.error("Error locking room {}: {}", roomId, e.getMessage());
        } finally {
            DatabaseConnection.closeConnection();
        }
        return false;
    }

    private Room mapRow(ResultSet rs) throws SQLException {
        Room r = new Room();
        r.setRoomId(rs.getInt("ROOM_ID"));
        r.setRoomNumber(rs.getString("ROOM_NUMBER"));
        r.setCategory(Room.Category.valueOf(rs.getString("CATEGORY")));
        r.setFloor(rs.getInt("FLOOR"));
        r.setCapacity(rs.getInt("CAPACITY"));
        r.setPricePerNight(rs.getDouble("PRICE_PER_NIGHT"));
        r.setStatus(Room.Status.valueOf(rs.getString("STATUS")));
        r.setDescription(rs.getString("DESCRIPTION"));
        r.setAmenities(rs.getString("AMENITIES"));
        r.setBedType(rs.getString("BED_TYPE"));
        r.setHasAC(rs.getInt("HAS_AC") == 1);
        r.setHasWifi(rs.getInt("HAS_WIFI") == 1);
        r.setHasTV(rs.getInt("HAS_TV") == 1);
        return r;
    }
}
