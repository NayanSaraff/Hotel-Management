package com.hotel.model;

import java.time.LocalDate;

/**
 * Model for hotel expense tracking (Admin only).
 */
public class Expense {

    public enum Category {
        MAINTENANCE, UTILITIES, SALARIES, SUPPLIES, FOOD_BEVERAGE,
        MARKETING, HOUSEKEEPING, EQUIPMENT, MISCELLANEOUS
    }

    private int expenseId;
    private String title;
    private Category category;
    private double amount;
    private LocalDate expenseDate;
    private String description;
    private String approvedBy;
    private String receiptRef;

    public Expense() {}

    public int getExpenseId()                        { return expenseId; }
    public void setExpenseId(int id)                 { this.expenseId = id; }
    public String getTitle()                         { return title; }
    public void setTitle(String t)                   { this.title = t; }
    public Category getCategory()                    { return category; }
    public void setCategory(Category c)              { this.category = c; }
    public double getAmount()                        { return amount; }
    public void setAmount(double a)                  { this.amount = a; }
    public LocalDate getExpenseDate()                { return expenseDate; }
    public void setExpenseDate(LocalDate d)          { this.expenseDate = d; }
    public String getDescription()                   { return description; }
    public void setDescription(String d)             { this.description = d; }
    public String getApprovedBy()                    { return approvedBy; }
    public void setApprovedBy(String a)              { this.approvedBy = a; }
    public String getReceiptRef()                    { return receiptRef; }
    public void setReceiptRef(String r)              { this.receiptRef = r; }
}
