package com.hotel.dao;

import com.hotel.model.InventoryItem;
import com.hotel.util.DatabaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO for InventoryItem CRUD operations.
 */
public class InventoryDAO implements GenericDAO<InventoryItem, Integer> {

    private static final Logger logger = LoggerFactory.getLogger(InventoryDAO.class);

    @Override
    public int save(InventoryItem item) {
        String sql = "INSERT INTO INVENTORY (ITEM_NAME,CATEGORY,QUANTITY_AVAILABLE," +
                     "MINIMUM_THRESHOLD,UNIT,UNIT_PRICE,SUPPLIER,LAST_RESTOCKED) " +
                     "VALUES (?,?,?,?,?,?,?,?)";
        try {
            try (PreparedStatement ps = DatabaseConnection.getConnection()
                .prepareStatement(sql, new String[]{"ITEM_ID"})) {
            ps.setString(1, item.getItemName());
            ps.setString(2, item.getCategory().name());
            ps.setInt(3, item.getQuantityAvailable());
            ps.setInt(4, item.getMinimumThreshold());
            ps.setString(5, item.getUnit());
            ps.setDouble(6, item.getUnitPrice());
            ps.setString(7, item.getSupplier());
            ps.setDate(8, item.getLastRestocked() != null ?
                    Date.valueOf(item.getLastRestocked()) : null);
            ps.executeUpdate();
            DatabaseConnection.commit();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
            }
        } catch (SQLException e) {
            DatabaseConnection.rollback();
            logger.error("Error saving inventory item: {}", e.getMessage());
        } finally {
            DatabaseConnection.closeConnection();
        }
        return -1;
    }

    @Override
    public boolean update(InventoryItem item) {
        String sql = "UPDATE INVENTORY SET ITEM_NAME=?,CATEGORY=?,QUANTITY_AVAILABLE=?," +
                     "MINIMUM_THRESHOLD=?,UNIT=?,UNIT_PRICE=?,SUPPLIER=?,LAST_RESTOCKED=? " +
                     "WHERE ITEM_ID=?";
        try {
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, item.getItemName());
            ps.setString(2, item.getCategory().name());
            ps.setInt(3, item.getQuantityAvailable());
            ps.setInt(4, item.getMinimumThreshold());
            ps.setString(5, item.getUnit());
            ps.setDouble(6, item.getUnitPrice());
            ps.setString(7, item.getSupplier());
            ps.setDate(8, item.getLastRestocked() != null ?
                    Date.valueOf(item.getLastRestocked()) : null);
            ps.setInt(9, item.getItemId());
            int rows = ps.executeUpdate();
            DatabaseConnection.commit();
            return rows > 0;
            }
        } catch (SQLException e) {
            DatabaseConnection.rollback();
            logger.error("Error updating inventory item: {}", e.getMessage());
            return false;
        } finally {
            DatabaseConnection.closeConnection();
        }
    }

    @Override
    public boolean delete(Integer id) {
        String sql = "DELETE FROM INVENTORY WHERE ITEM_ID=?";
        try {
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            int rows = ps.executeUpdate();
            DatabaseConnection.commit();
            return rows > 0;
            }
        } catch (SQLException e) {
            DatabaseConnection.rollback();
            logger.error("Error deleting inventory item: {}", e.getMessage());
            return false;
        } finally {
            DatabaseConnection.closeConnection();
        }
    }

    @Override
    public Optional<InventoryItem> findById(Integer id) {
        String sql = "SELECT * FROM INVENTORY WHERE ITEM_ID=?";
        try {
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding inventory item: {}", e.getMessage());
        } finally {
            DatabaseConnection.closeConnection();
        }
        return Optional.empty();
    }

    @Override
    public List<InventoryItem> findAll() {
        List<InventoryItem> list = new ArrayList<>();
        String sql = "SELECT * FROM INVENTORY ORDER BY CATEGORY, ITEM_NAME";
        try {
            try (Statement st = DatabaseConnection.getConnection().createStatement();
                 ResultSet rs = st.executeQuery(sql)) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            logger.error("Error fetching inventory: {}", e.getMessage());
        } finally {
            DatabaseConnection.closeConnection();
        }
        return list;
    }

    /** Return items at or below minimum threshold. */
    public List<InventoryItem> findLowStock() {
        List<InventoryItem> list = new ArrayList<>();
        String sql = "SELECT * FROM INVENTORY WHERE QUANTITY_AVAILABLE <= MINIMUM_THRESHOLD " +
                     "ORDER BY QUANTITY_AVAILABLE";
        try {
            try (Statement st = DatabaseConnection.getConnection().createStatement();
                 ResultSet rs = st.executeQuery(sql)) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            logger.error("Error fetching low-stock items: {}", e.getMessage());
        } finally {
            DatabaseConnection.closeConnection();
        }
        return list;
    }

    /** Adjust stock quantity (positive = restock, negative = consume). */
    public boolean adjustQuantity(int itemId, int delta) {
        String sql = "UPDATE INVENTORY SET QUANTITY_AVAILABLE = QUANTITY_AVAILABLE + ?, " +
                     "LAST_RESTOCKED = CASE WHEN ? > 0 THEN SYSDATE ELSE LAST_RESTOCKED END " +
                     "WHERE ITEM_ID=?";
        try {
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, delta);
            ps.setInt(2, delta);
            ps.setInt(3, itemId);
            int rows = ps.executeUpdate();
            DatabaseConnection.commit();
            return rows > 0;
            }
        } catch (SQLException e) {
            DatabaseConnection.rollback();
            logger.error("Error adjusting inventory quantity: {}", e.getMessage());
            return false;
        } finally {
            DatabaseConnection.closeConnection();
        }
    }

    private InventoryItem mapRow(ResultSet rs) throws SQLException {
        InventoryItem i = new InventoryItem();
        i.setItemId(rs.getInt("ITEM_ID"));
        i.setItemName(rs.getString("ITEM_NAME"));
        i.setCategory(InventoryItem.Category.valueOf(rs.getString("CATEGORY")));
        i.setQuantityAvailable(rs.getInt("QUANTITY_AVAILABLE"));
        i.setMinimumThreshold(rs.getInt("MINIMUM_THRESHOLD"));
        i.setUnit(rs.getString("UNIT"));
        i.setUnitPrice(rs.getDouble("UNIT_PRICE"));
        i.setSupplier(rs.getString("SUPPLIER"));
        Date d = rs.getDate("LAST_RESTOCKED");
        if (d != null) i.setLastRestocked(d.toLocalDate());
        return i;
    }
}
