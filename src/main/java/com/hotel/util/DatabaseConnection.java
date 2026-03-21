package com.hotel.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Singleton class to manage Oracle database connections.
 */
public class DatabaseConnection {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnection.class);
    private static Connection connection;

    private static String url;
    private static String username;
    private static String password;

    static {
        loadProperties();
    }

    private static void loadProperties() {
        try (InputStream input = DatabaseConnection.class
                .getClassLoader()
                .getResourceAsStream("db.properties")) {

            if (input == null) {
                logger.error("db.properties not found in classpath.");
                return;
            }
            Properties props = new Properties();
            props.load(input);

            url      = props.getProperty("db.url");
            username = props.getProperty("db.username");
            password = props.getProperty("db.password");

        } catch (IOException e) {
            logger.error("Failed to load database properties: {}", e.getMessage());
        }
    }

    /**
     * Returns an active Oracle JDBC connection (creates one if needed).
     */
    public static Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                Class.forName("oracle.jdbc.driver.OracleDriver");
                connection = DriverManager.getConnection(url, username, password);
                connection.setAutoCommit(false);
                logger.info("Database connection established.");
            }
        } catch (ClassNotFoundException e) {
            logger.error("Oracle JDBC Driver not found: {}", e.getMessage());
        } catch (SQLException e) {
            logger.error("Database connection failed: {}", e.getMessage());
        }
        return connection;
    }

    /**
     * Commit the current transaction.
     */
    public static void commit() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.commit();
            }
        } catch (SQLException e) {
            logger.error("Commit failed: {}", e.getMessage());
        }
    }

    /**
     * Rollback the current transaction.
     */
    public static void rollback() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.rollback();
            }
        } catch (SQLException e) {
            logger.error("Rollback failed: {}", e.getMessage());
        }
    }

    /**
     * Close the connection.
     */
    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                logger.info("Database connection closed.");
            }
        } catch (SQLException e) {
            logger.error("Error closing connection: {}", e.getMessage());
        }
    }
}
