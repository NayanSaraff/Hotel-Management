package com.hotel.portal.model;

/**
 * Represents an item on the hotel food menu.
 */
public class FoodMenuItem {

    public enum Category {
        BREAKFAST, LUNCH, DINNER, SNACKS,
        BEVERAGES, DESSERTS, ROOM_SERVICE
    }

    private int menuItemId;
    private String itemName;
    private Category category;
    private String description;
    private double price;
    private boolean available;
    private int prepTimeMins;

    // Cart quantity (not stored in DB)
    private int cartQuantity = 0;

    public FoodMenuItem() {}

    public int getMenuItemId()                      { return menuItemId; }
    public void setMenuItemId(int id)               { this.menuItemId = id; }

    public String getItemName()                     { return itemName; }
    public void setItemName(String n)               { this.itemName = n; }

    public Category getCategory()                   { return category; }
    public void setCategory(Category c)             { this.category = c; }

    public String getDescription()                  { return description; }
    public void setDescription(String d)            { this.description = d; }

    public double getPrice()                        { return price; }
    public void setPrice(double p)                  { this.price = p; }

    public boolean isAvailable()                    { return available; }
    public void setAvailable(boolean a)             { this.available = a; }

    public int getPrepTimeMins()                    { return prepTimeMins; }
    public void setPrepTimeMins(int m)              { this.prepTimeMins = m; }

    public int getCartQuantity()                    { return cartQuantity; }
    public void setCartQuantity(int q)              { this.cartQuantity = q; }

    public String getCategoryDisplayName() {
        if (category == null) return "";
        return switch (category) {
            case BREAKFAST    -> "☀️ Breakfast";
            case LUNCH        -> "🍱 Lunch";
            case DINNER       -> "🍽️ Dinner";
            case SNACKS       -> "🍟 Snacks";
            case BEVERAGES    -> "☕ Beverages";
            case DESSERTS     -> "🍰 Desserts";
            case ROOM_SERVICE -> "🛎️ Room Service";
        };
    }
}
