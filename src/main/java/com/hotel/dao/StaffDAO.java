package com.hotel.dao;

import com.hotel.model.Staff;
import com.hotel.util.DatabaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO for Staff CRUD operations.
 */
public class StaffDAO implements GenericDAO<Staff, Integer> {

    private static final Logger logger = LoggerFactory.getLogger(StaffDAO.class);

    @Override
    public int save(Staff s) {
        String sql = "INSERT INTO STAFF (EMPLOYEE_ID,FIRST_NAME,LAST_NAME,DEPARTMENT," +
                     "DESIGNATION,SALARY,PHONE,EMAIL,JOINING_DATE,ACTIVE,ADDRESS,EMERGENCY_CONTACT) " +
                     "VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
        try (PreparedStatement ps = DatabaseConnection.getConnection()
                .prepareStatement(sql, new String[]{"STAFF_ID"})) {
            ps.setString(1, s.getEmployeeId());
            ps.setString(2, s.getFirstName());
            ps.setString(3, s.getLastName());
            ps.setString(4, s.getDepartment().name());
            ps.setString(5, s.getDesignation());
            ps.setDouble(6, s.getSalary());
            ps.setString(7, s.getPhone());
            ps.setString(8, s.getEmail());
            ps.setDate(9, s.getJoiningDate() != null ? Date.valueOf(s.getJoiningDate()) : null);
            ps.setInt(10, s.isActive() ? 1 : 0);
            ps.setString(11, s.getAddress());
            ps.setString(12, s.getEmergencyContact());
            ps.executeUpdate();
            DatabaseConnection.commit();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            DatabaseConnection.rollback();
            logger.error("Error saving staff: {}", e.getMessage());
        }
        return -1;
    }

    @Override
    public boolean update(Staff s) {
        String sql = "UPDATE STAFF SET FIRST_NAME=?,LAST_NAME=?,DEPARTMENT=?,DESIGNATION=?," +
                     "SALARY=?,PHONE=?,EMAIL=?,ACTIVE=? WHERE STAFF_ID=?";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, s.getFirstName()); ps.setString(2, s.getLastName());
            ps.setString(3, s.getDepartment().name()); ps.setString(4, s.getDesignation());
            ps.setDouble(5, s.getSalary()); ps.setString(6, s.getPhone());
            ps.setString(7, s.getEmail()); ps.setInt(8, s.isActive() ? 1 : 0);
            ps.setInt(9, s.getStaffId());
            int rows = ps.executeUpdate();
            DatabaseConnection.commit();
            return rows > 0;
        } catch (SQLException e) {
            DatabaseConnection.rollback();
            logger.error("Error updating staff: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean delete(Integer id) {
        String sql = "UPDATE STAFF SET ACTIVE=0 WHERE STAFF_ID=?";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            int rows = ps.executeUpdate();
            DatabaseConnection.commit();
            return rows > 0;
        } catch (SQLException e) {
            DatabaseConnection.rollback();
            logger.error("Error deactivating staff: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public Optional<Staff> findById(Integer id) {
        String sql = "SELECT * FROM STAFF WHERE STAFF_ID=?";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) { logger.error("Error: {}", e.getMessage()); }
        return Optional.empty();
    }

    @Override
    public List<Staff> findAll() {
        List<Staff> list = new ArrayList<>();
        String sql = "SELECT * FROM STAFF ORDER BY FIRST_NAME, LAST_NAME";
        try (Statement st = DatabaseConnection.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { logger.error("Error fetching staff: {}", e.getMessage()); }
        return list;
    }

    private Staff mapRow(ResultSet rs) throws SQLException {
        Staff s = new Staff();
        s.setStaffId(rs.getInt("STAFF_ID"));
        s.setUserId(rs.getInt("USER_ID"));
        s.setEmployeeId(rs.getString("EMPLOYEE_ID"));
        s.setFirstName(rs.getString("FIRST_NAME"));
        s.setLastName(rs.getString("LAST_NAME"));
        String dept = rs.getString("DEPARTMENT");
        if (dept != null) s.setDepartment(Staff.Department.valueOf(dept));
        s.setDesignation(rs.getString("DESIGNATION"));
        s.setSalary(rs.getDouble("SALARY"));
        s.setPhone(rs.getString("PHONE"));
        s.setEmail(rs.getString("EMAIL"));
        Date jd = rs.getDate("JOINING_DATE");
        if (jd != null) s.setJoiningDate(jd.toLocalDate());
        s.setActive(rs.getInt("ACTIVE") == 1);
        s.setAddress(rs.getString("ADDRESS"));
        s.setEmergencyContact(rs.getString("EMERGENCY_CONTACT"));
        return s;
    }
}
