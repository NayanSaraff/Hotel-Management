package com.hotel.service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hotel.dao.BookingDAO;
import com.hotel.dao.CustomerDAO;
import com.hotel.dao.PaymentDAO;
import com.hotel.dao.RoomDAO;
import com.hotel.model.Booking;
import com.hotel.model.Customer;
import com.hotel.model.Payment;
import com.hotel.model.Room;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

/**
 * Generates a luxury hotel invoice in the style of Marriott / Taj Hotels.
 *
 * Layout (top → bottom):
 *   1. Elegant header bar   – hotel monogram | name centred | invoice meta right
 *   2. Thin gold rule
 *   3. Two-column info band – "Billed To" (guest) | "Stay Details" (booking)
 *   4. Itemised charges table with clean lines
 *   5. Settlement / balance section
 *   6. Payment history (subtle, alternating rows)
 *   7. Authorisation block – printed name | signature image | designation line
 *   8. Coupon voucher (dashed-border strip, optional)
 *   9. Footer strip – contact line
 */
public class InvoiceService {

    private static final Logger logger = LoggerFactory.getLogger(InvoiceService.class);

    // ── Date formats ──────────────────────────────────────────────────────
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter DT_FMT   = DateTimeFormatter.ofPattern("dd MMM yyyy  hh:mm a");
    private static final DateTimeFormatter PH_FMT   = DateTimeFormatter.ofPattern("dd MMM yy");

    // ── Luxury Colour Palette ─────────────────────────────────────────────
    /** Deep charcoal – primary text and header background */
    private static final BaseColor C_CHARCOAL  = new BaseColor(28,  28,  28);
    /** Warm champagne gold – accent lines, headings, monogram */
    private static final BaseColor C_GOLD      = new BaseColor(182, 148, 81);
    /** Light gold tint – section header backgrounds */
    private static final BaseColor C_GOLD_TINT = new BaseColor(250, 246, 238);
    /** Off-white ivory – table row fill */
    private static final BaseColor C_IVORY     = new BaseColor(253, 252, 249);
    /** Silver divider */
    private static final BaseColor C_SILVER    = new BaseColor(210, 210, 205);
    /** Muted warm grey – labels, footers */
    private static final BaseColor C_WARM_GREY = new BaseColor(130, 120, 108);
    /** Near-black text */
    private static final BaseColor C_TEXT      = new BaseColor(40,  38,  36);
    /** Emerald for PAID */
    private static final BaseColor C_GREEN     = new BaseColor(34, 139, 87);
    /** Crimson for BALANCE DUE */
    private static final BaseColor C_RED       = new BaseColor(185, 52,  52);

    // ── Fonts ─────────────────────────────────────────────────────────────
    /** Hotel name – Great Vibes calligraphy script */
    private final Font fHotelName  = loadScriptFont(32, C_CHARCOAL);
    /** Hotel tagline */
    private final Font fTagline    = FontFactory.getFont(FontFactory.HELVETICA,      8, C_WARM_GREY);
    /** Invoice title */
    private final Font fInvTitle   = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, C_GOLD);
    /** Invoice meta (ref / date) */
    private final Font fInvMeta    = FontFactory.getFont(FontFactory.HELVETICA,      8, C_WARM_GREY);
    /** Section header – uppercase small */
    private final Font fSecHdr     = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, C_GOLD);
    /** Label text */
    private final Font fLabel      = FontFactory.getFont(FontFactory.HELVETICA,      8, C_WARM_GREY);
    /** Value text */
    private final Font fValue      = FontFactory.getFont(FontFactory.HELVETICA,      9, C_TEXT);
    /** Bold value */
    private final Font fValueBold  = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, C_TEXT);
    /** Table column header */
    private final Font fColHdr     = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, C_CHARCOAL);
    /** Subtotal row */
    private final Font fSubtotal   = FontFactory.getFont(FontFactory.HELVETICA_BOLD,10, C_CHARCOAL);
    /** Grand total */
    private final Font fGrandTotal = FontFactory.getFont(FontFactory.TIMES_BOLD,    12, C_CHARCOAL);
    /** PAID stamp */
    private final Font fPaid       = FontFactory.getFont(FontFactory.HELVETICA_BOLD,13, C_GREEN);
    /** Balance due */
    private final Font fBalance    = FontFactory.getFont(FontFactory.HELVETICA_BOLD,13, C_RED);
    /** Footer */
    private final Font fFooter     = FontFactory.getFont(FontFactory.HELVETICA,      7, C_WARM_GREY);
    /** Auth designation */
    private final Font fDesig      = FontFactory.getFont(FontFactory.HELVETICA,      8, C_WARM_GREY);
    /** Auth name (printed) */
    private final Font fAuthName   = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, C_TEXT);
    /** Coupon header */
    private final Font fCoupHdr    = FontFactory.getFont(FontFactory.HELVETICA_BOLD,10, C_CHARCOAL);
    /** Coupon code */
    private final Font fCoupCode   = FontFactory.getFont(FontFactory.TIMES_BOLD,    18, C_GOLD);
    /** Coupon body */
    private final Font fCoupBody   = FontFactory.getFont(FontFactory.HELVETICA,      8, C_WARM_GREY);

    // ── DAOs ──────────────────────────────────────────────────────────────
    private final BookingDAO    bookingDAO    = new BookingDAO();
    private final CustomerDAO   customerDAO   = new CustomerDAO();
    private final RoomDAO       roomDAO       = new RoomDAO();
    private final PaymentDAO    paymentDAO    = new PaymentDAO();
    private final CouponService couponService = new CouponService();

    // ── Signature path (at project root) ─────────────────────────────────
    private static final String SIGNATURE_PATH = resolveSignaturePath();

    private static String resolveSignaturePath() {
        String[] candidates = {
            "signature.png",
            "C:/Users/sarad/OneDrive/Desktop/HotelManagementSystem/signature.png",
            System.getProperty("user.dir") + "/signature.png",
            System.getProperty("user.dir") + "\\signature.png"
        };
        for (String path : candidates) {
            if (new java.io.File(path).exists()) {
                LoggerFactory.getLogger(InvoiceService.class).info("Signature found at: {}", path);
                return path;
            }
        }
        LoggerFactory.getLogger(InvoiceService.class).warn("signature.png not found in any candidate path.");
        return "signature.png";
    }

    private static String resolveLogoPath() {
        String[] candidates = {
            "logo.png",
            "C:/Users/sarad/OneDrive/Desktop/HotelManagementSystem/logo.png",
            System.getProperty("user.dir") + "/logo.png",
            System.getProperty("user.dir") + "\\logo.png"
        };
        for (String path : candidates) {
            if (new java.io.File(path).exists()) {
                LoggerFactory.getLogger(InvoiceService.class).info("Logo found at: {}", path);
                return path;
            }
        }
        LoggerFactory.getLogger(InvoiceService.class).warn("logo.png not found in any candidate path.");
        return "logo.png";
    }

    // ═════════════════════════════════════════════════════════════════════
    //  Public API
    // ═════════════════════════════════════════════════════════════════════

    public boolean generateInvoice(int bookingId, String outputPath) {
        return generateInvoiceWithCoupon(bookingId, outputPath, null);
    }

    public boolean generateInvoiceWithCoupon(int bookingId, String outputPath, String couponCode) {
        Optional<Booking> bookingOpt = bookingDAO.findById(bookingId);
        if (bookingOpt.isEmpty()) {
            logger.error("Booking not found: {}", bookingId);
            return false;
        }

        Booking         booking  = bookingOpt.get();
        Optional<Customer> custOpt  = customerDAO.findById(booking.getCustomerId());
        Optional<Room>     roomOpt  = roomDAO.findById(booking.getRoomId());
        List<Payment>      payments = paymentDAO.findByBookingId(bookingId);

        try (FileOutputStream fos = new FileOutputStream(outputPath)) {
            // A4 with compact margins to keep the full invoice on one page.
            Document doc = new Document(PageSize.A4, 36, 36, 28, 28);
            PdfWriter writer = PdfWriter.getInstance(doc, fos);
            doc.open();

            renderHeader(doc, booking);
            renderGoldRule(doc, writer);
            renderGuestAndStayBand(doc, booking, custOpt.orElse(null), roomOpt.orElse(null));
            renderChargesTable(doc, booking);
            renderSettlement(doc, booking, payments);
            if (!payments.isEmpty()) renderPaymentHistory(doc, payments);
            pushAuthorisationAndFooterToBottom(doc, writer, couponCode != null);
            renderAuthorisation(doc);
            if (couponCode != null) renderCouponVoucher(doc, couponCode, booking);
            renderFooter(doc);

            doc.close();
            logger.info("Invoice generated: {}", outputPath);
            return true;

        } catch (DocumentException | IOException e) {
            logger.error("Invoice generation error: {}", e.getMessage(), e);
            return false;
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    //  1. HEADER
    // ═════════════════════════════════════════════════════════════════════

    private void renderHeader(Document doc, Booking booking) throws DocumentException {
        /*
         * Two-column layout:
         *   [Hotel text + invoice meta left-aligned] | [Logo right-aligned]
         */
        PdfPTable header = new PdfPTable(2);
        header.setWidthPercentage(100);
        header.setWidths(new float[]{72, 28});
        header.setSpacingAfter(0);

        // ── Left: Hotel identity + invoice details ──
        PdfPCell textCell = new PdfPCell();
        textCell.setBorder(Rectangle.NO_BORDER);
        textCell.setPadding(4);
        textCell.setVerticalAlignment(Element.ALIGN_MIDDLE);

        Paragraph nameP = new Paragraph();
        nameP.setAlignment(Element.ALIGN_LEFT);
        nameP.add(new Chunk("The Marcelli", fHotelName));
        nameP.add(Chunk.NEWLINE);
        nameP.add(new Chunk("777 Obsidian Strip · Paradise District, Las Vegas, NV 89109", fTagline));
        nameP.add(Chunk.NEWLINE);
        nameP.add(new Chunk("+1-702-555-0177  ·  reservations@themarcelli.com  ·  EIN: 88-1234567", fTagline));
        nameP.add(Chunk.NEWLINE);
        nameP.add(new Chunk("TAX INVOICE", fInvTitle));
        nameP.add(Chunk.NEWLINE);
        nameP.add(new Chunk("No: " + safe(booking.getBookingReference()), fInvMeta));
        nameP.add(Chunk.NEWLINE);
        if (booking.getBookingDate() != null) {
            nameP.add(new Chunk(booking.getBookingDate().format(DT_FMT), fInvMeta));
        }
        textCell.addElement(nameP);

        // ── Right: Logo ──
        PdfPCell logoCell = new PdfPCell();
        logoCell.setBorder(Rectangle.NO_BORDER);
        logoCell.setPadding(4);
        logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        logoCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        try {
            Image logo = Image.getInstance(resolveLogoPath());
            logo.scaleToFit(130, 130);
            logo.setAlignment(Element.ALIGN_RIGHT);
            logoCell.addElement(logo);
        } catch (Exception e) {
            logger.warn("Logo not loaded: {}", e.getMessage());
        }

        header.addCell(textCell);
        header.addCell(logoCell);
        doc.add(header);
    }

    // ═════════════════════════════════════════════════════════════════════
    //  2. GOLD RULE
    // ═════════════════════════════════════════════════════════════════════

    private void renderGoldRule(Document doc, PdfWriter writer) throws DocumentException {
        /*
         * Draw a 1 pt gold horizontal line at the current cursor position.
         * We use a zero-height table row with a coloured bottom border.
         */
        PdfPTable rule = new PdfPTable(1);
        rule.setWidthPercentage(100);
        rule.setSpacingBefore(1);
        rule.setSpacingAfter(4);

        PdfPCell line = new PdfPCell(new Phrase(""));
        line.setFixedHeight(1.5f);
        line.setBackgroundColor(C_GOLD);
        line.setBorder(Rectangle.NO_BORDER);
        rule.addCell(line);
        doc.add(rule);
    }

    // ═════════════════════════════════════════════════════════════════════
    //  3. GUEST & STAY BAND
    // ═════════════════════════════════════════════════════════════════════

    private void renderGuestAndStayBand(Document doc, Booking booking,
                                         Customer customer, Room room)
            throws DocumentException {

        PdfPTable band = new PdfPTable(2);
        band.setWidthPercentage(100);
        band.setWidths(new float[]{50, 50});
        band.setSpacingBefore(0);
        band.setSpacingAfter(6);

        // ── Billed To ──
        PdfPCell billedCell = styledInfoPanel("BILLED TO");
        String name  = customer != null ? customer.getFullName()  : safe(booking.getCustomerName());
        String phone = customer != null ? customer.getPhone()      : "—";
        String email = customer != null && customer.getEmail() != null ? customer.getEmail() : "—";
        String idPrf = customer != null && customer.getIdNumber() != null
                ? customer.getIdType().name() + " · " + customer.getIdNumber() : "—";
        String city  = customer != null && customer.getCity() != null
                ? customer.getCity() + (customer.getState() != null ? ", " + customer.getState() : "") : "—";

        addInfoPair(billedCell, "Guest Name",  name,  true);
        addInfoPair(billedCell, "Mobile",      phone, false);
        addInfoPair(billedCell, "Email",       email, false);
        addInfoPair(billedCell, "ID Proof",    idPrf, false);
        addInfoPair(billedCell, "City",        city,  false);

        // ── Stay Details ──
        PdfPCell stayCell = styledInfoPanel("STAY DETAILS");
        addInfoPair(stayCell, "Room Number",   safe(booking.getRoomNumber()),   true);
        addInfoPair(stayCell, "Room Category", safe(booking.getRoomCategory()), false);
        addInfoPair(stayCell, "Check-In",
                booking.getCheckInDate()  != null ? booking.getCheckInDate().format(DATE_FMT)  : "—", false);
        addInfoPair(stayCell, "Check-Out",
                booking.getCheckOutDate() != null ? booking.getCheckOutDate().format(DATE_FMT) : "—", false);
        addInfoPair(stayCell, "Nights",        String.valueOf(booking.getNumberOfNights()), false);
        addInfoPair(stayCell, "Guests",        String.valueOf(booking.getNumberOfGuests()), false);
        if (room != null)
            addInfoPair(stayCell, "Rate / Night",
                    "Rs. " + String.format("%,.2f", room.getPricePerNight()), false);
        addInfoPair(stayCell, "Status", booking.getStatus().name(), false);

        band.addCell(billedCell);
        band.addCell(stayCell);
        doc.add(band);
    }

    // ═════════════════════════════════════════════════════════════════════
    //  4. ITEMISED CHARGES TABLE
    // ═════════════════════════════════════════════════════════════════════

    private void renderChargesTable(Document doc, Booking booking) throws DocumentException {
        /*
         * Columns: Description | Nights | Rate | Amount
         * Taj-style: thin bottom border only on rows, no outer box.
         */
        PdfPTable t = new PdfPTable(new float[]{50, 15, 20, 15});
        t.setWidthPercentage(100);
        t.setSpacingBefore(2);
        t.setSpacingAfter(0);

        // Column headers
        addColHeader(t, "DESCRIPTION");
        addColHeader(t, "NIGHTS");
        addColHeader(t, "RATE (Rs.)");
        addColHeader(t, "AMOUNT (Rs.)");

        // Gold rule under headers
        addFullWidthRule(t, 4, C_GOLD, 1.2f);

        // Row: Room Charges
        addItemRow(t, "Accommodation Charges",
                String.valueOf(booking.getNumberOfNights()),
                fmt(booking.getRoomCharges() / Math.max(booking.getNumberOfNights(), 1)),
                fmt(booking.getRoomCharges()), false);

        // Row: Service Charges
        addItemRow(t, "Service & Amenities", "—", "—",
                fmt(booking.getServiceCharges()), true);

        // Row: GST
        addItemRow(t, "GST / Taxes", "—", "—",
                fmt(booking.getGstAmount()), false);

        // Divider
        addFullWidthRule(t, 4, C_SILVER, 0.6f);

        // Total row (right-aligned spanning, TIMES_BOLD)
        PdfPCell totalLbl = new PdfPCell(new Phrase("TOTAL AMOUNT", fSubtotal));
        totalLbl.setColspan(3);
        totalLbl.setBorder(Rectangle.NO_BORDER);
        totalLbl.setPadding(5);
        totalLbl.setHorizontalAlignment(Element.ALIGN_RIGHT);
        t.addCell(totalLbl);

        PdfPCell totalAmt = new PdfPCell(
                new Phrase(fmt(booking.getTotalAmount()), fGrandTotal));
        totalAmt.setBorder(Rectangle.NO_BORDER);
        totalAmt.setPadding(5);
        totalAmt.setHorizontalAlignment(Element.ALIGN_RIGHT);
        t.addCell(totalAmt);

        addFullWidthRule(t, 4, C_GOLD, 1.0f);

        doc.add(t);
    }

    // ═════════════════════════════════════════════════════════════════════
    //  5. SETTLEMENT BLOCK
    // ═════════════════════════════════════════════════════════════════════

    private void renderSettlement(Document doc, Booking booking, List<Payment> payments)
            throws DocumentException {
        /*
         * Right-aligned two-column strip: label | amount
         * Shows advance paid, then PAID IN FULL or BALANCE DUE.
         */
        PdfPTable s = new PdfPTable(new float[]{70, 30});
        s.setWidthPercentage(55);
        s.setHorizontalAlignment(Element.ALIGN_RIGHT);
        s.setSpacingBefore(2);
        s.setSpacingAfter(8);

        addSettlementRow(s, "Less: Advance Paid",
                "Rs. " + fmt(booking.getAdvancePaid()), fValueBold, fValue, false);

        boolean paid = booking.getBalanceDue() <= 0;
        String  lblFinal   = paid ? "SETTLED — PAID IN FULL" : "BALANCE DUE";
        String  amtFinal   = "Rs. " + fmt(paid ? booking.getTotalAmount() : booking.getBalanceDue());
        Font    fontFinal  = paid ? fPaid : fBalance;

        addFullWidthRule(s, 2, C_SILVER, 0.6f);
        addSettlementRow(s, lblFinal, amtFinal, fontFinal, fontFinal, true);

        doc.add(s);
    }

    // ═════════════════════════════════════════════════════════════════════
    //  6. PAYMENT HISTORY
    // ═════════════════════════════════════════════════════════════════════

    private void renderPaymentHistory(Document doc, List<Payment> payments) throws DocumentException {
        // Section heading
        doc.add(sectionLabel("PAYMENT HISTORY"));

        PdfPTable ph = new PdfPTable(new float[]{20, 20, 20, 40});
        ph.setWidthPercentage(100);
        ph.setSpacingBefore(2);
        ph.setSpacingAfter(30);

        // Column headers
        for (String col : new String[]{"Date", "Amount (Rs.)", "Mode", "Reference / Transaction"}) {
            PdfPCell c = new PdfPCell(new Phrase(col, fColHdr));
            c.setBackgroundColor(C_GOLD_TINT);
            c.setBorderColor(C_SILVER);
            c.setBorderWidth(0.5f);
            c.setBorder(Rectangle.BOTTOM);
            c.setPadding(4);
            ph.addCell(c);
        }

        boolean alt = false;
        for (Payment p : payments) {
            BaseColor bg = alt ? C_IVORY : BaseColor.WHITE;
            addPhCell(ph, p.getPaymentDate() != null ? p.getPaymentDate().format(PH_FMT) : "—", bg);
            addPhCell(ph, fmt(p.getAmount()), bg);
            addPhCell(ph, p.getPaymentMode().name(), bg);
            addPhCell(ph, p.getTransactionId() != null ? p.getTransactionId() : "—", bg);
            alt = !alt;
        }

        doc.add(ph);
    }

    // ═════════════════════════════════════════════════════════════════════
    //  7. AUTHORISATION BLOCK
    // ═════════════════════════════════════════════════════════════════════

    private void renderAuthorisation(Document doc) throws DocumentException {
        /*
         * Right-aligned panel:
         *   [signature image if available]
         *   ─────────────────────────
         *   Front Office Manager
         *   The Marcelli, Las Vegas
         *
         * If signature.png cannot be read we fall back to a blank space.
         */
        PdfPTable auth = new PdfPTable(new float[]{60, 40});
        auth.setWidthPercentage(100);
        auth.setSpacingBefore(0);
        auth.setSpacingAfter(8);

        // Left: disclaimer note
        PdfPCell noteCell = new PdfPCell();
        noteCell.setBorder(Rectangle.NO_BORDER);
        noteCell.setPadding(4);
        noteCell.setVerticalAlignment(Element.ALIGN_BOTTOM);
        Paragraph note = new Paragraph();
        note.add(new Chunk("This is a computer-generated invoice and does not require a physical seal.\n", fDesig));
        note.add(new Chunk("Please retain this document for your records.", fDesig));
        noteCell.addElement(note);

        // Right: signature block
        PdfPCell sigCell = new PdfPCell();
        sigCell.setBorder(Rectangle.NO_BORDER);
        sigCell.setPadding(6);
        sigCell.setHorizontalAlignment(Element.ALIGN_CENTER);

        // Attempt to embed signature image
        boolean sigLoaded = false;
        if (new java.io.File(SIGNATURE_PATH).exists()) {
            try {
                Image sig = Image.getInstance(SIGNATURE_PATH);
                sig.scaleToFit(120, 45);
                sig.setAlignment(Element.ALIGN_CENTER);
                sigCell.addElement(sig);
                sigLoaded = true;
            } catch (Exception e) {
                logger.warn("Could not load signature image: {}", e.getMessage());
            }
        }

        if (!sigLoaded) {
            // Blank placeholder for manual signature
            PdfPTable blank = new PdfPTable(1);
            blank.setWidthPercentage(80);
            blank.setHorizontalAlignment(Element.ALIGN_CENTER);
            PdfPCell blankLine = new PdfPCell(new Phrase(""));
            blankLine.setFixedHeight(28);
            blankLine.setBorder(Rectangle.NO_BORDER);
            blank.addCell(blankLine);
            sigCell.addElement(blank);
        }

        // Thin rule
        PdfPTable sigRule = new PdfPTable(1);
        sigRule.setWidthPercentage(70);
        sigRule.setHorizontalAlignment(Element.ALIGN_CENTER);
        PdfPCell ruleLine = new PdfPCell(new Phrase(""));
        ruleLine.setFixedHeight(0.8f);
        ruleLine.setBorder(Rectangle.NO_BORDER);
        ruleLine.setBackgroundColor(C_CHARCOAL);
        sigRule.addCell(ruleLine);
        sigCell.addElement(sigRule);

        Paragraph authName = new Paragraph("Front Office Manager", fAuthName);
        authName.setAlignment(Element.ALIGN_CENTER);
        sigCell.addElement(authName);

        Paragraph authDesig = new Paragraph("The Marcelli, Las Vegas", fDesig);
        authDesig.setAlignment(Element.ALIGN_CENTER);
        sigCell.addElement(authDesig);

        auth.addCell(noteCell);
        auth.addCell(sigCell);
        doc.add(auth);
    }

    /**
     * Pushes the closing blocks (authorisation + footer) closer to the page base
     * so they don't appear floating in the middle when content above is short.
     */
    private void pushAuthorisationAndFooterToBottom(Document doc, PdfWriter writer, boolean hasCoupon)
            throws DocumentException {
        if (hasCoupon) {
            return;
        }

        // Keep enough room for: authorisation block + footer rule + footer line.
        float reservedBottomBand = 132f;
        float targetStartY = doc.bottom() + reservedBottomBand;
        float currentY = writer.getVerticalPosition(true);
        float spacerHeight = currentY - targetStartY;

        if (spacerHeight > 4f) {
            PdfPTable spacer = new PdfPTable(1);
            spacer.setWidthPercentage(100);
            PdfPCell spacerCell = new PdfPCell(new Phrase(""));
            spacerCell.setFixedHeight(spacerHeight);
            spacerCell.setBorder(Rectangle.NO_BORDER);
            spacer.addCell(spacerCell);
            doc.add(spacer);
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    //  8. COUPON VOUCHER (optional)
    // ═════════════════════════════════════════════════════════════════════

    private void renderCouponVoucher(Document doc, String couponCode, Booking booking)
            throws DocumentException {
        String details   = couponService.getCouponDetails(couponCode);
        int    discount  = 0;
        String expiry    = "90 days from issue";

        try {
            String[] parts = details.split("\\|");
            if (parts.length > 0)
                discount = Integer.parseInt(parts[0].trim().replace("% discount", "").trim());
            if (parts.length > 1)
                expiry = parts[1].trim().replace("Expiry:", "").trim();
        } catch (Exception ignored) {}

        // Outer dashed-border panel
        PdfPTable voucher = new PdfPTable(1);
        voucher.setWidthPercentage(100);
        voucher.setSpacingBefore(6);
        voucher.setSpacingAfter(4);

        PdfPCell vCell = new PdfPCell();
        vCell.setBackgroundColor(C_IVORY);
        vCell.setBorderColor(C_GOLD);
        vCell.setBorderWidth(1.2f);
        vCell.setBorder(Rectangle.BOX);
        vCell.setPadding(10);

        // Header label
        Paragraph hdr = new Paragraph("EXCLUSIVE RETURN GUEST OFFER", fCoupHdr);
        hdr.setAlignment(Element.ALIGN_CENTER);
        vCell.addElement(hdr);

        // Gold rule
        PdfPTable vRule = new PdfPTable(1);
        vRule.setWidthPercentage(40);
        vRule.setHorizontalAlignment(Element.ALIGN_CENTER);
        PdfPCell vRuleLine = new PdfPCell(new Phrase(""));
        vRuleLine.setFixedHeight(0.8f);
        vRuleLine.setBackgroundColor(C_GOLD);
        vRuleLine.setBorder(Rectangle.NO_BORDER);
        vRule.addCell(vRuleLine);
        vCell.addElement(vRule);

        Paragraph subtext = new Paragraph("\nThank you for staying with us. Enjoy " +
                discount + "% off your next reservation.\n", fCoupBody);
        subtext.setAlignment(Element.ALIGN_CENTER);
        vCell.addElement(subtext);

        Paragraph code = new Paragraph(couponCode, fCoupCode);
        code.setAlignment(Element.ALIGN_CENTER);
        vCell.addElement(code);

        Paragraph terms = new Paragraph(
                "\nValid until: " + expiry +
                "  ·  Single use  ·  Not combinable with other promotions" +
                "  ·  Quote code at time of booking", fCoupBody);
        terms.setAlignment(Element.ALIGN_CENTER);
        vCell.addElement(terms);

        voucher.addCell(vCell);
        doc.add(voucher);
    }

    // ═════════════════════════════════════════════════════════════════════
    //  9. FOOTER
    // ═════════════════════════════════════════════════════════════════════

    private void renderFooter(Document doc) throws DocumentException {
        // Thin gold rule
        PdfPTable rule = new PdfPTable(1);
        rule.setWidthPercentage(100);
        rule.setSpacingBefore(2);
        rule.setSpacingAfter(2);
        PdfPCell rc = new PdfPCell(new Phrase(""));
        rc.setFixedHeight(1f);
        rc.setBackgroundColor(C_GOLD);
        rc.setBorder(Rectangle.NO_BORDER);
        rule.addCell(rc);
        doc.add(rule);

        Paragraph footer = new Paragraph(
                "The Marcelli · 777 Obsidian Strip, Paradise District, Las Vegas, NV 89109" +
                "  ·  Tel: +1-702-555-0177  ·  reservations@themarcelli.com  ·  www.themarcelli.com",
                fFooter);
        footer.setAlignment(Element.ALIGN_CENTER);
        doc.add(footer);
    }

    // ═════════════════════════════════════════════════════════════════════
    //  Helper Builders
    // ═════════════════════════════════════════════════════════════════════

    /** Creates a styled panel cell with section label header */
    private PdfPCell styledInfoPanel(String title) throws DocumentException {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setBorderWidthBottom(0.6f);
        cell.setBorderColorBottom(C_SILVER);
        cell.setBorder(Rectangle.BOTTOM);
        cell.setPadding(6);
        cell.setPaddingTop(0);

        Paragraph lbl = new Paragraph(title + "\n", fSecHdr);
        lbl.setSpacingAfter(2);
        cell.addElement(lbl);

        // Thin gold underline for section label
        PdfPTable underline = new PdfPTable(1);
        underline.setWidthPercentage(30);
        PdfPCell ul = new PdfPCell(new Phrase(""));
        ul.setFixedHeight(0.8f);
        ul.setBackgroundColor(C_GOLD);
        ul.setBorder(Rectangle.NO_BORDER);
        underline.addCell(ul);
        cell.addElement(underline);

        cell.addElement(new Paragraph("\n"));
        return cell;
    }

    /** Adds a label-value pair row inside an info panel cell */
    private void addInfoPair(PdfPCell parent, String label, String value, boolean first) {
        PdfPTable row = new PdfPTable(new float[]{38, 62});
        row.setWidthPercentage(100);

        PdfPCell lc = new PdfPCell(new Phrase(label, fLabel));
        lc.setBorder(Rectangle.NO_BORDER);
        lc.setPaddingBottom(2);
        lc.setPaddingTop(first ? 1 : 2);

        PdfPCell vc = new PdfPCell(new Phrase(value, fValue));
        vc.setBorder(Rectangle.NO_BORDER);
        vc.setPaddingBottom(2);
        vc.setPaddingTop(first ? 1 : 2);

        row.addCell(lc);
        row.addCell(vc);
        try { parent.addElement(row); } catch (Exception ignored) {}
    }

    /** Adds a column header cell to the charges table */
    private void addColHeader(PdfPTable t, String text) {
        PdfPCell c = new PdfPCell(new Phrase(text, fColHdr));
        c.setBorder(Rectangle.BOTTOM);
        c.setBorderColor(C_SILVER);
        c.setBorderWidth(0.5f);
        c.setPadding(4);
        c.setBackgroundColor(C_GOLD_TINT);
        c.setHorizontalAlignment(Element.ALIGN_RIGHT);
        if (text.equals("DESCRIPTION")) c.setHorizontalAlignment(Element.ALIGN_LEFT);
        t.addCell(c);
    }

    /** Adds one line-item row in the charges table */
    private void addItemRow(PdfPTable t, String desc, String nights,
                             String rate, String amount, boolean shade) {
        BaseColor bg = shade ? C_IVORY : BaseColor.WHITE;

        PdfPCell dc = new PdfPCell(new Phrase(desc, fValue));
        dc.setBackgroundColor(bg); dc.setBorder(Rectangle.BOTTOM);
        dc.setBorderColor(C_SILVER); dc.setBorderWidth(0.4f); dc.setPadding(4);
        t.addCell(dc);

        for (String val : new String[]{nights, rate, amount}) {
            PdfPCell vc = new PdfPCell(new Phrase(val, fValue));
            vc.setBackgroundColor(bg); vc.setBorder(Rectangle.BOTTOM);
            vc.setBorderColor(C_SILVER); vc.setBorderWidth(0.4f); vc.setPadding(4);
            vc.setHorizontalAlignment(Element.ALIGN_RIGHT);
            t.addCell(vc);
        }
    }

    /** Adds a horizontal colour rule spanning colspan columns */
    private void addFullWidthRule(PdfPTable t, int colspan, BaseColor colour, float height) {
        PdfPCell div = new PdfPCell(new Phrase(""));
        div.setColspan(colspan);
        div.setFixedHeight(height);
        div.setBackgroundColor(colour);
        div.setBorder(Rectangle.NO_BORDER);
        t.addCell(div);
    }

    /** Adds a row in the settlement block */
    private void addSettlementRow(PdfPTable t, String label, String amount,
                                   Font lf, Font af, boolean highlight) {
        BaseColor bg = highlight ? C_GOLD_TINT : BaseColor.WHITE;

        PdfPCell lc = new PdfPCell(new Phrase(label, lf));
        lc.setBackgroundColor(bg); lc.setBorder(Rectangle.NO_BORDER);
        lc.setPadding(4); lc.setHorizontalAlignment(Element.ALIGN_RIGHT);
        t.addCell(lc);

        PdfPCell ac = new PdfPCell(new Phrase(amount, af));
        ac.setBackgroundColor(bg); ac.setBorder(Rectangle.NO_BORDER);
        ac.setPadding(4); ac.setHorizontalAlignment(Element.ALIGN_RIGHT);
        t.addCell(ac);
    }

    /** Adds a cell in the payment history table */
    private void addPhCell(PdfPTable t, String text, BaseColor bg) {
        PdfPCell c = new PdfPCell(new Phrase(text, fValue));
        c.setBackgroundColor(bg);
        c.setBorderColor(C_SILVER);
        c.setBorderWidth(0.4f);
        c.setBorder(Rectangle.BOTTOM);
        c.setPadding(4);
        t.addCell(c);
    }

    /** Returns a small section-label paragraph with gold underline */
    private Paragraph sectionLabel(String text) {
        Paragraph p = new Paragraph(text, fSecHdr);
        p.setSpacingBefore(2);
        p.setSpacingAfter(1);
        return p;
    }

    /** Formats a double as Indian-locale currency string (no symbol) */
    private String fmt(double amount) {
        return String.format("%,.2f", amount);
    }

    /** Null-safe string helper */
    private String safe(String s) {
        return s != null ? s : "—";
    }

    /**
     * Loads the Great Vibes calligraphy TTF from the project root and returns
     * an embedded iText Font.  Falls back to Times Bold if the file is missing.
     */
    private static Font loadScriptFont(float size, BaseColor colour) {
        String[] candidates = {
            "GreatVibes-Regular.ttf",                          // project root (relative)
            "C:/Users/sarad/OneDrive/Desktop/HotelManagementSystem/GreatVibes-Regular.ttf" // absolute fallback
        };
        for (String path : candidates) {
            if (new java.io.File(path).exists()) {
                try {
                    BaseFont bf = BaseFont.createFont(path, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                    return new Font(bf, size, Font.NORMAL, colour);
                } catch (Exception e) {
                    LoggerFactory.getLogger(InvoiceService.class)
                            .warn("Could not load script font from {}: {}", path, e.getMessage());
                }
            }
        }
        // Graceful fallback – Times Bold looks reasonable if TTF is missing
        LoggerFactory.getLogger(InvoiceService.class)
                .warn("GreatVibes-Regular.ttf not found – falling back to Times Bold.");
        return FontFactory.getFont(FontFactory.TIMES_BOLD, size, colour);
    }
}