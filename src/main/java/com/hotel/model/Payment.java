package com.hotel.model;

import java.time.LocalDateTime;

/**
 * Model class representing a Payment transaction.
 */
public class Payment {

    public enum Mode {
        CASH, CREDIT_CARD, DEBIT_CARD, UPI, NET_BANKING, CHEQUE
    }

    public enum Type {
        ADVANCE, PARTIAL, FULL, REFUND
    }

    private int paymentId;
    private int bookingId;
    private double amount;
    private Mode paymentMode;
    private Type paymentType;
    private String transactionId;
    private LocalDateTime paymentDate;
    private String remarks;

    // Display
    private String bookingReference;

    public Payment() {}

    public Payment(int bookingId, double amount, Mode mode, Type type) {
        this.bookingId   = bookingId;
        this.amount      = amount;
        this.paymentMode = mode;
        this.paymentType = type;
        this.paymentDate = LocalDateTime.now();
    }

    // ── Getters & Setters ──────────────────────────────────────────────────

    public int getPaymentId()                    { return paymentId; }
    public void setPaymentId(int id)             { this.paymentId = id; }

    public int getBookingId()                    { return bookingId; }
    public void setBookingId(int id)             { this.bookingId = id; }

    public double getAmount()                    { return amount; }
    public void setAmount(double a)              { this.amount = a; }

    public Mode getPaymentMode()                 { return paymentMode; }
    public void setPaymentMode(Mode m)           { this.paymentMode = m; }

    public Type getPaymentType()                 { return paymentType; }
    public void setPaymentType(Type t)           { this.paymentType = t; }

    public String getTransactionId()             { return transactionId; }
    public void setTransactionId(String t)       { this.transactionId = t; }

    public LocalDateTime getPaymentDate()        { return paymentDate; }
    public void setPaymentDate(LocalDateTime d)  { this.paymentDate = d; }

    public String getRemarks()                   { return remarks; }
    public void setRemarks(String r)             { this.remarks = r; }

    public String getBookingReference()          { return bookingReference; }
    public void setBookingReference(String r)    { this.bookingReference = r; }

    @Override
    public String toString() {
        return "Payment#" + paymentId + " ₹" + amount + " [" + paymentMode + "]";
    }
}
