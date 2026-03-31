package com.hotel.controllers;

import java.io.File;

import com.hotel.MainApp;
import com.hotel.service.AuthService;

import javafx.animation.FadeTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
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
    @FXML private ImageView     logoImageView;

    private final AuthService authService = new AuthService();

    @FXML
    public void initialize() {
        errorLabel.setVisible(false);
        spinner.setVisible(false);
        loadLogo();

        // Fade in the card
        FadeTransition ft = new FadeTransition(Duration.millis(600), loginCard);
        ft.setFromValue(0); ft.setToValue(1); ft.play();

        // Allow Enter key to submit
        passwordField.setOnAction(this::handleLogin);
    }

    private void loadLogo() {
        String[] candidates = {
                "logo (1).png",
                System.getProperty("user.dir") + File.separator + "logo (1).png"
        };

        for (String path : candidates) {
            File file = new File(path);
            if (file.exists()) {
                logoImageView.setImage(new Image(file.toURI().toString()));
                return;
            }
        }

        // Fallback in case logo is packaged in resources.
        if (getClass().getResource("/logo (1).png") != null) {
            logoImageView.setImage(new Image(getClass().getResource("/logo (1).png").toExternalForm()));
        }
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
