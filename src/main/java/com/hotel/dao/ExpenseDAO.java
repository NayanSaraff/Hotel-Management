package com.hotel.dao;

import com.hotel.model.Expense;
import com.hotel.util.DatabaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ExpenseDAO implements GenericDAO<Expense, Integer> {

    private static final Logger logger = LoggerFactory.getLogger(ExpenseDAO.class);

    @Override
    public int save(Expense e) {
        String sql = "INSERT INTO EXPENSES (TITLE,CATEGORY,AMOUNT,EXPENSE_DATE,DESCRIPTION,APPROVED_BY,RECEIPT_REF) " +
                     "VALUES (?,?,?,?,?,?,?)";
        try (PreparedStatement ps = DatabaseConnection.getConnection()
                .prepareStatement(sql, new String[]{"EXPENSE_ID"})) {
            ps.setString(1, e.getTitle());
            ps.setString(2, e.getCategory().name());
            ps.setDouble(3, e.getAmount());
            ps.setDate(4, e.getExpenseDate() != null ? Date.valueOf(e.getExpenseDate()) : null);
            ps.setString(5, e.getDescription());
            ps.setString(6, e.getApprovedBy());
            ps.setString(7, e.getReceiptRef());
            ps.executeUpdate();
            DatabaseConnection.commit();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException ex) {
            DatabaseConnection.rollback();
            logger.error("Error saving expense: {}", ex.getMessage());
        }
        return -1;
    }

    @Override
    public boolean update(Expense e) {
        String sql = "UPDATE EXPENSES SET TITLE=?,CATEGORY=?,AMOUNT=?,EXPENSE_DATE=?,DESCRIPTION=? WHERE EXPENSE_ID=?";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, e.getTitle());
            ps.setString(2, e.getCategory().name());
            ps.setDouble(3, e.getAmount());
            ps.setDate(4, e.getExpenseDate() != null ? Date.valueOf(e.getExpenseDate()) : null);
            ps.setString(5, e.getDescription());
            ps.setInt(6, e.getExpenseId());
            int rows = ps.executeUpdate();
            DatabaseConnection.commit();
            return rows > 0;
        } catch (SQLException ex) {
            DatabaseConnection.rollback();
            logger.error("Error updating expense: {}", ex.getMessage());
            return false;
        }
    }

    @Override
    public boolean delete(Integer id) {
        String sql = "DELETE FROM EXPENSES WHERE EXPENSE_ID=?";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            int rows = ps.executeUpdate();
            DatabaseConnection.commit();
            return rows > 0;
        } catch (SQLException ex) {
            DatabaseConnection.rollback();
            logger.error("Error deleting expense: {}", ex.getMessage());
            return false;
        }
    }

    @Override
    public Optional<Expense> findById(Integer id) {
        String sql = "SELECT * FROM EXPENSES WHERE EXPENSE_ID=?";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) { logger.error("Error: {}", e.getMessage()); }
        return Optional.empty();
    }

    @Override
    public List<Expense> findAll() {
        List<Expense> list = new ArrayList<>();
        String sql = "SELECT * FROM EXPENSES ORDER BY EXPENSE_DATE DESC";
        try (Statement st = DatabaseConnection.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { logger.error("Error fetching expenses: {}", e.getMessage()); }
        return list;
    }

    public double getTotalExpensesThisMonth() {
        String sql = "SELECT NVL(SUM(AMOUNT),0) FROM EXPENSES WHERE TRUNC(EXPENSE_DATE,'MM')=TRUNC(SYSDATE,'MM')";
        try (Statement st = DatabaseConnection.getConnection().createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) { logger.error("Error: {}", e.getMessage()); }
        return 0;
    }

    public List<Expense> findByCategory(Expense.Category category) {
        List<Expense> list = new ArrayList<>();
        String sql = "SELECT * FROM EXPENSES WHERE CATEGORY=? ORDER BY EXPENSE_DATE DESC";
        try (PreparedStatement ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
            ps.setString(1, category.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) { logger.error("Error: {}", e.getMessage()); }
        return list;
    }

    private Expense mapRow(ResultSet rs) throws SQLException {
        Expense e = new Expense();
        e.setExpenseId(rs.getInt("EXPENSE_ID"));
        e.setTitle(rs.getString("TITLE"));
        String cat = rs.getString("CATEGORY");
        if (cat != null) e.setCategory(Expense.Category.valueOf(cat));
        e.setAmount(rs.getDouble("AMOUNT"));
        Date d = rs.getDate("EXPENSE_DATE");
        if (d != null) e.setExpenseDate(d.toLocalDate());
        e.setDescription(rs.getString("DESCRIPTION"));
        e.setApprovedBy(rs.getString("APPROVED_BY"));
        e.setReceiptRef(rs.getString("RECEIPT_REF"));
        return e;
    }
}
