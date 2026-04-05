package com.hotel.controllers;

import com.hotel.dao.BookingDAO;
import com.hotel.model.Booking;
import com.hotel.portal.dao.AvailableServicesDAO;
import com.hotel.portal.dao.FoodOrderDAO;
import com.hotel.portal.dao.ServiceRequestDAO;
import com.hotel.portal.model.FoodOrder;
import com.hotel.portal.model.ServiceRequest;
import com.hotel.util.AlertUtil;
import com.hotel.util.SessionManager;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.util.Duration;

import java.lang.reflect.Field;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class PortalRequestsController {

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd MMM HH:mm");

    // ── Portal Bookings ───────────────────────────────────────────────────
    @FXML private TableView<Booking>          portalBookingTable;
    @FXML private TableColumn<Booking,String> pbColRef, pbColGuest, pbColRoom,
                                               pbColCheckIn, pbColCheckOut, pbColStatus, pbColTotal;
    @FXML private Label newBookingsBadge;

    // ── Service Requests ──────────────────────────────────────────────────
    @FXML private TableView<ServiceRequest>         serviceTable;
    @FXML private TableColumn<ServiceRequest,String> srColGuest, srColType, srColStatus, srColPriority, srColTime, srColDesc;
    @FXML private Label        unseenSvcBadge;
    @FXML private ComboBox<String> svcStatusCombo;
    @FXML private TextArea     svcNotesArea;
    @FXML private Button       updateSvcBtn;

    // ── Food Orders ───────────────────────────────────────────────────────
    @FXML private TableView<FoodOrder>          foodOrderTable;
    @FXML private TableColumn<FoodOrder,String> foColId, foColGuest, foColStatus, foColTotal, foColTime, foColItems;
    @FXML private Label        unseenFoodBadge;
    @FXML private ComboBox<String> foodStatusCombo;
    @FXML private Button       updateFoodBtn;
    @FXML private Label        foodOrderDetailLabel;

    // ── Phone Calls (Receptionist only) ──────────────────────────────────
    @FXML private VBox         phoneCallsPanel;
    @FXML private VBox         ringingCallsBox;
    @FXML private Label        ringingCountLabel;

    // ── Services Management ───────────────────────────────────────────────
    @FXML private TableView<String[]>          svcMgmtTable;
    @FXML private TableColumn<String[],String> smColId, smColName, smColType, smColIcon, smColActive;
    @FXML private TextField    newSvcName, newSvcIcon;
    @FXML private ComboBox<String> newSvcTypeCombo;
    @FXML private TextArea     newSvcDesc;

    private final BookingDAO          bookingDAO  = new BookingDAO();
    private final ServiceRequestDAO   svcDAO      = new ServiceRequestDAO();
    private final FoodOrderDAO        foodDAO     = new FoodOrderDAO();
    private final AvailableServicesDAO svcDefDAO  = new AvailableServicesDAO();

    private ServiceRequest selectedServiceReq = null;
    private FoodOrder      selectedFoodOrder  = null;
    private Timeline       callPollTimeline;
    private Timeline       portalSyncTimeline;

    @FXML
    public void initialize() {
        setupPortalBookingsTab();
        setupServiceRequestsTab();
        setupFoodOrdersTab();
        setupPhoneCallsPanel();
        setupServicesMgmtTab();
        loadAll();
        startPortalSyncPolling();
    }

    private void startPortalSyncPolling() {
        if (portalSyncTimeline != null) {
            portalSyncTimeline.stop();
        }
        portalSyncTimeline = new Timeline(new KeyFrame(Duration.seconds(8), e -> Platform.runLater(this::loadAll)));
        portalSyncTimeline.setCycleCount(Timeline.INDEFINITE);
        portalSyncTimeline.play();
    }

    // ════════════════════════════════════════════════════════════════════
    //  NAVIGATION
    // ════════════════════════════════════════════════════════════════════

    @FXML public void goBack() {
        try {
            com.hotel.MainApp.loadDashboard();
        } catch (Exception e) { }
    }

    // ════════════════════════════════════════════════════════════════════
    //  PORTAL BOOKINGS
    // ════════════════════════════════════════════════════════════════════

    private void setupPortalBookingsTab() {
        pbColRef.setCellValueFactory(new PropertyValueFactory<>("bookingReference"));
        pbColGuest.setCellValueFactory(new PropertyValueFactory<>("customerName"));
        pbColRoom.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().getRoomNumber() + " (" + c.getValue().getRoomCategory() + ")"));
        pbColCheckIn.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().getCheckInDate() != null ? c.getValue().getCheckInDate().toString() : ""));
        pbColCheckOut.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().getCheckOutDate() != null ? c.getValue().getCheckOutDate().toString() : ""));
        pbColStatus.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().getStatus() != null ? c.getValue().getStatus().name() : ""));
        pbColTotal.setCellValueFactory(c -> new SimpleStringProperty(
            "$" + String.format("%.2f", c.getValue().getTotalAmount())));

        portalBookingTable.setRowFactory(tv -> new TableRow<>() {
            @Override protected void updateItem(Booking b, boolean empty) {
                super.updateItem(b, empty);
                if (empty || b == null) { setStyle(""); return; }
                boolean isPortal = b.getBookingReference() != null && b.getBookingReference().startsWith("PRT-");
                setStyle(isPortal ? "-fx-background-color:#fff3e0;" : "");
            }
        });
    }

    private void loadPortalBookings() {
        List<Booking> all = bookingDAO.findAll().stream()
            .filter(b -> b.getBookingReference() != null && b.getBookingReference().startsWith("PRT-"))
            .toList();
        portalBookingTable.setItems(FXCollections.observableArrayList(all));
        int unseen = bookingDAO.countNewPortalBookings();
        newBookingsBadge.setText(unseen + " unreviewed");
        newBookingsBadge.setVisible(unseen > 0);
    }

    @FXML public void markPortalBookingsSeen() {
        bookingDAO.markPortalBookingsSeen();
        loadPortalBookings();
        AlertUtil.showInfo("Synced", "All portal bookings marked as reviewed.");
    }

    @FXML public void refreshPortalBookings() { loadPortalBookings(); }

    // ════════════════════════════════════════════════════════════════════
    //  SERVICE REQUESTS
    // ════════════════════════════════════════════════════════════════════

    private void setupServiceRequestsTab() {
        srColGuest.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCustomerName()));
        srColType.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().getRequestType() != null ? c.getValue().getTypeDisplayName() : ""));
        srColStatus.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().getStatus() != null ? c.getValue().getStatus().name() : ""));
        srColPriority.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().getPriority() != null ? c.getValue().getPriority().name() : ""));
        srColTime.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().getRequestedAt() != null ? c.getValue().getRequestedAt().format(DT_FMT) : ""));
        srColDesc.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDescription()));

        srColStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                setStyle(switch (item) {
                    case "PENDING"     -> "-fx-text-fill:#e67e22;-fx-font-weight:bold;";
                    case "IN_PROGRESS" -> "-fx-text-fill:#2980b9;-fx-font-weight:bold;";
                    case "COMPLETED"   -> "-fx-text-fill:#27ae60;";
                    case "CANCELLED"   -> "-fx-text-fill:#e74c3c;";
                    default            -> "";
                });
            }
        });

        svcStatusCombo.setItems(FXCollections.observableArrayList("PENDING","IN_PROGRESS","COMPLETED","CANCELLED"));
        svcStatusCombo.setStyle("-fx-background-color:#1a2d42;-fx-text-fill:#e2e8f0;-fx-border-color:#2d3f55;-fx-border-radius:6;-fx-background-radius:6;");

        serviceTable.getSelectionModel().selectedItemProperty().addListener((obs, o, sel) -> {
            selectedServiceReq = sel;
            updateSvcBtn.setDisable(sel == null);
            if (sel != null) {
                svcStatusCombo.setValue(sel.getStatus() != null ? sel.getStatus().name() : "PENDING");
                svcNotesArea.setText(sel.getNotes() != null ? sel.getNotes() : "");
            }
        });
        updateSvcBtn.setDisable(true);
    }

    private void loadServiceRequests() {
        serviceTable.setItems(FXCollections.observableArrayList(svcDAO.findAll()));
        int unseen = svcDAO.countUnseen();
        unseenSvcBadge.setText(unseen + " new");
        unseenSvcBadge.setVisible(unseen > 0);
    }

    @FXML public void updateServiceRequest() {
        if (selectedServiceReq == null) return;
        String statusStr = svcStatusCombo.getValue();
        if (statusStr == null) { AlertUtil.showWarning("Select Status", "Choose a status."); return; }
        try {
            ServiceRequest.Status status = ServiceRequest.Status.valueOf(statusStr);
            if (svcDAO.updateStatus(selectedServiceReq.getRequestId(), status, svcNotesArea.getText().trim())) {
                AlertUtil.showInfo("Updated", "Request updated to: " + statusStr);
                loadServiceRequests();
            } else AlertUtil.showError("Error", "Update failed.");
        } catch (Exception e) { AlertUtil.showError("Error", e.getMessage()); }
    }

    @FXML public void markAllServicesSeen() { svcDAO.markAllSeen(); loadServiceRequests(); }
    @FXML public void refreshServiceRequests() { loadServiceRequests(); }

    // ════════════════════════════════════════════════════════════════════
    //  FOOD ORDERS
    // ════════════════════════════════════════════════════════════════════

    private void setupFoodOrdersTab() {
        foColId.setCellValueFactory(c -> new SimpleStringProperty("#" + c.getValue().getOrderId()));
        foColGuest.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCustomerName()));
        foColStatus.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().getOrderStatus() != null ? c.getValue().getStatusDisplayName() : ""));
        foColTotal.setCellValueFactory(c -> new SimpleStringProperty(
            "$" + String.format("%.2f", c.getValue().getTotalAmount())));
        foColTime.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().getOrderedAt() != null ? c.getValue().getOrderedAt().format(DT_FMT) : ""));
        foColItems.setCellValueFactory(c -> {
            var items = c.getValue().getItems();
            if (items == null || items.isEmpty()) return new SimpleStringProperty("—");
            return new SimpleStringProperty(items.stream()
                .map(i -> i.getItemName() + " ×" + i.getQuantity())
                .reduce((a, b) -> a + ", " + b).orElse(""));
        });

        foodStatusCombo.setItems(FXCollections.observableArrayList(
            "RECEIVED","PREPARING","OUT_FOR_DELIVERY","DELIVERED","CANCELLED"));
        foodStatusCombo.setStyle("-fx-background-color:#1a2d42;-fx-text-fill:#e2e8f0;-fx-border-color:#2d3f55;-fx-border-radius:6;-fx-background-radius:6;");

        foodOrderTable.getSelectionModel().selectedItemProperty().addListener((obs, o, sel) -> {
            selectedFoodOrder = sel;
            updateFoodBtn.setDisable(sel == null);
            if (sel != null) {
                foodStatusCombo.setValue(sel.getOrderStatus() != null ? sel.getOrderStatus().name() : "RECEIVED");
                StringBuilder sb = new StringBuilder("Order #" + sel.getOrderId() + "\n");
                sb.append("─".repeat(30)).append("\n");
                if (sel.getItems() != null)
                    sel.getItems().forEach(i -> sb.append(i.getItemName()).append(" ×").append(i.getQuantity())
                        .append("  $").append(String.format("%.2f", i.getSubtotal())).append("\n"));
                sb.append("─".repeat(30)).append("\n");
                sb.append("TOTAL: $").append(String.format("%.2f", sel.getTotalAmount()));
                if (sel.getSpecialNotes() != null && !sel.getSpecialNotes().isEmpty())
                    sb.append("\nNote: ").append(sel.getSpecialNotes());
                foodOrderDetailLabel.setText(sb.toString());
            }
        });
        updateFoodBtn.setDisable(true);
        foodOrderDetailLabel.setText("Select an order to see details.");
    }

    private void loadFoodOrders() {
        Integer selectedOrderId = selectedFoodOrder != null ? selectedFoodOrder.getOrderId() : null;
        foodOrderTable.setItems(FXCollections.observableArrayList(foodDAO.findAll()));
        if (selectedOrderId != null) {
            foodOrderTable.getItems().stream()
                .filter(o -> o.getOrderId() == selectedOrderId)
                .findFirst()
                .ifPresent(o -> {
                    foodOrderTable.getSelectionModel().select(o);
                    selectedFoodOrder = o;
                });
        }
        int unseen = foodDAO.countUnseenOrders();
        unseenFoodBadge.setText(unseen + " new");
        unseenFoodBadge.setVisible(unseen > 0);
    }

    @FXML public void updateFoodOrderStatus() {
        if (selectedFoodOrder == null) return;
        int orderId = selectedFoodOrder.getOrderId();
        String statusStr = foodStatusCombo.getValue();
        if (statusStr == null) { AlertUtil.showWarning("Select Status", "Choose a status."); return; }
        try {
            FoodOrder.Status status = FoodOrder.Status.valueOf(statusStr);
            if (foodDAO.updateOrderStatus(orderId, status)) {
                AlertUtil.showInfo("Updated", "Order #" + orderId + " → " + statusStr);
                loadFoodOrders();
            } else AlertUtil.showError("Error", "Update failed.");
        } catch (Exception e) { AlertUtil.showError("Error", e.getMessage()); }
    }

    @FXML public void markAllFoodSeen() { foodDAO.markAllOrdersSeen(); loadFoodOrders(); }
    @FXML public void refreshFoodOrders() { loadFoodOrders(); }

    // ════════════════════════════════════════════════════════════════════
    //  PHONE CALLS (Receptionist only)
    // ════════════════════════════════════════════════════════════════════

    private void setupPhoneCallsPanel() {
        // Only show for RECEPTIONIST role
        boolean isReceptionist = SessionManager.getInstance().isReceptionist();
        if (phoneCallsPanel != null) {
            phoneCallsPanel.setVisible(true);
            phoneCallsPanel.setManaged(true);
        }

        if (!isReceptionist) {
            if (phoneCallsPanel != null) {
                phoneCallsPanel.getChildren().clear();
                Label msg = new Label("📞 Phone calls are only visible to Receptionists.");
                msg.setStyle("-fx-text-fill:#888;-fx-font-size:13px;-fx-padding:20;");
                phoneCallsPanel.getChildren().add(msg);
            }
            return;
        }

        loadRingingCalls();

        // Poll every 5 seconds for new calls
        callPollTimeline = new Timeline(new KeyFrame(Duration.seconds(5), e ->
            Platform.runLater(this::loadRingingCalls)));
        callPollTimeline.setCycleCount(Timeline.INDEFINITE);
        callPollTimeline.play();
    }

    private void loadRingingCalls() {
        if (ringingCallsBox == null) return;
        var calls = svcDefDAO.getRingingCallsInfo();
        if (ringingCountLabel != null) {
            ringingCountLabel.setText(calls.isEmpty() ? "No incoming calls" : calls.size() + " incoming call(s)");
            ringingCountLabel.setStyle(calls.isEmpty()
                ? "-fx-text-fill:#888;-fx-font-size:13px;"
                : "-fx-text-fill:#e74c3c;-fx-font-weight:bold;-fx-font-size:14px;");
        }
        ringingCallsBox.getChildren().clear();

        if (calls.isEmpty()) {
            Label none = new Label("No active calls right now.");
            none.setStyle("-fx-text-fill:#888;-fx-font-size:12px;");
            ringingCallsBox.getChildren().add(none);
            return;
        }

        for (String[] call : calls) {
            HBox row = new HBox(12);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(10, 14, 10, 14));
            row.setStyle("-fx-background-color:#fff8e1;-fx-background-radius:8;" +
                         "-fx-border-color:#f39c12;-fx-border-radius:8;-fx-border-width:1.5;");

            Label icon = new Label("📞"); icon.setStyle("-fx-font-size:22px;");
            VBox info = new VBox(2);
            Label nameL = new Label("Guest: " + (call[1] != null ? call[1] : "Unknown"));
            nameL.setStyle("-fx-font-weight:bold;-fx-font-size:13px;-fx-text-fill:#1a1a2e;");
            Label timeL = new Label("Called at: " + call[3] + "  |  Room: " + call[2]);
            timeL.setStyle("-fx-font-size:11px;-fx-text-fill:#555;");
            info.getChildren().addAll(nameL, timeL);
            HBox.setHgrow(info, Priority.ALWAYS);

            Button answerBtn = new Button("✅ Answer");
            answerBtn.setStyle("-fx-background-color:#27ae60;-fx-text-fill:white;-fx-font-weight:bold;" +
                               "-fx-background-radius:8;-fx-padding:7 16;-fx-cursor:hand;");
            Button missBtn = new Button("Dismiss");
            missBtn.setStyle("-fx-background-color:#e74c3c;-fx-text-fill:white;-fx-font-weight:bold;" +
                             "-fx-background-radius:8;-fx-padding:7 14;-fx-cursor:hand;");

            final int callId = Integer.parseInt(call[0]);
            final String callerName = call[1];

            answerBtn.setOnAction(e -> {
                String staffName = SessionManager.getInstance().getCurrentUser() != null
                    ? SessionManager.getInstance().getCurrentUser().getFullName()
                    : "Receptionist";
                svcDefDAO.answerCall(callId, staffName);
                AlertUtil.showInfo("Call Answered",
                    "You have answered the call from " + callerName + ".\n\n" +
                    "The guest can see they are connected.\n" +
                    "Click 'End Call' below when finished.");
                loadRingingCalls();
                showActiveCallControl(callId, callerName);
            });

            missBtn.setOnAction(e -> {
                svcDefDAO.missCall(callId);
                loadRingingCalls();
            });

            row.getChildren().addAll(icon, info, answerBtn, missBtn);
            ringingCallsBox.getChildren().add(row);
        }
    }

    private void showActiveCallControl(int callId, String callerName) {
        if (ringingCallsBox == null) return;
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 14, 10, 14));
        row.setStyle("-fx-background-color:#e8f5e9;-fx-background-radius:8;" +
                     "-fx-border-color:#27ae60;-fx-border-radius:8;-fx-border-width:1.5;");

        Label icon = new Label("🟢 ON CALL");
        icon.setStyle("-fx-font-weight:bold;-fx-text-fill:#27ae60;-fx-font-size:14px;");
        Label name = new Label("Connected with: " + callerName);
        name.setStyle("-fx-text-fill:#1a1a2e;-fx-font-size:13px;");
        HBox.setHgrow(name, Priority.ALWAYS);

        Button endBtn = new Button("📵 End Call");
        endBtn.setStyle("-fx-background-color:#e74c3c;-fx-text-fill:white;-fx-font-weight:bold;" +
                        "-fx-background-radius:8;-fx-padding:7 16;-fx-cursor:hand;");
        endBtn.setOnAction(e -> {
            svcDefDAO.endCall(callId);
            loadRingingCalls();
            row.getChildren().clear();
        });

        row.getChildren().addAll(icon, name, endBtn);
        ringingCallsBox.getChildren().add(0, row);
    }

    @FXML public void refreshCalls() { loadRingingCalls(); }

    // ════════════════════════════════════════════════════════════════════
    //  SERVICES MANAGEMENT
    // ════════════════════════════════════════════════════════════════════

    private void setupServicesMgmtTab() {
        if (smColId == null) return;

        smColId.setCellValueFactory(c -> new SimpleStringProperty(c.getValue()[0]));
        smColIcon.setCellValueFactory(c -> new SimpleStringProperty(c.getValue()[1]));
        smColName.setCellValueFactory(c -> new SimpleStringProperty(c.getValue()[2]));
        smColType.setCellValueFactory(c -> new SimpleStringProperty(c.getValue()[3]));
        smColActive.setCellValueFactory(c -> new SimpleStringProperty(c.getValue()[4]));

        smColActive.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                setStyle("1".equals(item)
                    ? "-fx-text-fill:#27ae60;-fx-font-weight:bold;"
                    : "-fx-text-fill:#e74c3c;");
            }
        });

        newSvcTypeCombo.setItems(FXCollections.observableArrayList(
            "HOUSEKEEPING","EXTRA_TOWELS","EXTRA_PILLOW","WAKE_UP_CALL","LAUNDRY",
            "TAXI","MAINTENANCE","PHONE_CALL","GENERAL"));
        newSvcTypeCombo.setStyle("-fx-background-color:#1a2d42;-fx-text-fill:#e2e8f0;-fx-border-color:#2d3f55;-fx-border-radius:6;-fx-background-radius:6;");

        loadServicesMgmt();
    }

    private void loadServicesMgmt() {
        if (svcMgmtTable == null) return;
        List<?> services = svcDefDAO.findAll(false);
        var rows = new java.util.ArrayList<String[]>();
        for (Object svc : services) {
            rows.add(new String[]{
                String.valueOf(readServiceField(svc, "serviceId", 0)),
                String.valueOf(readServiceField(svc, "icon", "🔧")),
                String.valueOf(readServiceField(svc, "serviceName", "")),
                String.valueOf(readServiceField(svc, "serviceType", "")),
                Boolean.TRUE.equals(readServiceField(svc, "active", false)) ? "1" : "0"
            });
        }
        svcMgmtTable.setItems(FXCollections.observableArrayList(rows));
    }

    private Object readServiceField(Object target, String fieldName, Object fallback) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            Object value = field.get(target);
            return value != null ? value : fallback;
        } catch (ReflectiveOperationException e) {
            return fallback;
        }
    }

    @FXML public void addService() {
        String name = newSvcName.getText().trim();
        String type = newSvcTypeCombo.getValue();
        String icon = normalizeServiceIconToken(newSvcIcon.getText().trim(), type);
        String desc = newSvcDesc.getText().trim();
        if (name.isEmpty() || type == null) {
            AlertUtil.showWarning("Missing Fields", "Service name and type are required."); return;
        }
        int id = svcDefDAO.addService(name, type, desc.isEmpty() ? null : desc, icon);
        if (id > 0) {
            AlertUtil.showInfo("Added", "Service \"" + name + "\" added successfully.");
            newSvcName.clear(); newSvcIcon.clear(); newSvcDesc.clear();
            newSvcTypeCombo.setValue(null);
            loadServicesMgmt();
        } else AlertUtil.showError("Error", "Failed to add service.");
    }

    private String normalizeServiceIconToken(String iconInput, String type) {
        if (iconInput == null || iconInput.isBlank()) {
            return type != null ? type : "GENERAL";
        }
        // Keep DB values ASCII-safe; the portal maps type/token to display emoji.
        if (!iconInput.chars().allMatch(ch -> ch <= 127)) {
            return type != null ? type : "GENERAL";
        }
        return iconInput.toUpperCase();
    }

    @FXML public void toggleServiceActive() {
        String[] selected = svcMgmtTable.getSelectionModel().getSelectedItem();
        if (selected == null) { AlertUtil.showWarning("Select Service", "Select a service first."); return; }
        int id = Integer.parseInt(selected[0]);
        boolean currentlyActive = "1".equals(selected[4]);
        svcDefDAO.toggleActive(id, !currentlyActive);
        loadServicesMgmt();
    }

    @FXML public void deleteService() {
        String[] selected = svcMgmtTable.getSelectionModel().getSelectedItem();
        if (selected == null) { AlertUtil.showWarning("Select Service", "Select a service first."); return; }
        if (AlertUtil.showConfirm("Delete Service", "Delete service \"" + selected[2] + "\"?")) {
            svcDefDAO.deleteService(Integer.parseInt(selected[0]));
            loadServicesMgmt();
        }
    }

    @FXML public void refreshServicesMgmt() { loadServicesMgmt(); }

    // ════════════════════════════════════════════════════════════════════
    //  COMBINED REFRESH
    // ════════════════════════════════════════════════════════════════════

    private void loadAll() {
        loadPortalBookings();
        loadServiceRequests();
        loadFoodOrders();
        loadServicesMgmt();
    }

    @FXML public void refreshAll() { loadAll(); loadRingingCalls(); }
}
