package com.hotel.model;

/**
 * Model class representing a Hotel Room.
 */
public class Room {

    public enum Status {
        AVAILABLE, OCCUPIED, MAINTENANCE, HOUSEKEEPING
    }

    public enum Category {
        STANDARD, DELUXE, SUITE, PRESIDENTIAL
    }

    private int roomId;
    private String roomNumber;
    private Category category;
    private int floor;
    private int capacity;
    private double pricePerNight;
    private Status status;
    private String description;
    private String amenities;    // comma-separated list
    private String bedType;      // SINGLE | DOUBLE | KING | QUEEN | TWIN
    private boolean hasAC;
    private boolean hasWifi;
    private boolean hasTV;

    public Room() {}

    public Room(int roomId, String roomNumber, Category category,
                int floor, int capacity, double pricePerNight, Status status) {
        this.roomId        = roomId;
        this.roomNumber    = roomNumber;
        this.category      = category;
        this.floor         = floor;
        this.capacity      = capacity;
        this.pricePerNight = pricePerNight;
        this.status        = status;
    }

    // ── Getters & Setters ──────────────────────────────────────────────────

    public int getRoomId()                    { return roomId; }
    public void setRoomId(int id)             { this.roomId = id; }

    public String getRoomNumber()             { return roomNumber; }
    public void setRoomNumber(String n)       { this.roomNumber = n; }

    public Category getCategory()             { return category; }
    public void setCategory(Category c)       { this.category = c; }

    public int getFloor()                     { return floor; }
    public void setFloor(int f)               { this.floor = f; }

    public int getCapacity()                  { return capacity; }
    public void setCapacity(int c)            { this.capacity = c; }

    public double getPricePerNight()          { return pricePerNight; }
    public void setPricePerNight(double p)    { this.pricePerNight = p; }

    public Status getStatus()                 { return status; }
    public void setStatus(Status s)           { this.status = s; }

    public String getDescription()            { return description; }
    public void setDescription(String d)      { this.description = d; }

    public String getAmenities()              { return amenities; }
    public void setAmenities(String a)        { this.amenities = a; }

    public String getBedType()                { return bedType; }
    public void setBedType(String b)          { this.bedType = b; }

    public boolean isHasAC()                  { return hasAC; }
    public void setHasAC(boolean h)           { this.hasAC = h; }

    public boolean isHasWifi()                { return hasWifi; }
    public void setHasWifi(boolean h)         { this.hasWifi = h; }

    public boolean isHasTV()                  { return hasTV; }
    public void setHasTV(boolean h)           { this.hasTV = h; }

    public String getStatusDisplay() {
        return status != null ? status.name() : "—";
    }

    @Override
    public String toString() {
        return "Room " + roomNumber + " [" + category + "] - ₹" + pricePerNight + "/night";
    }
}
