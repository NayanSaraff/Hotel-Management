package com.hotel.portal.dao;

import com.hotel.portal.model.CustomerAccount;
import com.hotel.util.DatabaseConnection;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.Optional;

/**
 * DAO for CUSTOMER_ACCOUNTS – portal authentication.
 */
public class CustomerAccountDAO {

    private static final Logger logger = LoggerFactory.getLogger(CustomerAccountDAO.class);

    /**
     * Register a new customer account linked to an existing CUSTOMERS row.
     * @param customerId  the CUSTOMERS.CUSTOMER_ID to link
     * @param email       used as username
     * @param plainPassword raw password to hash
     * @return new ACCOUNT_ID, or -1 on failure
     */
    public int register(int customerId, String email, String plainPassword) {
        String sql = "INSERT INTO CUSTOMER_ACCOUNTS (CUSTOMER_ID, USERNAME, PASSWORD_HASH) " +
                     "VALUES (?, ?, ?)";
        String hash = BCrypt.hashpw(plainPassword, BCrypt.gensalt(10));
        try {
            try (PreparedStatement ps = DatabaseConnection.getConnection()
                    .prepareStatement(sql, new String[]{"ACCOUNT_ID"})) {
                ps.setInt(1, customerId);
                ps.setString(2, email.toLowerCase().trim());
                ps.setString(3, hash);
                ps.executeUpdate();
                DatabaseConnection.commit();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            DatabaseConnection.rollback();
            logger.error("Error registering customer account: {}", e.getMessage());
        } finally {
            DatabaseConnection.closeConnection();
        }
        return -1;
    }

    /**
     * Authenticate a customer by email + password.
     * @return populated CustomerAccount, or empty if auth fails
     */
    public Optional<CustomerAccount> login(String email, String plainPassword) {
        String sql = "SELECT ca.*, c.FIRST_NAME, c.LAST_NAME, c.EMAIL, c.PHONE " +
                     "FROM CUSTOMER_ACCOUNTS ca " +
                     "JOIN CUSTOMERS c ON ca.CUSTOMER_ID = c.CUSTOMER_ID " +
                     "WHERE ca.USERNAME = ? AND ca.ACTIVE = 1";
        try {
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
                ps.setString(1, email.toLowerCase().trim());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String storedHash = rs.getString("PASSWORD_HASH");
                        if (BCrypt.checkpw(plainPassword, storedHash)) {
                            CustomerAccount acct = mapRow(rs);
                            updateLastLogin(acct.getAccountId());
                            return Optional.of(acct);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Error during customer login: {}", e.getMessage());
        } finally {
            DatabaseConnection.closeConnection();
        }
        return Optional.empty();
    }

    /**
     * Check if an email is already registered.
     */
    public boolean emailExists(String email) {
        String sql = "SELECT COUNT(*) FROM CUSTOMER_ACCOUNTS WHERE USERNAME = ?";
        try {
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
                ps.setString(1, email.toLowerCase().trim());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            logger.error("Error checking email: {}", e.getMessage());
        } finally {
            DatabaseConnection.closeConnection();
        }
        return false;
    }

    /**
     * Delete a customer account (and cascade-deletes the CUSTOMERS row).
     */
    public boolean deleteAccount(int customerId) {
        // Due to ON DELETE CASCADE on CUSTOMER_ACCOUNTS, deleting the CUSTOMERS row
        // will cascade and delete bookings only if FK allows - so we just delete the account
        // and set customer inactive instead of hard delete (to preserve booking history)
        String sql = "DELETE FROM CUSTOMER_ACCOUNTS WHERE CUSTOMER_ID = ?";
        try {
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
                ps.setInt(1, customerId);
                int rows = ps.executeUpdate();
                DatabaseConnection.commit();
                return rows > 0;
            }
        } catch (SQLException e) {
            DatabaseConnection.rollback();
            logger.error("Error deleting account: {}", e.getMessage());
            return false;
        } finally {
            DatabaseConnection.closeConnection();
        }
    }

    /**
     * Change password for an account.
     */
    public boolean changePassword(int customerId, String oldPassword, String newPassword) {
        // First verify old password
        String selectSql = "SELECT PASSWORD_HASH FROM CUSTOMER_ACCOUNTS WHERE CUSTOMER_ID = ?";
        try {
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(selectSql)) {
                ps.setInt(1, customerId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        if (!BCrypt.checkpw(oldPassword, rs.getString("PASSWORD_HASH"))) {
                            return false; // wrong old password
                        }
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Error verifying old password: {}", e.getMessage());
            return false;
        }
        String updateSql = "UPDATE CUSTOMER_ACCOUNTS SET PASSWORD_HASH = ? WHERE CUSTOMER_ID = ?";
        String newHash = BCrypt.hashpw(newPassword, BCrypt.gensalt(10));
        try {
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(updateSql)) {
                ps.setString(1, newHash);
                ps.setInt(2, customerId);
                ps.executeUpdate();
                DatabaseConnection.commit();
                return true;
            }
        } catch (SQLException e) {
            DatabaseConnection.rollback();
            logger.error("Error changing password: {}", e.getMessage());
            return false;
        } finally {
            DatabaseConnection.closeConnection();
        }
    }

    private void updateLastLogin(int accountId) {
        String sql = "UPDATE CUSTOMER_ACCOUNTS SET LAST_LOGIN = SYSDATE WHERE ACCOUNT_ID = ?";
        try {
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
                ps.setInt(1, accountId);
                ps.executeUpdate();
                DatabaseConnection.commit();
            }
        } catch (SQLException e) {
            logger.error("Error updating last login: {}", e.getMessage());
        } finally {
            DatabaseConnection.closeConnection();
        }
    }

    private CustomerAccount mapRow(ResultSet rs) throws SQLException {
        CustomerAccount a = new CustomerAccount();
        a.setAccountId(rs.getInt("ACCOUNT_ID"));
        a.setCustomerId(rs.getInt("CUSTOMER_ID"));
        a.setUsername(rs.getString("USERNAME"));
        a.setActive(rs.getInt("ACTIVE") == 1);
        Timestamp created = rs.getTimestamp("CREATED_AT");
        if (created != null) a.setCreatedAt(created.toLocalDateTime());
        Timestamp lastLogin = rs.getTimestamp("LAST_LOGIN");
        if (lastLogin != null) a.setLastLogin(lastLogin.toLocalDateTime());
        // Joined from CUSTOMERS
        try { a.setFirstName(rs.getString("FIRST_NAME")); } catch (SQLException ignored) {}
        try { a.setLastName(rs.getString("LAST_NAME")); }  catch (SQLException ignored) {}
        try { a.setEmail(rs.getString("EMAIL")); }         catch (SQLException ignored) {}
        try { a.setPhone(rs.getString("PHONE")); }         catch (SQLException ignored) {}
        return a;
    }
}
