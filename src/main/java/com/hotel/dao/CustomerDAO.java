package com.hotel.dao;

import com.hotel.model.Customer;
import com.hotel.util.DatabaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO for Customer CRUD operations.
 */
public class CustomerDAO implements GenericDAO<Customer, Integer> {

    private static final Logger logger = LoggerFactory.getLogger(CustomerDAO.class);

    @Override
    public int save(Customer c) {
        String sql = "INSERT INTO CUSTOMERS (FIRST_NAME,LAST_NAME,EMAIL,PHONE,ADDRESS,CITY,STATE," +
                     "COUNTRY,PIN_CODE,ID_TYPE,ID_NUMBER,DATE_OF_BIRTH,NATIONALITY,REGISTERED_DATE) " +
                     "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,SYSDATE)";
        try (PreparedStatement ps = DatabaseConnection.getConnection()
                .prepareStatement(sql, new String[]{"CUSTOMER_ID"})) {
            ps.setString(1, c.getFirstName());
            ps.setString(2, c.getLastName());
            ps.setString(3, c.getEmail());
            ps.setString(4, c.getPhone());
            ps.setString(5, c.getAddress());
            ps.setString(6, c.getCity());
            ps.setString(7, c.getState());
            ps.setString(8, c.getCountry());
            ps.setString(9, c.getPinCode());
            ps.setString(10, c.getIdType() != null ? c.getIdType().name() : null);
            ps.setString(11, c.getIdNumber());
            ps.setDate(12, c.getDateOfBirth() != null ? Date.valueOf(c.getDateOfBirth()) : null);
            ps.setString(13, c.getNationality());
            ps.executeUpdate();
            DatabaseConnection.commit();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            DatabaseConnection.rollback();
            logger.error("Error saving customer: {}", e.getMessage());
        }
        return -1;
    }

    @Override
    public boolean update(Customer c) {
        String sql = "UPDATE CUSTOMERS SET FIRST_NAME=?,LAST_NAME=?,EMAIL=?,PHONE=?,ADDRESS=?," +
                     "CITY=?,STATE=?,COUNTRY=?,PIN_CODE=?,NATIONALITY=? WHERE CUSTOMER_ID=?";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, c.getFirstName());
            ps.setString(2, c.getLastName());
            ps.setString(3, c.getEmail());
            ps.setString(4, c.getPhone());
            ps.setString(5, c.getAddress());
            ps.setString(6, c.getCity());
            ps.setString(7, c.getState());
            ps.setString(8, c.getCountry());
            ps.setString(9, c.getPinCode());
            ps.setString(10, c.getNationality());
            ps.setInt(11, c.getCustomerId());
            int rows = ps.executeUpdate();
            DatabaseConnection.commit();
            return rows > 0;
        } catch (SQLException e) {
            DatabaseConnection.rollback();
            logger.error("Error updating customer: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean delete(Integer id) {
        // Customers are not deleted — bookings depend on them
        logger.warn("Customer deletion is disabled for data integrity.");
        return false;
    }

    @Override
    public Optional<Customer> findById(Integer id) {
        String sql = "SELECT * FROM CUSTOMERS WHERE CUSTOMER_ID=?";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            logger.error("Error finding customer: {}", e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public List<Customer> findAll() {
        List<Customer> list = new ArrayList<>();
        String sql = "SELECT * FROM CUSTOMERS ORDER BY LAST_NAME, FIRST_NAME";
        try (Statement st = DatabaseConnection.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            logger.error("Error fetching customers: {}", e.getMessage());
        }
        return list;
    }

    /**
     * Search customers by name, phone, or email.
     */
    public List<Customer> search(String keyword) {
        List<Customer> list = new ArrayList<>();
        String kw = "%" + keyword.toUpperCase() + "%";
        String sql = "SELECT * FROM CUSTOMERS WHERE UPPER(FIRST_NAME||' '||LAST_NAME) LIKE ? " +
                     "OR UPPER(PHONE) LIKE ? OR UPPER(EMAIL) LIKE ? ORDER BY LAST_NAME";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, kw); ps.setString(2, kw); ps.setString(3, kw);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            logger.error("Error searching customers: {}", e.getMessage());
        }
        return list;
    }

    private Customer mapRow(ResultSet rs) throws SQLException {
        Customer c = new Customer();
        c.setCustomerId(rs.getInt("CUSTOMER_ID"));
        c.setFirstName(rs.getString("FIRST_NAME"));
        c.setLastName(rs.getString("LAST_NAME"));
        c.setEmail(rs.getString("EMAIL"));
        c.setPhone(rs.getString("PHONE"));
        c.setAddress(rs.getString("ADDRESS"));
        c.setCity(rs.getString("CITY"));
        c.setState(rs.getString("STATE"));
        c.setCountry(rs.getString("COUNTRY"));
        c.setPinCode(rs.getString("PIN_CODE"));
        String idt = rs.getString("ID_TYPE");
        if (idt != null) c.setIdType(Customer.IDType.valueOf(idt));
        c.setIdNumber(rs.getString("ID_NUMBER"));
        Date dob = rs.getDate("DATE_OF_BIRTH");
        if (dob != null) c.setDateOfBirth(dob.toLocalDate());
        c.setNationality(rs.getString("NATIONALITY"));
        Date rd = rs.getDate("REGISTERED_DATE");
        if (rd != null) c.setRegisteredDate(rd.toLocalDate());
        return c;
    }
}
