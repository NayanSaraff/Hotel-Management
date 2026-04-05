package com.hotel.controllers;

import com.hotel.model.Customer;
import com.hotel.service.CustomerService;
import com.hotel.util.AlertUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.List;

/**
 * Controller for Customer Management module.
 */
public class CustomerController {

    // ── Table ─────────────────────────────────────────────────────────────
    @FXML private TableView<Customer>             customerTable;
    @FXML private TableColumn<Customer,String>    colName;
    @FXML private TableColumn<Customer,String>    colPhone;
    @FXML private TableColumn<Customer,String>    colEmail;
    @FXML private TableColumn<Customer,String>    colCity;
    @FXML private TableColumn<Customer,String>    colNationality;
    @FXML private TableColumn<Customer,String>    colIdType;

    // ── Form ──────────────────────────────────────────────────────────────
    @FXML private VBox              formPane;
    @FXML private TextField         firstNameField;
    @FXML private TextField         lastNameField;
    @FXML private TextField         emailField;
    @FXML private TextField         phoneField;
    @FXML private TextArea          addressArea;
    @FXML private TextField         cityField;
    @FXML private TextField         stateField;
    @FXML private TextField         countryField;
    @FXML private TextField         pinField;
    @FXML private ComboBox<String>  idTypeCombo;
    @FXML private TextField         idNumberField;
    @FXML private DatePicker        dobPicker;
    @FXML private TextField         nationalityField;
    @FXML private Label             formTitleLabel;

    // ── Toolbar ───────────────────────────────────────────────────────────
    @FXML private TextField         searchField;

    private final CustomerService customerService = new CustomerService();
    private Customer selectedCustomer = null;
    private boolean isEditing = false;

    @FXML
    public void initialize() {
        setupColumns();
        idTypeCombo.setItems(FXCollections.observableArrayList(
                "AADHAR","PASSPORT","PAN","DRIVING_LICENSE","VOTER_ID"));
        loadCustomers();
        showForm(false);

        customerTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, o, n) -> selectedCustomer = n);
        searchField.textProperty().addListener((obs, o, n) -> {
            if (n != null && !n.isBlank()) {
                customerTable.setItems(FXCollections.observableArrayList(customerService.searchCustomers(n)));
            } else {
                loadCustomers();
            }
        });
    }

    private void setupColumns() {
        colName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getFullName()));
        colPhone.setCellValueFactory(new PropertyValueFactory<>("phone"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colCity.setCellValueFactory(new PropertyValueFactory<>("city"));
        colNationality.setCellValueFactory(new PropertyValueFactory<>("nationality"));
        colIdType.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getIdType() != null ? c.getValue().getIdType().name() : ""));
                customerTable.getColumns().forEach(col -> col.setResizable(false));
customerTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void loadCustomers() {
        customerTable.setItems(FXCollections.observableArrayList(customerService.getAllCustomers()));
    }

    @FXML private void handleAdd() {
        selectedCustomer = null; isEditing = false;
        clearForm(); formTitleLabel.setText("Add New Customer");
        showForm(true);
    }

    @FXML private void handleEdit() {
        selectedCustomer = customerTable.getSelectionModel().getSelectedItem();
        if (selectedCustomer == null) { AlertUtil.showWarning("Select Customer", "Please select a customer."); return; }
        isEditing = true;
        populateForm(selectedCustomer);
        formTitleLabel.setText("Edit Customer – " + selectedCustomer.getFullName());
        showForm(true);
    }

    @FXML private void handleSave() {
        try {
            Customer c = buildCustomerFromForm();
            if (isEditing) {
                c.setCustomerId(selectedCustomer.getCustomerId());
                customerService.updateCustomer(c);
                AlertUtil.showInfo("Updated", "Customer updated.");
            } else {
                int id = customerService.addCustomer(c);
                if (id < 0) throw new Exception("Failed to add customer.");
                AlertUtil.showInfo("Added", "Customer registered successfully.");
            }
            loadCustomers();
            showForm(false);
        } catch (Exception e) {
            AlertUtil.showError("Validation Error", e.getMessage());
        }
    }

    @FXML private void handleCancel() { showForm(false); }

    @FXML private void handleDelete() {
        Customer c = customerTable.getSelectionModel().getSelectedItem();
        if (c == null) { AlertUtil.showWarning("Select Customer", "Please select a customer to delete."); return; }

        javafx.scene.control.Alert confirm = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Customer");
        confirm.setHeaderText("Delete " + c.getFullName() + "?");
        confirm.setContentText(
            "This will permanently delete the customer's profile and their portal account (if any).\n" +
            "Booking history will also be removed.\n\nThis action cannot be undone.");
        confirm.showAndWait().ifPresent(result -> {
            if (result == javafx.scene.control.ButtonType.OK) {
                boolean deleted = deleteCustomerCascade(c.getCustomerId());
                if (deleted) {
                    AlertUtil.showInfo("Deleted", "Customer \"" + c.getFullName() + "\" has been deleted.");
                } else {
                    AlertUtil.showError("Delete Failed", "Customer could not be deleted.");
                }
                loadCustomers();
                showForm(false);
            }
        });
    }

    private boolean deleteCustomerCascade(int customerId) {
        String[] statements = new String[] {
                "DELETE FROM FOOD_ORDER_ITEMS WHERE ORDER_ID IN (SELECT ORDER_ID FROM FOOD_ORDERS WHERE CUSTOMER_ID = ?)",
                "DELETE FROM FOOD_ORDERS WHERE CUSTOMER_ID = ?",
                "DELETE FROM SERVICE_REQUESTS WHERE CUSTOMER_ID = ?",
                "DELETE FROM PHONE_CALLS WHERE CUSTOMER_ID = ?",
                "DELETE FROM PAYMENTS WHERE BOOKING_ID IN (SELECT BOOKING_ID FROM BOOKINGS WHERE CUSTOMER_ID = ?)",
                "DELETE FROM BOOKINGS WHERE CUSTOMER_ID = ?",
                "DELETE FROM CUSTOMER_ACCOUNTS WHERE CUSTOMER_ID = ?",
                "DELETE FROM COUPONS WHERE CUSTOMER_ID = ?",
                "DELETE FROM CUSTOMERS WHERE CUSTOMER_ID = ?"
        };

        try {
            for (String sql : statements) {
                try (java.sql.PreparedStatement ps = com.hotel.util.DatabaseConnection.getConnection().prepareStatement(sql)) {
                    ps.setInt(1, customerId);
                    ps.executeUpdate();
                }
            }
            com.hotel.util.DatabaseConnection.commit();
            return true;
        } catch (java.sql.SQLException e) {
            com.hotel.util.DatabaseConnection.rollback();
            AlertUtil.showError("Delete Failed", "Could not delete customer: " + e.getMessage());
            return false;
        } finally {
            com.hotel.util.DatabaseConnection.closeConnection();
        }
    }

    private Customer buildCustomerFromForm() {
        Customer c = new Customer();
        c.setFirstName(firstNameField.getText().trim());
        c.setLastName(lastNameField.getText().trim());
        c.setEmail(emailField.getText().trim());
        c.setPhone(phoneField.getText().trim());
        c.setAddress(addressArea.getText());
        c.setCity(cityField.getText().trim());
        c.setState(stateField.getText().trim());
        c.setCountry(countryField.getText().trim());
        c.setPinCode(pinField.getText().trim());
        if (idTypeCombo.getValue() != null)
            c.setIdType(Customer.IDType.valueOf(idTypeCombo.getValue()));
        c.setIdNumber(idNumberField.getText().trim());
        c.setDateOfBirth(dobPicker.getValue());
        c.setNationality(nationalityField.getText().trim());
        return c;
    }

    private void populateForm(Customer c) {
        firstNameField.setText(c.getFirstName());
        lastNameField.setText(c.getLastName());
        emailField.setText(c.getEmail());
        phoneField.setText(c.getPhone());
        addressArea.setText(c.getAddress());
        cityField.setText(c.getCity());
        stateField.setText(c.getState());
        countryField.setText(c.getCountry());
        pinField.setText(c.getPinCode());
        if (c.getIdType() != null) idTypeCombo.setValue(c.getIdType().name());
        idNumberField.setText(c.getIdNumber());
        dobPicker.setValue(c.getDateOfBirth());
        nationalityField.setText(c.getNationality());
    }

    private void clearForm() {
        firstNameField.clear(); lastNameField.clear(); emailField.clear();
        phoneField.clear(); addressArea.clear(); cityField.clear();
        stateField.clear(); countryField.clear(); pinField.clear();
        idNumberField.clear(); nationalityField.clear();
        idTypeCombo.getSelectionModel().clearSelection();
        dobPicker.setValue(null);
    }

    private void showForm(boolean show) {
        formPane.setVisible(show);
        formPane.setManaged(show);
    }
}
