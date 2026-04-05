package com.hotel.portal.model;

import java.time.LocalDateTime;

/**
 * Represents a service request placed by a customer via the portal.
 */
public class ServiceRequest {

    public enum Type {
        HOUSEKEEPING, PHONE_CALL, EXTRA_TOWELS,
        EXTRA_PILLOW, WAKE_UP_CALL, LAUNDRY,
        TAXI, GENERAL, MAINTENANCE
    }

    public enum Status {
        PENDING, IN_PROGRESS, COMPLETED, CANCELLED
    }

    public enum Priority {
        LOW, NORMAL, HIGH, URGENT
    }

    private int requestId;
    private int customerId;
    private int bookingId;
    private Type requestType;
    private String description;
    private String phoneNumber;
    private Status status;
    private Priority priority;
    private LocalDateTime requestedAt;
    private LocalDateTime completedAt;
    private String notes;
    private boolean seenByHotel;

    // Display fields
    private String customerName;

    public ServiceRequest() {
    }

    public int getRequestId() {
        return requestId;
    }

    public void setRequestId(int id) {
        this.requestId = id;
    }

    public int getCustomerId() {
        return customerId;
    }

    public void setCustomerId(int id) {
        this.customerId = id;
    }

    public int getBookingId() {
        return bookingId;
    }

    public void setBookingId(int id) {
        this.bookingId = id;
    }

    public Type getRequestType() {
        return requestType;
    }

    public void setRequestType(Type t) {
        this.requestType = t;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String d) {
        this.description = d;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String p) {
        this.phoneNumber = p;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status s) {
        this.status = s;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority p) {
        this.priority = p;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(LocalDateTime t) {
        this.requestedAt = t;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime t) {
        this.completedAt = t;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String n) {
        this.notes = n;
    }

    public boolean isSeenByHotel() {
        return seenByHotel;
    }

    public void setSeenByHotel(boolean b) {
        this.seenByHotel = b;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String n) {
        this.customerName = n;
    }

    public String getTypeDisplayName() {
        if (requestType == null)
            return "";
        return switch (requestType) {
            case HOUSEKEEPING -> "Housekeeping";
            case PHONE_CALL -> "Phone Call";
            case EXTRA_TOWELS -> "Extra Towels";
            case EXTRA_PILLOW -> "Extra Pillow";
            case WAKE_UP_CALL -> "Wake-up Call";
            case LAUNDRY -> "Laundry";
            case TAXI -> "Taxi / Transfer";
            case GENERAL -> "General Request";
            case MAINTENANCE -> "Maintenance";
        };
    }
}
