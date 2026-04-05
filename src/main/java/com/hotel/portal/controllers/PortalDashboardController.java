package com.hotel.portal.controllers;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.hotel.dao.BookingDAO;
import com.hotel.dao.CustomerDAO;
import com.hotel.dao.RoomDAO;
import com.hotel.model.Booking;
import com.hotel.model.Room;
import com.hotel.portal.CustomerPortalApp;
import com.hotel.portal.dao.AvailableServicesDAO;
import com.hotel.portal.dao.CustomerAccountDAO;
import com.hotel.portal.dao.FoodOrderDAO;
import com.hotel.portal.dao.ServiceRequestDAO;
import com.hotel.portal.model.FoodMenuItem;
import com.hotel.portal.model.FoodOrder;
import com.hotel.portal.model.FoodOrder.FoodOrderItem;
import com.hotel.portal.model.ServiceRequest;
import com.hotel.portal.service.PortalSession;
import com.hotel.service.BookingService;
import com.hotel.service.EmailService;
import com.hotel.util.DatabaseConnection;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Separator;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

public class PortalDashboardController {
    private String resolveServiceIcon(String serviceType, String rawIcon) {
        String icon = rawIcon != null ? rawIcon.trim() : "";
        if (!icon.isEmpty() && !icon.matches(".*[A-Za-z].*")) {
            return icon;
        }
        return switch (serviceType) {
            case "HOUSEKEEPING" -> "🧹";
            case "EXTRA_TOWELS" -> "🛁";
            case "EXTRA_PILLOW" -> "🛏";
            case "WAKE_UP_CALL" -> "⏰";
            case "LAUNDRY" -> "👕";
            case "TAXI" -> "🚕";
            case "MAINTENANCE" -> "🛠";
            case "PHONE_CALL" -> "☎";
            case "GENERAL" -> "📝";
            default -> "🔧";
        };
    }

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DT_FMT   = DateTimeFormatter.ofPattern("dd MMM HH:mm");

    // ── Navbar ────────────────────────────────────────────────────────────
    @FXML private Label    welcomeLabel;
    @FXML private Label    navTimeLabel;
    @FXML private Label    navDateLabel;
    @FXML private TabPane  mainTabPane;
    @FXML private Button   profileBtn;

    // ── Dashboard tab ─────────────────────────────────────────────────────
    @FXML private VBox     activeBookingBanner;
    @FXML private Label    activeBookingRef, activeBookingRoom;
    @FXML private Label    activeBookingCheckIn, activeBookingCheckOut, activeBookingTotal;
    @FXML private Label    activeBookingStatusLabel;
    @FXML private VBox     recentActivityBox;

    // ── My Bookings tab ───────────────────────────────────────────────────
    @FXML private TableView<Booking>          bookingTable;
    @FXML private TableColumn<Booking,String> colRef, colRoom, colCheckIn, colCheckOut, colStatus, colTotal;

    // ── Book Room tab ─────────────────────────────────────────────────────
    @FXML private DatePicker checkInPicker, checkOutPicker;
    @FXML private TableView<Room>    roomTable;
    @FXML private TableColumn<Room,String> colRoomNo, colCategory, colBedType, colCapacity, colPrice, colAmenities;
    @FXML private Label   bookingSummaryLabel, roomSelectedLabel;
    @FXML private Spinner<Integer> guestSpinner;
    @FXML private TextArea specialRequestArea;
    @FXML private Button  confirmBookingBtn;

    // ── Services tab ──────────────────────────────────────────────────────
    @FXML private FlowPane serviceCardsPane;
    @FXML private TextArea serviceDescArea;
    @FXML private TextField phoneCallField;
    @FXML private VBox      phoneCallBox;
    @FXML private TableView<ServiceRequest> serviceTable;
    @FXML private TableColumn<ServiceRequest,String> colSvcType, colSvcStatus, colSvcDate, colSvcDesc;

    // ── Food tab ──────────────────────────────────────────────────────────
    @FXML private TabPane foodTabPane;
    @FXML private VBox    menuVBox;
    @FXML private VBox    cartVBox;
    @FXML private Label   cartTotalLabel;
    @FXML private TextField foodNotesField;
    @FXML private Button  placeOrderBtn;
    @FXML private TableView<FoodOrder> orderHistoryTable;
    @FXML private TableColumn<FoodOrder,String> colOrderId, colOrderStatus, colOrderTotal, colOrderTime;
    @FXML private VBox    orderTrackingBox;
    @FXML private ProgressBar deliveryProgressBar;
    @FXML private Label   deliveryStatusLabel;

    // ── Account tab ───────────────────────────────────────────────────────
    @FXML private TextField accFirstName, accLastName, accEmail, accPhone, accCity, accCountry;
    @FXML private PasswordField accOldPassword, accNewPassword, accConfirmPassword;
    @FXML private Label accUpdateMsg;

    // ── DAOs / Services ───────────────────────────────────────────────────
    private final BookingService      bookingService  = new BookingService();
    private final BookingDAO          bookingDAO      = new BookingDAO();
    private final RoomDAO             roomDAO         = new RoomDAO();
    private final CustomerDAO         customerDAO     = new CustomerDAO();
    private final CustomerAccountDAO  accountDAO      = new CustomerAccountDAO();
    private final ServiceRequestDAO   serviceReqDAO   = new ServiceRequestDAO();
    private final FoodOrderDAO        foodOrderDAO    = new FoodOrderDAO();
    private final AvailableServicesDAO svcDefDAO      = new AvailableServicesDAO();
    private final EmailService        emailService    = new EmailService();

    // ── State ─────────────────────────────────────────────────────────────
    private Room     selectedRoom    = null;
    private String   selectedSvcType = null;
    private final Map<Integer, Integer> cartItems = new LinkedHashMap<>();
    private List<FoodMenuItem> menuItems = new ArrayList<>();
    private int  activeOrderId = -1;
    private Timeline clockTimeline, orderPollingTimeline, portalSyncTimeline;

    // ══════════════════════════════════════════════════════════════════════

    @FXML
    public void initialize() {
        String name = PortalSession.getInstance().getCurrentAccount().getFirstName();
        welcomeLabel.setText(name.toUpperCase());

        // Keep only dashboard action buttons as the primary navigation.
        Platform.runLater(() -> {
            var header = mainTabPane.lookup(".tab-header-area");
            if (header != null) {
                header.setManaged(false);
                header.setVisible(false);
            }
        });

        startClock();
        setupBookingTable();
        setupRoomTable();
        setupServiceTab();
        setupFoodTab();
        setupAccountTab();

        foodTabPane.getSelectionModel().selectedIndexProperty().addListener((obs, oldIndex, newIndex) -> {
            if (newIndex == null || newIndex.intValue() != 2) {
                stopOrderTracking();
            } else if (activeOrderId > 0) {
                startOrderTracking(activeOrderId);
            }
        });

        loadMyBookings();
        refreshDashboard();
        startPortalSync();
    }

    private void startPortalSync() {
        if (portalSyncTimeline != null) {
            portalSyncTimeline.stop();
        }
        portalSyncTimeline = new Timeline(new KeyFrame(Duration.seconds(8), e -> {
            loadMyBookings();
            refreshDashboard();
            loadOrderHistory();
        }));
        portalSyncTimeline.setCycleCount(Timeline.INDEFINITE);
        portalSyncTimeline.play();
    }

    // ════════════════════════════════════════════════════════════════════
    //  CLOCK
    // ════════════════════════════════════════════════════════════════════

    private void startClock() {
        clockTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            LocalDateTime now = LocalDateTime.now();
            navTimeLabel.setText(now.format(TIME_FMT));
            navDateLabel.setText(now.format(DateTimeFormatter.ofPattern("EEE, dd MMM")));
        }));
        clockTimeline.setCycleCount(Timeline.INDEFINITE);
        clockTimeline.play();
    }

    // ════════════════════════════════════════════════════════════════════
    //  NAVBAR / PROFILE
    // ════════════════════════════════════════════════════════════════════

    @FXML public void openProfile() { mainTabPane.getSelectionModel().select(5); }

    @FXML public void goToHome() { mainTabPane.getSelectionModel().select(0); }

    @FXML public void goToHomeFromProfile() { mainTabPane.getSelectionModel().select(0); }

    @FXML public void logout() {
        if (clockTimeline != null)        clockTimeline.stop();
        if (orderPollingTimeline != null)  orderPollingTimeline.stop();
        if (portalSyncTimeline != null)    portalSyncTimeline.stop();
        PortalSession.getInstance().logout();
        try { CustomerPortalApp.loadLogin(); }
        catch (Exception e) { showAlert("Logout error: " + e.getMessage()); }
    }

    // ════════════════════════════════════════════════════════════════════
    //  DASHBOARD HUB
    // ════════════════════════════════════════════════════════════════════

    @FXML public void goToBookRoom()    { mainTabPane.getSelectionModel().select(2); }
    @FXML public void goToMyBookings()  { mainTabPane.getSelectionModel().select(1); }
    @FXML public void goToFoodMenu()    { mainTabPane.getSelectionModel().select(3); foodTabPane.getSelectionModel().select(0); }
    @FXML public void goToMyOrders()    { mainTabPane.getSelectionModel().select(3); foodTabPane.getSelectionModel().select(1); }
    @FXML public void goToServices()    { mainTabPane.getSelectionModel().select(4); }

    private Optional<Booking> getCheckedInBooking() {
        int cid = PortalSession.getInstance().getCustomerId();
        return bookingDAO.findByCustomerId(cid).stream()
            .filter(b -> b.getStatus() == Booking.Status.CHECKED_IN)
            .findFirst();
    }

    private void refreshDashboard() {
        int cid = PortalSession.getInstance().getCustomerId();
        List<Booking> bookings = bookingDAO.findByCustomerId(cid);

        // Active booking (CONFIRMED or CHECKED_IN)
        Optional<Booking> active = bookings.stream()
            .filter(b -> b.getStatus() == Booking.Status.CONFIRMED || b.getStatus() == Booking.Status.CHECKED_IN)
            .findFirst();

        if (active.isPresent()) {
            Booking b = active.get();
            activeBookingBanner.setVisible(true);
            activeBookingBanner.setManaged(true);
            activeBookingRef.setText(b.getBookingReference());
            activeBookingRoom.setText(b.getRoomNumber() + " — " + b.getRoomCategory());
            activeBookingCheckIn.setText(b.getCheckInDate() != null ? b.getCheckInDate().format(DATE_FMT) : "—");
            activeBookingCheckOut.setText(b.getCheckOutDate() != null ? b.getCheckOutDate().format(DATE_FMT) : "—");
            activeBookingTotal.setText("$" + String.format("%.2f", b.getTotalAmount()));
            activeBookingStatusLabel.setText(b.getStatus().name());
        } else {
            activeBookingBanner.setVisible(false);
            activeBookingBanner.setManaged(false);
        }

        // Recent activity
        recentActivityBox.getChildren().clear();
        List<FoodOrder> orders = foodOrderDAO.findByCustomer(cid);
        List<ServiceRequest> svcReqs = serviceReqDAO.findByCustomer(cid);

        if (orders.isEmpty() && svcReqs.isEmpty()) {
            recentActivityBox.getChildren().add(new Label("No recent activity.") {{
                getStyleClass().add("portal-hint-text");
            }});
        } else {
            orders.stream().limit(3).forEach(o -> recentActivityBox.getChildren().add(buildActivityRow(
                "🍽  Food Order #" + o.getOrderId(),
                o.getStatusDisplayName(),
                o.getOrderedAt() != null ? o.getOrderedAt().format(DT_FMT) : ""
            )));
            svcReqs.stream().limit(3).forEach(r -> recentActivityBox.getChildren().add(buildActivityRow(
                r.getTypeDisplayName(),
                r.getStatus() != null ? r.getStatus().name() : "",
                r.getRequestedAt() != null ? r.getRequestedAt().format(DT_FMT) : ""
            )));
        }
    }

    private HBox buildActivityRow(String title, String status, String time) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8, 12, 8, 12));
        row.setStyle("-fx-background-color:#12122a; -fx-background-radius:6; -fx-border-color:#2a2a4a; -fx-border-radius:6;");
        Label t = new Label(title); t.setStyle("-fx-text-fill:#ddd; -fx-font-size:12px;");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label s = new Label(status); s.setStyle("-fx-text-fill:#ffeb3b; -fx-font-size:11px; -fx-font-weight:bold;");
        Label tm = new Label(time); tm.setStyle("-fx-text-fill:#666; -fx-font-size:11px;");
        row.getChildren().addAll(t, sp, s, tm);
        return row;
    }

    // ════════════════════════════════════════════════════════════════════
    //  MY BOOKINGS
    // ════════════════════════════════════════════════════════════════════

    private void setupBookingTable() {
        colRef.setCellValueFactory(new PropertyValueFactory<>("bookingReference"));
        colRoom.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().getRoomNumber() + " (" + c.getValue().getRoomCategory() + ")"));
        colCheckIn.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().getCheckInDate() != null ? c.getValue().getCheckInDate().format(DATE_FMT) : ""));
        colCheckOut.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().getCheckOutDate() != null ? c.getValue().getCheckOutDate().format(DATE_FMT) : ""));
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().getStatus() != null ? c.getValue().getStatus().name() : ""));
        colTotal.setCellValueFactory(c -> new SimpleStringProperty(
            "$" + String.format("%.2f", c.getValue().getTotalAmount())));

        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                setStyle(switch (item) {
                    case "CONFIRMED"   -> "-fx-text-fill:#ffeb3b; -fx-font-weight:bold;";
                    case "CHECKED_IN"  -> "-fx-text-fill:#4ade80; -fx-font-weight:bold;";
                    case "CHECKED_OUT" -> "-fx-text-fill:#666;";
                    case "CANCELLED"   -> "-fx-text-fill:#ff6b6b;";
                    default            -> "-fx-text-fill:#aaa;";
                });
            }
        });
    }

    private void loadMyBookings() {
        int cid = PortalSession.getInstance().getCustomerId();
        bookingTable.setItems(FXCollections.observableArrayList(bookingDAO.findByCustomerId(cid)));
    }

    @FXML public void refreshBookings() { loadMyBookings(); refreshDashboard(); }

    // ════════════════════════════════════════════════════════════════════
    //  BOOK A ROOM
    // ════════════════════════════════════════════════════════════════════

    private void setupRoomTable() {
        colRoomNo.setCellValueFactory(new PropertyValueFactory<>("roomNumber"));
        colCategory.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().getCategory() != null ? c.getValue().getCategory().name() : ""));
        colBedType.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().getBedType() != null ? c.getValue().getBedType() : ""));
        colCapacity.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getCapacity() + " guests"));
        colPrice.setCellValueFactory(c -> new SimpleStringProperty(
            "$" + String.format("%.0f", c.getValue().getPricePerNight()) + "/night"));
        colAmenities.setCellValueFactory(c -> {
            Room r = c.getValue();
            List<String> am = new ArrayList<>();
            if (r.isHasAC())   am.add("AC");
            if (r.isHasWifi()) am.add("WiFi");
            if (r.isHasTV())   am.add("TV");
            return new SimpleStringProperty(String.join(", ", am));
        });

        roomTable.setRowFactory(tv -> {
            TableRow<Room> row = new TableRow<>();
            row.setOnMouseClicked(e -> { if (!row.isEmpty()) selectRoom(row.getItem()); });
            return row;
        });

        checkInPicker.setValue(LocalDate.now().plusDays(1));
        checkOutPicker.setValue(LocalDate.now().plusDays(2));
        guestSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 6, 1));
    }

    @FXML public void searchAvailableRooms() {
        LocalDate ci = checkInPicker.getValue();
        LocalDate co = checkOutPicker.getValue();
        if (ci == null || co == null || !co.isAfter(ci)) {
            showAlert("Check-out date must be after check-in date."); return;
        }
        if (ci.isBefore(LocalDate.now())) {
            showAlert("Check-in date cannot be in the past."); return;
        }
        List<Room> rooms = bookingService.getAvailableRooms(ci, co);
        roomTable.setItems(FXCollections.observableArrayList(rooms));
        selectedRoom = null;
        roomSelectedLabel.setText("Click a room above to select it");
        confirmBookingBtn.setDisable(true);
        bookingSummaryLabel.setText("");
    }

    private void selectRoom(Room room) {
        selectedRoom = room;
        LocalDate ci = checkInPicker.getValue();
        LocalDate co = checkOutPicker.getValue();
        if (ci != null && co != null) {
            long nights = co.toEpochDay() - ci.toEpochDay();
            double total = nights * room.getPricePerNight();
            roomSelectedLabel.setText("Room " + room.getRoomNumber() + "  —  " + room.getCategory().name());
            bookingSummaryLabel.setText(nights + " night(s)  ×  $" + String.format("%.0f", room.getPricePerNight())
                + " = $" + String.format("%.2f", total) + "  (+tax)");
            confirmBookingBtn.setDisable(false);
        }
    }

    @FXML public void confirmBooking() {
        if (selectedRoom == null) { showAlert("Please select a room first."); return; }
        LocalDate ci = checkInPicker.getValue();
        LocalDate co = checkOutPicker.getValue();
        int cid = PortalSession.getInstance().getCustomerId();
        String ref = bookingDAO.generateBookingReference("PRT-");

        long nights = co.toEpochDay() - ci.toEpochDay();
        double roomCharges = nights * selectedRoom.getPricePerNight();
        double gstRate = selectedRoom.getPricePerNight() > 7500 ? 0.18
                       : selectedRoom.getPricePerNight() > 2500 ? 0.12 : 0.0;
        double gst   = roomCharges * gstRate;
        double total = roomCharges + gst;

        String sql = "INSERT INTO BOOKINGS (BOOKING_REFERENCE,CUSTOMER_ID,ROOM_ID,USER_ID," +
                     "CHECK_IN_DATE,CHECK_OUT_DATE,NUMBER_OF_GUESTS,ROOM_CHARGES," +
                     "SERVICE_CHARGES,GST_AMOUNT,TOTAL_AMOUNT,ADVANCE_PAID,BALANCE_DUE," +
                     "STATUS,SPECIAL_REQUESTS,BOOKING_DATE,BOOKING_SOURCE,SEEN_BY_STAFF) " +
                     "VALUES (?,?,?,?,?,?,?,?,0,?,?,0,?,?,?,SYSDATE,'PORTAL',0)";
        int bookingId = -1;
        try {
            try (var ps = DatabaseConnection.getConnection()
                    .prepareStatement(sql, new String[]{"BOOKING_ID"})) {
                ps.setString(1, ref);
                ps.setInt(2, cid);
                ps.setInt(3, selectedRoom.getRoomId());
                ps.setInt(4, 1);
                ps.setDate(5, java.sql.Date.valueOf(ci));
                ps.setDate(6, java.sql.Date.valueOf(co));
                ps.setInt(7, guestSpinner.getValue());
                ps.setDouble(8, roomCharges);
                ps.setDouble(9, gst);
                ps.setDouble(10, total);
                ps.setDouble(11, total);
                ps.setString(12, Booking.Status.CONFIRMED.name());
                ps.setString(13, specialRequestArea.getText().trim());
                ps.executeUpdate();
                DatabaseConnection.commit();
                try (var rs = ps.getGeneratedKeys()) { if (rs.next()) bookingId = rs.getInt(1); }
            }
        } catch (Exception e) {
            DatabaseConnection.rollback();
            showAlert("Booking failed: " + e.getMessage()); return;
        } finally {
            DatabaseConnection.closeConnection();
        }

        if (bookingId > 0) {
            // Send confirmation email in background
            final int finalBid = bookingId;
            new Thread(() -> {
                try {
                    customerDAO.findById(cid).ifPresent(customer -> {
                        Booking bk = bookingDAO.findById(finalBid).orElse(null);
                        if (bk != null) emailService.sendBookingConfirmation(customer, bk);
                    });
                } catch (Exception ignored) {}
            }).start();

            showInfo("✦  Reservation Confirmed!",
                "Booking Ref: " + ref + "\n" +
                "Room: " + selectedRoom.getRoomNumber() + " — " + selectedRoom.getCategory().name() + "\n" +
                "Check-in: " + ci.format(DATE_FMT) + "\n" +
                "Check-out: " + co.format(DATE_FMT) + "\n" +
                "Total: $" + String.format("%.2f", total) + "\n\n" +
                "A confirmation email has been sent to you.");
            loadMyBookings();
            refreshDashboard();
            mainTabPane.getSelectionModel().select(1);
            selectedRoom = null;
            roomSelectedLabel.setText("Click a room above to select it");
            confirmBookingBtn.setDisable(true);
            bookingSummaryLabel.setText("");
            specialRequestArea.clear();
        }
    }

    // ════════════════════════════════════════════════════════════════════
    //  PHONE CALL
    // ════════════════════════════════════════════════════════════════════

    @FXML public void placeCall() {
        var acc = PortalSession.getInstance().getCurrentAccount();
        int callId = svcDefDAO.placeCall(acc.getCustomerId(), acc.getFullName(), "—");
        if (callId <= 0) { showAlert("Could not place call. Try again."); return; }
        showCallDialog(callId);
    }

    private void showCallDialog(int callId) {
        Stage dialog = new Stage(StageStyle.UNDECORATED);
        dialog.initModality(Modality.APPLICATION_MODAL);

        VBox box = new VBox(16);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(32));
        box.setPrefWidth(360);
        box.setStyle("-fx-background-color:linear-gradient(to bottom,#1a0a2e,#0a0a1e);" +
                 "-fx-border-color:#ffeb3b;-fx-border-width:2;-fx-border-radius:14;-fx-background-radius:14;");

        Label icon   = new Label("📞"); icon.setStyle("-fx-font-size:48px;");
        Label status = new Label("Calling Reception…"); status.getStyleClass().add("call-status-label");
        status.setStyle("-fx-font-size:18px;-fx-font-weight:bold;-fx-text-fill:#ffeb3b;");
        Label sub    = new Label("Please hold on…"); sub.setStyle("-fx-text-fill:#aaa;-fx-font-size:13px;");

        Button endBtn = new Button("End Call");
        endBtn.setStyle("-fx-background-color:#3a0a0a;-fx-text-fill:#ff6b6b;-fx-font-weight:bold;" +
                        "-fx-background-radius:8;-fx-border-color:#ff6b6b;-fx-border-radius:8;-fx-padding:8 22;-fx-cursor:hand;");

        box.getChildren().addAll(icon, status, sub, endBtn);

        Scene scene = new Scene(box);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        dialog.setScene(scene);
        dialog.show();

        // Poll for answer status every 2s, timeout after 45s
        Timeline poll = new Timeline(new KeyFrame(Duration.seconds(2), e -> {
            String s = svcDefDAO.getCallStatus(callId);
            Platform.runLater(() -> {
                if ("ANSWERED".equals(s)) {
                    icon.setText("🟢");
                    status.setText("Connected ✓");
                    status.setStyle("-fx-font-size:18px;-fx-font-weight:bold;-fx-text-fill:#4ade80;");
                    sub.setText("You are connected to the Receptionist.");
                    endBtn.setText("End Call");
                } else if ("ENDED".equals(s) || "MISSED".equals(s)) {
                    dialog.close();
                }
            });
        }));
        poll.setCycleCount(22); // ~45 seconds
        poll.setOnFinished(e -> {
            svcDefDAO.missCall(callId);
            Platform.runLater(dialog::close);
        });
        poll.play();

        endBtn.setOnAction(e -> {
            poll.stop();
            svcDefDAO.endCall(callId);
            dialog.close();
        });
    }

    // ════════════════════════════════════════════════════════════════════
    //  SERVICES
    // ════════════════════════════════════════════════════════════════════

    private void setupServiceTab() {
        colSvcType.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().getRequestType() != null ? c.getValue().getTypeDisplayName() : ""));
        colSvcStatus.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().getStatus() != null ? c.getValue().getStatus().name() : ""));
        colSvcDate.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().getRequestedAt() != null ? c.getValue().getRequestedAt().format(DT_FMT) : ""));
        colSvcDesc.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getDescription()));

        loadServiceCards();
        loadServiceRequests();
    }

    private void loadServiceCards() {
        serviceCardsPane.getChildren().clear();
        selectedSvcType = null;

        List<?> services = svcDefDAO.findAll(true);
        Set<String> seenServiceKeys = new HashSet<>();
        for (Object svc : services) {
            String svcName = String.valueOf(readServiceField(svc, "serviceName", "Service"));
            String svcType = String.valueOf(readServiceField(svc, "serviceType", "GENERAL"));
            String dedupeKey = (svcType + "|" + svcName).toUpperCase();
            if (!seenServiceKeys.add(dedupeKey)) {
                continue;
            }

            VBox card = new VBox(12);
            card.setAlignment(Pos.TOP_CENTER);
            card.setPrefWidth(130);
            card.setPrefHeight(140);
            card.getStyleClass().add("service-card");
            card.setPadding(new Insets(12));

            String rawIcon = String.valueOf(readServiceField(svc, "icon", ""));
            String svcIcon = resolveServiceIcon(svcType, rawIcon);

            Label iconL = new Label(svcIcon);
            iconL.setStyle("-fx-font-size: 48px;");
            Label nameL = new Label(svcName);
            nameL.setStyle("-fx-font-size: 12px; -fx-text-fill: #ffeb3b; -fx-font-weight: bold;");
            nameL.setWrapText(true);
            nameL.setMaxWidth(110);
            nameL.setAlignment(Pos.CENTER);

            card.getChildren().addAll(iconL, nameL);

            card.setOnMouseClicked(e -> {
                selectedSvcType = svcType;
                // Reset all card borders
                serviceCardsPane.getChildren().forEach(n ->
                    n.setStyle("-fx-background-color:#12122a;-fx-background-radius:10;" +
                               "-fx-border-color:#2a2a4a;-fx-border-radius:10;-fx-padding:14;-fx-cursor:hand;"));
                // Highlight selected
                card.setStyle("-fx-background-color:#1a0a2e;-fx-background-radius:10;" +
                              "-fx-border-color:#ffeb3b;-fx-border-radius:10;-fx-padding:14;-fx-cursor:hand;" +
                              "-fx-effect:dropshadow(gaussian,rgba(255,235,59,0.4),12,0,0,0);");
                // Show/hide phone box
                boolean isPhone = "PHONE_CALL".equals(svcType);
                phoneCallBox.setVisible(isPhone);
                phoneCallBox.setManaged(isPhone);
            });

            serviceCardsPane.getChildren().add(card);
        }
    }

    private void loadServiceRequests() {
        int cid = PortalSession.getInstance().getCustomerId();
        serviceTable.setItems(FXCollections.observableArrayList(serviceReqDAO.findByCustomer(cid)));
    }

    @FXML public void submitServiceRequest() {
        if (selectedSvcType == null) { showAlert("Please select a service type first."); return; }
        Optional<Booking> checkedInBooking = getCheckedInBooking();
        if (checkedInBooking.isEmpty()) {
            showAlert("Service requests are only available after check-in. Please check in to your room first.");
            return;
        }
        ServiceRequest req = new ServiceRequest();
        req.setCustomerId(PortalSession.getInstance().getCustomerId());
        req.setBookingId(checkedInBooking.get().getBookingId());
        try { req.setRequestType(ServiceRequest.Type.valueOf(selectedSvcType)); }
        catch (Exception e) { showAlert("Unknown service type."); return; }
        req.setDescription(serviceDescArea.getText().trim());
        req.setPriority(ServiceRequest.Priority.NORMAL);
        if ("PHONE_CALL".equals(selectedSvcType)) {
            String ph = phoneCallField.getText().trim();
            if (ph.isEmpty()) { showAlert("Please enter the phone number to call."); return; }
            req.setPhoneNumber(ph);
            req.setDescription("Phone call requested to: " + ph);
        }
        int id = serviceReqDAO.save(req);
        if (id > 0) {
            showInfo("Request Submitted", "Your request has been sent to our team.\nWe will attend to it shortly.");
            serviceDescArea.clear(); phoneCallField.clear();
            selectedSvcType = null;
            serviceCardsPane.getChildren().forEach(n ->
                n.setStyle("-fx-background-color:#12122a;-fx-background-radius:10;" +
                           "-fx-border-color:#2a2a4a;-fx-border-radius:10;-fx-padding:14;-fx-cursor:hand;"));
            loadServiceRequests();
            refreshDashboard();
        } else { showAlert("Failed to submit request."); }
    }

    @FXML public void refreshServices() {
        loadServiceCards();
        loadServiceRequests();
    }

    // ════════════════════════════════════════════════════════════════════
    //  FOOD ORDERING
    // ════════════════════════════════════════════════════════════════════

    private void setupFoodTab() {
        setupOrderHistoryTable();
        loadMenu();
        loadOrderHistory();
    }

    private void loadMenu() {
        menuItems = foodOrderDAO.getAllMenuItems();
        renderMenu();
    }

    private void renderMenu() {
        menuVBox.getChildren().clear();

        Map<FoodMenuItem.Category, List<FoodMenuItem>> grouped = menuItems.stream()
            .collect(Collectors.groupingBy(FoodMenuItem::getCategory, LinkedHashMap::new, Collectors.toList()));

        for (var entry : grouped.entrySet()) {
            // Category header
            Label catLabel = new Label(getCatDisplay(entry.getKey()));
            catLabel.getStyleClass().add("menu-category-label");
            catLabel.setPrefWidth(Double.MAX_VALUE);
            VBox.setMargin(catLabel, new Insets(10, 0, 4, 0));
            menuVBox.getChildren().add(catLabel);

            for (FoodMenuItem item : entry.getValue()) {
                menuVBox.getChildren().add(buildMenuRow(item));
            }
        }
    }

    private HBox buildMenuRow(FoodMenuItem item) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10, 14, 10, 14));
        row.getStyleClass().add("menu-row");

        VBox nameBox = new VBox(2);
        Label name = new Label(item.getItemName());
        name.getStyleClass().add("menu-item-name");
        Label desc = new Label(item.getDescription() != null ? item.getDescription() : "");
        desc.getStyleClass().add("menu-item-desc");
        nameBox.getChildren().addAll(name, desc);
        HBox.setHgrow(nameBox, Priority.ALWAYS);

        Label prepL = new Label("⏱ " + item.getPrepTimeMins() + "m");
        prepL.setStyle("-fx-font-size:10px;-fx-text-fill:#555;-fx-min-width:40;");

        Label price = new Label("$" + String.format("%.0f", item.getPrice()));
        price.getStyleClass().add("menu-item-price");
        price.setMinWidth(55);

        // Qty controls
        Button minus = new Button("−"); minus.getStyleClass().add("qty-btn");
        Label  qty   = new Label("0"); qty.getStyleClass().add("qty-label");
        Button plus  = new Button("+"); plus.getStyleClass().add("qty-btn");

        int current = cartItems.getOrDefault(item.getMenuItemId(), 0);
        qty.setText(String.valueOf(current));

        plus.setOnAction(e -> {
            int q = cartItems.getOrDefault(item.getMenuItemId(), 0) + 1;
            cartItems.put(item.getMenuItemId(), q);
            qty.setText(String.valueOf(q));
            updateCartView();
        });
        minus.setOnAction(e -> {
            int q = Math.max(0, cartItems.getOrDefault(item.getMenuItemId(), 0) - 1);
            if (q == 0) cartItems.remove(item.getMenuItemId());
            else cartItems.put(item.getMenuItemId(), q);
            qty.setText(String.valueOf(q));
            updateCartView();
        });

        row.getChildren().addAll(nameBox, prepL, price, minus, qty, plus);
        return row;
    }

    private void updateCartView() {
        cartVBox.getChildren().clear();
        double total = 0;

        if (cartItems.isEmpty()) {
            Label empty = new Label("Cart is empty"); empty.getStyleClass().add("cart-empty-label");
            cartVBox.getChildren().add(empty);
            placeOrderBtn.setDisable(true);
        } else {
            placeOrderBtn.setDisable(false);
            for (var entry : cartItems.entrySet()) {
                FoodMenuItem mi = menuItems.stream()
                    .filter(m -> m.getMenuItemId() == entry.getKey()).findFirst().orElse(null);
                if (mi == null) continue;
                double sub = mi.getPrice() * entry.getValue();
                total += sub;

                HBox row = new HBox(6); row.setAlignment(Pos.CENTER_LEFT);
                Label n = new Label(mi.getItemName()); n.getStyleClass().add("cart-item-name"); HBox.setHgrow(n, Priority.ALWAYS);
                n.setMaxWidth(130); n.setWrapText(true);
                Label q = new Label("×" + entry.getValue()); q.setStyle("-fx-text-fill:#888;-fx-font-size:12px;");
                Label p = new Label("$" + String.format("%.0f", sub)); p.getStyleClass().add("cart-price");
                row.getChildren().addAll(n, q, p);
                cartVBox.getChildren().add(row);
                Separator sep = new Separator(); sep.setOpacity(0.2);
                cartVBox.getChildren().add(sep);
            }
        }
        cartTotalLabel.setText("TOTAL: $" + String.format("%.2f", total));
    }

    @FXML public void clearCart() {
        cartItems.clear();
        loadMenu();
        updateCartView();
    }

    @FXML public void placeOrder() {
        if (cartItems.isEmpty()) { showAlert("Cart is empty."); return; }

        Optional<Booking> checkedInBooking = getCheckedInBooking();
        if (checkedInBooking.isEmpty()) {
            showAlert("Food ordering is only available after check-in. Please check in to your room first.");
            return;
        }

        FoodOrder order = new FoodOrder();
        int cid = PortalSession.getInstance().getCustomerId();
        order.setCustomerId(cid);
        order.setSpecialNotes(foodNotesField.getText().trim());

        double total = 0;
        List<FoodOrderItem> items = new ArrayList<>();
        for (var entry : cartItems.entrySet()) {
            FoodMenuItem mi = menuItems.stream()
                .filter(m -> m.getMenuItemId() == entry.getKey()).findFirst().orElse(null);
            if (mi == null) continue;
            double sub = mi.getPrice() * entry.getValue();
            total += sub;
            items.add(new FoodOrderItem(mi.getMenuItemId(), mi.getItemName(), entry.getValue(), mi.getPrice()));
        }
        order.setTotalAmount(total);
        order.setItems(items);
        final double orderTotal = total;

        // Add to BOOKINGS balance via active checked-in booking
        Booking checkedIn = checkedInBooking.get();
        order.setBookingId(checkedIn.getBookingId());
        try {
            String sql = "UPDATE BOOKINGS SET SERVICE_CHARGES = NVL(SERVICE_CHARGES,0) + ?, " +
                         "TOTAL_AMOUNT = NVL(TOTAL_AMOUNT,0) + ?, BALANCE_DUE = NVL(BALANCE_DUE,0) + ? " +
                         "WHERE BOOKING_ID = ?";
            try (var ps = DatabaseConnection.getConnection().prepareStatement(sql)) {
                ps.setDouble(1, orderTotal); ps.setDouble(2, orderTotal); ps.setDouble(3, orderTotal);
                ps.setInt(4, checkedIn.getBookingId());
                ps.executeUpdate();
                DatabaseConnection.commit();
            }
        } catch (Exception ignored) {
        } finally {
            DatabaseConnection.closeConnection();
        }

        int orderId = foodOrderDAO.placeOrder(order);
        if (orderId > 0) {
            activeOrderId = orderId;
            showInfo("Order Placed!",
                "Order #" + orderId + " received!\nTotal: $" + String.format("%.2f", total) +
                "\n\nThis amount has been added to your room bill.\nTrack your order in the 'Track' tab.");
            cartItems.clear();
            foodNotesField.clear();
            loadMenu();
            updateCartView();
            loadOrderHistory();
            startOrderTracking(orderId);
            foodTabPane.getSelectionModel().select(2);
            refreshDashboard();
        } else { showAlert("Failed to place order."); }
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

    private void setupOrderHistoryTable() {
        colOrderId.setCellValueFactory(c -> new SimpleStringProperty("#" + c.getValue().getOrderId()));
        colOrderStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatusDisplayName()));
        colOrderTotal.setCellValueFactory(c -> new SimpleStringProperty("$" + String.format("%.2f", c.getValue().getTotalAmount())));
        colOrderTime.setCellValueFactory(c -> new SimpleStringProperty(
            c.getValue().getOrderedAt() != null ? c.getValue().getOrderedAt().format(DT_FMT) : ""));
        orderHistoryTable.setRowFactory(tv -> {
            TableRow<FoodOrder> row = new TableRow<>();
            row.setOnMouseClicked(e -> { if (!row.isEmpty()) startOrderTracking(row.getItem().getOrderId()); });
            return row;
        });
    }

    private void loadOrderHistory() {
        int cid = PortalSession.getInstance().getCustomerId();
        orderHistoryTable.setItems(FXCollections.observableArrayList(foodOrderDAO.findByCustomer(cid)));
    }

    private void startOrderTracking(int orderId) {
        activeOrderId = orderId;
        orderTrackingBox.setVisible(true);
        updateTrackingUI(foodOrderDAO.getOrderStatus(orderId));
        if (orderPollingTimeline != null) orderPollingTimeline.stop();
        orderPollingTimeline = new Timeline(new KeyFrame(Duration.seconds(8), e -> {
            FoodOrder.Status s = foodOrderDAO.getOrderStatus(activeOrderId);
            Platform.runLater(() -> updateTrackingUI(s));
            if (s == FoodOrder.Status.DELIVERED || s == FoodOrder.Status.CANCELLED) {
                orderPollingTimeline.stop();
                Platform.runLater(this::loadOrderHistory);
            }
        }));
        orderPollingTimeline.setCycleCount(Timeline.INDEFINITE);
        orderPollingTimeline.play();
    }

    private void stopOrderTracking() {
        if (orderPollingTimeline != null) {
            orderPollingTimeline.stop();
            orderPollingTimeline = null;
        }
        activeOrderId = -1;
        if (orderTrackingBox != null) {
            orderTrackingBox.setVisible(false);
            orderTrackingBox.setManaged(false);
        }
    }

    private void updateTrackingUI(FoodOrder.Status status) {
        double p = switch (status) {
            case RECEIVED -> 0.15; case PREPARING -> 0.45;
            case OUT_FOR_DELIVERY -> 0.75; case DELIVERED -> 1.0; case CANCELLED -> 0.0;
        };
        deliveryProgressBar.setProgress(p);
        deliveryStatusLabel.setText("Order #" + activeOrderId + "  —  " + status.name().replace("_", " "));
        if (status == FoodOrder.Status.DELIVERED)
            deliveryProgressBar.setStyle("-fx-accent:#4ade80;");
        else if (status == FoodOrder.Status.CANCELLED)
            deliveryProgressBar.setStyle("-fx-accent:#ff6b6b;");
        else
            deliveryProgressBar.setStyle("-fx-accent:#ffeb3b;");
    }

    @FXML public void refreshOrderHistory() { loadOrderHistory(); }

    // ════════════════════════════════════════════════════════════════════
    //  MY ACCOUNT
    // ════════════════════════════════════════════════════════════════════

    private void setupAccountTab() {
        var acc = PortalSession.getInstance().getCurrentAccount();
        accFirstName.setText(acc.getFirstName());
        accLastName.setText(acc.getLastName());
        accEmail.setText(acc.getEmail());
        accPhone.setText(acc.getPhone() != null ? acc.getPhone() : "");
        accUpdateMsg.setVisible(false);
        customerDAO.findById(acc.getCustomerId()).ifPresent(c -> {
            accCity.setText(c.getCity() != null ? c.getCity() : "");
            accCountry.setText(c.getCountry() != null ? c.getCountry() : "");
        });
    }

    @FXML public void saveProfile() {
        String fn = accFirstName.getText().trim(), ln = accLastName.getText().trim();
        String ph = accPhone.getText().trim();
        if (fn.isEmpty() || ln.isEmpty()) { showAccMsg("Name fields are required.", true); return; }
        int cid = PortalSession.getInstance().getCustomerId();
        customerDAO.findById(cid).ifPresent(c -> {
            c.setFirstName(fn); c.setLastName(ln); c.setPhone(ph);
            c.setCity(accCity.getText().trim()); c.setCountry(accCountry.getText().trim());
            if (customerDAO.update(c)) {
                PortalSession.getInstance().getCurrentAccount().setFirstName(fn);
                PortalSession.getInstance().getCurrentAccount().setLastName(ln);
                welcomeLabel.setText(fn.toUpperCase());
                showAccMsg("Profile updated successfully.", false);
            } else { showAccMsg("Update failed.", true); }
        });
    }

    @FXML public void changePassword() {
        String old = accOldPassword.getText(), nw = accNewPassword.getText(), cf = accConfirmPassword.getText();
        if (old.isEmpty() || nw.isEmpty() || cf.isEmpty()) { showAccMsg("All password fields required.", true); return; }
        if (nw.length() < 6) { showAccMsg("New password must be at least 6 characters.", true); return; }
        if (!nw.equals(cf))  { showAccMsg("Passwords do not match.", true); return; }
        boolean ok = accountDAO.changePassword(PortalSession.getInstance().getCustomerId(), old, nw);
        if (ok) { showAccMsg("Password changed.", false); accOldPassword.clear(); accNewPassword.clear(); accConfirmPassword.clear(); }
        else    { showAccMsg("Current password is incorrect.", true); }
    }

    @FXML public void deleteAccount() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Account");
        confirm.setHeaderText("Delete your portal account?");
        confirm.setContentText("Your booking history will be preserved. This cannot be undone.");
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                boolean ok = accountDAO.deleteAccount(PortalSession.getInstance().getCustomerId());
                if (ok) {
                    if (clockTimeline != null) clockTimeline.stop();
                    if (orderPollingTimeline != null) orderPollingTimeline.stop();
                    PortalSession.getInstance().logout();
                    try { CustomerPortalApp.loadLogin(); } catch (Exception e) { showAlert("Error: " + e.getMessage()); }
                } else { showAlert("Could not delete account."); }
            }
        });
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private void showAlert(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle("Notice"); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }

    private void showInfo(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(msg); a.showAndWait();
    }

    private void showAccMsg(String msg, boolean err) {
        accUpdateMsg.setText(msg);
        accUpdateMsg.setStyle(err ? "-fx-text-fill:#ff6b6b;" : "-fx-text-fill:#4ade80;");
        accUpdateMsg.setVisible(true);
    }

    private String getCatDisplay(FoodMenuItem.Category cat) {
        return switch (cat) {
            case BREAKFAST    -> "🍳  BREAKFAST";
            case LUNCH        -> "🍝  LUNCH";
            case DINNER       -> "🍽️  DINNER";
            case SNACKS       -> "🍟  SNACKS & APPETIZERS";
            case BEVERAGES    -> "🍹  BEVERAGES";
            case DESSERTS     -> "🍰  DESSERTS";
            case ROOM_SERVICE -> "🛎️  ROOM SERVICE";
        };
    }
}
