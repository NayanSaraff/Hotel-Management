package com.hotel.portal.dao;

import com.hotel.portal.model.FoodMenuItem;
import com.hotel.portal.model.FoodOrder;
import com.hotel.portal.model.FoodOrder.FoodOrderItem;
import com.hotel.util.DatabaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for FOOD_ORDERS, FOOD_ORDER_ITEMS, FOOD_MENU_ITEMS tables.
 */
public class FoodOrderDAO {

    private static final Logger logger = LoggerFactory.getLogger(FoodOrderDAO.class);

    // ─── Menu ─────────────────────────────────────────────────────────────

    public List<FoodMenuItem> getAllMenuItems() {
        List<FoodMenuItem> list = new ArrayList<>();
        String sql = "SELECT * FROM FOOD_MENU_ITEMS WHERE AVAILABLE = 1 ORDER BY CATEGORY, ITEM_NAME";
        try {
            try (Statement st = DatabaseConnection.getConnection().createStatement();
                 ResultSet rs = st.executeQuery(sql)) {
                while (rs.next()) list.add(mapMenuItem(rs));
            }
        } catch (SQLException e) {
            logger.error("Error fetching menu: {}", e.getMessage());
        } finally {
            DatabaseConnection.closeConnection();
        }
        return list;
    }

    // ─── Orders ───────────────────────────────────────────────────────────

    public int placeOrder(FoodOrder order) {
        String orderSql = "INSERT INTO FOOD_ORDERS " +
                "(CUSTOMER_ID, BOOKING_ID, ORDER_STATUS, TOTAL_AMOUNT, SPECIAL_NOTES, SEEN_BY_HOTEL) " +
                "VALUES (?, ?, 'RECEIVED', ?, ?, 0)";
        try {
            try (PreparedStatement ps = DatabaseConnection.getConnection()
                    .prepareStatement(orderSql, new String[]{"ORDER_ID"})) {
                ps.setInt(1, order.getCustomerId());
                if (order.getBookingId() > 0) ps.setInt(2, order.getBookingId());
                else ps.setNull(2, Types.INTEGER);
                ps.setDouble(3, order.getTotalAmount());
                ps.setString(4, order.getSpecialNotes());
                ps.executeUpdate();

                int orderId;
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (!rs.next()) { DatabaseConnection.rollback(); return -1; }
                    orderId = rs.getInt(1);
                }

                // Insert line items
                String itemSql = "INSERT INTO FOOD_ORDER_ITEMS " +
                        "(ORDER_ID, MENU_ITEM_ID, ITEM_NAME, QUANTITY, UNIT_PRICE, SUBTOTAL) " +
                        "VALUES (?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ip = DatabaseConnection.getConnection().prepareStatement(itemSql)) {
                    for (FoodOrderItem item : order.getItems()) {
                        ip.setInt(1, orderId);
                        ip.setInt(2, item.getMenuItemId());
                        ip.setString(3, item.getItemName());
                        ip.setInt(4, item.getQuantity());
                        ip.setDouble(5, item.getUnitPrice());
                        ip.setDouble(6, item.getSubtotal());
                        ip.addBatch();
                    }
                    ip.executeBatch();
                }
                DatabaseConnection.commit();
                return orderId;
            }
        } catch (SQLException e) {
            DatabaseConnection.rollback();
            logger.error("Error placing food order: {}", e.getMessage());
            return -1;
        } finally {
            DatabaseConnection.closeConnection();
        }
    }

    public List<FoodOrder> findByCustomer(int customerId) {
        List<FoodOrder> list = new ArrayList<>();
        String sql = "SELECT fo.*, c.FIRST_NAME || ' ' || c.LAST_NAME AS CUSTOMER_NAME " +
                     "FROM FOOD_ORDERS fo " +
                     "JOIN CUSTOMERS c ON fo.CUSTOMER_ID = c.CUSTOMER_ID " +
                     "WHERE fo.CUSTOMER_ID = ? ORDER BY fo.ORDERED_AT DESC";
        try {
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
                ps.setInt(1, customerId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        FoodOrder order = mapOrder(rs);
                        order.setItems(findItemsForOrder(order.getOrderId()));
                        list.add(order);
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Error fetching food orders: {}", e.getMessage());
        } finally {
            DatabaseConnection.closeConnection();
        }
        return list;
    }

    public List<FoodOrder> findAll() {
        List<FoodOrder> list = new ArrayList<>();
        String sql = "SELECT fo.*, c.FIRST_NAME || ' ' || c.LAST_NAME AS CUSTOMER_NAME " +
                     "FROM FOOD_ORDERS fo " +
                     "JOIN CUSTOMERS c ON fo.CUSTOMER_ID = c.CUSTOMER_ID " +
                     "ORDER BY fo.SEEN_BY_HOTEL ASC, fo.ORDERED_AT DESC";
        try {
            try (Statement st = DatabaseConnection.getConnection().createStatement();
                 ResultSet rs = st.executeQuery(sql)) {
                while (rs.next()) {
                    FoodOrder order = mapOrder(rs);
                    order.setItems(findItemsForOrder(order.getOrderId()));
                    list.add(order);
                }
            }
        } catch (SQLException e) {
            logger.error("Error fetching all food orders: {}", e.getMessage());
        } finally {
            DatabaseConnection.closeConnection();
        }
        return list;
    }

    private List<FoodOrderItem> findItemsForOrder(int orderId) {
        List<FoodOrderItem> items = new ArrayList<>();
        String sql = "SELECT * FROM FOOD_ORDER_ITEMS WHERE ORDER_ID = ?";
        try {
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
                ps.setInt(1, orderId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        FoodOrderItem item = new FoodOrderItem();
                        item.setOrderItemId(rs.getInt("ORDER_ITEM_ID"));
                        item.setOrderId(rs.getInt("ORDER_ID"));
                        item.setMenuItemId(rs.getInt("MENU_ITEM_ID"));
                        item.setItemName(rs.getString("ITEM_NAME"));
                        item.setQuantity(rs.getInt("QUANTITY"));
                        item.setUnitPrice(rs.getDouble("UNIT_PRICE"));
                        item.setSubtotal(rs.getDouble("SUBTOTAL"));
                        items.add(item);
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Error fetching order items: {}", e.getMessage());
        } finally {
            DatabaseConnection.closeConnection();
        }
        return items;
    }

    public boolean updateOrderStatus(int orderId, FoodOrder.Status status) {
        String sql = "UPDATE FOOD_ORDERS SET ORDER_STATUS = ?, SEEN_BY_HOTEL = 1, " +
                     "DELIVERED_AT = CASE WHEN ? = 'DELIVERED' THEN SYSDATE ELSE DELIVERED_AT END " +
                     "WHERE ORDER_ID = ?";
        try {
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
                ps.setString(1, status.name());
                ps.setString(2, status.name());
                ps.setInt(3, orderId);
                int rows = ps.executeUpdate();
                DatabaseConnection.commit();
                return rows > 0;
            }
        } catch (SQLException e) {
            DatabaseConnection.rollback();
            logger.error("Error updating order status: {}", e.getMessage());
            return false;
        } finally {
            DatabaseConnection.closeConnection();
        }
    }

    /** Fetch a single order's latest status (for polling) */
    public FoodOrder.Status getOrderStatus(int orderId) {
        String sql = "SELECT ORDER_STATUS FROM FOOD_ORDERS WHERE ORDER_ID = ?";
        try {
            try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
                ps.setInt(1, orderId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return FoodOrder.Status.valueOf(rs.getString("ORDER_STATUS"));
                }
            }
        } catch (SQLException e) {
            logger.error("Error fetching order status: {}", e.getMessage());
        } finally {
            DatabaseConnection.closeConnection();
        }
        return FoodOrder.Status.RECEIVED;
    }

    public void markAllOrdersSeen() {
        String sql = "UPDATE FOOD_ORDERS SET SEEN_BY_HOTEL = 1 WHERE SEEN_BY_HOTEL = 0";
        try {
            try (Statement st = DatabaseConnection.getConnection().createStatement()) {
                st.executeUpdate(sql);
                DatabaseConnection.commit();
            }
        } catch (SQLException e) {
            logger.error("Error marking orders seen: {}", e.getMessage());
        } finally {
            DatabaseConnection.closeConnection();
        }
    }

    public int countUnseenOrders() {
        String sql = "SELECT COUNT(*) FROM FOOD_ORDERS WHERE SEEN_BY_HOTEL = 0";
        try {
            try (Statement st = DatabaseConnection.getConnection().createStatement();
                 ResultSet rs = st.executeQuery(sql)) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            logger.error("Error counting unseen orders: {}", e.getMessage());
        } finally {
            DatabaseConnection.closeConnection();
        }
        return 0;
    }

    private FoodMenuItem mapMenuItem(ResultSet rs) throws SQLException {
        FoodMenuItem m = new FoodMenuItem();
        m.setMenuItemId(rs.getInt("MENU_ITEM_ID"));
        m.setItemName(rs.getString("ITEM_NAME"));
        try { m.setCategory(FoodMenuItem.Category.valueOf(rs.getString("CATEGORY"))); } catch (Exception ignored) {}
        m.setDescription(rs.getString("DESCRIPTION"));
        m.setPrice(rs.getDouble("PRICE"));
        m.setAvailable(rs.getInt("AVAILABLE") == 1);
        m.setPrepTimeMins(rs.getInt("PREP_TIME_MINS"));
        return m;
    }

    private FoodOrder mapOrder(ResultSet rs) throws SQLException {
        FoodOrder o = new FoodOrder();
        o.setOrderId(rs.getInt("ORDER_ID"));
        o.setCustomerId(rs.getInt("CUSTOMER_ID"));
        o.setBookingId(rs.getInt("BOOKING_ID"));
        try { o.setOrderStatus(FoodOrder.Status.valueOf(rs.getString("ORDER_STATUS"))); } catch (Exception ignored) {}
        o.setTotalAmount(rs.getDouble("TOTAL_AMOUNT"));
        o.setSpecialNotes(rs.getString("SPECIAL_NOTES"));
        Timestamp ordered = rs.getDate("ORDERED_AT") != null ? new java.sql.Timestamp(rs.getDate("ORDERED_AT").getTime()) : null;
        if (ordered != null) o.setOrderedAt(ordered.toLocalDateTime());
        Timestamp delivered = rs.getDate("DELIVERED_AT") != null ? new java.sql.Timestamp(rs.getDate("DELIVERED_AT").getTime()) : null;
        if (delivered != null) o.setDeliveredAt(delivered.toLocalDateTime());
        o.setSeenByHotel(rs.getInt("SEEN_BY_HOTEL") == 1);
        try { o.setCustomerName(rs.getString("CUSTOMER_NAME")); } catch (SQLException ignored) {}
        return o;
    }
}
