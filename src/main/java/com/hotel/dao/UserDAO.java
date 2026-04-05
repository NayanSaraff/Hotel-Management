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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DAO for User CRUD operations against the USERS table.
 */
public class UserDAO implements GenericDAO<User, Integer> {

    private static final Logger logger = LoggerFactory.getLogger(UserDAO.class);
    private static final Pattern BCRYPT_FRAGMENT =
            Pattern.compile("\\$2([abxyABXY])\\$\\d{2}\\$[./A-Za-z0-9]{53}");

    @Override
    public int save(User user) {
        String sql = "INSERT INTO USERS (USERNAME, PASSWORD_HASH, FULL_NAME, EMAIL, PHONE, ROLE, ACTIVE, CREATED_AT) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try {
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
            }
        } catch (SQLException e) {
            DatabaseConnection.rollback();
            logger.error("Error saving user: {}", e.getMessage());
        } finally {
            DatabaseConnection.closeConnection();
        }
        return -1;
    }

    @Override
    public boolean update(User user) {
        String sql = "UPDATE USERS SET FULL_NAME=?, EMAIL=?, PHONE=?, ROLE=?, ACTIVE=? WHERE USER_ID=?";
        try {
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
            }
        } catch (SQLException e) {
            DatabaseConnection.rollback();
            logger.error("Error updating user: {}", e.getMessage());
            return false;
        } finally {
            DatabaseConnection.closeConnection();
        }
    }

    @Override
    public boolean delete(Integer id) {
        String sql = "UPDATE USERS SET ACTIVE=0 WHERE USER_ID=?";  // Soft delete
        try {
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
                ps.setInt(1, id);
                int rows = ps.executeUpdate();
                DatabaseConnection.commit();
                return rows > 0;
            }
        } catch (SQLException e) {
            DatabaseConnection.rollback();
            logger.error("Error deleting user: {}", e.getMessage());
            return false;
        } finally {
            DatabaseConnection.closeConnection();
        }
    }

    @Override
    public Optional<User> findById(Integer id) {
        String sql = "SELECT * FROM USERS WHERE USER_ID=?";
        try {
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding user by id: {}", e.getMessage());
        } finally {
            DatabaseConnection.closeConnection();
        }
        return Optional.empty();
    }

    @Override
    public List<User> findAll() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM USERS ORDER BY FULL_NAME";
        try {
            try (Statement st = DatabaseConnection.getConnection().createStatement();
                 ResultSet rs = st.executeQuery(sql)) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            logger.error("Error fetching users: {}", e.getMessage());
        } finally {
            DatabaseConnection.closeConnection();
        }
        return list;
    }

    /**
     * Authenticate user with username and plain-text password.
     * SECURITY: BCrypt hash verification only; no plaintext fallback.
     */
    public Optional<User> authenticate(String username, String plainPassword) {
        String sql = "SELECT * FROM USERS WHERE USERNAME=? AND ACTIVE=1";
        try {
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String hash = normalizeBcryptHash(rs.getString("PASSWORD_HASH"));
                        // BCrypt verification — no fallback to plaintext
                        if (BCrypt.checkpw(plainPassword, hash)) {
                            User user = mapRow(rs);
                            updateLastLogin(user.getUserId());
                            return Optional.of(user);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Error authenticating user: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            // BCrypt throws IllegalArgumentException for invalid hash format
            logger.error("Invalid password hash format for user {}: {}", username, e.getMessage());
        } finally {
            DatabaseConnection.closeConnection();
        }
        return Optional.empty();
    }

    /**
     * Force-reset the admin user's password hash.
     */
    public boolean forceResetAdminPassword(String newPlainPassword) {
        String sql = "UPDATE USERS SET PASSWORD_HASH=? WHERE LOWER(USERNAME)='admin'";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, BCrypt.hashpw(newPlainPassword, BCrypt.gensalt(10)));
            int rows = ps.executeUpdate();
            DatabaseConnection.commit();
            return rows > 0;
        } catch (SQLException e) {
            DatabaseConnection.rollback();
            logger.error("Error resetting admin password: {}", e.getMessage());
            return false;
        } finally {
            DatabaseConnection.closeConnection();
        }
    }

    /**
     * Force-reset password hash for a specific username.
     */
    public boolean forceResetPassword(String username, String newPlainPassword) {
        String sql = "UPDATE USERS SET PASSWORD_HASH=? WHERE LOWER(USERNAME)=LOWER(?)";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, BCrypt.hashpw(newPlainPassword, BCrypt.gensalt(10)));
            ps.setString(2, username);
            int rows = ps.executeUpdate();
            DatabaseConnection.commit();
            return rows > 0;
        } catch (SQLException e) {
            DatabaseConnection.rollback();
            logger.error("Error resetting password for user {}: {}", username, e.getMessage());
            return false;
        } finally {
            DatabaseConnection.closeConnection();
        }
    }

    /**
     * Normalizes legacy BCrypt formats to jBCrypt-compatible form.
     * Supports hashes prefixed with {bcrypt} and $2y$ / $2b$ versions.
     */
    private String normalizeBcryptHash(String hash) {
        if (hash == null) {
            return "";
        }

        String normalized = hash.trim();
        if (normalized.length() >= 2) {
            char first = normalized.charAt(0);
            char last = normalized.charAt(normalized.length() - 1);
            if ((first == '\'' && last == '\'') || (first == '"' && last == '"')) {
                normalized = normalized.substring(1, normalized.length() - 1).trim();
            }
        }
        if (normalized.startsWith("{bcrypt}")) {
            normalized = normalized.substring("{bcrypt}".length());
        }
        normalized = normalized.replace("\\$", "$");

        if (normalized.startsWith("2a$") || normalized.startsWith("2b$") || normalized.startsWith("2y$") || normalized.startsWith("2x$")
                || normalized.startsWith("2A$") || normalized.startsWith("2B$") || normalized.startsWith("2Y$") || normalized.startsWith("2X$")) {
            normalized = "$" + normalized;
        }
        if (normalized.startsWith("$2A$") || normalized.startsWith("$2B$")
                || normalized.startsWith("$2Y$") || normalized.startsWith("$2X$")) {
            normalized = "$2" + Character.toLowerCase(normalized.charAt(2)) + normalized.substring(3);
        }
        if (normalized.startsWith("$2$")) {
            normalized = "$2a$" + normalized.substring(3);
        }
        if (normalized.startsWith("$2y$") || normalized.startsWith("$2b$") || normalized.startsWith("$2x$")) {
            normalized = "$2a$" + normalized.substring(4);
        }

        if (!normalized.startsWith("$2a$") || normalized.length() < 60) {
            Matcher matcher = BCRYPT_FRAGMENT.matcher(normalized);
            if (matcher.find()) {
                String fragment = matcher.group();
                if (fragment.startsWith("$2a$")) {
                    normalized = fragment;
                } else {
                    normalized = "$2a$" + fragment.substring(4);
                }
            }
        }

        return normalized;
    }

    private void updateLastLogin(int userId) {
        String sql = "UPDATE USERS SET LAST_LOGIN=? WHERE USER_ID=?";
        try {
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
                ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
                ps.setInt(2, userId);
                ps.executeUpdate();
                DatabaseConnection.commit();
            }
        } catch (SQLException e) {
            logger.error("Error updating last login: {}", e.getMessage());
        } finally {
            DatabaseConnection.closeConnection();
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
