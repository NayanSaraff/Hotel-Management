package com.hotel.model;

import java.time.LocalDate;

/**
 * Model class representing a Staff member.
 */
public class Staff {

    public enum Department {
        FRONT_DESK, HOUSEKEEPING, FOOD_BEVERAGE, SECURITY, MAINTENANCE, MANAGEMENT
    }

    private int staffId;
    private int userId;         // linked User account
    private String employeeId;
    private String firstName;
    private String lastName;
    private Department department;
    private String designation;
    private double salary;
    private String phone;
    private String email;
    private LocalDate joiningDate;
    private boolean active;
    private String address;
    private String emergencyContact;

    public Staff() {}

    // ── Getters & Setters ──────────────────────────────────────────────────

    public int getStaffId()                      { return staffId; }
    public void setStaffId(int id)               { this.staffId = id; }

    public int getUserId()                       { return userId; }
    public void setUserId(int id)                { this.userId = id; }

    public String getEmployeeId()                { return employeeId; }
    public void setEmployeeId(String id)         { this.employeeId = id; }

    public String getFirstName()                 { return firstName; }
    public void setFirstName(String n)           { this.firstName = n; }

    public String getLastName()                  { return lastName; }
    public void setLastName(String n)            { this.lastName = n; }

    public String getFullName()                  { return firstName + " " + lastName; }

    public Department getDepartment()            { return department; }
    public void setDepartment(Department d)      { this.department = d; }

    public String getDesignation()               { return designation; }
    public void setDesignation(String d)         { this.designation = d; }

    public double getSalary()                    { return salary; }
    public void setSalary(double s)              { this.salary = s; }

    public String getPhone()                     { return phone; }
    public void setPhone(String p)               { this.phone = p; }

    public String getEmail()                     { return email; }
    public void setEmail(String e)               { this.email = e; }

    public LocalDate getJoiningDate()            { return joiningDate; }
    public void setJoiningDate(LocalDate d)      { this.joiningDate = d; }

    public boolean isActive()                    { return active; }
    public void setActive(boolean a)             { this.active = a; }

    public String getAddress()                   { return address; }
    public void setAddress(String a)             { this.address = a; }

    public String getEmergencyContact()          { return emergencyContact; }
    public void setEmergencyContact(String c)    { this.emergencyContact = c; }

    @Override
    public String toString() {
        return getFullName() + " - " + designation + " (" + department + ")";
    }
}
