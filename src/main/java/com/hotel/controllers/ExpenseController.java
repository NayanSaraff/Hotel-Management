package com.hotel.controllers;

import com.hotel.dao.ExpenseDAO;
import com.hotel.model.Expense;
import com.hotel.util.AlertUtil;
import com.hotel.util.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.List;

/**
 * Expense Tracker — Admin only module.
 */
public class ExpenseController {

    @FXML private TableView<Expense>            expenseTable;
    @FXML private TableColumn<Expense,String>   colTitle;
    @FXML private TableColumn<Expense,String>   colCategory;
    @FXML private TableColumn<Expense,Double>   colAmount;
    @FXML private TableColumn<Expense,String>   colDate;
    @FXML private TableColumn<Expense,String>   colApproved;
    @FXML private TableColumn<Expense,String>   colDesc;

    @FXML private VBox          formPane;
    @FXML private TextField     titleField;
    @FXML private ComboBox<String> categoryCombo;
    @FXML private TextField     amountField;
    @FXML private DatePicker    datePicker;
    @FXML private TextArea      descArea;
    @FXML private TextField     approvedByField;
    @FXML private TextField     receiptRefField;
    @FXML private Label         formTitleLabel;

    @FXML private Label         totalMonthLabel;
    @FXML private ComboBox<String> filterCategoryCombo;

    private final ExpenseDAO expenseDAO = new ExpenseDAO();
    private Expense selectedExpense = null;
    private boolean isEditing = false;

    @FXML
    public void initialize() {
        if (!SessionManager.getInstance().isAdmin()) {
            AlertUtil.showWarning("Access Denied", "Expense Tracker is for Admin only.");
            return;
        }
        setupColumns();
        categoryCombo.setItems(FXCollections.observableArrayList(
                "MAINTENANCE","UTILITIES","SALARIES","SUPPLIES","FOOD_BEVERAGE",
                "MARKETING","HOUSEKEEPING","EQUIPMENT","MISCELLANEOUS"));
        filterCategoryCombo.setItems(FXCollections.observableArrayList(
                "ALL","MAINTENANCE","UTILITIES","SALARIES","SUPPLIES","FOOD_BEVERAGE",
                "MARKETING","HOUSEKEEPING","EQUIPMENT","MISCELLANEOUS"));
        filterCategoryCombo.setValue("ALL");
        filterCategoryCombo.valueProperty().addListener((obs, o, n) -> filterExpenses(n));
        loadExpenses();
        showForm(false);
        expenseTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, o, n) -> selectedExpense = n);
    }

    private void setupColumns() {
        colTitle.setCellValueFactory(new PropertyValueFactory<>("title"));
        colCategory.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getCategory() != null ? c.getValue().getCategory().name() : ""));
        colAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
        colDate.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getExpenseDate() != null ? c.getValue().getExpenseDate().toString() : ""));
        colApproved.setCellValueFactory(new PropertyValueFactory<>("approvedBy"));
        colDesc.setCellValueFactory(new PropertyValueFactory<>("description"));

        expenseTable.getColumns().forEach(col -> col.setResizable(false));
        expenseTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void loadExpenses() {
        List<Expense> list = expenseDAO.findAll();
        expenseTable.setItems(FXCollections.observableArrayList(list));
        double total = expenseDAO.getTotalExpensesThisMonth();
        totalMonthLabel.setText(String.format("This Month Total: ₹ %.2f", total));
    }

    private void filterExpenses(String category) {
        if ("ALL".equals(category)) { loadExpenses(); return; }
        try {
            List<Expense> filtered = expenseDAO.findByCategory(Expense.Category.valueOf(category));
            expenseTable.setItems(FXCollections.observableArrayList(filtered));
        } catch (Exception e) { loadExpenses(); }
    }

    @FXML private void handleAdd() {
        selectedExpense = null; isEditing = false;
        clearForm(); formTitleLabel.setText("Add Expense");
        datePicker.setValue(LocalDate.now());
        approvedByField.setText(SessionManager.getInstance().getCurrentUser().getFullName());
        showForm(true);
    }

    @FXML private void handleEdit() {
        selectedExpense = expenseTable.getSelectionModel().getSelectedItem();
        if (selectedExpense == null) { AlertUtil.showWarning("Select", "Please select an expense."); return; }
        isEditing = true; populateForm(selectedExpense);
        formTitleLabel.setText("Edit Expense");
        showForm(true);
    }

    @FXML private void handleSave() {
        try {
            Expense exp = buildFromForm();
            if (isEditing) {
                exp.setExpenseId(selectedExpense.getExpenseId());
                expenseDAO.update(exp);
                AlertUtil.showInfo("Updated", "Expense updated.");
            } else {
                int id = expenseDAO.save(exp);
                if (id < 0) throw new Exception("Failed to save.");
                AlertUtil.showInfo("Added", "Expense recorded.");
            }
            loadExpenses(); showForm(false);
        } catch (Exception e) {
            AlertUtil.showError("Error", e.getMessage());
        }
    }

    @FXML private void handleDelete() {
        if (selectedExpense == null) return;
        if (AlertUtil.showConfirm("Delete", "Delete this expense?")) {
            expenseDAO.delete(selectedExpense.getExpenseId());
            loadExpenses(); showForm(false);
        }
    }

    @FXML private void handleCancel() { showForm(false); }

    private Expense buildFromForm() {
        if (titleField.getText().isBlank()) throw new IllegalArgumentException("Title is required.");
        if (categoryCombo.getValue() == null) throw new IllegalArgumentException("Category is required.");
        if (amountField.getText().isBlank()) throw new IllegalArgumentException("Amount is required.");
        Expense e = new Expense();
        e.setTitle(titleField.getText().trim());
        e.setCategory(Expense.Category.valueOf(categoryCombo.getValue()));
        e.setAmount(Double.parseDouble(amountField.getText().trim()));
        e.setExpenseDate(datePicker.getValue());
        e.setDescription(descArea.getText());
        e.setApprovedBy(approvedByField.getText().trim());
        e.setReceiptRef(receiptRefField.getText().trim());
        return e;
    }

    private void populateForm(Expense e) {
        titleField.setText(e.getTitle());
        if (e.getCategory() != null) categoryCombo.setValue(e.getCategory().name());
        amountField.setText(String.valueOf(e.getAmount()));
        datePicker.setValue(e.getExpenseDate());
        descArea.setText(e.getDescription());
        approvedByField.setText(e.getApprovedBy());
        receiptRefField.setText(e.getReceiptRef());
    }

    private void clearForm() {
        titleField.clear(); amountField.clear(); descArea.clear();
        approvedByField.clear(); receiptRefField.clear();
        categoryCombo.getSelectionModel().clearSelection();
        datePicker.setValue(null);
    }

    private void showForm(boolean show) { formPane.setVisible(show); formPane.setManaged(show); }
}
