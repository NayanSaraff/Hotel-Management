package com.hotel.portal;

import com.hotel.portal.controllers.*;
import com.hotel.portal.service.PortalSession;
import com.hotel.util.DatabaseConnection;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Entry point for the Customer Self-Service Portal.
 * This is a separate JavaFX application that shares the same Oracle DB
 * as the Hotel Management System.
 *
 * Run with: mvn javafx:run -Djavafx.mainClass=com.hotel.portal.CustomerPortalApp
 * Or package separately after building.
 */
public class CustomerPortalApp extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        primaryStage.setTitle("Marcelli Living – Guest Portal");
        primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(680);
        primaryStage.setResizable(true);
        loadLogin();
        primaryStage.show();
    }

    public static void loadLogin() throws IOException {
        FXMLLoader loader = new FXMLLoader(
            CustomerPortalApp.class.getResource("/fxml/portal/portal_login.fxml"));
        loader.setController(new PortalLoginController());
        Parent root = loader.load();
        Scene scene = new Scene(root, 520, 440);
        scene.getStylesheets().add(
            CustomerPortalApp.class.getResource("/css/portal_styles.css").toExternalForm());
        primaryStage.setTitle("Guest Login – Marcelli Living");
        primaryStage.setScene(scene);
        primaryStage.centerOnScreen();
    }

    public static void loadDashboard() throws IOException {
        FXMLLoader loader = new FXMLLoader(
            CustomerPortalApp.class.getResource("/fxml/portal/portal_dashboard.fxml"));
        PortalDashboardController ctrl = new PortalDashboardController();
        loader.setController(ctrl);
        Parent root = loader.load();
        Scene scene = new Scene(root, 1100, 720);
        scene.getStylesheets().add(
            CustomerPortalApp.class.getResource("/css/portal_styles.css").toExternalForm());
        primaryStage.setTitle("Guest Portal – " + PortalSession.getInstance().getCurrentAccount().getFullName());
        primaryStage.setScene(scene);
        primaryStage.centerOnScreen();
    }

    public static Stage getPrimaryStage() { return primaryStage; }

    @Override
    public void stop() {
        DatabaseConnection.shutdownPool();
        PortalSession.getInstance().logout();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
