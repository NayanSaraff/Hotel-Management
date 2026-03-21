package com.hotel.model;

import java.time.LocalDateTime;

/**
 * Model class representing a system User (Admin, Manager, Receptionist).
 */
public class User {

    private int userId;
    private String username;
    private String passwordHash;
    private String fullName;
    private String email;
    private String phone;
    private String role;          // ADMIN | MANAGER | RECEPTIONIST
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;

    public User() {}

    public User(int userId, String username, String fullName, String email,
                String phone, String role, boolean active) {
        this.userId   = userId;
        this.username = username;
        this.fullName = fullName;
        this.email    = email;
        this.phone    = phone;
        this.role     = role;
        this.active   = active;
    }

    // ── Getters & Setters ──────────────────────────────────────────────────

    public int getUserId()             { return userId; }
    public void setUserId(int id)      { this.userId = id; }

    public String getUsername()              { return username; }
    public void setUsername(String u)        { this.username = u; }

    public String getPasswordHash()          { return passwordHash; }
    public void setPasswordHash(String h)    { this.passwordHash = h; }

    public String getFullName()              { return fullName; }
    public void setFullName(String n)        { this.fullName = n; }

    public String getEmail()                 { return email; }
    public void setEmail(String e)           { this.email = e; }

    public String getPhone()                 { return phone; }
    public void setPhone(String p)           { this.phone = p; }

    public String getRole()                  { return role; }
    public void setRole(String r)            { this.role = r; }

    public boolean isActive()               { return active; }
    public void setActive(boolean a)        { this.active = a; }

    public LocalDateTime getCreatedAt()      { return createdAt; }
    public void setCreatedAt(LocalDateTime t){ this.createdAt = t; }

    public LocalDateTime getLastLogin()      { return lastLogin; }
    public void setLastLogin(LocalDateTime t){ this.lastLogin = t; }

    @Override
    public String toString() {
        return fullName + " (" + role + ")";
    }
}
