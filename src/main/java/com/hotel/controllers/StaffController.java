package com.hotel.controllers;

import com.hotel.dao.StaffDAO;
import com.hotel.model.Staff;
import com.hotel.util.AlertUtil;
import com.hotel.util.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

/**
 * Controller for Staff Management (Admin-only).
 */
public class StaffController {

    @FXML private TableView<Staff>             staffTable;
    @FXML private TableColumn<Staff,String>    colEmpId;
    @FXML private TableColumn<Staff,String>    colName;
    @FXML private TableColumn<Staff,String>    colDept;
    @FXML private TableColumn<Staff,String>    colDesignation;
    @FXML private TableColumn<Staff,String>    colPhone;
    @FXML private TableColumn<Staff,Double>    colSalary;
    @FXML private TableColumn<Staff,String>    colStatus;

    @FXML private VBox              formPane;
    @FXML private TextField         empIdField;
    @FXML private TextField         firstNameField;
    @FXML private TextField         lastNameField;
    @FXML private ComboBox<String>  departmentCombo;
    @FXML private TextField         designationField;
    @FXML private TextField         phoneField;
    @FXML private TextField         emailField;
    @FXML private TextField         salaryField;
    @FXML private DatePicker        joiningDatePicker;
    @FXML private CheckBox          activeCheckBox;
    @FXML private Label             formTitleLabel;

    @FXML private TextField searchField;

    private final StaffDAO staffDAO = new StaffDAO();
    private Staff selectedStaff = null;
    private boolean isEditing = false;

    @FXML
    public void initialize() {
        // Only admins can reach this view
        if (!SessionManager.getInstance().isAdmin()) {
            AlertUtil.showWarning("Access Denied", "Staff management is restricted to admins.");
            return;
        }
        setupColumns();
        departmentCombo.setItems(FXCollections.observableArrayList(
                "FRONT_DESK","HOUSEKEEPING","FOOD_BEVERAGE","SECURITY","MAINTENANCE","MANAGEMENT"));
        loadStaff();
        showForm(false);

        staffTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, o, n) -> selectedStaff = n);
        searchField.textProperty().addListener((obs, o, n) -> filterStaff(n));
    }

    private void setupColumns() {
        colEmpId.setCellValueFactory(new PropertyValueFactory<>("employeeId"));
        colName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFullName()));
        colDept.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDepartment().name()));
        colDesignation.setCellValueFactory(new PropertyValueFactory<>("designation"));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colSalary.setCellValueFactory(new PropertyValueFactory<>("salary"));
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().isActive() ? "Active" : "Inactive"));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                setStyle("Active".equals(item) ? "-fx-text-fill:#27ae60;-fx-font-weight:bold;" : "-fx-text-fill:#e74c3c;");
            }
        });
        staffTable.getColumns().forEach(col -> col.setResizable(false));
staffTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void loadStaff() {
        staffTable.setItems(FXCollections.observableArrayList(staffDAO.findAll()));
    }

    private void filterStaff(String kw) {
        if (kw == null || kw.isBlank()) { loadStaff(); return; }
        var filtered = staffDAO.findAll().stream()
                .filter(s -> s.getFullName().toLowerCase().contains(kw.toLowerCase())
                        || s.getEmployeeId().toLowerCase().contains(kw.toLowerCase()))
                .toList();
        staffTable.setItems(FXCollections.observableArrayList(filtered));
    }

    @FXML private void handleAdd() {
        selectedStaff = null; isEditing = false;
        clearForm(); formTitleLabel.setText("Add Staff Member");
        activeCheckBox.setSelected(true);
        showForm(true);
    }

    @FXML private void handleEdit() {
        selectedStaff = staffTable.getSelectionModel().getSelectedItem();
        if (selectedStaff == null) { AlertUtil.showWarning("Select Staff", "Please select a staff member."); return; }
        isEditing = true; populateForm(selectedStaff);
        formTitleLabel.setText("Edit – " + selectedStaff.getFullName());
        showForm(true);
    }

    @FXML private void handleSave() {
        try {
            Staff s = buildStaffFromForm();
            if (isEditing) {
                s.setStaffId(selectedStaff.getStaffId());
                staffDAO.update(s);
                AlertUtil.showInfo("Updated", "Staff record updated.");
            } else {
                int id = staffDAO.save(s);
                if (id < 0) throw new Exception("Failed to save staff.");
                AlertUtil.showInfo("Added", "Staff member added successfully.");
            }
            loadStaff(); showForm(false);
        } catch (Exception e) {
            AlertUtil.showError("Error", e.getMessage());
        }
    }

    @FXML private void handleCancel() { showForm(false); }

    private Staff buildStaffFromForm() {
        Staff s = new Staff();
        s.setEmployeeId(empIdField.getText().trim());
        s.setFirstName(firstNameField.getText().trim());
        s.setLastName(lastNameField.getText().trim());
        s.setDepartment(Staff.Department.valueOf(departmentCombo.getValue()));
        s.setDesignation(designationField.getText().trim());
        s.setPhone(phoneField.getText().trim());
        s.setEmail(emailField.getText().trim());
        s.setSalary(Double.parseDouble(salaryField.getText().trim()));
        s.setJoiningDate(joiningDatePicker.getValue());
        s.setActive(activeCheckBox.isSelected());
        return s;
    }

    private void populateForm(Staff s) {
        empIdField.setText(s.getEmployeeId());
        firstNameField.setText(s.getFirstName());
        lastNameField.setText(s.getLastName());
        if (s.getDepartment() != null) departmentCombo.setValue(s.getDepartment().name());
        designationField.setText(s.getDesignation());
        phoneField.setText(s.getPhone());
        emailField.setText(s.getEmail());
        salaryField.setText(String.valueOf(s.getSalary()));
        joiningDatePicker.setValue(s.getJoiningDate());
        activeCheckBox.setSelected(s.isActive());
    }

    private void clearForm() {
        empIdField.clear(); firstNameField.clear(); lastNameField.clear();
        designationField.clear(); phoneField.clear(); emailField.clear(); salaryField.clear();
        departmentCombo.getSelectionModel().clearSelection();
        joiningDatePicker.setValue(null); activeCheckBox.setSelected(true);
    }

    private void showForm(boolean show) { formPane.setVisible(show); formPane.setManaged(show); }
}
