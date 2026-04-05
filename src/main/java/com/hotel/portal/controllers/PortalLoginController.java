package com.hotel.portal.controllers;

import com.hotel.dao.CustomerDAO;
import com.hotel.model.Customer;
import com.hotel.portal.CustomerPortalApp;
import com.hotel.portal.dao.CustomerAccountDAO;
import com.hotel.portal.model.CustomerAccount;
import com.hotel.portal.service.PortalSession;
import com.hotel.util.DatabaseConnection;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Controller for the Customer Portal Login / Registration screen.
 */
public class PortalLoginController {

    // ── Login pane ────────────────────────────────────────────────────────
    @FXML private VBox          loginPane;
    @FXML private TextField     loginEmailField;
    @FXML private PasswordField loginPasswordField;
    @FXML private Button        loginBtn;
    @FXML private Label         loginErrorLabel;
    @FXML private ProgressIndicator loginSpinner;

    // ── Register pane ─────────────────────────────────────────────────────
    @FXML private VBox          registerPane;
    @FXML private TextField     regFirstName;
    @FXML private TextField     regLastName;
    @FXML private TextField     regEmail;
    @FXML private TextField     regPhone;
    @FXML private TextField     regCity;
    @FXML private TextField     regCountry;
    @FXML private PasswordField regPassword;
    @FXML private PasswordField regConfirmPassword;
    @FXML private Label         registerErrorLabel;
    @FXML private ProgressIndicator regSpinner;

    private final CustomerAccountDAO accountDAO = new CustomerAccountDAO();
    private final CustomerDAO        customerDAO = new CustomerDAO();

    @FXML
    public void initialize() {
        loginErrorLabel.setVisible(false);
        loginSpinner.setVisible(false);
        registerErrorLabel.setVisible(false);
        regSpinner.setVisible(false);
        showLogin();

        FadeTransition ft = new FadeTransition(Duration.millis(600), loginPane);
        ft.setFromValue(0); ft.setToValue(1); ft.play();

        loginPasswordField.setOnAction(this::handleLogin);
    }

    // ── Toggle panels ─────────────────────────────────────────────────────

    @FXML
    public void showLogin() {
        loginPane.setVisible(true);
        loginPane.setManaged(true);
        registerPane.setVisible(false);
        registerPane.setManaged(false);
    }

    @FXML
    public void showRegister() {
        registerPane.setVisible(true);
        registerPane.setManaged(true);
        loginPane.setVisible(false);
        loginPane.setManaged(false);
        FadeTransition ft = new FadeTransition(Duration.millis(400), registerPane);
        ft.setFromValue(0); ft.setToValue(1); ft.play();
    }

    // ── Login ─────────────────────────────────────────────────────────────

    @FXML
    public void handleLogin(ActionEvent event) {
        String email    = loginEmailField.getText().trim();
        String password = loginPasswordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            showLoginError("Please enter your email and password.");
            return;
        }

        loginBtn.setDisable(true);
        loginSpinner.setVisible(true);
        loginErrorLabel.setVisible(false);

        new Thread(() -> {
            try {
                Optional<CustomerAccount> result = accountDAO.login(email, password);
                javafx.application.Platform.runLater(() -> {
                    loginSpinner.setVisible(false);
                    loginBtn.setDisable(false);
                    if (result.isPresent()) {
                        PortalSession.getInstance().setCurrentAccount(result.get());
                        try {
                            CustomerPortalApp.loadDashboard();
                        } catch (Exception e) {
                            showLoginError("Failed to load portal: " + e.getMessage());
                        }
                    } else {
                        showLoginError("Invalid email or password. Please try again.");
                        loginPasswordField.clear();
                        shakePane(loginPane);
                    }
                });
            } finally {
                DatabaseConnection.closeConnection();
            }
        }).start();
    }

    // ── Register ──────────────────────────────────────────────────────────

    @FXML
    public void handleRegister(ActionEvent event) {
        // Trim and validate input lengths (match database VARCHAR2 limits)
        String firstName = regFirstName.getText().trim();
        String lastName  = regLastName.getText().trim();
        String email     = regEmail.getText().trim();
        String phone     = regPhone.getText().trim();
        String city      = regCity.getText().trim();
        String country   = regCountry.getText().trim();
        String password  = regPassword.getText();
        String confirm   = regConfirmPassword.getText();

        // Validation: required fields
        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || phone.isEmpty()) {
            showRegError("Please fill in all required fields (First Name, Last Name, Email, Phone).");
            return;
        }

        // Validation: field lengths (CUSTOMERS table VARCHAR2 limits)
        if (firstName.length() > 60) {
            showRegError("First name cannot exceed 60 characters.");
            return;
        }
        if (lastName.length() > 60) {
            showRegError("Last name cannot exceed 60 characters.");
            return;
        }
        if (email.length() > 150) {
            showRegError("Email cannot exceed 150 characters.");
            return;
        }
        if (city.length() > 60) {
            showRegError("City cannot exceed 60 characters.");
            return;
        }
        if (country.length() > 60) {
            showRegError("Country cannot exceed 60 characters.");
            return;
        }

        // Validation: email format
        if (!email.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            showRegError("Please enter a valid email address.");
            return;
        }

        // Validation: phone (at least 10 digits, alphanumeric + hyphens allowed)
        if (phone.length() < 10 || phone.length() > 15) {
            showRegError("Please enter a valid phone number (10-15 characters).");
            return;
        }
        if (!phone.matches("[0-9\\-\\+\\s]{10,15}")) {
            showRegError("Phone must contain only numbers, spaces, hyphens, or plus signs.");
            return;
        }

        // Validation: password strength (8+ chars, at least one number or symbol)
        if (password.length() < 8) {
            showRegError("Password must be at least 8 characters long.");
            return;
        }
        if (!password.matches(".*[0-9].*") && !password.matches(".*[!@#$%^&*()_+=\\-\\[\\]{};:'\",.<>?/\\\\|`~].*")) {
            showRegError("Password must contain at least one number or special character.");
            return;
        }

        // Validation: password confirmation
        if (!password.equals(confirm)) {
            showRegError("Passwords do not match.");
            return;
        }

        regSpinner.setVisible(true);
        registerErrorLabel.setVisible(false);

        new Thread(() -> {
            try {
                // Check email not already registered in portal accounts
                if (accountDAO.emailExists(email)) {
                    javafx.application.Platform.runLater(() -> {
                        regSpinner.setVisible(false);
                        showRegError("This email is already registered. Please login instead.");
                    });
                    return;
                }

                // Reuse existing customer if one already exists with this email.
                int customerId = customerDAO.findByEmail(email)
                        .map(Customer::getCustomerId)
                        .orElseGet(() -> {
                            Customer customer = new Customer();
                            customer.setFirstName(firstName);
                            customer.setLastName(lastName);
                            customer.setEmail(email);
                            customer.setPhone(phone);
                            customer.setCity(city.isEmpty() ? null : city);
                            customer.setCountry(country.isEmpty() ? "India" : country);
                            customer.setNationality("Indian");
                            customer.setRegisteredDate(LocalDate.now());
                            return customerDAO.save(customer);
                        });

                if (customerId <= 0) {
                    javafx.application.Platform.runLater(() -> {
                        regSpinner.setVisible(false);
                        showRegError("Registration failed. Please try again.");
                    });
                    return;
                }

                // Create portal account
                int accountId = accountDAO.register(customerId, email, password);

                javafx.application.Platform.runLater(() -> {
                    regSpinner.setVisible(false);
                    if (accountId > 0) {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Registration Successful");
                        alert.setHeaderText("Welcome, " + firstName + "!");
                        alert.setContentText("Your account has been created successfully.\nPlease log in with your email and password.");
                        alert.showAndWait();
                        // Pre-fill login
                        loginEmailField.setText(email);
                        showLogin();
                    } else {
                        showRegError("Registration failed. Please try again.");
                    }
                });
            } finally {
                DatabaseConnection.closeConnection();
            }
        }).start();
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private void showLoginError(String msg) {
        loginErrorLabel.setText(msg);
        loginErrorLabel.setVisible(true);
    }

    private void showRegError(String msg) {
        registerErrorLabel.setText(msg);
        registerErrorLabel.setVisible(true);
    }

    private void shakePane(VBox pane) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(60), pane);
        tt.setFromX(-8); tt.setToX(8); tt.setCycleCount(4); tt.setAutoReverse(true); tt.play();
    }
}
