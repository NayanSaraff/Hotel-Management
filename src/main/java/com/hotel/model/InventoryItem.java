package com.hotel.model;

import java.time.LocalDate;

/**
 * Model class representing an Inventory item.
 */
public class InventoryItem {

    public enum Category {
        LINEN, TOILETRIES, FOOD_BEVERAGE, CLEANING, ELECTRONICS, FURNITURE, STATIONERY, OTHER
    }

    private int itemId;
    private String itemName;
    private Category category;
    private int quantityAvailable;
    private int minimumThreshold;    // low-stock alert
    private String unit;             // pieces, kg, litre, etc.
    private double unitPrice;
    private String supplier;
    private LocalDate lastRestocked;

    public InventoryItem() {}

    // ── Getters & Setters ──────────────────────────────────────────────────

    public int getItemId()                       { return itemId; }
    public void setItemId(int id)                { this.itemId = id; }

    public String getItemName()                  { return itemName; }
    public void setItemName(String n)            { this.itemName = n; }

    public Category getCategory()                { return category; }
    public void setCategory(Category c)          { this.category = c; }

    public int getQuantityAvailable()            { return quantityAvailable; }
    public void setQuantityAvailable(int q)      { this.quantityAvailable = q; }

    public int getMinimumThreshold()             { return minimumThreshold; }
    public void setMinimumThreshold(int t)       { this.minimumThreshold = t; }

    public String getUnit()                      { return unit; }
    public void setUnit(String u)                { this.unit = u; }

    public double getUnitPrice()                 { return unitPrice; }
    public void setUnitPrice(double p)           { this.unitPrice = p; }

    public String getSupplier()                  { return supplier; }
    public void setSupplier(String s)            { this.supplier = s; }

    public LocalDate getLastRestocked()          { return lastRestocked; }
    public void setLastRestocked(LocalDate d)    { this.lastRestocked = d; }

    public boolean isLowStock() {
        return quantityAvailable <= minimumThreshold;
    }

    @Override
    public String toString() {
        return itemName + " [" + category + "] - " + quantityAvailable + " " + unit;
    }
}
