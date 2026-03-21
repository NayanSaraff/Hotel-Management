package com.hotel.service;

import com.hotel.dao.RoomDAO;
import com.hotel.model.Room;
import com.hotel.util.DatabaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Housekeeping Service:
 * - When a guest checks out, room is marked HOUSEKEEPING.
 * - After 20 minutes, room is automatically marked AVAILABLE.
 * - Provides status messages and task tracking.
 */
public class HousekeepingService {

    private static final Logger logger = LoggerFactory.getLogger(HousekeepingService.class);
    private static final int CLEANING_DELAY_MINUTES = 20;

    private final RoomDAO roomDAO = new RoomDAO();
    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(2);

    // ── Trigger Housekeeping After Checkout ───────────────────────────────

    /**
     * Call this after guest checkout.
     * Sets room to HOUSEKEEPING and schedules auto-availability after 20 min.
     *
     * @param roomId       the room to clean
     * @param roomNumber   for logging/display
     * @param onComplete   optional Runnable to refresh UI when done (run on FX thread)
     */
    public void startCleaning(int roomId, String roomNumber, Runnable onComplete) {
        // Mark room as HOUSEKEEPING immediately
        roomDAO.updateStatus(roomId, Room.Status.HOUSEKEEPING);
        logHousekeepingTask(roomId, "CLEANING_STARTED");
        logger.info("Room {} housekeeping started. Will be available in {} minutes.",
                roomNumber, CLEANING_DELAY_MINUTES);

        // Schedule room to become AVAILABLE after 20 minutes
        scheduler.schedule(() -> {
            try {
                roomDAO.updateStatus(roomId, Room.Status.AVAILABLE);
                logHousekeepingTask(roomId, "CLEANING_COMPLETE");
                logger.info("Room {} cleaning complete. Now AVAILABLE.", roomNumber);
                if (onComplete != null) {
                    javafx.application.Platform.runLater(onComplete);
                }
            } catch (Exception e) {
                logger.error("Error completing housekeeping for room {}: {}", roomNumber, e.getMessage());
            }
        }, CLEANING_DELAY_MINUTES, TimeUnit.MINUTES);
    }

    /**
     * For testing: start cleaning with custom delay in seconds.
     */
    public void startCleaningWithDelay(int roomId, String roomNumber,
                                        int delaySeconds, Runnable onComplete) {
        roomDAO.updateStatus(roomId, Room.Status.HOUSEKEEPING);
        logHousekeepingTask(roomId, "CLEANING_STARTED");
        logger.info("Room {} housekeeping started. Will be available in {} seconds (test mode).",
                roomNumber, delaySeconds);

        scheduler.schedule(() -> {
            roomDAO.updateStatus(roomId, Room.Status.AVAILABLE);
            logHousekeepingTask(roomId, "CLEANING_COMPLETE");
            logger.info("Room {} cleaning complete. Now AVAILABLE.", roomNumber);
            if (onComplete != null) {
                javafx.application.Platform.runLater(onComplete);
            }
        }, delaySeconds, TimeUnit.SECONDS);
    }

    /**
     * Manually mark room as clean (override the timer).
     */
    public boolean markRoomClean(int roomId) {
        logHousekeepingTask(roomId, "MANUAL_CLEAN");
        return roomDAO.updateStatus(roomId, Room.Status.AVAILABLE);
    }

    /**
     * Mark room as under maintenance.
     */
    public boolean markMaintenance(int roomId) {
        return roomDAO.updateStatus(roomId, Room.Status.MAINTENANCE);
    }

    /**
     * Get status message for display.
     */
    public String getStatusMessage(Room.Status status) {
        return switch (status) {
            case HOUSEKEEPING -> "🧹 Room is being cleaned. Will be available in ~" +
                    CLEANING_DELAY_MINUTES + " minutes.";
            case AVAILABLE    -> "✅ Room is available for booking.";
            case OCCUPIED     -> "🔴 Room is currently occupied.";
            case MAINTENANCE  -> "🔧 Room is under maintenance.";
        };
    }

    /**
     * Get cleaning delay minutes.
     */
    public int getCleaningDelayMinutes() { return CLEANING_DELAY_MINUTES; }

    // ── Housekeeping Log ──────────────────────────────────────────────────

    private void logHousekeepingTask(int roomId, String taskType) {
        String sql = "INSERT INTO HOUSEKEEPING_LOG (ROOM_ID, TASK_TYPE, TASK_TIME) VALUES (?,?,SYSDATE)";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, roomId);
            ps.setString(2, taskType);
            ps.executeUpdate();
            DatabaseConnection.commit();
        } catch (SQLException e) {
            // Table may not exist yet — silently skip
            logger.debug("Housekeeping log skipped: {}", e.getMessage());
        }
    }

    public void shutdown() {
        scheduler.shutdown();
    }
}
