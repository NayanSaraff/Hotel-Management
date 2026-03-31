-- ============================================================
--  Hotel Management System – Oracle Database Schema
--  Run as: hotel_admin user in Oracle XE / Oracle 21c
-- ============================================================

-- ── Drop existing tables (safe re-run) ─────────────────────
BEGIN
    FOR t IN (
        SELECT table_name FROM user_tables
        WHERE table_name IN ('PAYMENTS','BOOKINGS','INVENTORY',
                             'STAFF','CUSTOMERS','ROOMS','USERS')
    ) LOOP
        EXECUTE IMMEDIATE 'DROP TABLE ' || t.table_name || ' CASCADE CONSTRAINTS';
    END LOOP;
END;
/

-- ── Drop sequences ──────────────────────────────────────────
BEGIN
    FOR s IN (
        SELECT sequence_name FROM user_sequences
        WHERE sequence_name IN ('SEQ_USER_ID','SEQ_ROOM_ID','SEQ_CUSTOMER_ID',
                                'SEQ_BOOKING_ID','SEQ_PAYMENT_ID',
                                'SEQ_STAFF_ID','SEQ_INVENTORY_ID')
    ) LOOP
        EXECUTE IMMEDIATE 'DROP SEQUENCE ' || s.sequence_name;
    END LOOP;
END;
/

-- ── Sequences ───────────────────────────────────────────────
CREATE SEQUENCE SEQ_USER_ID      START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE SEQ_ROOM_ID      START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE SEQ_CUSTOMER_ID  START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE SEQ_BOOKING_ID   START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE SEQ_PAYMENT_ID   START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE SEQ_STAFF_ID     START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE SEQ_INVENTORY_ID START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

-- ═══════════════════════════════════════════════════════════
--  TABLE: USERS
-- ═══════════════════════════════════════════════════════════
CREATE TABLE USERS (
    USER_ID         NUMBER          DEFAULT SEQ_USER_ID.NEXTVAL PRIMARY KEY,
    USERNAME        VARCHAR2(50)    NOT NULL UNIQUE,
    PASSWORD_HASH   VARCHAR2(200)   NOT NULL,
    FULL_NAME       VARCHAR2(100)   NOT NULL,
    EMAIL           VARCHAR2(150)   UNIQUE,
    PHONE           VARCHAR2(15),
    ROLE            VARCHAR2(20)    NOT NULL
                        CONSTRAINT chk_user_role CHECK (ROLE IN ('ADMIN','MANAGER','RECEPTIONIST')),
    ACTIVE          NUMBER(1)       DEFAULT 1 NOT NULL,
    CREATED_AT      TIMESTAMP       DEFAULT SYSTIMESTAMP,
    LAST_LOGIN      TIMESTAMP
);
COMMENT ON TABLE  USERS             IS 'System users: admins, managers, receptionists';
COMMENT ON COLUMN USERS.ROLE        IS 'ADMIN | MANAGER | RECEPTIONIST';
COMMENT ON COLUMN USERS.ACTIVE      IS '1=active, 0=deactivated';

-- ═══════════════════════════════════════════════════════════
--  TABLE: ROOMS
-- ═══════════════════════════════════════════════════════════
CREATE TABLE ROOMS (
    ROOM_ID             NUMBER          DEFAULT SEQ_ROOM_ID.NEXTVAL PRIMARY KEY,
    ROOM_NUMBER         VARCHAR2(10)    NOT NULL UNIQUE,
    CATEGORY            VARCHAR2(20)    NOT NULL
                            CONSTRAINT chk_room_cat CHECK (CATEGORY IN
                                ('STANDARD','DELUXE','SUITE','PRESIDENTIAL')),
    FLOOR               NUMBER(3)       NOT NULL,
    CAPACITY            NUMBER(2)       NOT NULL,
    PRICE_PER_NIGHT     NUMBER(10,2)    NOT NULL,
    STATUS              VARCHAR2(20)    DEFAULT 'AVAILABLE' NOT NULL
                            CONSTRAINT chk_room_status CHECK (STATUS IN
                                ('AVAILABLE','OCCUPIED','MAINTENANCE','HOUSEKEEPING')),
    DESCRIPTION         VARCHAR2(500),
    AMENITIES           VARCHAR2(300),
    BED_TYPE            VARCHAR2(20)
                            CONSTRAINT chk_bed_type CHECK (BED_TYPE IN
                                ('SINGLE','DOUBLE','QUEEN','KING','TWIN') OR BED_TYPE IS NULL),
    HAS_AC              NUMBER(1)       DEFAULT 0,
    HAS_WIFI            NUMBER(1)       DEFAULT 0,
    HAS_TV              NUMBER(1)       DEFAULT 0
);
COMMENT ON TABLE ROOMS IS 'Hotel room inventory';

-- ═══════════════════════════════════════════════════════════
--  TABLE: CUSTOMERS
-- ═══════════════════════════════════════════════════════════
CREATE TABLE CUSTOMERS (
    CUSTOMER_ID     NUMBER          DEFAULT SEQ_CUSTOMER_ID.NEXTVAL PRIMARY KEY,
    FIRST_NAME      VARCHAR2(60)    NOT NULL,
    LAST_NAME       VARCHAR2(60)    NOT NULL,
    EMAIL           VARCHAR2(150),
    PHONE           VARCHAR2(15)    NOT NULL,
    ADDRESS         VARCHAR2(300),
    CITY            VARCHAR2(60),
    STATE           VARCHAR2(60),
    COUNTRY         VARCHAR2(60)    DEFAULT 'India',
    PIN_CODE        VARCHAR2(10),
    ID_TYPE         VARCHAR2(20)
                        CONSTRAINT chk_id_type CHECK (ID_TYPE IN
                            ('AADHAR','PASSPORT','PAN','DRIVING_LICENSE','VOTER_ID') OR ID_TYPE IS NULL),
    ID_NUMBER       VARCHAR2(30),
    DATE_OF_BIRTH   DATE,
    NATIONALITY     VARCHAR2(50)    DEFAULT 'Indian',
    REGISTERED_DATE DATE            DEFAULT SYSDATE
);
COMMENT ON TABLE CUSTOMERS IS 'Hotel guest / customer records';

-- ═══════════════════════════════════════════════════════════
--  TABLE: BOOKINGS
-- ═══════════════════════════════════════════════════════════
CREATE TABLE BOOKINGS (
    BOOKING_ID          NUMBER          DEFAULT SEQ_BOOKING_ID.NEXTVAL PRIMARY KEY,
    BOOKING_REFERENCE   VARCHAR2(20)    NOT NULL UNIQUE,
    CUSTOMER_ID         NUMBER          NOT NULL,
    ROOM_ID             NUMBER          NOT NULL,
    USER_ID             NUMBER          NOT NULL,
    CHECK_IN_DATE       DATE            NOT NULL,
    CHECK_OUT_DATE      DATE            NOT NULL,
    ACTUAL_CHECK_IN     TIMESTAMP,
    ACTUAL_CHECK_OUT    TIMESTAMP,
    NUMBER_OF_GUESTS    NUMBER(2)       DEFAULT 1 NOT NULL,
    ROOM_CHARGES        NUMBER(12,2)    DEFAULT 0,
    SERVICE_CHARGES     NUMBER(12,2)    DEFAULT 0,
    GST_AMOUNT          NUMBER(12,2)    DEFAULT 0,
    TOTAL_AMOUNT        NUMBER(12,2)    DEFAULT 0,
    ADVANCE_PAID        NUMBER(12,2)    DEFAULT 0,
    BALANCE_DUE         NUMBER(12,2)    DEFAULT 0,
    STATUS              VARCHAR2(20)    DEFAULT 'CONFIRMED' NOT NULL
                            CONSTRAINT chk_booking_status CHECK (STATUS IN
                                ('CONFIRMED','CHECKED_IN','CHECKED_OUT','CANCELLED','NO_SHOW')),
    SPECIAL_REQUESTS    VARCHAR2(500),
    BOOKING_DATE        TIMESTAMP       DEFAULT SYSTIMESTAMP,
    CONSTRAINT fk_booking_customer FOREIGN KEY (CUSTOMER_ID) REFERENCES CUSTOMERS(CUSTOMER_ID),
    CONSTRAINT fk_booking_room     FOREIGN KEY (ROOM_ID)     REFERENCES ROOMS(ROOM_ID),
    CONSTRAINT fk_booking_user     FOREIGN KEY (USER_ID)     REFERENCES USERS(USER_ID),
    CONSTRAINT chk_dates           CHECK (CHECK_OUT_DATE > CHECK_IN_DATE)
);
COMMENT ON TABLE BOOKINGS IS 'Room bookings and reservations';

-- ═══════════════════════════════════════════════════════════
--  TABLE: PAYMENTS
-- ═══════════════════════════════════════════════════════════
CREATE TABLE PAYMENTS (
    PAYMENT_ID      NUMBER          DEFAULT SEQ_PAYMENT_ID.NEXTVAL PRIMARY KEY,
    BOOKING_ID      NUMBER          NOT NULL,
    AMOUNT          NUMBER(12,2)    NOT NULL,
    PAYMENT_MODE    VARCHAR2(20)    NOT NULL
                        CONSTRAINT chk_pay_mode CHECK (PAYMENT_MODE IN
                            ('CASH','CREDIT_CARD','DEBIT_CARD','UPI','NET_BANKING','CHEQUE')),
    PAYMENT_TYPE    VARCHAR2(20)    NOT NULL
                        CONSTRAINT chk_pay_type CHECK (PAYMENT_TYPE IN
                            ('ADVANCE','PARTIAL','FULL','REFUND')),
    TRANSACTION_ID  VARCHAR2(60),
    PAYMENT_DATE    TIMESTAMP       DEFAULT SYSTIMESTAMP,
    REMARKS         VARCHAR2(300),
    CONSTRAINT fk_payment_booking FOREIGN KEY (BOOKING_ID) REFERENCES BOOKINGS(BOOKING_ID)
);
COMMENT ON TABLE PAYMENTS IS 'Payment records for bookings';

-- ═══════════════════════════════════════════════════════════
--  TABLE: STAFF
-- ═══════════════════════════════════════════════════════════
CREATE TABLE STAFF (
    STAFF_ID            NUMBER          DEFAULT SEQ_STAFF_ID.NEXTVAL PRIMARY KEY,
    USER_ID             NUMBER,
    EMPLOYEE_ID         VARCHAR2(20)    NOT NULL UNIQUE,
    FIRST_NAME          VARCHAR2(60)    NOT NULL,
    LAST_NAME           VARCHAR2(60)    NOT NULL,
    DEPARTMENT          VARCHAR2(30)    NOT NULL
                            CONSTRAINT chk_dept CHECK (DEPARTMENT IN
                                ('FRONT_DESK','HOUSEKEEPING','FOOD_BEVERAGE',
                                 'SECURITY','MAINTENANCE','MANAGEMENT')),
    DESIGNATION         VARCHAR2(80),
    SALARY              NUMBER(12,2),
    PHONE               VARCHAR2(15),
    EMAIL               VARCHAR2(150),
    JOINING_DATE        DATE            DEFAULT SYSDATE,
    ACTIVE              NUMBER(1)       DEFAULT 1,
    ADDRESS             VARCHAR2(300),
    EMERGENCY_CONTACT   VARCHAR2(15),
    CONSTRAINT fk_staff_user FOREIGN KEY (USER_ID) REFERENCES USERS(USER_ID)
);
COMMENT ON TABLE STAFF IS 'Hotel staff records';

-- ═══════════════════════════════════════════════════════════
--  TABLE: INVENTORY
-- ═══════════════════════════════════════════════════════════
CREATE TABLE INVENTORY (
    ITEM_ID             NUMBER          DEFAULT SEQ_INVENTORY_ID.NEXTVAL PRIMARY KEY,
    ITEM_NAME           VARCHAR2(100)   NOT NULL,
    CATEGORY            VARCHAR2(30)    NOT NULL
                            CONSTRAINT chk_inv_cat CHECK (CATEGORY IN
                                ('LINEN','TOILETRIES','FOOD_BEVERAGE','CLEANING',
                                 'ELECTRONICS','FURNITURE','STATIONERY','OTHER')),
    QUANTITY_AVAILABLE  NUMBER(10)      DEFAULT 0 NOT NULL,
    MINIMUM_THRESHOLD   NUMBER(10)      DEFAULT 5 NOT NULL,
    UNIT                VARCHAR2(20)    DEFAULT 'pieces',
    UNIT_PRICE          NUMBER(10,2)    DEFAULT 0,
    SUPPLIER            VARCHAR2(100),
    LAST_RESTOCKED      DATE
);
COMMENT ON TABLE INVENTORY IS 'Hotel inventory / stock management';

-- ═══════════════════════════════════════════════════════════
--  INDEXES (performance)
-- ═══════════════════════════════════════════════════════════
CREATE INDEX idx_bookings_status       ON BOOKINGS(STATUS);
CREATE INDEX idx_bookings_checkin      ON BOOKINGS(CHECK_IN_DATE);
CREATE INDEX idx_bookings_customer     ON BOOKINGS(CUSTOMER_ID);
CREATE INDEX idx_bookings_room         ON BOOKINGS(ROOM_ID);
CREATE INDEX idx_rooms_status          ON ROOMS(STATUS);
CREATE INDEX idx_payments_booking      ON PAYMENTS(BOOKING_ID);
CREATE INDEX idx_customers_phone       ON CUSTOMERS(PHONE);
CREATE INDEX idx_customers_name        ON CUSTOMERS(UPPER(FIRST_NAME || ' ' || LAST_NAME));
CREATE INDEX idx_inventory_lowstock    ON INVENTORY(QUANTITY_AVAILABLE, MINIMUM_THRESHOLD);

-- ═══════════════════════════════════════════════════════════
--  SEED DATA
-- ═══════════════════════════════════════════════════════════

-- Default admin user (password: Admin@123)
-- BCrypt hash generated externally and stored here
INSERT INTO USERS (USERNAME, PASSWORD_HASH, FULL_NAME, EMAIL, PHONE, ROLE)
VALUES ('admin',
        '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
        'System Administrator', 'admin@marcelliliving.com', '9999900000', 'ADMIN');

-- Manager
INSERT INTO USERS (USERNAME, PASSWORD_HASH, FULL_NAME, EMAIL, PHONE, ROLE)
VALUES ('manager',
        '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
        'Hotel Manager', 'manager@marcelliliving.com', '9999900001', 'MANAGER');

-- Receptionist
INSERT INTO USERS (USERNAME, PASSWORD_HASH, FULL_NAME, EMAIL, PHONE, ROLE)
VALUES ('reception',
        '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
        'Front Desk Staff', 'reception@marcelliliving.com', '9999900002', 'RECEPTIONIST');

-- Sample Rooms
INSERT INTO ROOMS (ROOM_NUMBER,CATEGORY,FLOOR,CAPACITY,PRICE_PER_NIGHT,STATUS,BED_TYPE,HAS_AC,HAS_WIFI,HAS_TV,DESCRIPTION)
VALUES ('101','STANDARD',1,2,1500,'AVAILABLE','DOUBLE',1,1,1,'Comfortable standard room with garden view');
INSERT INTO ROOMS (ROOM_NUMBER,CATEGORY,FLOOR,CAPACITY,PRICE_PER_NIGHT,STATUS,BED_TYPE,HAS_AC,HAS_WIFI,HAS_TV,DESCRIPTION)
VALUES ('102','STANDARD',1,2,1500,'AVAILABLE','TWIN',1,1,1,'Twin bed standard room');
INSERT INTO ROOMS (ROOM_NUMBER,CATEGORY,FLOOR,CAPACITY,PRICE_PER_NIGHT,STATUS,BED_TYPE,HAS_AC,HAS_WIFI,HAS_TV,DESCRIPTION)
VALUES ('201','DELUXE',2,2,3500,'AVAILABLE','QUEEN',1,1,1,'Deluxe room with sea view and balcony');
INSERT INTO ROOMS (ROOM_NUMBER,CATEGORY,FLOOR,CAPACITY,PRICE_PER_NIGHT,STATUS,BED_TYPE,HAS_AC,HAS_WIFI,HAS_TV,DESCRIPTION)
VALUES ('202','DELUXE',2,3,4000,'AVAILABLE','KING',1,1,1,'Spacious deluxe room with extra bed option');
INSERT INTO ROOMS (ROOM_NUMBER,CATEGORY,FLOOR,CAPACITY,PRICE_PER_NIGHT,STATUS,BED_TYPE,HAS_AC,HAS_WIFI,HAS_TV,DESCRIPTION)
VALUES ('301','SUITE',3,4,8500,'AVAILABLE','KING',1,1,1,'Luxury suite with separate living area');
INSERT INTO ROOMS (ROOM_NUMBER,CATEGORY,FLOOR,CAPACITY,PRICE_PER_NIGHT,STATUS,BED_TYPE,HAS_AC,HAS_WIFI,HAS_TV,DESCRIPTION)
VALUES ('401','PRESIDENTIAL',4,4,18000,'AVAILABLE','KING',1,1,1,'Presidential suite with panoramic views');

-- Sample Inventory
INSERT INTO INVENTORY (ITEM_NAME,CATEGORY,QUANTITY_AVAILABLE,MINIMUM_THRESHOLD,UNIT,UNIT_PRICE,SUPPLIER)
VALUES ('Bath Towels','LINEN',200,50,'pieces',150,'Linen Supplies Co.');
INSERT INTO INVENTORY (ITEM_NAME,CATEGORY,QUANTITY_AVAILABLE,MINIMUM_THRESHOLD,UNIT,UNIT_PRICE,SUPPLIER)
VALUES ('Bed Sheets (Double)','LINEN',150,40,'sets',450,'Linen Supplies Co.');
INSERT INTO INVENTORY (ITEM_NAME,CATEGORY,QUANTITY_AVAILABLE,MINIMUM_THRESHOLD,UNIT,UNIT_PRICE,SUPPLIER)
VALUES ('Shampoo Bottles','TOILETRIES',8,20,'pieces',35,'Hygiene Pro');
INSERT INTO INVENTORY (ITEM_NAME,CATEGORY,QUANTITY_AVAILABLE,MINIMUM_THRESHOLD,UNIT,UNIT_PRICE,SUPPLIER)
VALUES ('Mineral Water (500ml)','FOOD_BEVERAGE',500,100,'bottles',20,'AquaFresh');
INSERT INTO INVENTORY (ITEM_NAME,CATEGORY,QUANTITY_AVAILABLE,MINIMUM_THRESHOLD,UNIT,UNIT_PRICE,SUPPLIER)
VALUES ('Floor Cleaner','CLEANING',30,10,'litres',180,'CleanCo');

-- Sample Staff
INSERT INTO STAFF (EMPLOYEE_ID,FIRST_NAME,LAST_NAME,DEPARTMENT,DESIGNATION,PHONE,EMAIL,SALARY)
VALUES ('EMP001','Ravi','Kumar','FRONT_DESK','Senior Receptionist','9876501234','ravi@marcelliliving.com',35000);
INSERT INTO STAFF (EMPLOYEE_ID,FIRST_NAME,LAST_NAME,DEPARTMENT,DESIGNATION,PHONE,EMAIL,SALARY)
VALUES ('EMP002','Priya','Sharma','HOUSEKEEPING','Housekeeping Supervisor','9876501235','priya@marcelliliving.com',28000);
INSERT INTO STAFF (EMPLOYEE_ID,FIRST_NAME,LAST_NAME,DEPARTMENT,DESIGNATION,PHONE,EMAIL,SALARY)
VALUES ('EMP003','Suresh','Nair','FOOD_BEVERAGE','Head Chef','9876501236','suresh@marcelliliving.com',45000);

COMMIT;

-- ═══════════════════════════════════════════════════════════
--  VIEWS
-- ═══════════════════════════════════════════════════════════
CREATE OR REPLACE VIEW V_BOOKING_DETAILS AS
SELECT
    b.BOOKING_ID,
    b.BOOKING_REFERENCE,
    b.STATUS,
    b.CHECK_IN_DATE,
    b.CHECK_OUT_DATE,
    b.CHECK_OUT_DATE - b.CHECK_IN_DATE AS NIGHTS,
    b.TOTAL_AMOUNT,
    b.BALANCE_DUE,
    c.FIRST_NAME || ' ' || c.LAST_NAME AS GUEST_NAME,
    c.PHONE AS GUEST_PHONE,
    r.ROOM_NUMBER,
    r.CATEGORY,
    r.PRICE_PER_NIGHT
FROM BOOKINGS b
JOIN CUSTOMERS c ON b.CUSTOMER_ID = c.CUSTOMER_ID
JOIN ROOMS     r ON b.ROOM_ID = r.ROOM_ID;

CREATE OR REPLACE VIEW V_ROOM_OCCUPANCY AS
SELECT
    r.ROOM_NUMBER,
    r.CATEGORY,
    r.STATUS,
    r.PRICE_PER_NIGHT,
    COUNT(b.BOOKING_ID) AS TOTAL_BOOKINGS,
    NVL(SUM(b.TOTAL_AMOUNT), 0) AS LIFETIME_REVENUE
FROM ROOMS r
LEFT JOIN BOOKINGS b ON r.ROOM_ID = b.ROOM_ID
    AND b.STATUS IN ('CHECKED_IN','CHECKED_OUT')
GROUP BY r.ROOM_NUMBER, r.CATEGORY, r.STATUS, r.PRICE_PER_NIGHT;

-- ═══════════════════════════════════════════════════════════
--  PL/SQL: Trigger to prevent double-booking
-- ═══════════════════════════════════════════════════════════
CREATE OR REPLACE TRIGGER trg_prevent_double_booking
BEFORE INSERT OR UPDATE ON BOOKINGS
FOR EACH ROW
WHEN (NEW.STATUS NOT IN ('CANCELLED','NO_SHOW'))
DECLARE
    v_conflict NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_conflict
    FROM   BOOKINGS
    WHERE  ROOM_ID    = :NEW.ROOM_ID
      AND  STATUS     NOT IN ('CANCELLED','NO_SHOW')
      AND  BOOKING_ID != NVL(:NEW.BOOKING_ID, -1)
      AND  NOT (:NEW.CHECK_OUT_DATE <= CHECK_IN_DATE
             OR :NEW.CHECK_IN_DATE  >= CHECK_OUT_DATE);

    IF v_conflict > 0 THEN
        RAISE_APPLICATION_ERROR(-20001,
            'Room ' || :NEW.ROOM_ID || ' is already booked for the requested dates.');
    END IF;
END;
/

-- ═══════════════════════════════════════════════════════════
--  PL/SQL: Procedure – Generate Monthly Revenue Report
-- ═══════════════════════════════════════════════════════════
CREATE OR REPLACE PROCEDURE SP_MONTHLY_REVENUE_REPORT (
    p_year   IN NUMBER,
    p_month  IN NUMBER
) AS
    v_total_revenue NUMBER;
    v_total_bookings NUMBER;
    v_occupancy_rate NUMBER;
BEGIN
    SELECT NVL(SUM(TOTAL_AMOUNT), 0),
           COUNT(*)
    INTO   v_total_revenue, v_total_bookings
    FROM   BOOKINGS
    WHERE  STATUS IN ('CHECKED_OUT','CHECKED_IN')
      AND  EXTRACT(YEAR  FROM BOOKING_DATE) = p_year
      AND  EXTRACT(MONTH FROM BOOKING_DATE) = p_month;

    DBMS_OUTPUT.PUT_LINE('=== Monthly Revenue Report ===');
    DBMS_OUTPUT.PUT_LINE('Period    : ' || TO_CHAR(TO_DATE(p_month||'-'||p_year,'MM-YYYY'),'Month YYYY'));
    DBMS_OUTPUT.PUT_LINE('Revenue   : INR ' || TO_CHAR(v_total_revenue,'999,999,999.00'));
    DBMS_OUTPUT.PUT_LINE('Bookings  : ' || v_total_bookings);
END SP_MONTHLY_REVENUE_REPORT;
/

COMMIT;
-- End of schema script
