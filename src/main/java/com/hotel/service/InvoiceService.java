package com.hotel.service;

import com.hotel.dao.BookingDAO;
import com.hotel.dao.CustomerDAO;
import com.hotel.dao.PaymentDAO;
import com.hotel.dao.RoomDAO;
import com.hotel.model.Booking;
import com.hotel.model.Customer;
import com.hotel.model.Payment;
import com.hotel.model.Room;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Generates a compact, single-page PDF invoice with coupon voucher.
 */
public class InvoiceService {

    private static final Logger logger = LoggerFactory.getLogger(InvoiceService.class);
    private static final DateTimeFormatter DATE_FMT  = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter DT_FMT    = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    // ── Colour Palette ────────────────────────────────────────────────────
    private static final BaseColor C_NAVY    = new BaseColor(15,  37,  64);   // #0f2540
    private static final BaseColor C_BLUE    = new BaseColor(41, 128, 185);   // #2980b9
    private static final BaseColor C_GOLD    = new BaseColor(212, 168, 67);   // #d4a843
    private static final BaseColor C_GREEN   = new BaseColor(39, 174, 96);    // #27ae60
    private static final BaseColor C_RED     = new BaseColor(231, 76,  60);   // #e74c3c
    private static final BaseColor C_LIGHT   = new BaseColor(244, 247, 251);  // #f4f7fb
    private static final BaseColor C_BORDER  = new BaseColor(221, 227, 235);  // #dde3eb
    private static final BaseColor C_TEXT    = new BaseColor(44,  62,  80);   // #2c3e50
    private static final BaseColor C_MUTED   = new BaseColor(127, 140, 141);  // #7f8c8d
    private static final BaseColor C_PURPLE  = new BaseColor(142, 68, 173);   // #8e44ad

    private final BookingDAO  bookingDAO  = new BookingDAO();
    private final CustomerDAO customerDAO = new CustomerDAO();
    private final RoomDAO     roomDAO     = new RoomDAO();
    private final PaymentDAO  paymentDAO  = new PaymentDAO();
    private final CouponService couponService = new CouponService();

    // ── Fonts ─────────────────────────────────────────────────────────────
    private final Font fHotelName  = FontFactory.getFont(FontFactory.HELVETICA_BOLD,  20, C_GOLD);
    private final Font fTagline    = FontFactory.getFont(FontFactory.HELVETICA,         8, C_MUTED);
    private final Font fInvoiceHdr = FontFactory.getFont(FontFactory.HELVETICA_BOLD,  14, BaseColor.WHITE);
    private final Font fRefNo      = FontFactory.getFont(FontFactory.HELVETICA,         9, C_MUTED);
    private final Font fSectionHdr = FontFactory.getFont(FontFactory.HELVETICA_BOLD,   8, BaseColor.WHITE);
    private final Font fLabel      = FontFactory.getFont(FontFactory.HELVETICA_BOLD,   8, C_MUTED);
    private final Font fValue      = FontFactory.getFont(FontFactory.HELVETICA,         9, C_TEXT);
    private final Font fValueBold  = FontFactory.getFont(FontFactory.HELVETICA_BOLD,   9, C_TEXT);
    private final Font fTotal      = FontFactory.getFont(FontFactory.HELVETICA_BOLD,  11, C_NAVY);
    private final Font fBalance    = FontFactory.getFont(FontFactory.HELVETICA_BOLD,  12, C_RED);
    private final Font fPaid       = FontFactory.getFont(FontFactory.HELVETICA_BOLD,  12, C_GREEN);
    private final Font fCouponHdr  = FontFactory.getFont(FontFactory.HELVETICA_BOLD,  10, BaseColor.WHITE);
    private final Font fCouponCode = FontFactory.getFont(FontFactory.HELVETICA_BOLD,  16, C_GOLD);
    private final Font fCouponSub  = FontFactory.getFont(FontFactory.HELVETICA,        8, BaseColor.WHITE);
    private final Font fFooter     = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE,7, C_MUTED);
    private final Font fThankYou   = FontFactory.getFont(FontFactory.HELVETICA_BOLD,  10, C_NAVY);

    /**
     * Generate invoice PDF. Pass couponCode=null if no coupon to include.
     */
    public boolean generateInvoice(int bookingId, String outputPath) {
        return generateInvoiceWithCoupon(bookingId, outputPath, null);
    }

    public boolean generateInvoiceWithCoupon(int bookingId, String outputPath, String couponCode) {
        Optional<Booking> bookingOpt = bookingDAO.findById(bookingId);
        if (bookingOpt.isEmpty()) { logger.error("Booking not found: {}", bookingId); return false; }

        Booking  booking  = bookingOpt.get();
        Optional<Customer> custOpt = customerDAO.findById(booking.getCustomerId());
        Optional<Room>     roomOpt = roomDAO.findById(booking.getRoomId());
        List<Payment>      payments = paymentDAO.findByBookingId(bookingId);

        try (FileOutputStream fos = new FileOutputStream(outputPath)) {
            // A4 with tight margins for single-page
            Document doc = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter writer = PdfWriter.getInstance(doc, fos);
            doc.open();

            addHeader(doc, booking, writer);
            addGuestAndBookingInfo(doc, booking, custOpt.orElse(null), roomOpt.orElse(null));
            addChargesTable(doc, booking, payments);
            if (couponCode != null) addCouponVoucher(doc, couponCode, booking);
            addThankYouFooter(doc);

            doc.close();
            logger.info("Invoice generated: {}", outputPath);
            return true;
        } catch (DocumentException | IOException e) {
            logger.error("Invoice error: {}", e.getMessage());
            return false;
        }
    }

    // ── HEADER ────────────────────────────────────────────────────────────

    private void addHeader(Document doc, Booking booking, PdfWriter writer)
            throws DocumentException {
        // Full-width dark header banner
        PdfContentByte cb = writer.getDirectContent();

        // Navy background rectangle (top banner)
        // Draw via table instead for compatibility
        PdfPTable banner = new PdfPTable(2);
        banner.setWidthPercentage(100);
        banner.setWidths(new float[]{60, 40});
        banner.setSpacingAfter(0);

        // Left: Hotel name
        PdfPCell left = new PdfPCell();
        left.setBackgroundColor(C_NAVY);
        left.setBorder(Rectangle.NO_BORDER);
        left.setPadding(14);
        Paragraph hotelP = new Paragraph();
        hotelP.add(new Chunk("🏨  GRAND HOTEL\n", fHotelName));
        hotelP.add(new Chunk("123 Hotel Street, Udupi, Karnataka – 576101\n", fTagline));
        hotelP.add(new Chunk("📞 +91-98765-43210  |  ✉ info@grandhotel.com\n", fTagline));
        hotelP.add(new Chunk("GSTIN: 29ABCDE1234F1Z5", fTagline));
        left.addElement(hotelP);

        // Right: Invoice label + ref
        PdfPCell right = new PdfPCell();
        right.setBackgroundColor(C_BLUE);
        right.setBorder(Rectangle.NO_BORDER);
        right.setPadding(14);
        right.setHorizontalAlignment(Element.ALIGN_RIGHT);
        right.setVerticalAlignment(Element.ALIGN_MIDDLE);
        Paragraph invoiceP = new Paragraph();
        invoiceP.setAlignment(Element.ALIGN_RIGHT);
        invoiceP.add(new Chunk("TAX INVOICE\n", fInvoiceHdr));
        invoiceP.add(new Chunk(booking.getBookingReference() + "\n", fRefNo));
        if (booking.getBookingDate() != null)
            invoiceP.add(new Chunk(booking.getBookingDate().format(DT_FMT), fRefNo));
        right.addElement(invoiceP);

        banner.addCell(left);
        banner.addCell(right);
        doc.add(banner);
        doc.add(new Paragraph(" ")); // small spacer
    }

    // ── GUEST + BOOKING INFO ──────────────────────────────────────────────

    private void addGuestAndBookingInfo(Document doc, Booking booking,
                                         Customer customer, Room room)
            throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{50, 50});
        table.setSpacingBefore(4);
        table.setSpacingAfter(6);

        // ── Left: Guest Details ──
        PdfPCell guestCell = new PdfPCell();
        guestCell.setBorder(Rectangle.BOX);
        guestCell.setBorderColor(C_BORDER);
        guestCell.setPadding(10);
        guestCell.setBackgroundColor(C_LIGHT);

        Paragraph guestHeader = new Paragraph(new Chunk("  GUEST DETAILS  ", fSectionHdr));
        PdfPTable ghBg = sectionHeader("GUEST DETAILS");
        guestCell.addElement(ghBg);
        guestCell.addElement(new Paragraph(" "));

        String guestName  = customer != null ? customer.getFullName() : booking.getCustomerName();
        String guestPhone = customer != null ? customer.getPhone() : "—";
        String guestEmail = customer != null ? (customer.getEmail() != null ? customer.getEmail() : "—") : "—";
        String guestId    = customer != null && customer.getIdNumber() != null
                ? customer.getIdType().name() + ": " + customer.getIdNumber() : "—";
        String guestAddr  = customer != null && customer.getCity() != null
                ? customer.getCity() + ", " + (customer.getState() != null ? customer.getState() : "") : "—";

        addInfoRow(guestCell, "Name",     guestName);
        addInfoRow(guestCell, "Phone",    guestPhone);
        addInfoRow(guestCell, "Email",    guestEmail);
        addInfoRow(guestCell, "ID Proof", guestId);
        addInfoRow(guestCell, "City",     guestAddr);

        // ── Right: Booking Details ──
        PdfPCell bookCell = new PdfPCell();
        bookCell.setBorder(Rectangle.BOX);
        bookCell.setBorderColor(C_BORDER);
        bookCell.setPadding(10);
        bookCell.setBackgroundColor(C_LIGHT);

        bookCell.addElement(sectionHeader("BOOKING DETAILS"));
        bookCell.addElement(new Paragraph(" "));

        addInfoRow(bookCell, "Room No.",    booking.getRoomNumber() != null ? booking.getRoomNumber() : "—");
        addInfoRow(bookCell, "Category",   booking.getRoomCategory() != null ? booking.getRoomCategory() : "—");
        addInfoRow(bookCell, "Check-In",   booking.getCheckInDate() != null ? booking.getCheckInDate().format(DATE_FMT) : "—");
        addInfoRow(bookCell, "Check-Out",  booking.getCheckOutDate() != null ? booking.getCheckOutDate().format(DATE_FMT) : "—");
        addInfoRow(bookCell, "Nights",     String.valueOf(booking.getNumberOfNights()));
        addInfoRow(bookCell, "Guests",     String.valueOf(booking.getNumberOfGuests()));
        if (room != null)
            addInfoRow(bookCell, "Rate/Night", "₹ " + String.format("%,.2f", room.getPricePerNight()));
        addInfoRow(bookCell, "Status",     booking.getStatus().name());

        table.addCell(guestCell);
        table.addCell(bookCell);
        doc.add(table);
    }

    // ── CHARGES TABLE ─────────────────────────────────────────────────────

    private void addChargesTable(Document doc, Booking booking, List<Payment> payments)
            throws DocumentException {
        // Charges summary — compact 2-column
        PdfPTable charges = new PdfPTable(2);
        charges.setWidthPercentage(55);
        charges.setHorizontalAlignment(Element.ALIGN_RIGHT);
        charges.setWidths(new float[]{65, 35});
        charges.setSpacingBefore(2);
        charges.setSpacingAfter(4);

        // Header spanning 2 cols
        PdfPCell hdr = new PdfPCell(sectionHeader("CHARGES SUMMARY"));
        hdr.setColspan(2); hdr.setBorder(Rectangle.NO_BORDER); hdr.setPadding(0);
        charges.addCell(hdr);

        addChargeRow(charges, "Room Charges",    booking.getRoomCharges(),    false);
        addChargeRow(charges, "Service Charges", booking.getServiceCharges(), false);
        addChargeRow(charges, "GST Amount",      booking.getGstAmount(),      false);
        addDividerRow(charges);
        addChargeRowBold(charges, "TOTAL AMOUNT", booking.getTotalAmount(),   C_NAVY,  fTotal);
        addChargeRow(charges, "Advance Paid",    booking.getAdvancePaid(),    false);
        addDividerRow(charges);

        boolean isPaid = booking.getBalanceDue() <= 0;
        addChargeRowBold(charges, isPaid ? "PAID IN FULL" : "BALANCE DUE",
                isPaid ? booking.getTotalAmount() : booking.getBalanceDue(),
                isPaid ? C_GREEN : C_RED,
                isPaid ? fPaid : fBalance);

        doc.add(charges);

        // Payment history — compact
        if (!payments.isEmpty()) {
            PdfPTable ph = new PdfPTable(new float[]{20, 20, 20, 40});
            ph.setWidthPercentage(100);
            ph.setSpacingBefore(4);
            ph.setSpacingAfter(4);

            PdfPCell phHdr = new PdfPCell(sectionHeader("PAYMENT HISTORY"));
            phHdr.setColspan(4); phHdr.setBorder(Rectangle.NO_BORDER); phHdr.setPadding(0);
            ph.addCell(phHdr);

            // Column headers
            for (String col : new String[]{"Date", "Amount (₹)", "Mode", "Transaction ID"}) {
                PdfPCell c = new PdfPCell(new Phrase(col, fLabel));
                c.setBackgroundColor(C_LIGHT);
                c.setBorderColor(C_BORDER);
                c.setPadding(5);
                ph.addCell(c);
            }

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-MMM-yy");
            boolean alt = false;
            for (Payment p : payments) {
                BaseColor bg = alt ? new BaseColor(252, 252, 252) : BaseColor.WHITE;
                addPhCell(ph, p.getPaymentDate() != null ? p.getPaymentDate().format(dtf) : "—", bg);
                addPhCell(ph, String.format("%.2f", p.getAmount()), bg);
                addPhCell(ph, p.getPaymentMode().name(), bg);
                addPhCell(ph, p.getTransactionId() != null ? p.getTransactionId() : "—", bg);
                alt = !alt;
            }
            doc.add(ph);
        }
    }

    // ── COUPON VOUCHER ────────────────────────────────────────────────────

    private void addCouponVoucher(Document doc, String couponCode, Booking booking)
            throws DocumentException {
        // Get coupon details
        String details  = couponService.getCouponDetails(couponCode);
        int discPercent = 0;
        String expiry   = "90 days";
        try {
            // Parse discount from details string like "15% discount | Expiry: 2025-06-01 | Status: Valid"
            String[] parts = details.split("\\|");
            if (parts.length > 0) {
                String discStr = parts[0].trim().replace("% discount", "").trim();
                discPercent = Integer.parseInt(discStr);
            }
            if (parts.length > 1) {
                expiry = parts[1].trim().replace("Expiry:", "").trim();
            }
        } catch (Exception ignored) {}

        PdfPTable voucher = new PdfPTable(1);
        voucher.setWidthPercentage(100);
        voucher.setSpacingBefore(8);
        voucher.setSpacingAfter(6);

        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(C_PURPLE);
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(C_GOLD);
        cell.setBorderWidth(2);
        cell.setPadding(14);

        // Header
        Paragraph header = new Paragraph();
        header.setAlignment(Element.ALIGN_CENTER);
        header.add(new Chunk("🎫  SPECIAL DISCOUNT VOUCHER  🎫\n", fCouponHdr));
        header.add(new Chunk("Thank you for staying with us!\n\n", fCouponSub));
        cell.addElement(header);

        // Discount amount — big
        Paragraph disc = new Paragraph();
        disc.setAlignment(Element.ALIGN_CENTER);
        disc.add(new Chunk(discPercent + "% OFF\n", fCouponCode));
        disc.add(new Chunk("on your next booking\n\n", fCouponSub));
        cell.addElement(disc);

        // Code box
        PdfPTable codeBox = new PdfPTable(1);
        codeBox.setWidthPercentage(60);
        codeBox.setHorizontalAlignment(Element.ALIGN_CENTER);
        PdfPCell codeCell = new PdfPCell();
        codeCell.setBackgroundColor(C_NAVY);
        codeCell.setPadding(10);
        codeCell.setBorderColor(C_GOLD);
        codeCell.setBorderWidth(1.5f);
        Paragraph codeP = new Paragraph();
        codeP.setAlignment(Element.ALIGN_CENTER);
        codeP.add(new Chunk("YOUR COUPON CODE\n",
                FontFactory.getFont(FontFactory.HELVETICA, 7, C_MUTED)));
        codeP.add(new Chunk(couponCode, fCouponCode));
        codeCell.addElement(codeP);
        codeBox.addCell(codeCell);
        cell.addElement(codeBox);

        // Terms
        Paragraph terms = new Paragraph();
        terms.setAlignment(Element.ALIGN_CENTER);
        terms.setSpacingBefore(8);
        Font tFont = FontFactory.getFont(FontFactory.HELVETICA, 7, new BaseColor(200, 200, 200));
        terms.add(new Chunk("\nValid until: " + expiry +
                "  |  Single use only  |  Cannot be combined with other offers\n" +
                "Present this voucher at check-in or enter code online.", tFont));
        cell.addElement(terms);

        voucher.addCell(cell);
        doc.add(voucher);
    }

    // ── THANK YOU FOOTER ──────────────────────────────────────────────────

    private void addThankYouFooter(Document doc) throws DocumentException {
        PdfPTable footer = new PdfPTable(1);
        footer.setWidthPercentage(100);
        footer.setSpacingBefore(4);

        PdfPCell cell = new PdfPCell();
        cell.setBackgroundColor(C_NAVY);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(10);

        Paragraph p = new Paragraph();
        p.setAlignment(Element.ALIGN_CENTER);
        p.add(new Chunk("✨  Thank you for choosing Grand Hotel. We look forward to welcoming you again!  ✨\n",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, C_GOLD)));
        p.add(new Chunk("This is a computer-generated invoice. No signature required.  |  " +
                "For queries: info@grandhotel.com  |  +91-98765-43210",
                FontFactory.getFont(FontFactory.HELVETICA, 7, C_MUTED)));
        cell.addElement(p);
        footer.addCell(cell);
        doc.add(footer);
    }

    // ── HELPERS ───────────────────────────────────────────────────────────

    private PdfPTable sectionHeader(String title) throws DocumentException {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);
        PdfPCell c = new PdfPCell(new Phrase(title, fSectionHdr));
        c.setBackgroundColor(C_NAVY);
        c.setBorder(Rectangle.NO_BORDER);
        c.setPadding(5);
        t.addCell(c);
        return t;
    }

    private void addInfoRow(PdfPCell parent, String label, String value) {
        PdfPTable row = new PdfPTable(new float[]{38, 62});
        row.setWidthPercentage(100);
        PdfPCell lc = new PdfPCell(new Phrase(label, fLabel));
        lc.setBorder(Rectangle.BOTTOM); lc.setBorderColor(C_BORDER); lc.setPadding(3);
        lc.setBackgroundColor(BaseColor.WHITE);
        PdfPCell vc = new PdfPCell(new Phrase(value, fValue));
        vc.setBorder(Rectangle.BOTTOM); vc.setBorderColor(C_BORDER); vc.setPadding(3);
        row.addCell(lc); row.addCell(vc);
        try { parent.addElement(row); } catch (Exception ignored) {}
    }

    private void addChargeRow(PdfPTable t, String label, double amount, boolean highlight) {
        BaseColor bg = highlight ? C_LIGHT : BaseColor.WHITE;
        PdfPCell lc = new PdfPCell(new Phrase(label, fValue));
        lc.setBorderColor(C_BORDER); lc.setBackgroundColor(bg); lc.setPadding(5);
        PdfPCell vc = new PdfPCell(new Phrase(String.format("₹ %,.2f", amount), fValue));
        vc.setBorderColor(C_BORDER); vc.setBackgroundColor(bg); vc.setPadding(5);
        vc.setHorizontalAlignment(Element.ALIGN_RIGHT);
        t.addCell(lc); t.addCell(vc);
    }

    private void addChargeRowBold(PdfPTable t, String label, double amount,
                                   BaseColor bg, Font f) {
        PdfPCell lc = new PdfPCell(new Phrase(label, f));
        lc.setBackgroundColor(bg == C_GREEN || bg == C_RED ? new BaseColor(bg.getRed(),
                bg.getGreen(), bg.getBlue(), 30) : C_LIGHT);
        lc.setBorderColor(C_BORDER); lc.setPadding(6);
        PdfPCell vc = new PdfPCell(new Phrase(String.format("₹ %,.2f", amount), f));
        vc.setBackgroundColor(lc.getBackgroundColor());
        vc.setBorderColor(C_BORDER); vc.setPadding(6);
        vc.setHorizontalAlignment(Element.ALIGN_RIGHT);
        t.addCell(lc); t.addCell(vc);
    }

    private void addDividerRow(PdfPTable t) {
        PdfPCell div = new PdfPCell(new Phrase(""));
        div.setColspan(2); div.setFixedHeight(1f);
        div.setBackgroundColor(C_NAVY); div.setBorder(Rectangle.NO_BORDER);
        t.addCell(div);
    }

    private void addPhCell(PdfPTable t, String text, BaseColor bg) {
        PdfPCell c = new PdfPCell(new Phrase(text, fValue));
        c.setBackgroundColor(bg); c.setBorderColor(C_BORDER); c.setPadding(4);
        t.addCell(c);
    }
}
