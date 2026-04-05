# Hotel Management System + Customer Portal

Unified project for hotel staff operations and guest self-service, built on JavaFX, Oracle, and Maven.

![Java](https://img.shields.io/badge/Java-17-1f6feb)
![JavaFX](https://img.shields.io/badge/JavaFX-21-0a2540)
![Maven](https://img.shields.io/badge/Maven-3.8%2B-c71a36)
![Oracle](https://img.shields.io/badge/Oracle-XE%20%2F%2021c-f80000)
![Portal](https://img.shields.io/badge/Profile-portal-7c3aed)

---

## Index

- [Overview](#overview)
- [Applications in This Repository](#applications-in-this-repository)
- [Feature Summary](#feature-summary)
- [Technology Stack](#technology-stack)
- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Database Setup](#database-setup)
- [Configuration](#configuration)
- [Run Commands](#run-commands)
- [Default Credentials](#default-credentials)
- [Portal User Journey](#portal-user-journey)
- [Staff and Portal Synchronization](#staff-and-portal-synchronization)
- [Staff Modules](#staff-modules)
- [Portal Modules](#portal-modules)
- [Architecture](#architecture)
- [Folder Structure](#folder-structure)
- [Useful Commands](#useful-commands)
- [Troubleshooting](#troubleshooting)

---

## Overview

This repository contains two applications that run independently but share the same Oracle database:

- Hotel Staff Application: front desk, operations, billing, reports, management
- Customer Portal Application: booking, requests, food ordering, account management

The shared data model enables real-time operational flow between guests and hotel staff.

---

## Applications in This Repository

### 1) Hotel Staff Application

- main class: com.hotel.MainApp
- purpose: daily hotel operations for admin, manager, and receptionist roles

### 2) Customer Portal Application

- main class: com.hotel.portal.CustomerPortalApp
- Maven profile: portal
- purpose: guest self-service before and during stay

---

## Feature Summary

### Staff Side

- role-based authentication
- dashboard metrics and reports
- booking lifecycle: create, check-in, check-out, cancel
- customer and room management
- inventory, staff, expense, payment workflows
- invoice generation and email flows
- portal requests management (bookings, service requests, food orders, calls)

### Customer Portal Side

- registration and login
- room search and booking creation
- service requests (housekeeping, laundry, taxi, wake-up, maintenance, more)
- food ordering with status tracking
- profile and password management

---

## Technology Stack

| Layer | Technology |
|---|---|
| Language | Java 17 |
| UI | JavaFX 21 (FXML + Controllers) |
| Build | Maven |
| Database | Oracle via ojdbc8 21.9 |
| Connection Pool | HikariCP |
| Security | BCrypt |
| Reporting/PDF | iText 5 |
| Logging | SLF4J + Logback |
| Testing | JUnit 5 |

---

## Prerequisites

- Java 17+
- Maven 3.8+
- Oracle XE / 21c

---

## Quick Start

1. Configure db.properties
2. Run SQL scripts in order
3. Run tests
4. Launch staff app or portal app

---

## Database Setup

Run scripts in this order:

```sql
@src/main/resources/db/schema.sql
@src/main/resources/db/new_tables.sql
@src/main/resources/db/portal_tables.sql
```

portal_tables.sql is required for customer portal and portal management tabs inside staff app.

---

## Configuration

Edit src/main/resources/db.properties:

```properties
db.url=jdbc:oracle:thin:@localhost:1521:XE
db.username=hotel_admin
db.password=hotel@123
```

Both applications use this same configuration.

---

## Run Commands

### Staff Application

```bash
mvn javafx:run
```

### Customer Portal Application

```bash
mvn javafx:run -Pportal
```

---

## Default Credentials

### Staff Users

| Role | Username | Password |
|---|---|---|
| Admin | admin | admin123 |
| Manager | manager | manager123 |
| Reception | reception | reception123 |

### Portal Users

- guests self-register in the portal login screen
- portal credentials are separate from staff users

---

## Portal User Journey

1. Register account
2. Login
3. Search rooms and create booking
4. Raise service requests
5. Place food order and track status
6. Update profile/password

---

## Staff and Portal Synchronization

Synchronization is table-driven through shared Oracle data:

- portal bookings are visible in staff booking/portal requests screens
- service request status updates flow back to the portal
- food order status changes flow back to portal tracking
- checkout and related booking state updates are reflected via refresh/sync paths

---

## Staff Modules

- Login and session management
- Dashboard and operational summary
- Booking management
- Room management
- Customer management
- Inventory management
- Staff management
- Payments and expenses
- Reports and analytics
- Portal requests management

---

## Portal Modules

- Login/Registration
- My Bookings
- Book Room
- Services
- Food and Dining
- My Account

---

## Architecture

```text
JavaFX UI (Staff + Portal)
        |
Service Layer
        |
DAO Layer (JDBC)
        |
Oracle Database (shared schema)
```

---

## Folder Structure

### Hotel Staff Application Structure

```text
src/
├── main/
│   ├── java/com/hotel/
│   │   ├── MainApp.java
│   │   ├── controllers/
│   │   │   ├── BookingController.java
│   │   │   ├── CustomerController.java
│   │   │   ├── DashboardController.java
│   │   │   ├── ExpenseController.java
│   │   │   ├── InventoryController.java
│   │   │   ├── LoginController.java
│   │   │   ├── PaymentController.java
│   │   │   ├── PortalRequestsController.java
│   │   │   ├── ReportsController.java
│   │   │   ├── RoomController.java
│   │   │   └── StaffController.java
│   │   ├── dao/
│   │   ├── model/
│   │   ├── service/
│   │   └── util/
│   └── resources/
│       ├── css/
│       │   └── styles.css
│       ├── db/
│       │   ├── schema.sql
│       │   ├── new_tables.sql
│       │   └── portal_tables.sql
│       ├── fxml/
│       │   ├── bookings.fxml
│       │   ├── customers.fxml
│       │   ├── dashboard.fxml
│       │   ├── expenses.fxml
│       │   ├── inventory.fxml
│       │   ├── login.fxml
│       │   ├── payments.fxml
│       │   ├── portal_requests.fxml
│       │   ├── reports.fxml
│       │   ├── rooms.fxml
│       │   └── staff.fxml
│       ├── db.properties
│       └── logback.xml
└── test/
        └── java/
```

### Customer Portal Application Structure

```text
src/
├── main/
│   ├── java/com/hotel/portal/
│   │   ├── CustomerPortalApp.java
│   │   ├── controllers/
│   │   │   ├── PortalLoginController.java
│   │   │   └── PortalDashboardController.java
│   │   ├── dao/
│   │   │   ├── AvailableServicesDAO.java
│   │   │   ├── CustomerAccountDAO.java
│   │   │   ├── FoodOrderDAO.java
│   │   │   └── ServiceRequestDAO.java
│   │   ├── model/
│   │   │   ├── CustomerAccount.java
│   │   │   ├── FoodMenuItem.java
│   │   │   ├── FoodOrder.java
│   │   │   └── ServiceRequest.java
│   │   └── service/
│   │       └── PortalSession.java
│   └── resources/
│       ├── css/
│       │   └── portal_styles.css
│       └── fxml/portal/
│           ├── portal_dashboard.fxml
│           └── portal_login.fxml
└── test/
        └── java/
```

---

## Useful Commands

```bash
# run staff app
mvn javafx:run

# run customer portal app
mvn javafx:run -Pportal

# run tests
mvn test -q

# package
mvn clean package
```

---

## Troubleshooting

### DB connection issues

- verify db.properties values
- verify Oracle listener/service name and credentials
- rerun SQL scripts if schema is incomplete

### Login issues

- staff users must use staff credentials shown above
- portal users must register via portal UI

### Portal/staff data mismatch

- confirm both apps point to the same schema
- use refresh/sync controls in the relevant modules

