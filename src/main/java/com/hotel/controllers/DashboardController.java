package com.hotel.controllers;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import com.hotel.MainApp;
import com.hotel.service.AlertService;
import com.hotel.service.AuthService;
import com.hotel.service.BookingService;
import com.hotel.service.ReportService;
import com.hotel.util.AlertUtil;
import com.hotel.util.SessionManager;

import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

public class DashboardController {

    @FXML private Label totalRoomsLabel;
    @FXML private Label availableRoomsLabel;
    @FXML private Label occupiedRoomsLabel;
    @FXML private Label checkedInLabel;
    @FXML private Label monthlyRevenueLabel;
    @FXML private Label todayCheckInsLabel;
    @FXML private Label todayCheckOutsLabel;
    @FXML private Label todayRevenueLabel;
    @FXML private Label todayDateLabel;
    @FXML private Label alertBadge;
    @FXML private Label portalRequestsBadge;

    @FXML private StackPane contentArea;
    @FXML private Label     userNameLabel;
    @FXML private Label     userRoleLabel;

    @FXML private Button navStaff;
    @FXML private Button navExpenses;

    private final BookingService bookingService = new BookingService();
    private final ReportService  reportService  = new ReportService();
    private final AuthService    authService    = new AuthService();
    private final AlertService   alertService   = new AlertService();

    @FXML
    public void initialize() {
        var user = SessionManager.getInstance().getCurrentUser();
        if (user != null) {
            userNameLabel.setText(user.getFullName());
            userRoleLabel.setText(user.getRole());
        }

        todayDateLabel.setText(LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy")));

        // Only admin sees Staff and Expenses
        if (!SessionManager.getInstance().isAdmin()) {
            if (navStaff != null) { navStaff.setVisible(false); navStaff.setManaged(false); }
            if (navExpenses != null) { navExpenses.setVisible(false); navExpenses.setManaged(false); }
        }

        loadDashboardStats();
        loadAlertBadge();
    }

    private void loadDashboardStats() {
        try {
            totalRoomsLabel.setText(String.valueOf(bookingService.getTotalRooms()));
            availableRoomsLabel.setText(String.valueOf(bookingService.getAvailableRoomCount()));
            occupiedRoomsLabel.setText(String.valueOf(bookingService.getOccupiedRoomCount()));
            checkedInLabel.setText(String.valueOf(bookingService.getCheckedInCount()));
            monthlyRevenueLabel.setText(String.format("₹%.0f", bookingService.getMonthlyRevenue()));
            todayRevenueLabel.setText(String.format("₹%.0f", reportService.getTodayRevenue()));
            todayCheckInsLabel.setText(String.valueOf(reportService.getTodayCheckIns()));
            todayCheckOutsLabel.setText(String.valueOf(reportService.getTodayCheckOuts()));
        } catch (Exception e) {
            totalRoomsLabel.setText("0"); availableRoomsLabel.setText("0");
            occupiedRoomsLabel.setText("0"); checkedInLabel.setText("0");
            monthlyRevenueLabel.setText("₹0"); todayRevenueLabel.setText("₹0");
            todayCheckInsLabel.setText("0"); todayCheckOutsLabel.setText("0");
        }
    }

    private void loadAlertBadge() {
        try {
            int count = alertService.getTotalAlertCount();
            if (alertBadge != null) {
                if (count > 0) {
                    alertBadge.setText(String.valueOf(count));
                    alertBadge.setVisible(true);
                } else {
                    alertBadge.setVisible(false);
                }
            }
        } catch (Exception ignored) {}

        // Portal requests badge
        try {
            com.hotel.portal.dao.ServiceRequestDAO svcDAO = new com.hotel.portal.dao.ServiceRequestDAO();
            com.hotel.portal.dao.FoodOrderDAO foodDAO = new com.hotel.portal.dao.FoodOrderDAO();
            com.hotel.dao.BookingDAO bookingDAO = new com.hotel.dao.BookingDAO();
            int total = svcDAO.countUnseen() + foodDAO.countUnseenOrders() + bookingDAO.countNewPortalBookings();
            if (portalRequestsBadge != null) {
                if (total > 0) {
                    portalRequestsBadge.setText(String.valueOf(total));
                    portalRequestsBadge.setVisible(true);
                } else {
                    portalRequestsBadge.setVisible(false);
                }
            }
        } catch (Exception ignored) {}
    }

    @FXML private void goToDashboard()  { loadDashboardStats(); clearContent(); }
    @FXML private void goToRooms()      { loadContent("rooms.fxml",     new RoomController()); }
    @FXML private void goToBookings()   { loadContent("bookings.fxml",  new BookingController()); }
    @FXML private void goToCustomers()  { loadContent("customers.fxml", new CustomerController()); }
    @FXML private void goToPayments()   { loadContent("payments.fxml",  new PaymentController()); }
    @FXML private void goToStaff()      { loadContent("staff.fxml",     new StaffController()); }
    @FXML private void goToInventory()  { loadContent("inventory.fxml", new InventoryController()); }
    @FXML private void goToReports()    { loadContent("reports.fxml",   new ReportsController()); }
    @FXML private void goToExpenses()   { loadContent("expenses.fxml",  new ExpenseController()); }
    @FXML private void goToPortalRequests() {
        loadContent("portal_requests.fxml", new com.hotel.controllers.PortalRequestsController());
    }

    @FXML
    private void handleLogout() {
        if (AlertUtil.showConfirm("Logout", "Are you sure you want to logout?")) {
            authService.logout();
            try { MainApp.loadLogin(); } catch (Exception e) {
                AlertUtil.showError("Error", "Could not return to login screen.");
            }
        }
    }

    private void loadContent(String fxmlFile, Object controller) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/" + fxmlFile));
            loader.setController(controller);
            Parent view = loader.load();
            FadeTransition ft = new FadeTransition(Duration.millis(200), view);
            ft.setFromValue(0); ft.setToValue(1);
            contentArea.getChildren().setAll(view);
            ft.play();
            loadDashboardStats();
            loadAlertBadge();
        } catch (IOException e) {
            AlertUtil.showError("Navigation Error", "Could not load: " + fxmlFile + "\n" + e.getMessage());
        }
    }

    private void clearContent() {
        contentArea.getChildren().clear();
        Label lbl = new Label("👈  Select a module from the sidebar.");
        lbl.setStyle("-fx-font-size:15px;-fx-text-fill:#95a5a6;");
        contentArea.getChildren().add(lbl);
    }

    public void refresh() { loadDashboardStats(); }
}
