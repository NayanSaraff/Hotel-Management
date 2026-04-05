package com.hotel.controllers;

import com.hotel.model.*;
import com.hotel.service.*;
import com.hotel.dao.CustomerDAO;
import com.hotel.util.AlertUtil;
import com.hotel.util.SessionManager;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class BookingController {

    @FXML
    private TableView<Booking> bookingTable;
    @FXML
    private TableColumn<Booking, String> colRef;
    @FXML
    private TableColumn<Booking, String> colGuest;
    @FXML
    private TableColumn<Booking, String> colRoom;
    @FXML
    private TableColumn<Booking, String> colCheckIn;
    @FXML
    private TableColumn<Booking, String> colCheckOut;
    @FXML
    private TableColumn<Booking, Integer> colNights;
    @FXML
    private TableColumn<Booking, String> colStatus;
    @FXML
    private TableColumn<Booking, Double> colTotal;
    @FXML
    private TableColumn<Booking, Double> colBalance;

    @FXML
    private VBox bookingForm;
    @FXML
    private ComboBox<Customer> customerCombo;
    @FXML
    private ComboBox<Room> roomCombo;
    @FXML
    private DatePicker checkInPicker;
    @FXML
    private DatePicker checkOutPicker;
    @FXML
    private Spinner<Integer> guestSpinner;
    @FXML
    private TextField advanceField;
    @FXML
    private ComboBox<String> paymentModeCombo;
    @FXML
    private TextArea specialRequestsArea;
    @FXML
    private Label roomChargesLabel;
    @FXML
    private Label gstLabel;
    @FXML
    private Label totalLabel;
    @FXML
    private Label balanceLabel;

    @FXML
    private TextField couponField;
    @FXML
    private Label couponStatusLabel;
    @FXML
    private Label discountedTotalLabel;

    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<String> filterStatusCombo;
    @FXML
    private Button btnCheckIn;
    @FXML
    private Button btnCheckOut;
    @FXML
    private Button btnCancel;
    @FXML
    private Button btnInvoice;

    private final BookingService bookingService = new BookingService();
    private final CustomerDAO customerDAO = new CustomerDAO();
    private final InvoiceService invoiceService = new InvoiceService();
    private final EmailService emailService = new EmailService();
    private final CouponService couponService = new CouponService();
    private final PaymentGatewayService paymentGatewayService = new PaymentGatewayService();
    private final com.hotel.dao.BookingDAO bookingDAO = new com.hotel.dao.BookingDAO();

    // Portal sync badge label – injected via FXML if present, else created dynamically
    @FXML private Label portalNewBadge;

    @FXML
    public void initialize() {
        setupColumns();
        setupFilters();
        loadBookings();
        bookingForm.setVisible(false);
        bookingForm.setManaged(false);

        bookingTable.getSelectionModel().selectedItemProperty().addListener((obs, o, selected) -> {
            boolean hasSel = selected != null;
            btnCheckIn.setDisable(!hasSel || selected.getStatus() != Booking.Status.CONFIRMED);
            btnCheckOut.setDisable(!hasSel || selected.getStatus() != Booking.Status.CHECKED_IN);
            btnCancel.setDisable(!hasSel || selected.getStatus() == Booking.Status.CHECKED_OUT
                    || selected.getStatus() == Booking.Status.CANCELLED);
            btnInvoice.setDisable(!hasSel);
        });

        checkInPicker.valueProperty().addListener((obs, o, n) -> calculateCharges());
        checkOutPicker.valueProperty().addListener((obs, o, n) -> calculateCharges());
        roomCombo.valueProperty().addListener((obs, o, n) -> calculateCharges());
        advanceField.textProperty().addListener((obs, o, n) -> calculateCharges());
    }

    private void setupColumns() {
        colRef.setCellValueFactory(new PropertyValueFactory<>("bookingReference"));
        colGuest.setCellValueFactory(new PropertyValueFactory<>("customerName"));
        colRoom.setCellValueFactory(new PropertyValueFactory<>("roomNumber"));
        colCheckIn.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getCheckInDate() != null ? c.getValue().getCheckInDate().toString() : ""));
        colCheckOut.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getCheckOutDate() != null ? c.getValue().getCheckOutDate().toString() : ""));
        colNights.setCellValueFactory(c -> new javafx.beans.property.SimpleIntegerProperty(
                (int) c.getValue().getNumberOfNights()).asObject());
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus().name()));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));
        colBalance.setCellValueFactory(new PropertyValueFactory<>("balanceDue"));

        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(item);
                setStyle(switch (item) {
                    case "CONFIRMED" -> "-fx-text-fill:#2980b9;-fx-font-weight:bold;";
                    case "CHECKED_IN" -> "-fx-text-fill:#27ae60;-fx-font-weight:bold;";
                    case "CHECKED_OUT" -> "-fx-text-fill:#7f8c8d;";
                    case "CANCELLED" -> "-fx-text-fill:#e74c3c;";
                    default -> "";
                });
            }
        });
        bookingTable.getColumns().forEach(col -> col.setResizable(false));
        bookingTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void setupFilters() {
        filterStatusCombo.setItems(FXCollections.observableArrayList(
                "ALL", "CONFIRMED", "CHECKED_IN", "CHECKED_OUT", "CANCELLED"));
        filterStatusCombo.setValue("ALL");
        filterStatusCombo.valueProperty().addListener((obs, o, n) -> applyFilter());
        searchField.textProperty().addListener((obs, o, n) -> applyFilter());
    }

    private void loadBookings() {
        applyFilter();
    }

    private void applyFilter() {
        String statusStr = filterStatusCombo.getValue();
        String keyword = searchField.getText() != null ? searchField.getText().toLowerCase() : "";
        List<Booking> list = bookingService.getAllBookings().stream()
                .filter(b -> statusStr.equals("ALL") || b.getStatus().name().equals(statusStr))
                .filter(b -> keyword.isBlank()
                        || b.getBookingReference().toLowerCase().contains(keyword)
                        || (b.getCustomerName() != null && b.getCustomerName().toLowerCase().contains(keyword))
                        || (b.getRoomNumber() != null && b.getRoomNumber().toLowerCase().contains(keyword)))
                .toList();
        bookingTable.setItems(FXCollections.observableArrayList(list));

        // Keep booking rows visually consistent with the rest of booking management.
        bookingTable.setRowFactory(tv -> new javafx.scene.control.TableRow<Booking>() {
            @Override
            protected void updateItem(Booking item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("");
                } else {
                    // Use BOOKING_SOURCE field directly (reliable, not fragile prefix checking)
                    boolean isPortal = item.getBookingSource() != null &&
                                       item.getBookingSource() == Booking.Source.PORTAL;
                    if (isPortal) {
                        setStyle("-fx-background-color: white;");
                        setTooltip(new Tooltip("🌐 Online Portal Booking"));
                    } else {
                        setStyle("-fx-background-color: white;");
                        setTooltip(null);
                    }
                }
            }
        });
        updatePortalBadge();
    }

    /** Sync / refresh button – marks portal bookings as seen and reloads. */
    @FXML
    public void syncPortalBookings() {
        bookingDAO.markPortalBookingsSeen();
        loadBookings();
        int newCount = bookingDAO.countNewPortalBookings();
        if (portalNewBadge != null) {
            portalNewBadge.setVisible(newCount > 0);
            portalNewBadge.setText(newCount + " NEW");
        }
        AlertUtil.showInfo("Sync Complete",
            "Portal bookings synced. All new online bookings are now marked as reviewed.");
    }

    private void updatePortalBadge() {
        if (portalNewBadge != null) {
            int count = bookingDAO.countNewPortalBookings();
            portalNewBadge.setVisible(count > 0);
            portalNewBadge.setText(count + " NEW");
        }
    }

    @FXML
    private void handleNewBooking() {
        clearBookingForm();

        final List<Customer> allCustomers = customerDAO.findAll();
        customerCombo.setEditable(true);

        // StringConverter so Customer objects display as names
        customerCombo.setConverter(new StringConverter<Customer>() {
            @Override
            public String toString(Customer c) {
                return c == null ? "" : c.getFullName() + " — " + c.getPhone();
            }

            @Override
            public Customer fromString(String s) {
                if (s == null || s.isBlank())
                    return null;
                return allCustomers.stream()
                        .filter(c -> (c.getFullName() + " — " + c.getPhone()).equals(s)
                                || c.getFullName().toLowerCase().contains(s.toLowerCase())
                                || c.getPhone().contains(s))
                        .findFirst().orElse(null);
            }
        });

        customerCombo.setItems(FXCollections.observableArrayList(allCustomers));

        // Live search as user types
        customerCombo.getEditor().textProperty().addListener((obs, oldVal, newVal) -> {
            // Skip if a customer is already selected
            if (customerCombo.getSelectionModel().getSelectedItem() != null)
                return;
            if (newVal == null || newVal.isBlank()) {
                javafx.application.Platform.runLater(() -> {
                    customerCombo.hide();
                    customerCombo.setItems(FXCollections.observableArrayList(allCustomers));
                });
                return;
            }
            String kw = newVal.toLowerCase();
            var filtered = allCustomers.stream()
                    .filter(c -> c.getFullName().toLowerCase().contains(kw)
                            || c.getPhone().contains(kw))
                    .toList();
            javafx.application.Platform.runLater(() -> {
                customerCombo.hide();
                customerCombo.setItems(FXCollections.observableArrayList(filtered));
                if (!filtered.isEmpty())
                    customerCombo.show();
            });
        });

        paymentModeCombo.setItems(FXCollections.observableArrayList(
                "CASH", "CREDIT_CARD", "DEBIT_CARD", "UPI", "NET_BANKING"));
        paymentModeCombo.setValue("CASH");
        guestSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10, 1));
        checkInPicker.setValue(LocalDate.now());
        checkOutPicker.setValue(LocalDate.now().plusDays(1));
        bookingForm.setVisible(true);
        bookingForm.setManaged(true);
    }

    @FXML
    private void handleSearchAvailableRooms() {
        LocalDate ci = checkInPicker.getValue();
        LocalDate co = checkOutPicker.getValue();
        if (ci == null || co == null || !co.isAfter(ci)) {
            AlertUtil.showWarning("Invalid Dates", "Please select valid check-in and check-out dates.");
            return;
        }
        try {
            List<Room> rooms = bookingService.getAvailableRooms(ci, co);
            roomCombo.setItems(FXCollections.observableArrayList(rooms));
            if (rooms.isEmpty())
                AlertUtil.showInfo("No Rooms", "No rooms available for selected dates.");
        } catch (Exception e) {
            AlertUtil.showError("Error", e.getMessage());
        }
    }

    @FXML
    private void handleApplyCoupon() {
        if (couponField == null || couponField.getText().isBlank())
            return;
        String code = couponField.getText().trim();
        int result = couponService.validateCoupon(code);
        if (result > 0) {
            couponStatusLabel.setText("✅ " + result + "% discount applied!");
            couponStatusLabel.setStyle("-fx-text-fill:#27ae60;-fx-font-weight:bold;");
            calculateCharges();
        } else if (result == -1) {
            couponStatusLabel.setText("❌ Coupon already used.");
            couponStatusLabel.setStyle("-fx-text-fill:#e74c3c;");
            if (discountedTotalLabel != null) discountedTotalLabel.setText("Discounted Total: —");
        } else if (result == -2) {
            couponStatusLabel.setText("❌ Coupon expired.");
            couponStatusLabel.setStyle("-fx-text-fill:#e74c3c;");
            if (discountedTotalLabel != null) discountedTotalLabel.setText("Discounted Total: —");
        } else {
            couponStatusLabel.setText("❌ Invalid coupon code.");
            couponStatusLabel.setStyle("-fx-text-fill:#e74c3c;");
            if (discountedTotalLabel != null) discountedTotalLabel.setText("Discounted Total: —");
        }
    }

    @FXML
    private void handleShowUPIQR() {
        Booking sel = getSelected();
        if (sel == null)
            return;
        double amount = sel.getBalanceDue() > 0 ? sel.getBalanceDue() : sel.getTotalAmount();
        showUPIQRDialog(amount, sel.getBookingReference());
    }

    private void showUPIQRDialog(double amount, String bookingRef) {
        try {
            byte[] qrBytes = paymentGatewayService.generateUPIQRCode(amount, bookingRef, 260);
            if (qrBytes.length == 0) {
                AlertUtil.showError("QR Error", "Could not generate QR code.");
                return;
            }

            Image qrImage = new Image(new ByteArrayInputStream(qrBytes));
            ImageView imageView = new ImageView(qrImage);
            imageView.setFitWidth(260);
            imageView.setFitHeight(260);

            Label dummyBadge = new Label("⚠️  DEMO MODE — No real payment is processed");
            dummyBadge.setStyle("-fx-background-color:#fff3cd;-fx-text-fill:#856404;" +
                    "-fx-font-weight:bold;-fx-font-size:11px;-fx-padding:5 12;" +
                    "-fx-background-radius:6;-fx-border-color:#ffc107;-fx-border-radius:6;");

            Label simBtn = new Label("✅  Click OK below to simulate payment success");
            simBtn.setStyle("-fx-text-fill:#27ae60;-fx-font-size:12px;-fx-font-weight:bold;");

            VBox content = new VBox(10);
            content.setAlignment(javafx.geometry.Pos.CENTER);
            content.setStyle("-fx-padding:20;-fx-background-color:white;");
            content.getChildren().addAll(
                    new Label("📱  UPI Payment (Demo)") {
                        {
                            setStyle("-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:#1a3c5c;");
                        }
                    },
                    dummyBadge, imageView,
                    new Label("₹ " + String.format("%.2f", amount)) {
                        {
                            setStyle("-fx-font-size:22px;-fx-font-weight:bold;-fx-text-fill:#e74c3c;");
                        }
                    },
                    new Label("UPI ID: " + paymentGatewayService.getUPIVPA()) {
                        {
                            setStyle("-fx-font-size:12px;-fx-text-fill:#566573;");
                        }
                    },
                    new Label("Booking: " + bookingRef) {
                        {
                            setStyle("-fx-font-size:12px;-fx-text-fill:#566573;");
                        }
                    },
                    simBtn);

            ButtonType okBtn = new ButtonType("✅ Payment Done (Demo)");
            ButtonType cancelBtn = new ButtonType("Close");
            Alert alert = new Alert(Alert.AlertType.NONE, "", okBtn, cancelBtn);
            alert.setTitle("UPI Payment – " + bookingRef);
            alert.getDialogPane().setContent(content);
            alert.showAndWait().ifPresent(btn -> {
                if (btn == okBtn) {
                    String txnId = paymentGatewayService.generateDummyTransactionId(bookingRef);
                    AlertUtil.showInfo("Payment Simulated ✅",
                            "Payment of ₹" + String.format("%.2f", amount) + " recorded.\n" +
                                    "Transaction ID: " + txnId + "\n\n" +
                                    "(This is a demo — no real payment was made)");
                }
            });
        } catch (Exception e) {
            AlertUtil.showError("QR Error", "Could not show QR: " + e.getMessage());
        }
    }

    @FXML
    private void handleSaveBooking() {
        try {
            Booking booking = buildBookingFromForm();
            double advance = parseDouble(advanceField.getText(), 0);

            if (couponField != null && !couponField.getText().isBlank()) {
                int discount = couponService.validateCoupon(couponField.getText().trim());
                if (discount > 0)
                    couponService.applyCoupon(couponField.getText().trim(), booking.getTotalAmount());
            }

            Payment.Mode mode = Payment.Mode.valueOf(paymentModeCombo.getValue());
            int id = bookingService.createBooking(booking, advance, mode);
            if (id < 0)
                throw new Exception("Booking could not be saved.");

            Optional<Customer> customerOpt = customerDAO.findById(booking.getCustomerId());
            customerOpt.ifPresent(customer -> {
                booking.setBookingId(id);
                // Send email in background; report success/failure back to UI
                new Thread(() -> {
                    try {
                        emailService.sendBookingConfirmation(customer, booking);
                        javafx.application.Platform.runLater(() ->
                            AlertUtil.showInfo("Email Sent ✉️", 
                                "Confirmation email sent to: " + customer.getEmail()));
                    } catch (Exception emailEx) {
                        javafx.application.Platform.runLater(() ->
                            AlertUtil.showWarning("Email Failed ⚠️", 
                                "Could not send confirmation email: " + emailEx.getMessage() + 
                                "\nBooking was saved successfully."));
                    }
                }).start();
            });

            AlertUtil.showInfo("Booking Confirmed! ✅",
                    "Reference: " + booking.getBookingReference() +
                            "\n\nConfirmation email is being sent to the guest...");
            bookingForm.setVisible(false);
            bookingForm.setManaged(false);
            applyFilter();
        } catch (Exception e) {
            AlertUtil.showError("Error", e.getMessage());
        }
    }

    @FXML
    private void handleCancelForm() {
        bookingForm.setVisible(false);
        bookingForm.setManaged(false);
    }

    @FXML
    private void handleCheckIn() {
        Booking sel = getSelected();
        if (sel == null)
            return;
        if (AlertUtil.showConfirm("Check-In", "Confirm check-in for " + sel.getBookingReference() + "?")) {
            try {
                bookingService.checkIn(sel.getBookingId());
                AlertUtil.showInfo("Checked In ✅", "Guest checked in successfully.\nWelcome to The Marcelli!");
                applyFilter();
            } catch (Exception e) {
                AlertUtil.showError("Error", e.getMessage());
            }
        }
    }

    @FXML
    private void handleCheckOut() {
        Booking sel = getSelected();
        if (sel == null)
            return;
        showCheckoutDialog(sel);
    }

    private void showCheckoutDialog(Booking booking) {
        VBox content = new VBox(12);
        content.setStyle("-fx-padding:20;-fx-background-color:white;");

        Label title = new Label("🚪 Check-Out – " + booking.getBookingReference());
        title.setStyle("-fx-font-size:16px;-fx-font-weight:bold;-fx-text-fill:#1a3c5c;");

        Label balanceInfo = new Label("Balance Due: ₹ " + String.format("%.2f", booking.getBalanceDue()));
        balanceInfo.setStyle("-fx-font-size:14px;-fx-text-fill:#e74c3c;-fx-font-weight:bold;");

        Label demoNotice = new Label("⚠️  All payments below are DEMO only — no real transactions");
        demoNotice.setStyle("-fx-background-color:#fff3cd;-fx-text-fill:#856404;" +
                "-fx-font-size:11px;-fx-padding:5 10;-fx-background-radius:6;-fx-font-weight:bold;");

        Label couponTitle = new Label("🎫 Scratch Coupon (optional):");
        couponTitle.setStyle("-fx-font-weight:bold;-fx-text-fill:#566573;");
        TextField couponInput = new TextField();
        couponInput.setPromptText("Enter coupon code…");
        Button applyCouponBtn = new Button("Apply Coupon");
        applyCouponBtn.setStyle("-fx-background-color:#8e44ad;-fx-text-fill:white;" +
                "-fx-font-weight:bold;-fx-padding:6 14;-fx-background-radius:6;");
        Label couponResult = new Label();
        final double[] finalAmount = { booking.getBalanceDue() };
        final String[] appliedCoupon = { null };  // Track validated coupon; only marked used after checkout

        applyCouponBtn.setOnAction(e -> {
            String code = couponInput.getText().trim();
            int disc = couponService.validateCoupon(code);
            if (disc > 0) {
                // Calculate discount preview WITHOUT marking as used yet
                finalAmount[0] = booking.getBalanceDue() * (100.0 - disc) / 100.0;
                appliedCoupon[0] = code;  // Store coupon code; mark used only on confirmBtn
                couponResult.setText("✅ " + disc + "% off! New amount: ₹" +
                        String.format("%.2f", finalAmount[0]));
                couponResult.setStyle("-fx-text-fill:#27ae60;-fx-font-weight:bold;");
                balanceInfo.setText("Final Amount: ₹ " + String.format("%.2f", finalAmount[0]));
            } else {
                couponResult.setText(disc == -1 ? "❌ Already used"
                        : disc == -2 ? "❌ Expired" : "❌ Invalid");
                couponResult.setStyle("-fx-text-fill:#e74c3c;");
                appliedCoupon[0] = null;
            }
        });

        Label modeLabel = new Label("Payment Mode:");
        modeLabel.setStyle("-fx-font-weight:bold;-fx-text-fill:#566573;");
        ComboBox<String> modeCombo = new ComboBox<>();
        modeCombo.setItems(FXCollections.observableArrayList(
                "CASH (Demo)", "CREDIT_CARD (Demo)", "DEBIT_CARD (Demo)",
                "UPI (Demo)", "NET_BANKING (Demo)", "RAZORPAY (Demo)"));
        modeCombo.setValue("CASH (Demo)");

        Button qrBtn = new Button("📱 Show UPI QR (Demo)");
        qrBtn.setStyle("-fx-background-color:#27ae60;-fx-text-fill:white;" +
                "-fx-font-weight:bold;-fx-padding:6 14;-fx-background-radius:6;");
        qrBtn.setOnAction(e -> showUPIQRDialog(finalAmount[0], booking.getBookingReference()));

        Button rzpBtn = new Button("💳 Razorpay (Demo)");
        rzpBtn.setStyle("-fx-background-color:#3b5bdb;-fx-text-fill:white;" +
                "-fx-font-weight:bold;-fx-padding:6 14;-fx-background-radius:6;");
        rzpBtn.setOnAction(e -> AlertUtil.showInfo("Razorpay Demo",
                paymentGatewayService.getDummyRazorpayDetails(
                        finalAmount[0], booking.getBookingReference(),
                        booking.getCustomerName())));

        Button confirmBtn = new Button("✅ Confirm Check-Out");
        confirmBtn.setStyle("-fx-background-color:#2980b9;-fx-text-fill:white;" +
                "-fx-font-weight:bold;-fx-padding:10 20;-fx-background-radius:8;");

        content.getChildren().addAll(
                title, balanceInfo, demoNotice,
                new Separator(), couponTitle, couponInput, applyCouponBtn, couponResult,
                new Separator(), modeLabel, modeCombo,
                new HBox(8) {
                    {
                        getChildren().addAll(qrBtn, rzpBtn);
                        setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                    }
                },
                new Separator(), confirmBtn);

        Stage dialog = new Stage();
        dialog.setTitle("Check-Out");
        javafx.scene.Scene dialogScene = new javafx.scene.Scene(
                new ScrollPane(content) {
                    {
                        setFitToWidth(true);
                        setStyle("-fx-background-color:white;");
                    }
                }, 460, 600);
        dialog.setScene(dialogScene);
        dialog.setMinHeight(600);
        dialog.setMinWidth(460);
        dialog.show();

        confirmBtn.setOnAction(e -> {
            try {
                String modeStr = modeCombo.getValue().replace(" (Demo)", "");
                Payment.Mode mode = Payment.Mode.valueOf(modeStr);
                bookingService.checkOut(booking.getBookingId(), finalAmount[0], mode);

                // ONLY mark coupon as used after successful checkout
                if (appliedCoupon[0] != null) {
                    couponService.applyCoupon(appliedCoupon[0], booking.getBalanceDue());
                }

                HousekeepingService.getInstance().startCleaning(
                        booking.getRoomId(), booking.getRoomNumber(), null);

                String newCoupon = couponService.generateCheckoutCoupon(
                        booking.getCustomerId(), booking.getBookingReference());

                // Send invoice (with coupon) + feedback email automatically with error feedback
                final String finalCoupon = newCoupon;
                customerDAO.findById(booking.getCustomerId()).ifPresent(customer -> {
                    new Thread(() -> {
                        try {
                            // Generate invoice PDF with coupon voucher included
                            String invoiceDir = "C:/Users/sarad/OneDrive/Desktop/HotelManagementSystem/invoices/";
                            new java.io.File(invoiceDir).mkdirs(); // creates the folder if it doesn't exist
                            String tempPath = invoiceDir + "Invoice_" + booking.getBookingReference() + ".pdf";
                            invoiceService.generateInvoiceWithCoupon(
                                    booking.getBookingId(), tempPath, finalCoupon);
                            // Email invoice + coupon to guest
                            emailService.sendInvoiceEmail(customer, booking, tempPath);
                            // Email feedback form
                            emailService.sendFeedbackRequest(customer, booking);
                            javafx.application.Platform.runLater(() ->
                                AlertUtil.showInfo("Emails Sent ✉️", 
                                    "Invoice and feedback form sent to: " + customer.getEmail()));
                        } catch (Exception emailEx) {
                            javafx.application.Platform.runLater(() ->
                                AlertUtil.showWarning("Email Failed ⚠️", 
                                    "Could not send checkout emails: " + emailEx.getMessage() + 
                                    "\nCheckout was successful; invoices are saved locally."));
                        }
                    }).start();
                });

                dialog.close();

                // Show success with coupon
                String msg = "✅ Guest checked out successfully!\n\n" +
                        "🧹 HOUSEKEEPING STARTED\n" +
                        "Room " + booking.getRoomNumber() + " is now being cleaned.\n" +
                        "Status: HOUSEKEEPING\n" +
                        "⏱ Room available again in 20 minutes.\n\n" +
                        "📧 Invoice + coupon voucher sent to guest email.\n" +
                        "📋 Feedback form link sent to guest.\n\n";
                if (newCoupon != null) {
                    msg += "🎫 SCRATCH COUPON for next visit:\n" +
                            "Code: " + newCoupon + "\n" +
                            "(Valid for 90 days — included in invoice PDF!)";
                }
                AlertUtil.showInfo("Check-Out Complete!", msg);
                applyFilter();
            } catch (Exception ex) {
                AlertUtil.showError("Error", ex.getMessage());
            }
        });
    }

    @FXML
    private void handleCancelBooking() {
        Booking sel = getSelected();
        if (sel == null)
            return;
        if (AlertUtil.showConfirm("Cancel Booking",
                "Cancel booking " + sel.getBookingReference() + "?")) {
            try {
                bookingService.cancelBooking(sel.getBookingId());
                AlertUtil.showInfo("Cancelled", "Booking cancelled.");
                applyFilter();
            } catch (Exception e) {
                AlertUtil.showError("Error", e.getMessage());
            }
        }
    }

    @FXML
    private void handleGenerateInvoice() {
        Booking sel = getSelected();
        if (sel == null)
            return;
        String invoiceDir = "C:/Users/sarad/OneDrive/Desktop/HotelManagementSystem/invoices/";
        new java.io.File(invoiceDir).mkdirs();
        String invoicePath = invoiceDir + "Invoice_" + sel.getBookingReference() + ".pdf";

        boolean ok = invoiceService.generateInvoice(sel.getBookingId(), invoicePath);
        if (ok) {
            openInvoiceInApp(invoicePath, sel);
        } else {
            AlertUtil.showError("Error", "Failed to generate invoice.");
        }
    }

    private void openInvoiceInApp(String invoicePath, Booking sel) {
        File invoiceFile = new File(invoicePath);
        if (!invoiceFile.exists()) {
            AlertUtil.showError("Error", "Invoice file not found.");
            return;
        }

        boolean openedExternally = false;
        String openError = null;
        try {
            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop desktop = java.awt.Desktop.getDesktop();
                if (desktop.isSupported(java.awt.Desktop.Action.OPEN)) {
                    desktop.open(invoiceFile);
                    openedExternally = true;
                }
            }
        } catch (Exception ex) {
            openError = ex.getMessage();
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Invoice Generated");
        if (openedExternally) {
            alert.setHeaderText("Invoice opened in your default PDF viewer.");
        } else {
            alert.setHeaderText("Invoice generated successfully.");
        }

        String content = "Choose next action:\n"
                + "- Save & Email: keep invoice and email guest\n"
                + "- Discard: delete this invoice file";
        if (openError != null && !openError.isBlank()) {
            content += "\n\nNote: Could not auto-open PDF: " + openError;
        } else if (!openedExternally) {
            content += "\n\nNote: Auto-open is not available on this system.";
        }
        alert.setContentText(content);

        ButtonType saveEmailBtn = new ButtonType("Save & Email", ButtonBar.ButtonData.OK_DONE);
        ButtonType discardBtn = new ButtonType("Discard", ButtonBar.ButtonData.NO);
        ButtonType closeBtn = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(saveEmailBtn, discardBtn, closeBtn);

        Optional<ButtonType> choice = alert.showAndWait();
        if (choice.isPresent() && choice.get() == saveEmailBtn) {
            customerDAO.findById(sel.getCustomerId())
                    .ifPresent(customer -> new Thread(() -> emailService.sendInvoiceEmail(
                            customer, sel, invoicePath)).start());
            AlertUtil.showInfo("Invoice Saved", "Invoice saved and emailed to guest.");
        } else if (choice.isPresent() && choice.get() == discardBtn) {
            boolean deleted = invoiceFile.delete();
            if (deleted) {
                AlertUtil.showInfo("Discarded", "Invoice has been discarded.");
            } else {
                AlertUtil.showError("Discard Failed", "Could not delete the invoice file.");
            }
        }
    }

    

    private void calculateCharges() {
        Room room = roomCombo.getValue();
        LocalDate ci = checkInPicker.getValue();
        LocalDate co = checkOutPicker.getValue();
        if (room == null || ci == null || co == null || !co.isAfter(ci))
            return;

        long nights = java.time.temporal.ChronoUnit.DAYS.between(ci, co);
        double roomChrgs = nights * room.getPricePerNight();
        double gst = com.hotel.util.GSTCalculator.calculateGST(roomChrgs, room.getPricePerNight());
        double total = roomChrgs + gst;
        double advance = parseDouble(advanceField != null ? advanceField.getText() : "0", 0);
        double balance = total - advance;

        if (couponField != null && !couponField.getText().isBlank()) {
            int disc = couponService.validateCoupon(couponField.getText().trim());
            if (disc > 0) {
                double discounted = total * (1 - disc / 100.0);
                balance = discounted - advance;
                if (discountedTotalLabel != null)
                    discountedTotalLabel.setText(String.format("After coupon: ₹ %.2f", discounted));
            }
        }

        roomChargesLabel.setText(String.format("₹ %.2f", roomChrgs));
        gstLabel.setText(String.format("₹ %.2f", gst));
        totalLabel.setText(String.format("₹ %.2f", total));
        balanceLabel.setText(String.format("₹ %.2f", balance));
    }

    private Booking buildBookingFromForm() {
        // Handle editable ComboBox — value could be String or Customer
        Customer selectedCustomer = null;
        Object val = customerCombo.getValue();
        if (val instanceof Customer) {
            selectedCustomer = (Customer) val;
        } else {
            // Try converter first
            String typed = customerCombo.getEditor().getText();
            if (typed != null && !typed.isBlank()) {
                String kw = typed.toLowerCase();
                selectedCustomer = customerDAO.findAll().stream()
                        .filter(c -> c.getFullName().toLowerCase().contains(kw)
                                || c.getPhone().contains(kw)
                                || (c.getFullName() + " — " + c.getPhone()).equals(typed))
                        .findFirst().orElse(null);
            }
        }
        if (selectedCustomer == null)
            throw new IllegalArgumentException("Please select a valid customer from the list.");
        if (roomCombo.getValue() == null)
            throw new IllegalArgumentException("Please select a room.");

        Booking b = new Booking();
        b.setCustomerId(selectedCustomer.getCustomerId());
        b.setRoomId(roomCombo.getValue().getRoomId());
        b.setCheckInDate(checkInPicker.getValue());
        b.setCheckOutDate(checkOutPicker.getValue());
        b.setNumberOfGuests(guestSpinner.getValue());
        b.setSpecialRequests(specialRequestsArea.getText());
        return b;
    }

    private void clearBookingForm() {
        customerCombo.getSelectionModel().clearSelection();
        if (customerCombo.getEditor() != null)
            customerCombo.getEditor().clear();
        roomCombo.getSelectionModel().clearSelection();
        checkInPicker.setValue(null);
        checkOutPicker.setValue(null);
        advanceField.clear();
        specialRequestsArea.clear();
        if (couponField != null)
            couponField.clear();
        if (couponStatusLabel != null)
            couponStatusLabel.setText("");
        if (discountedTotalLabel != null)
            discountedTotalLabel.setText("");
        roomChargesLabel.setText("—");
        gstLabel.setText("—");
        totalLabel.setText("—");
        balanceLabel.setText("—");
    }

    private Booking getSelected() {
        Booking sel = bookingTable.getSelectionModel().getSelectedItem();
        if (sel == null)
            AlertUtil.showWarning("No Selection", "Please select a booking.");
        return sel;
    }

    private double parseDouble(String s, double def) {
        try {
            return Double.parseDouble(s.trim());
        } catch (Exception e) {
            return def;
        }
    }
}