package com.hotel.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Model class representing a Booking / Reservation.
 */
public class Booking {

    public enum Status {
        CONFIRMED, CHECKED_IN, CHECKED_OUT, CANCELLED, NO_SHOW
    }

    private int bookingId;
    private String bookingReference;   // e.g. "HMS-2024-001"
    private int customerId;
    private int roomId;
    private int userId;                // staff who created it

    private LocalDate checkInDate;
    private LocalDate checkOutDate;
    private LocalDateTime actualCheckIn;
    private LocalDateTime actualCheckOut;

    private int numberOfGuests;
    private double roomCharges;
    private double serviceCharges;
    private double gstAmount;
    private double totalAmount;
    private double advancePaid;
    private double balanceDue;

    private Status status;
    private String specialRequests;
    private LocalDateTime bookingDate;

    // Joined fields (for display only)
    private String customerName;
    private String roomNumber;
    private String roomCategory;

    public Booking() {}

    // ── Computed ──────────────────────────────────────────────────────────

    public long getNumberOfNights() {
        if (checkInDate == null || checkOutDate == null) return 0;
        return ChronoUnit.DAYS.between(checkInDate, checkOutDate);
    }

    // ── Getters & Setters ──────────────────────────────────────────────────

    public int getBookingId()                     { return bookingId; }
    public void setBookingId(int id)              { this.bookingId = id; }

    public String getBookingReference()           { return bookingReference; }
    public void setBookingReference(String r)     { this.bookingReference = r; }

    public int getCustomerId()                    { return customerId; }
    public void setCustomerId(int id)             { this.customerId = id; }

    public int getRoomId()                        { return roomId; }
    public void setRoomId(int id)                 { this.roomId = id; }

    public int getUserId()                        { return userId; }
    public void setUserId(int id)                 { this.userId = id; }

    public LocalDate getCheckInDate()             { return checkInDate; }
    public void setCheckInDate(LocalDate d)       { this.checkInDate = d; }

    public LocalDate getCheckOutDate()            { return checkOutDate; }
    public void setCheckOutDate(LocalDate d)      { this.checkOutDate = d; }

    public LocalDateTime getActualCheckIn()       { return actualCheckIn; }
    public void setActualCheckIn(LocalDateTime t) { this.actualCheckIn = t; }

    public LocalDateTime getActualCheckOut()      { return actualCheckOut; }
    public void setActualCheckOut(LocalDateTime t){ this.actualCheckOut = t; }

    public int getNumberOfGuests()               { return numberOfGuests; }
    public void setNumberOfGuests(int n)         { this.numberOfGuests = n; }

    public double getRoomCharges()               { return roomCharges; }
    public void setRoomCharges(double r)         { this.roomCharges = r; }

    public double getServiceCharges()            { return serviceCharges; }
    public void setServiceCharges(double s)      { this.serviceCharges = s; }

    public double getGstAmount()                 { return gstAmount; }
    public void setGstAmount(double g)           { this.gstAmount = g; }

    public double getTotalAmount()               { return totalAmount; }
    public void setTotalAmount(double t)         { this.totalAmount = t; }

    public double getAdvancePaid()               { return advancePaid; }
    public void setAdvancePaid(double a)         { this.advancePaid = a; }

    public double getBalanceDue()                { return balanceDue; }
    public void setBalanceDue(double b)          { this.balanceDue = b; }

    public Status getStatus()                    { return status; }
    public void setStatus(Status s)              { this.status = s; }

    public String getSpecialRequests()           { return specialRequests; }
    public void setSpecialRequests(String s)     { this.specialRequests = s; }

    public LocalDateTime getBookingDate()        { return bookingDate; }
    public void setBookingDate(LocalDateTime d)  { this.bookingDate = d; }

    public String getCustomerName()              { return customerName; }
    public void setCustomerName(String n)        { this.customerName = n; }

    public String getRoomNumber()                { return roomNumber; }
    public void setRoomNumber(String n)          { this.roomNumber = n; }

    public String getRoomCategory()              { return roomCategory; }
    public void setRoomCategory(String c)        { this.roomCategory = c; }

    @Override
    public String toString() {
        return bookingReference + " | " + customerName + " | Room " + roomNumber;
    }
}
