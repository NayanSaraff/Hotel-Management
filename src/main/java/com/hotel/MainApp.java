package com.hotel;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import com.hotel.controllers.LoginController;
import com.hotel.controllers.DashboardController;
import com.hotel.util.DatabaseConnection;
import com.hotel.util.SessionManager;

import java.io.IOException;

public class MainApp extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        primaryStage.setTitle("Hotel Management System");
        primaryStage.setMinWidth(1100);
        primaryStage.setMinHeight(700);
        loadLogin();
        primaryStage.show();
    }

    public static void loadLogin() throws IOException {
        FXMLLoader loader = new FXMLLoader(
            MainApp.class.getResource("/fxml/login.fxml"));
        loader.setController(new LoginController());
        Parent root = loader.load();
        Scene scene = new Scene(root, 500, 400);
        scene.getStylesheets().add(
            MainApp.class.getResource("/css/styles.css").toExternalForm());
        primaryStage.setTitle("Login - Hotel Management System");
        primaryStage.setScene(scene);
        primaryStage.centerOnScreen();
    }

    public static void loadDashboard() throws IOException {
        FXMLLoader loader = new FXMLLoader(
            MainApp.class.getResource("/fxml/dashboard.fxml"));
        loader.setController(new DashboardController());
        Parent root = loader.load();
        Scene scene = new Scene(root, 1200, 750);
        scene.getStylesheets().add(
            MainApp.class.getResource("/css/styles.css").toExternalForm());
        primaryStage.setTitle("Hotel Management System - Dashboard");
        primaryStage.setScene(scene);
        primaryStage.centerOnScreen();
    }

    public static void loadScene(String fxmlFile, String title,
                                  double width, double height) throws IOException {
        FXMLLoader loader = new FXMLLoader(
            MainApp.class.getResource("/fxml/" + fxmlFile));
        Parent root = loader.load();
        Scene scene = new Scene(root, width, height);
        scene.getStylesheets().add(
            MainApp.class.getResource("/css/styles.css").toExternalForm());
        primaryStage.setTitle(title);
        primaryStage.setScene(scene);
        primaryStage.centerOnScreen();
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    @Override
    public void stop() {
        DatabaseConnection.closeConnection();
        SessionManager.getInstance().clearSession();
    }

    public static void main(String[] args) {
        launch(args);
    }
}