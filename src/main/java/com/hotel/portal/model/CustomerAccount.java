package com.hotel.portal.model;

import java.time.LocalDateTime;

/**
 * Represents a customer's portal login account.
 * Links to the CUSTOMER_ACCOUNTS table.
 */
public class CustomerAccount {

    private int accountId;
    private int customerId;
    private String username;   // email
    private String passwordHash;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;

    // Joined fields
    private String firstName;
    private String lastName;
    private String email;
    private String phone;

    public CustomerAccount() {}

    // ── Getters & Setters ──────────────────────────────────────────────────

    public int getAccountId()                       { return accountId; }
    public void setAccountId(int id)                { this.accountId = id; }

    public int getCustomerId()                      { return customerId; }
    public void setCustomerId(int id)               { this.customerId = id; }

    public String getUsername()                     { return username; }
    public void setUsername(String u)               { this.username = u; }

    public String getPasswordHash()                 { return passwordHash; }
    public void setPasswordHash(String h)           { this.passwordHash = h; }

    public boolean isActive()                       { return active; }
    public void setActive(boolean a)                { this.active = a; }

    public LocalDateTime getCreatedAt()             { return createdAt; }
    public void setCreatedAt(LocalDateTime t)       { this.createdAt = t; }

    public LocalDateTime getLastLogin()             { return lastLogin; }
    public void setLastLogin(LocalDateTime t)       { this.lastLogin = t; }

    public String getFirstName()                    { return firstName; }
    public void setFirstName(String n)              { this.firstName = n; }

    public String getLastName()                     { return lastName; }
    public void setLastName(String n)               { this.lastName = n; }

    public String getFullName()                     { return firstName + " " + lastName; }

    public String getEmail()                        { return email; }
    public void setEmail(String e)                  { this.email = e; }

    public String getPhone()                        { return phone; }
    public void setPhone(String p)                  { this.phone = p; }
}
