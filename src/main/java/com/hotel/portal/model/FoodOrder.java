package com.hotel.portal.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a food order placed by a customer.
 */
public class FoodOrder {

    public enum Status {
        RECEIVED, PREPARING, OUT_FOR_DELIVERY, DELIVERED, CANCELLED
    }

    private int orderId;
    private int customerId;
    private int bookingId;
    private Status orderStatus;
    private double totalAmount;
    private String specialNotes;
    private LocalDateTime orderedAt;
    private LocalDateTime deliveredAt;
    private boolean seenByHotel;

    private List<FoodOrderItem> items = new ArrayList<>();

    // Display fields
    private String customerName;

    public FoodOrder() {}

    public int getOrderId()                         { return orderId; }
    public void setOrderId(int id)                  { this.orderId = id; }

    public int getCustomerId()                      { return customerId; }
    public void setCustomerId(int id)               { this.customerId = id; }

    public int getBookingId()                       { return bookingId; }
    public void setBookingId(int id)                { this.bookingId = id; }

    public Status getOrderStatus()                  { return orderStatus; }
    public void setOrderStatus(Status s)            { this.orderStatus = s; }

    public double getTotalAmount()                  { return totalAmount; }
    public void setTotalAmount(double a)            { this.totalAmount = a; }

    public String getSpecialNotes()                 { return specialNotes; }
    public void setSpecialNotes(String n)           { this.specialNotes = n; }

    public LocalDateTime getOrderedAt()             { return orderedAt; }
    public void setOrderedAt(LocalDateTime t)       { this.orderedAt = t; }

    public LocalDateTime getDeliveredAt()           { return deliveredAt; }
    public void setDeliveredAt(LocalDateTime t)     { this.deliveredAt = t; }

    public boolean isSeenByHotel()                  { return seenByHotel; }
    public void setSeenByHotel(boolean b)           { this.seenByHotel = b; }

    public List<FoodOrderItem> getItems()           { return items; }
    public void setItems(List<FoodOrderItem> i)     { this.items = i; }

    public String getCustomerName()                 { return customerName; }
    public void setCustomerName(String n)           { this.customerName = n; }

    public String getStatusDisplayName() {
        if (orderStatus == null) return "";
        return switch (orderStatus) {
            case RECEIVED         -> "📥 Received";
            case PREPARING        -> "👨‍🍳 Preparing";
            case OUT_FOR_DELIVERY -> "🚚 On the Way";
            case DELIVERED        -> "✅ Delivered";
            case CANCELLED        -> "❌ Cancelled";
        };
    }

    /** Line items inside a food order */
    public static class FoodOrderItem {
        private int orderItemId;
        private int orderId;
        private int menuItemId;
        private String itemName;
        private int quantity;
        private double unitPrice;
        private double subtotal;

        public FoodOrderItem() {}

        public FoodOrderItem(int menuItemId, String itemName, int quantity, double unitPrice) {
            this.menuItemId = menuItemId;
            this.itemName   = itemName;
            this.quantity   = quantity;
            this.unitPrice  = unitPrice;
            this.subtotal   = quantity * unitPrice;
        }

        public int getOrderItemId()                 { return orderItemId; }
        public void setOrderItemId(int id)          { this.orderItemId = id; }

        public int getOrderId()                     { return orderId; }
        public void setOrderId(int id)              { this.orderId = id; }

        public int getMenuItemId()                  { return menuItemId; }
        public void setMenuItemId(int id)           { this.menuItemId = id; }

        public String getItemName()                 { return itemName; }
        public void setItemName(String n)           { this.itemName = n; }

        public int getQuantity()                    { return quantity; }
        public void setQuantity(int q)              { this.quantity = q; }

        public double getUnitPrice()                { return unitPrice; }
        public void setUnitPrice(double p)          { this.unitPrice = p; }

        public double getSubtotal()                 { return subtotal; }
        public void setSubtotal(double s)           { this.subtotal = s; }
    }
}
