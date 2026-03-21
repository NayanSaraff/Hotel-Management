package com.hotel.controllers;

import com.hotel.MainApp;
import com.hotel.service.AuthService;
import com.hotel.util.AlertUtil;
import javafx.animation.FadeTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Controller for the Login screen.
 */
public class LoginController {

    @FXML private TextField     usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button        loginButton;
    @FXML private Label         errorLabel;
    @FXML private VBox          loginCard;
    @FXML private ProgressIndicator spinner;

    private final AuthService authService = new AuthService();

    @FXML
    public void initialize() {
        errorLabel.setVisible(false);
        spinner.setVisible(false);

        // Fade in the card
        FadeTransition ft = new FadeTransition(Duration.millis(600), loginCard);
        ft.setFromValue(0); ft.setToValue(1); ft.play();

        // Allow Enter key to submit
        passwordField.setOnAction(this::handleLogin);
    }

    @FXML
    private void handleLogin(ActionEvent event) {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Please enter username and password.");
            return;
        }

        loginButton.setDisable(true);
        spinner.setVisible(true);
        errorLabel.setVisible(false);

        // Run authentication off the FX thread to avoid UI freeze
        new Thread(() -> {
            boolean success = authService.login(username, password);
            javafx.application.Platform.runLater(() -> {
                spinner.setVisible(false);
                loginButton.setDisable(false);
                if (success) {
                    try {
                        MainApp.loadDashboard();
                    } catch (Exception e) {
                        showError("Failed to load dashboard: " + e.getMessage());
                    }
                } else {
                    showError("Invalid username or password.");
                    passwordField.clear();
                    shakeField();
                }
            });
        }).start();
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
    }

    private void shakeField() {
        // Simple horizontal translate animation to indicate error
        javafx.animation.TranslateTransition tt =
                new javafx.animation.TranslateTransition(Duration.millis(60), loginCard);
        tt.setFromX(-8); tt.setToX(8); tt.setCycleCount(4);
        tt.setAutoReverse(true);
        tt.play();
    }
}
