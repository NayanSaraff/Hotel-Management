package com.hotel.model;

import java.time.LocalDate;

/**
 * Model class representing a Hotel Guest / Customer.
 */
public class Customer {

    public enum IDType {
        AADHAR, PASSPORT, PAN, DRIVING_LICENSE, VOTER_ID
    }

    private int customerId;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String address;
    private String city;
    private String state;
    private String country;
    private String pinCode;
    private IDType idType;
    private String idNumber;
    private LocalDate dateOfBirth;
    private String nationality;
    private LocalDate registeredDate;

    public Customer() {}

    public Customer(int customerId, String firstName, String lastName,
                    String email, String phone) {
        this.customerId = customerId;
        this.firstName  = firstName;
        this.lastName   = lastName;
        this.email      = email;
        this.phone      = phone;
    }

    // ── Getters & Setters ──────────────────────────────────────────────────

    public int getCustomerId()                    { return customerId; }
    public void setCustomerId(int id)             { this.customerId = id; }

    public String getFirstName()                  { return firstName; }
    public void setFirstName(String n)            { this.firstName = n; }

    public String getLastName()                   { return lastName; }
    public void setLastName(String n)             { this.lastName = n; }

    public String getFullName()                   { return firstName + " " + lastName; }

    public String getEmail()                      { return email; }
    public void setEmail(String e)                { this.email = e; }

    public String getPhone()                      { return phone; }
    public void setPhone(String p)                { this.phone = p; }

    public String getAddress()                    { return address; }
    public void setAddress(String a)              { this.address = a; }

    public String getCity()                       { return city; }
    public void setCity(String c)                 { this.city = c; }

    public String getState()                      { return state; }
    public void setState(String s)                { this.state = s; }

    public String getCountry()                    { return country; }
    public void setCountry(String c)              { this.country = c; }

    public String getPinCode()                    { return pinCode; }
    public void setPinCode(String p)              { this.pinCode = p; }

    public IDType getIdType()                     { return idType; }
    public void setIdType(IDType t)               { this.idType = t; }

    public String getIdNumber()                   { return idNumber; }
    public void setIdNumber(String n)             { this.idNumber = n; }

    public LocalDate getDateOfBirth()             { return dateOfBirth; }
    public void setDateOfBirth(LocalDate d)       { this.dateOfBirth = d; }

    public String getNationality()                { return nationality; }
    public void setNationality(String n)          { this.nationality = n; }

    public LocalDate getRegisteredDate()          { return registeredDate; }
    public void setRegisteredDate(LocalDate d)    { this.registeredDate = d; }

    @Override
    public String toString() {
        return getFullName() + " (" + phone + ")";
    }
}
