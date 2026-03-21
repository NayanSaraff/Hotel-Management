package com.hotel.dao;

import com.hotel.model.User;
import com.hotel.util.DatabaseConnection;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO for User CRUD operations against the USERS table.
 */
public class UserDAO implements GenericDAO<User, Integer> {

    private static final Logger logger = LoggerFactory.getLogger(UserDAO.class);

    @Override
    public int save(User user) {
        String sql = "INSERT INTO USERS (USERNAME, PASSWORD_HASH, FULL_NAME, EMAIL, PHONE, ROLE, ACTIVE, CREATED_AT) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(
                sql, new String[]{"USER_ID"})) {

            ps.setString(1, user.getUsername());
            ps.setString(2, BCrypt.hashpw(user.getPasswordHash(), BCrypt.gensalt()));
            ps.setString(3, user.getFullName());
            ps.setString(4, user.getEmail());
            ps.setString(5, user.getPhone());
            ps.setString(6, user.getRole());
            ps.setInt(7, user.isActive() ? 1 : 0);
            ps.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now()));

            ps.executeUpdate();
            DatabaseConnection.commit();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            DatabaseConnection.rollback();
            logger.error("Error saving user: {}", e.getMessage());
        }
        return -1;
    }

    @Override
    public boolean update(User user) {
        String sql = "UPDATE USERS SET FULL_NAME=?, EMAIL=?, PHONE=?, ROLE=?, ACTIVE=? WHERE USER_ID=?";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, user.getFullName());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPhone());
            ps.setString(4, user.getRole());
            ps.setInt(5, user.isActive() ? 1 : 0);
            ps.setInt(6, user.getUserId());

            int rows = ps.executeUpdate();
            DatabaseConnection.commit();
            return rows > 0;
        } catch (SQLException e) {
            DatabaseConnection.rollback();
            logger.error("Error updating user: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean delete(Integer id) {
        String sql = "UPDATE USERS SET ACTIVE=0 WHERE USER_ID=?";  // Soft delete
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            int rows = ps.executeUpdate();
            DatabaseConnection.commit();
            return rows > 0;
        } catch (SQLException e) {
            DatabaseConnection.rollback();
            logger.error("Error deleting user: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public Optional<User> findById(Integer id) {
        String sql = "SELECT * FROM USERS WHERE USER_ID=?";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding user by id: {}", e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public List<User> findAll() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM USERS ORDER BY FULL_NAME";
        try (Statement st = DatabaseConnection.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            logger.error("Error fetching users: {}", e.getMessage());
        }
        return list;
    }

    /**
     * Authenticate user with username and plain-text password.
     */
    public Optional<User> authenticate(String username, String plainPassword) {
    String sql = "SELECT * FROM USERS WHERE USERNAME=? AND ACTIVE=1";
    try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
        ps.setString(1, username);
        try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                String hash = rs.getString("PASSWORD_HASH");
                boolean match = false;
                // Try BCrypt first
                try {
                    match = BCrypt.checkpw(plainPassword, hash);
                } catch (Exception e) {
                    // If BCrypt fails, fall back to plain text comparison
                    match = plainPassword.equals(hash);
                }
                if (match) {
                    User user = mapRow(rs);
                    updateLastLogin(user.getUserId());
                    return Optional.of(user);
                }
            }
        }
    } catch (SQLException e) {
        logger.error("Error authenticating user: {}", e.getMessage());
    }
    return Optional.empty();
}

    private void updateLastLogin(int userId) {
        String sql = "UPDATE USERS SET LAST_LOGIN=? WHERE USER_ID=?";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(2, userId);
            ps.executeUpdate();
            DatabaseConnection.commit();
        } catch (SQLException e) {
            logger.error("Error updating last login: {}", e.getMessage());
        }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        User u = new User();
        u.setUserId(rs.getInt("USER_ID"));
        u.setUsername(rs.getString("USERNAME"));
        u.setPasswordHash(rs.getString("PASSWORD_HASH"));
        u.setFullName(rs.getString("FULL_NAME"));
        u.setEmail(rs.getString("EMAIL"));
        u.setPhone(rs.getString("PHONE"));
        u.setRole(rs.getString("ROLE"));
        u.setActive(rs.getInt("ACTIVE") == 1);
        Timestamp ts = rs.getTimestamp("LAST_LOGIN");
        if (ts != null) u.setLastLogin(ts.toLocalDateTime());
        return u;
    }
}
