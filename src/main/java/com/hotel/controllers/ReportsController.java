package com.hotel.controllers;

import java.util.Map;

import com.hotel.service.AlertService;
import com.hotel.service.ReportService;

import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * Reports Controller — Revenue charts, occupancy, analytics.
 */
public class ReportsController {

    @FXML private Label totalRevenueLabel;
    @FXML private Label occupancyLabel;
    @FXML private Label todayCheckInsLabel;
    @FXML private Label todayCheckOutsLabel;

    @FXML private BarChart<String, Number>      revenueBarChart;
    @FXML private PieChart                      categoryPieChart;
    @FXML private LineChart<String, Number>     bookingTrendChart;
    @FXML private BarChart<String, Number>      topRoomsChart;

    @FXML private VBox alertsBox;

    private final ReportService reportService = new ReportService();
    private final AlertService  alertService  = new AlertService();

    @FXML
    public void initialize() {
        loadSummary();
        loadRevenueBarChart();
        loadCategoryPieChart();
        loadBookingStatusChart();
        loadTopRoomsChart();
        loadAlerts();
    }

    private void loadSummary() {
        try {
            totalRevenueLabel.setText(String.format("₹ %.2f", reportService.getMonthlyRevenue(6)
                    .values().stream().mapToDouble(Double::doubleValue).sum()));
            occupancyLabel.setText(String.format("%.1f%%", reportService.getOccupancyRate()));
            todayCheckInsLabel.setText(String.valueOf(reportService.getTodayCheckIns()));
            todayCheckOutsLabel.setText(String.valueOf(reportService.getTodayCheckOuts()));
        } catch (Exception e) {
            totalRevenueLabel.setText("₹ 0"); occupancyLabel.setText("0%");
            todayCheckInsLabel.setText("0"); todayCheckOutsLabel.setText("0");
        }
    }

    private void loadRevenueBarChart() {
        try {
            Map<String, Double> data = reportService.getMonthlyRevenue(6);
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Revenue (₹)");
            data.forEach((month, amount) -> series.getData().add(
                    new XYChart.Data<>(month, amount)));
            revenueBarChart.getData().clear();
            revenueBarChart.getData().add(series);
            revenueBarChart.setTitle("Monthly Revenue – Last 6 Months");
            revenueBarChart.setAnimated(true);
            styleBarChart(revenueBarChart);
        } catch (Exception e) {
            revenueBarChart.setTitle("No data available");
        }
    }

    private void loadCategoryPieChart() {
        try {
            Map<String, Double> data = reportService.getRevenueByCategory();
            categoryPieChart.getData().clear();
            data.forEach((cat, rev) ->
                categoryPieChart.getData().add(new PieChart.Data(cat + " ₹" +
                        String.format("%.0f", rev), rev)));
            categoryPieChart.setTitle("Revenue by Room Category");
            categoryPieChart.setAnimated(true);
            categoryPieChart.setLabelsVisible(true);
        } catch (Exception e) {
            categoryPieChart.setTitle("No data available");
        }
    }

    private void loadBookingStatusChart() {
        try {
            Map<String, Integer> data = reportService.getBookingsByStatus();
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Bookings");
            data.forEach((status, cnt) -> series.getData().add(
                    new XYChart.Data<>(status, cnt)));
            bookingTrendChart.getData().clear();
            bookingTrendChart.getData().add(series);
            bookingTrendChart.setTitle("Bookings by Status");
            bookingTrendChart.setAnimated(true);
        } catch (Exception e) {
            bookingTrendChart.setTitle("No data available");
        }
    }

    private void loadTopRoomsChart() {
        try {
            Map<String, Integer> data = reportService.getTopBookedRooms();
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Bookings");
            data.forEach((room, cnt) -> series.getData().add(
                    new XYChart.Data<>("Room " + room, cnt)));
            topRoomsChart.getData().clear();
            topRoomsChart.getData().add(series);
            topRoomsChart.setTitle("Top 5 Most Booked Rooms");
            topRoomsChart.setAnimated(true);
            styleBarChart(topRoomsChart);
        } catch (Exception e) {
            topRoomsChart.setTitle("No data available");
        }
    }

    private void loadAlerts() {
        alertsBox.getChildren().clear();
        try {
            // Low stock alerts
            alertService.getLowStockItems().forEach(item -> {
                Label lbl = new Label("⚠️  LOW STOCK: " + item.getItemName() +
                        " — only " + item.getQuantityAvailable() + " " + item.getUnit() + " left");
                lbl.setStyle("-fx-text-fill:#e67e22;-fx-font-weight:bold;-fx-font-size:12px;" +
                        "-fx-background-color:#fff3cd;-fx-padding:6 12;-fx-background-radius:6;");
                alertsBox.getChildren().add(lbl);
            });
            // Today's checkouts
            alertService.getTodayCheckouts().forEach(b -> {
                Label lbl = new Label("🚪  CHECKOUT TODAY: " + b.getCustomerName() +
                        " | " + b.getBookingReference() + " | Room " + b.getRoomNumber());
                lbl.setStyle("-fx-text-fill:#2980b9;-fx-font-weight:bold;-fx-font-size:12px;" +
                        "-fx-background-color:#d6eaf8;-fx-padding:6 12;-fx-background-radius:6;");
                alertsBox.getChildren().add(lbl);
            });
            // Today's check-ins
            alertService.getTodayCheckIns().forEach(b -> {
                Label lbl = new Label("✅  CHECK-IN TODAY: " + b.getCustomerName() +
                        " | " + b.getBookingReference() + " | Room " + b.getRoomNumber());
                lbl.setStyle("-fx-text-fill:#27ae60;-fx-font-weight:bold;-fx-font-size:12px;" +
                        "-fx-background-color:#d5f5e3;-fx-padding:6 12;-fx-background-radius:6;");
                alertsBox.getChildren().add(lbl);
            });
            if (alertsBox.getChildren().isEmpty()) {
                Label ok = new Label("✅  No alerts. Everything is running smoothly!");
                ok.setStyle("-fx-text-fill:#27ae60;-fx-font-size:13px;");
                alertsBox.getChildren().add(ok);
            }
        } catch (Exception e) {
            Label err = new Label("Could not load alerts.");
            alertsBox.getChildren().add(err);
        }
    }

    private void styleBarChart(BarChart<?, ?> chart) {
        chart.setBarGap(3);
        chart.setCategoryGap(20);
        chart.setStyle("-fx-background-color:white;");
    }

    @FXML private void handleRefresh() {
        loadSummary();
        loadRevenueBarChart();
        loadCategoryPieChart();
        loadBookingStatusChart();
        loadTopRoomsChart();
        loadAlerts();
    }
}
