package com.hotel.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * DUMMY Payment Gateway Service.
 * Simulates UPI QR and Razorpay — no real transactions.
 * For demo / presentation purposes only.
 */
public class PaymentGatewayService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentGatewayService.class);

    // ── Dummy UPI Details ─────────────────────────────────────────────────
    private static final String DUMMY_UPI_VPA  = "grandhotel@dummyupi";
    private static final String DUMMY_UPI_NAME = "Grand Hotel (Demo)";

    // ── Dummy Razorpay Details ────────────────────────────────────────────
    private static final String DUMMY_RZP_KEY  = "rzp_test_DEMO1234567890";
    private static final String DUMMY_RZP_NAME = "Grand Hotel Demo";

    // ── UPI QR Code (Dummy) ───────────────────────────────────────────────

    public byte[] generateUPIQRCode(double amount, String bookingRef, int widthHeight) {
        String dummyUPI = String.format(
            "upi://pay?pa=%s&pn=%s&am=%.2f&cu=INR&tn=DEMO%%20Payment%%20%s",
            DUMMY_UPI_VPA,
            DUMMY_UPI_NAME.replace(" ", "%20"),
            amount,
            bookingRef
        );

        try {
            QRCodeWriter writer = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 2);

            BitMatrix bitMatrix = writer.encode(
                dummyUPI, BarcodeFormat.QR_CODE, widthHeight, widthHeight, hints);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", baos);
            logger.info("[DUMMY] UPI QR generated for ₹{} | Booking: {}", amount, bookingRef);
            return baos.toByteArray();

        } catch (WriterException | java.io.IOException e) {
            logger.error("Failed to generate dummy QR: {}", e.getMessage());
            return new byte[0];
        }
    }

    public String getDummyRazorpayDetails(double amount, String bookingRef,
                                           String customerName) {
        return String.format(
            "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
            "  💳  RAZORPAY PAYMENT (DEMO)\n" +
            "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
            "  Merchant : %s\n" +
            "  Amount   : ₹ %.2f\n" +
            "  Booking  : %s\n" +
            "  Customer : %s\n" +
            "  Key ID   : %s\n" +
            "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n" +
            "  ⚠️  THIS IS A DEMO PAYMENT\n" +
            "  No real money is charged.\n" +
            "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━",
            DUMMY_RZP_NAME, amount, bookingRef, customerName, DUMMY_RZP_KEY
        );
    }

    public boolean simulatePayment(double amount, String bookingRef) {
        logger.info("[DUMMY] Simulating payment of ₹{} for booking {}...", amount, bookingRef);
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ignored) {}
        logger.info("[DUMMY] Payment SUCCESS for booking {}", bookingRef);
        return true;
    }

    public String generateDummyTransactionId(String bookingRef) {
        return "TXN-DEMO-" + bookingRef + "-" +
               String.format("%06d", (int)(Math.random() * 999999));
    }

    public String getUPIVPA()      { return DUMMY_UPI_VPA; }
    public String getRazorpayKey() { return DUMMY_RZP_KEY; }

    public String[] getPaymentModes() {
        return new String[]{
            "💵  Cash",
            "💳  Credit Card (Demo)",
            "💳  Debit Card (Demo)",
            "📱  UPI QR (Demo)",
            "🏦  Net Banking (Demo)",
            "📲  Razorpay (Demo)"
        };
    }

    public String getPaymentConfirmationMessage(String mode, double amount,
                                                  String bookingRef) {
        String txnId = generateDummyTransactionId(bookingRef);
        return String.format(
            "✅  PAYMENT SUCCESSFUL (DEMO)\n\n" +
            "  Amount      : ₹ %.2f\n" +
            "  Mode        : %s\n" +
            "  Booking     : %s\n" +
            "  Transaction : %s\n\n" +
            "⚠️  This is a simulated payment.\n" +
            "    No real transaction occurred.",
            amount, mode, bookingRef, txnId
        );
    }
}