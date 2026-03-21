package com.hotel.controllers;

import com.hotel.dao.PaymentDAO;
import com.hotel.model.Payment;
import com.hotel.util.AlertUtil;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class PaymentController {

    @FXML private TableView<Payment>           paymentTable;
    @FXML private TableColumn<Payment,String>  colBookingRef;
    @FXML private TableColumn<Payment,String>  colDate;
    @FXML private TableColumn<Payment,Double>  colAmount;
    @FXML private TableColumn<Payment,String>  colMode;
    @FXML private TableColumn<Payment,String>  colType;
    @FXML private TableColumn<Payment,String>  colTransaction;
    @FXML private TableColumn<Payment,String>  colRemarks;
    @FXML private TextField                    searchField;
    @FXML private Label                        totalLabel;

    private final PaymentDAO paymentDAO = new PaymentDAO();
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm");

    @FXML
    public void initialize() {
        setupColumns();
        loadPayments();
        searchField.textProperty().addListener((obs, o, n) -> filterPayments(n));
    }

    private void setupColumns() {
        colBookingRef.setCellValueFactory(new PropertyValueFactory<>("bookingReference"));
        colDate.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getPaymentDate() != null ?
                c.getValue().getPaymentDate().format(FMT) : "—"));
        colAmount.setCellValueFactory(new PropertyValueFactory<>("amount"));
        colMode.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPaymentMode().name()));
        colType.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPaymentType().name()));
        colTransaction.setCellValueFactory(new PropertyValueFactory<>("transactionId"));
        colRemarks.setCellValueFactory(new PropertyValueFactory<>("remarks"));

        // Colour payment type
        colType.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                setStyle(switch (item) {
                    case "ADVANCE" -> "-fx-text-fill:#2980b9;-fx-font-weight:bold;";
                    case "FULL"    -> "-fx-text-fill:#27ae60;-fx-font-weight:bold;";
                    case "REFUND"  -> "-fx-text-fill:#e74c3c;-fx-font-weight:bold;";
                    default        -> "-fx-text-fill:#e67e22;";
                });
            }
        });
        paymentTable.getColumns().forEach(col -> col.setResizable(false));
paymentTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void loadPayments() {
        List<Payment> list = paymentDAO.findAll();
        paymentTable.setItems(FXCollections.observableArrayList(list));
        double total = list.stream().filter(p -> p.getPaymentType() != Payment.Type.REFUND)
                .mapToDouble(Payment::getAmount).sum();
        totalLabel.setText(String.format("Total Received: ₹ %.2f", total));
    }

    private void filterPayments(String keyword) {
        if (keyword == null || keyword.isBlank()) { loadPayments(); return; }
        List<Payment> filtered = paymentDAO.findAll().stream()
                .filter(p -> (p.getBookingReference() != null &&
                        p.getBookingReference().toLowerCase().contains(keyword.toLowerCase()))
                        || p.getPaymentMode().name().toLowerCase().contains(keyword.toLowerCase())
                        || (p.getTransactionId() != null &&
                        p.getTransactionId().toLowerCase().contains(keyword.toLowerCase())))
                .toList();
        paymentTable.setItems(FXCollections.observableArrayList(filtered));
    }

    @FXML private void handleRefresh() { loadPayments(); }
}
