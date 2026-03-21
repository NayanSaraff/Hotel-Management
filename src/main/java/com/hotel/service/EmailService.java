package com.hotel.service;

import com.hotel.model.Booking;
import com.hotel.model.Customer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.*;
import javax.mail.internet.*;
import java.io.File;
import java.util.Properties;

/**
 * Service for sending emails — booking confirmations, invoices, feedback requests.
 * Configure SMTP in email.properties or set credentials directly below.
 */
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    // ── SMTP Configuration — change these to your Gmail/SMTP credentials ──
    private static final String SMTP_HOST     = "smtp.gmail.com";
    private static final String SMTP_PORT     = "587";
    private static final String SMTP_USERNAME = "nayansaraff3739@gmail.com"; // <-- change this
    private static final String SMTP_PASSWORD = "vclxkcyqvccohjkn";           // <-- change this (use App Password)
    private static final String FROM_NAME     = "Grand Hotel Management";

    // Google Form feedback link
    private static final String FEEDBACK_FORM_URL =
        "https://docs.google.com/forms/d/e/1FAIpQLSdvYL0YXLpxnCS7IbfTmOBdVsMRTXZDTn8Nb40YCb-0gmf8nA/viewform?usp=publish-editor";

    private Session getSession() {
        Properties props = new Properties();
        props.put("mail.smtp.auth",            "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host",            SMTP_HOST);
        props.put("mail.smtp.port",            SMTP_PORT);
        props.put("mail.smtp.ssl.trust",       SMTP_HOST);

        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SMTP_USERNAME, SMTP_PASSWORD);
            }
        });
    }

    // ── Booking Confirmation ──────────────────────────────────────────────

    public boolean sendBookingConfirmation(Customer customer, Booking booking) {
        if (customer.getEmail() == null || customer.getEmail().isBlank()) {
            logger.warn("No email for customer: {}", customer.getFullName());
            return false;
        }
        try {
            Session session = getSession();
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(SMTP_USERNAME, FROM_NAME));
            message.setRecipients(Message.RecipientType.TO,
                    InternetAddress.parse(customer.getEmail()));
            message.setSubject("Booking Confirmed – " + booking.getBookingReference());

            String body = buildBookingConfirmationHTML(customer, booking);
            message.setContent(body, "text/html; charset=utf-8");

            Transport.send(message);
            logger.info("Booking confirmation sent to {}", customer.getEmail());
            return true;
        } catch (Exception e) {
            logger.error("Failed to send booking confirmation: {}", e.getMessage());
            return false;
        }
    }

    // ── Invoice Email ─────────────────────────────────────────────────────

    public boolean sendInvoiceEmail(Customer customer, Booking booking, String pdfPath) {
        if (customer.getEmail() == null || customer.getEmail().isBlank()) {
            logger.warn("No email for customer: {}", customer.getFullName());
            return false;
        }
        try {
            Session session = getSession();
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(SMTP_USERNAME, FROM_NAME));
            message.setRecipients(Message.RecipientType.TO,
                    InternetAddress.parse(customer.getEmail()));
            message.setSubject("Invoice – " + booking.getBookingReference() + " | Grand Hotel");

            // Multipart: body + PDF attachment
            Multipart multipart = new MimeMultipart();

            // Text part
            MimeBodyPart textPart = new MimeBodyPart();
            textPart.setContent(buildInvoiceEmailHTML(customer, booking), "text/html; charset=utf-8");
            multipart.addBodyPart(textPart);

            // PDF attachment
            if (pdfPath != null && new File(pdfPath).exists()) {
                MimeBodyPart attachPart = new MimeBodyPart();
                attachPart.attachFile(new File(pdfPath));
                attachPart.setFileName("Invoice_" + booking.getBookingReference() + ".pdf");
                multipart.addBodyPart(attachPart);
            }

            message.setContent(multipart);
            Transport.send(message);
            logger.info("Invoice email sent to {}", customer.getEmail());
            return true;
        } catch (Exception e) {
            logger.error("Failed to send invoice email: {}", e.getMessage());
            return false;
        }
    }

    // ── Feedback Request Email ────────────────────────────────────────────

    public boolean sendFeedbackRequest(Customer customer, Booking booking) {
        if (customer.getEmail() == null || customer.getEmail().isBlank()) return false;
        try {
            Session session = getSession();
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(SMTP_USERNAME, FROM_NAME));
            message.setRecipients(Message.RecipientType.TO,
                    InternetAddress.parse(customer.getEmail()));
            message.setSubject("We'd love your feedback! – Grand Hotel");

            String body = buildFeedbackEmailHTML(customer, booking);
            message.setContent(body, "text/html; charset=utf-8");
            Transport.send(message);
            logger.info("Feedback request sent to {}", customer.getEmail());
            return true;
        } catch (Exception e) {
            logger.error("Failed to send feedback email: {}", e.getMessage());
            return false;
        }
    }

    // ── HTML Templates ────────────────────────────────────────────────────

    private String buildBookingConfirmationHTML(Customer customer, Booking booking) {
        return "<!DOCTYPE html><html><body style='font-family:Arial,sans-serif;color:#2c3e50;'>" +
            "<div style='max-width:600px;margin:auto;background:#fff;border-radius:12px;" +
            "box-shadow:0 2px 12px rgba(0,0,0,0.1);overflow:hidden;'>" +
            "<div style='background:linear-gradient(135deg,#1a3c5c,#2980b9);padding:30px;text-align:center;'>" +
            "<h1 style='color:white;margin:0;'>🏨 Grand Hotel</h1>" +
            "<p style='color:#aed6f1;margin:8px 0 0;'>Booking Confirmation</p></div>" +
            "<div style='padding:30px;'>" +
            "<p>Dear <strong>" + customer.getFullName() + "</strong>,</p>" +
            "<p>Your booking has been <strong style='color:#27ae60;'>confirmed!</strong> Here are your details:</p>" +
            "<table style='width:100%;border-collapse:collapse;margin:20px 0;'>" +
            "<tr style='background:#f4f7fb;'><td style='padding:10px;font-weight:bold;'>Booking Reference</td>" +
            "<td style='padding:10px;color:#2980b9;font-weight:bold;'>" + booking.getBookingReference() + "</td></tr>" +
            "<tr><td style='padding:10px;font-weight:bold;'>Room</td>" +
            "<td style='padding:10px;'>" + booking.getRoomNumber() + " (" + booking.getRoomCategory() + ")</td></tr>" +
            "<tr style='background:#f4f7fb;'><td style='padding:10px;font-weight:bold;'>Check-In</td>" +
            "<td style='padding:10px;'>" + booking.getCheckInDate() + "</td></tr>" +
            "<tr><td style='padding:10px;font-weight:bold;'>Check-Out</td>" +
            "<td style='padding:10px;'>" + booking.getCheckOutDate() + "</td></tr>" +
            "<tr style='background:#f4f7fb;'><td style='padding:10px;font-weight:bold;'>Total Amount</td>" +
            "<td style='padding:10px;font-size:18px;color:#e74c3c;font-weight:bold;'>₹ " +
            String.format("%.2f", booking.getTotalAmount()) + "</td></tr>" +
            "</table>" +
            "<p style='background:#eaf2ff;padding:15px;border-radius:8px;border-left:4px solid #2980b9;'>" +
            "📍 <strong>Grand Hotel</strong> | 123 Hotel Street, Udupi, Karnataka - 576101<br>" +
            "📞 +91-98765-43210 | ✉️ info@grandhotel.com</p>" +
            "<p>We look forward to welcoming you!</p>" +
            "<p style='color:#95a5a6;font-size:12px;'>This is an auto-generated email. Please do not reply.</p>" +
            "</div></div></body></html>";
    }

    private String buildInvoiceEmailHTML(Customer customer, Booking booking) {
        return "<!DOCTYPE html><html><body style='font-family:Arial,sans-serif;color:#2c3e50;'>" +
            "<div style='max-width:600px;margin:auto;'>" +
            "<div style='background:linear-gradient(135deg,#1a3c5c,#2980b9);padding:30px;border-radius:12px 12px 0 0;text-align:center;'>" +
            "<h1 style='color:white;margin:0;'>🏨 Grand Hotel</h1>" +
            "<p style='color:#aed6f1;'>Your Invoice is Attached</p></div>" +
            "<div style='background:white;padding:30px;border-radius:0 0 12px 12px;border:1px solid #dde3eb;'>" +
            "<p>Dear <strong>" + customer.getFullName() + "</strong>,</p>" +
            "<p>Thank you for staying with us! Please find your invoice attached for booking " +
            "<strong style='color:#2980b9;'>" + booking.getBookingReference() + "</strong>.</p>" +
            "<p style='background:#f9f9f9;padding:15px;border-radius:8px;'>" +
            "Total Amount: <strong style='font-size:20px;color:#e74c3c;'>₹ " +
            String.format("%.2f", booking.getTotalAmount()) + "</strong></p>" +
            "<p>We hope you had a wonderful stay. We look forward to seeing you again!</p>" +
            "<p style='color:#95a5a6;font-size:12px;'>Grand Hotel | info@grandhotel.com | +91-98765-43210</p>" +
            "</div></div></body></html>";
    }

    private String buildFeedbackEmailHTML(Customer customer, Booking booking) {
        return "<!DOCTYPE html><html><body style='font-family:Arial,sans-serif;color:#2c3e50;'>" +
            "<div style='max-width:600px;margin:auto;'>" +
            "<div style='background:linear-gradient(135deg,#1a3c5c,#2980b9);padding:30px;border-radius:12px 12px 0 0;text-align:center;'>" +
            "<h1 style='color:white;margin:0;'>🏨 Grand Hotel</h1>" +
            "<p style='color:#aed6f1;'>We Value Your Feedback</p></div>" +
            "<div style='background:white;padding:30px;border-radius:0 0 12px 12px;border:1px solid #dde3eb;'>" +
            "<p>Dear <strong>" + customer.getFullName() + "</strong>,</p>" +
            "<p>Thank you for your recent stay at Grand Hotel (Booking: " +
            "<strong>" + booking.getBookingReference() + "</strong>).</p>" +
            "<p>We'd love to hear about your experience! Please take 2 minutes to share your feedback:</p>" +
            "<div style='text-align:center;margin:30px 0;'>" +
            "<a href='" + FEEDBACK_FORM_URL + "' style='background:#2980b9;color:white;padding:15px 35px;" +
            "border-radius:8px;text-decoration:none;font-weight:bold;font-size:16px;'>⭐ Share Your Feedback</a></div>" +
            "<p>Your feedback helps us serve you better!</p>" +
            "<p style='color:#95a5a6;font-size:12px;'>Grand Hotel | info@grandhotel.com</p>" +
            "</div></div></body></html>";
    }
}
