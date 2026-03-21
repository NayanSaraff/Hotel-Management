# 🏨 Hotel Management System

A full-featured desktop Hotel Management System built with **JavaFX**, **Oracle Database**, **CSS**, and **Maven**.

---

## 📁 Project Structure

```
HotelManagementSystem/
├── pom.xml                              ← Maven build configuration
└── src/
    ├── main/
    │   ├── java/com/hotel/
    │   │   ├── MainApp.java             ← JavaFX Application entry point
    │   │   │
    │   │   ├── model/                   ← Domain entity classes
    │   │   │   ├── User.java
    │   │   │   ├── Room.java
    │   │   │   ├── Customer.java
    │   │   │   ├── Booking.java
    │   │   │   ├── Payment.java
    │   │   │   ├── Staff.java
    │   │   │   └── InventoryItem.java
    │   │   │
    │   │   ├── dao/                     ← Data Access Objects (JDBC / Oracle)
    │   │   │   ├── GenericDAO.java      ← Generic CRUD interface
    │   │   │   ├── UserDAO.java
    │   │   │   ├── RoomDAO.java
    │   │   │   ├── CustomerDAO.java
    │   │   │   ├── BookingDAO.java
    │   │   │   ├── PaymentDAO.java
    │   │   │   ├── StaffDAO.java
    │   │   │   └── InventoryDAO.java
    │   │   │
    │   │   ├── service/                 ← Business Logic Layer
    │   │   │   ├── AuthService.java
    │   │   │   ├── BookingService.java
    │   │   │   ├── RoomService.java
    │   │   │   ├── CustomerService.java
    │   │   │   ├── ReportService.java
    │   │   │   └── InvoiceService.java  ← PDF invoice generation (iText)
    │   │   │
    │   │   ├── controllers/             ← JavaFX FXML Controllers
    │   │   │   ├── LoginController.java
    │   │   │   ├── DashboardController.java
    │   │   │   ├── RoomController.java
    │   │   │   ├── BookingController.java
    │   │   │   ├── CustomerController.java
    │   │   │   ├── StaffController.java
    │   │   │   ├── InventoryController.java
    │   │   │   └── ReportsController.java
    │   │   │
    │   │   └── util/                    ← Utility / Helper classes
    │   │       ├── DatabaseConnection.java
    │   │       ├── SessionManager.java
    │   │       ├── AlertUtil.java
    │   │       └── GSTCalculator.java
    │   │
    │   └── resources/
    │       ├── fxml/                    ← JavaFX FXML UI layouts
    │       │   ├── login.fxml
    │       │   ├── dashboard.fxml
    │       │   ├── rooms.fxml
    │       │   ├── bookings.fxml
    │       │   ├── customers.fxml
    │       │   ├── staff.fxml
    │       │   ├── inventory.fxml
    │       │   └── reports.fxml
    │       ├── css/
    │       │   └── styles.css           ← Full UI theme (Navy + Gold)
    │       ├── db/
    │       │   └── schema.sql           ← Oracle DDL + seed data + triggers
    │       ├── db.properties            ← Database connection config
    │       └── logback.xml              ← SLF4J logging config
    │
    └── test/
        └── java/com/hotel/              ← JUnit 5 test classes (extendable)
```

---

## 🛠️ Technology Stack

| Layer        | Technology                        |
|--------------|-----------------------------------|
| UI           | JavaFX 21 (FXML + Controllers)    |
| Styling      | JavaFX CSS                        |
| Business     | Java 17 (OOP, layered arch.)      |
| Database     | Oracle XE / 21c (JDBC)            |
| Build        | Maven 3.9+                        |
| PDF Reports  | iText 5                           |
| Logging      | SLF4J + Logback                   |
| Security     | jBCrypt password hashing          |

---

## 🚀 Getting Started

### 1. Prerequisites
- Java 17+
- Maven 3.8+
- Oracle Database XE (or 21c)
- Oracle JDBC driver available via Maven Central

### 2. Database Setup
```sql
-- Connect as SYSDBA and create the schema user
CREATE USER hotel_admin IDENTIFIED BY hotel@123;
GRANT CONNECT, RESOURCE, CREATE VIEW TO hotel_admin;

-- Then run the schema script as hotel_admin:
@src/main/resources/db/schema.sql
```

### 3. Configure DB Connection
Edit `src/main/resources/db.properties`:
```properties
db.url=jdbc:oracle:thin:@localhost:1521:XE
db.username=hotel_admin
db.password=hotel@123
```

### 4. Build and Run
```bash
# Compile & run with JavaFX Maven plugin
mvn javafx:run

# Or build a fat JAR
mvn clean package
java -jar target/HotelManagementSystem-1.0-SNAPSHOT.jar
```

---

## 🔑 Default Login Credentials

| Role         | Username    | Password   |
|--------------|-------------|------------|
| Admin        | `admin`     | `Admin@123`|
| Manager      | `manager`   | `Admin@123`|
| Receptionist | `reception` | `Admin@123`|

---

## ✨ Features

### Core Modules
- **Login** – BCrypt-secured authentication with role-based access
- **Dashboard** – Live stat cards (rooms, bookings, revenue, check-ins)
- **Room Management** – Add, edit, delete rooms; real-time status tracking
- **Booking Management** – Create bookings, check-in/check-out, cancel, invoice
- **Customer Management** – Register and search guests, ID verification
- **Staff Management** – Employee records, departments (Admin only)
- **Inventory Management** – Track stock, low-stock alerts, restock
- **Reports** – Monthly revenue, occupancy rate, category breakdown, top rooms

### Technical Highlights
- **DAO Pattern** – All DB operations isolated in DAO classes
- **Service Layer** – Business logic separate from UI and DB
- **GST Calculation** – India GST slabs (0% / 12% / 18%) applied automatically
- **PDF Invoice** – iText-based professional invoice generator
- **Double-booking prevention** – Oracle trigger-level enforcement
- **Transaction management** – Commit/rollback on every DAO operation
- **Logging** – Logback file + console logging

---

## 🗄️ Database Design

```
USERS          → BOOKINGS (USER_ID)
ROOMS          → BOOKINGS (ROOM_ID)
CUSTOMERS      → BOOKINGS (CUSTOMER_ID)
BOOKINGS       → PAYMENTS (BOOKING_ID)
USERS          → STAFF    (USER_ID)
```

**Oracle-specific features used:**
- Sequences for auto-increment PKs
- Check constraints on enums
- Foreign keys with cascade
- Trigger `trg_prevent_double_booking`
- Stored procedure `SP_MONTHLY_REVENUE_REPORT`
- Views `V_BOOKING_DETAILS`, `V_ROOM_OCCUPANCY`

---

## 📌 Architecture

```
┌─────────────────────────────────────┐
│        JavaFX UI Layer              │
│   (FXML + Controllers + CSS)        │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│        Service Layer                │
│  (Business Logic, Validation, GST)  │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│          DAO Layer                  │
│   (JDBC + Oracle SQL Queries)       │
└──────────────┬──────────────────────┘
               │
┌──────────────▼──────────────────────┐
│       Oracle Database               │
│  (Tables, Views, Triggers, Procs)   │
└─────────────────────────────────────┘
```

---

## 📄 License
Academic / Educational project – Week 10 JavaFX submission.
