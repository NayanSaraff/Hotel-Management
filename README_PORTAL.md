# Hotel Management System – Enhanced with Customer Portal
## Marcelli Living

---

## What's New in This Version

This version adds a full **Customer Self-Service Portal** that integrates with the existing Hotel Management System using the **same Oracle database**.

---

## Setup: Database

### Step 1 – Run existing schema (if fresh install)
```sql
sqlplus nayan/nayan@localhost:1521/XE @src/main/resources/db/schema.sql
sqlplus nayan/nayan@localhost:1521/XE @src/main/resources/db/new_tables.sql
```

### Step 2 – Run portal tables (NEW – required for portal features)
```sql
sqlplus nayan/nayan@localhost:1521/XE @src/main/resources/db/portal_tables.sql
```

This creates:
- `CUSTOMER_ACCOUNTS` – portal login credentials
- `SERVICE_REQUESTS` – customer service requests
- `FOOD_MENU_ITEMS` – hotel menu (18 items seeded automatically)
- `FOOD_ORDERS` + `FOOD_ORDER_ITEMS` – food ordering
- Alters `BOOKINGS` to add `BOOKING_SOURCE` and `SEEN_BY_STAFF` columns

---

## Running the Apps

Both apps share the **same `db.properties`** and Oracle database.

### Hotel Management System (staff)
```bash
mvn javafx:run
```
Login: `admin` / `Admin@123`  (or `manager`, `reception`)

### Customer Portal (guests)
```bash
mvn javafx:run -Pportal
```
Customers register their own account via the portal.

---

## Feature Guide

### Customer Portal Features

#### 🔐 Login / Registration
- Customers register with: First Name, Last Name, Email, Phone, City, Country, Password
- Email is used as username
- BCrypt password hashing (same library as staff login)
- A new `CUSTOMERS` record AND `CUSTOMER_ACCOUNTS` record are both created on registration

#### 📋 My Bookings Tab
- Lists all bookings made by the logged-in customer
- Shows: Booking Ref, Room, Check-In/Out dates, Status, Total Amount
- Color-coded status (Confirmed = blue, Checked-In = green, etc.)

#### 🛏 Book a Room Tab
1. Select check-in and check-out dates → click **Search Rooms**
2. Available rooms are displayed in a table (room number, type, bed, capacity, price, amenities)
3. Click any room row to select it – a price summary appears
4. Set number of guests and any special requests
5. Click **Confirm Booking**
   - Booking is created with `BOOKING_SOURCE = 'PORTAL'` and `SEEN_BY_STAFF = 0`
   - Hotel staff see it highlighted in orange in the Bookings module
   - A reference number `PRT-XXXXXXXX` is generated

#### 🛎 Services Tab
- Submit service requests by type:
  - 🧹 Housekeeping, 🛁 Extra Towels, 🛏 Extra Pillow, ⏰ Wake-up Call
  - 👕 Laundry, 🚕 Taxi, 🔧 Maintenance, 📋 General Request
  - 📞 Phone Call – enter a phone number; staff will place the call
- Add description/details for each request
- View all past requests with their status (PENDING → IN_PROGRESS → COMPLETED)

#### 🍽 Food & Dining Tab
- **Menu** sub-tab: Browse 18+ items across 7 categories (Breakfast, Lunch, Dinner, Snacks, Beverages, Desserts, Room Service)
- Use `+` / `−` buttons to add items to cart
- Cart shows live total and item breakdown
- Add special notes (allergies, preferences)
- Click **Place Order** → order is created in DB with status `RECEIVED`
- **Order Tracking** sub-tab: Visual progress bar auto-updates every 8 seconds
  - `📥 Received → 👨‍🍳 Preparing → 🚚 On the Way → ✅ Delivered`
- **My Orders** sub-tab: Full order history; click any order to track it

#### 👤 My Account Tab
- Update profile (name, phone, city, country)
- Change password (requires current password verification)
- **Delete Account**: Removes portal login access. Booking history is preserved for hotel records.

---

### Hotel Management – New Features

#### 🌐 Portal Requests (new sidebar nav item)
Click **"🌐 Portal Requests"** in the left sidebar to open the portal management panel.

**Tab 1: Online Bookings**
- Shows all bookings made via the customer portal
- Orange-highlighted rows = portal bookings
- **"Mark All Reviewed"** button marks all portal bookings as seen by staff
- Unreviewed count shown as red badge on sidebar

**Tab 2: Service Requests**
- View all customer service requests in real time
- Select a request → update its status (PENDING → IN_PROGRESS → COMPLETED → CANCELLED)
- Add staff notes per request
- New/unseen requests shown with red badge count

**Tab 3: Food Orders**
- View all food orders from portal customers
- Select any order to see full item breakdown with quantities and prices
- Update order status: `RECEIVED → PREPARING → OUT_FOR_DELIVERY → DELIVERED`
- Customer sees the status update in their portal's order tracking view in real time (8s polling)

#### 🔄 Sync Portal Bookings (Bookings module)
In the **Bookings** module toolbar, a new **"🌐 Sync Portal Bookings"** button:
- Marks all new portal bookings as reviewed
- Shows a red badge `N NEW` when there are unreviewed portal bookings
- Portal booking rows are highlighted in **orange** in the booking table

#### 🗑 Delete Customer (Customers module)
- New **"🗑 Delete"** button in the Customers toolbar
- Confirms before deleting
- Cascade-deletes: portal account, service requests, food orders
- Shows error if customer has active bookings (cancel bookings first)

#### 🗑 Delete Room (Rooms module)
- Already existed; now properly wired and confirmed working

---

## Architecture

```
Oracle XE Database (shared)
├── Existing Tables: USERS, ROOMS, CUSTOMERS, BOOKINGS, PAYMENTS, STAFF, INVENTORY
├── Portal Tables:   CUSTOMER_ACCOUNTS, SERVICE_REQUESTS, FOOD_MENU_ITEMS, FOOD_ORDERS, FOOD_ORDER_ITEMS
└── BOOKINGS altered: + BOOKING_SOURCE (STAFF/PORTAL), + SEEN_BY_STAFF (0/1)

Hotel Management System App (com.hotel.MainApp)
└── Uses: All existing controllers + PortalRequestsController

Customer Portal App (com.hotel.portal.CustomerPortalApp)
├── PortalLoginController  (login + registration)
└── PortalDashboardController (5-tab portal UI)
    ├── My Bookings
    ├── Book a Room  
    ├── Services
    ├── Food & Dining
    └── My Account
```

---

## File Structure (New Files)

```
src/main/resources/db/
  portal_tables.sql                         ← NEW: Run this after schema.sql

src/main/resources/fxml/portal/
  portal_login.fxml                         ← Customer login & registration UI
  portal_dashboard.fxml                     ← Main customer portal (5 tabs)

src/main/resources/fxml/
  portal_requests.fxml                      ← Hotel staff: portal activity panel

src/main/resources/css/
  portal_styles.css                         ← Dark-navy & gold portal theme

src/main/java/com/hotel/portal/
  CustomerPortalApp.java                    ← Portal entry point

  model/
    CustomerAccount.java
    ServiceRequest.java
    FoodMenuItem.java
    FoodOrder.java (+ FoodOrderItem inner class)

  dao/
    CustomerAccountDAO.java                 ← Register, login, delete, change password
    ServiceRequestDAO.java                  ← Submit/fetch/update requests
    FoodOrderDAO.java                       ← Menu, place orders, update status

  service/
    PortalSession.java                      ← Singleton: logged-in customer session

  controllers/
    PortalLoginController.java
    PortalDashboardController.java

src/main/java/com/hotel/controllers/
  PortalRequestsController.java             ← NEW: Hotel staff portal management

Modified Files:
  BookingDAO.java          ← Added: findByCustomerId, findNewPortalBookings, count, markSeen
  BookingController.java   ← Added: syncPortalBookings(), orange highlight, portal badge
  CustomerController.java  ← Added: handleDelete() with full cascade
  DashboardController.java ← Added: goToPortalRequests(), portal badge
  dashboard.fxml           ← Added: 🌐 Portal Requests nav + badge
  bookings.fxml            ← Added: Sync button + portal badge label
  customers.fxml           ← Added: 🗑 Delete button
```

---

## Default Credentials

| App | User | Password |
|-----|------|----------|
| Hotel Management | `admin` | `Admin@123` |
| Hotel Management | `manager` | `Admin@123` |
| Hotel Management | `reception` | `Admin@123` |
| Customer Portal | (register via portal) | (set during registration) |
