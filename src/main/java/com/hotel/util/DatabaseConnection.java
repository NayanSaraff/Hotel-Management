package com.hotel.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thread-safe connection pool manager using HikariCP.
 * Replaces singleton static connection with connection pool for concurrent access.
 */
public class DatabaseConnection {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnection.class);
    private static HikariDataSource dataSource;
    private static final ThreadLocal<Connection> threadConnection = new ThreadLocal<>();

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
     * Returns a connection from the thread-safe pool.
     * Connections are stored per thread via ThreadLocal for commit/rollback/close operations.
     * Callers SHOULD use try-with-resources or call closeConnection() after use.
     */
    public static Connection getConnection() {
        try {
            if (dataSource == null) {
                initializePool();
            }
            Connection conn = threadConnection.get();
            if (conn == null || conn.isClosed()) {
                conn = dataSource.getConnection();
                threadConnection.set(conn);
                logger.trace("New connection acquired for thread: {}", Thread.currentThread().getName());
            }
            return conn;
        } catch (SQLException e) {
            logger.error("Failed to get connection from pool: {}", e.getMessage(), e);
            throw new RuntimeException("Database connection pool exhausted or unavailable", e);
        }
    }

    /**
     * Initialize HikariCP connection pool (synchronized to prevent race conditions on first call).
     */
    private static synchronized void initializePool() {
        if (dataSource != null) return;  // Double-check lock pattern

        try {
            Class.forName("oracle.jdbc.driver.OracleDriver");
            
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(url);
            config.setUsername(username);
            config.setPassword(password);
            config.setMaximumPoolSize(10);                    // Max 10 concurrent connections
            config.setMinimumIdle(3);                         // Keep 3 idle connections ready
            config.setConnectionTimeout(10000);               // 10 second timeout for acquiring connection
            config.setIdleTimeout(600000);                    // 10 minutes idle before closing
            config.setMaxLifetime(1800000);                   // 30 minutes max lifetime
            config.setAutoCommit(false);                      // Manual transaction control
            config.setLeakDetectionThreshold(60000);          // Log connections held >60s
            config.setPoolName("HotelDB-Pool");
            
            dataSource = new HikariDataSource(config);
            logger.info("HikariCP connection pool initialized with max size: {}", config.getMaximumPoolSize());
        } catch (ClassNotFoundException e) {
            logger.error("Oracle JDBC Driver not found: {}", e.getMessage());
            throw new RuntimeException("Oracle JDBC Driver not found", e);
        } catch (Exception e) {
            logger.error("Failed to initialize connection pool: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initialize connection pool", e);
        }
    }

    /**
     * Commit the current thread's transaction.
     */
    public static void commit() {
        try {
            Connection conn = threadConnection.get();
            if (conn != null && !conn.isClosed()) {
                conn.commit();
                logger.trace("Transaction committed for thread: {}", Thread.currentThread().getName());
            }
        } catch (SQLException e) {
            logger.error("Commit failed: {}", e.getMessage());
        }
    }

    /**
     * Rollback the current thread's transaction.
     */
    public static void rollback() {
        try {
            Connection conn = threadConnection.get();
            if (conn != null && !conn.isClosed()) {
                conn.rollback();
                logger.warn("Transaction rolled back for thread: {}", Thread.currentThread().getName());
            }
        } catch (SQLException e) {
            logger.error("Rollback failed: {}", e.getMessage());
        }
    }

    /**
     * Close the current thread's connection and return it to the pool.
     */
    public static void closeConnection() {
        try {
            Connection conn = threadConnection.get();
            if (conn != null && !conn.isClosed()) {
                conn.close();
                threadConnection.remove();
                logger.trace("Connection returned to pool for thread: {}", Thread.currentThread().getName());
            }
        } catch (SQLException e) {
            logger.error("Error closing connection: {}", e.getMessage());
        }
    }

    /**
     * Shutdown the entire connection pool (call on application exit).
     */
    public static void shutdownPool() {
        try {
            closeConnection();  // Close current thread's connection
            if (dataSource != null && !dataSource.isClosed()) {
                dataSource.close();
                logger.info("HikariCP connection pool shut down");
            }
        } catch (Exception e) {
            logger.error("Error shutting down pool: {}", e.getMessage());
        }
    }
}
