package com.hotel.service;

import com.hotel.dao.BookingDAO;
import com.hotel.dao.PaymentDAO;
import com.hotel.dao.RoomDAO;
import com.hotel.model.Booking;
import com.hotel.model.Payment;
import com.hotel.model.Room;
import com.hotel.util.GSTCalculator;
import com.hotel.util.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service layer for all booking-related business logic.
 * Coordinates BookingDAO, RoomDAO, PaymentDAO.
 */
public class BookingService {

    private static final Logger logger = LoggerFactory.getLogger(BookingService.class);

    private final BookingDAO  bookingDAO  = new BookingDAO();
    private final RoomDAO     roomDAO     = new RoomDAO();
    private final PaymentDAO  paymentDAO  = new PaymentDAO();

    // ── Query ─────────────────────────────────────────────────────────────

    public List<Booking> getAllBookings()          { return bookingDAO.findAll(); }
    public List<Booking> getConfirmedBookings()    { return bookingDAO.findConfirmed(); }
    public List<Booking> getCheckedInBookings()    { return bookingDAO.findCheckedIn(); }
    public Optional<Booking> getBookingById(int id){ return bookingDAO.findById(id); }

    /** Return rooms available for the requested dates. */
    public List<Room> getAvailableRooms(LocalDate checkIn, LocalDate checkOut) {
        if (checkIn == null || checkOut == null || !checkOut.isAfter(checkIn)) {
            throw new IllegalArgumentException("Check-out must be after check-in.");
        }
        return roomDAO.findAvailableRooms(checkIn, checkOut);
    }

    // ── Create Booking ────────────────────────────────────────────────────

    /**
     * Create a new confirmed booking and record an advance payment if provided.
     *
     * @return saved booking ID, or -1 on failure
     */
    public int createBooking(Booking booking, double advanceAmount, Payment.Mode paymentMode) {
        // 1. Validate dates
        if (!booking.getCheckOutDate().isAfter(booking.getCheckInDate())) {
            throw new IllegalArgumentException("Check-out date must be after check-in date.");
        }

        // 2. Verify room is still available
        Optional<Room> roomOpt = roomDAO.findById(booking.getRoomId());
        if (roomOpt.isEmpty()) throw new IllegalStateException("Room not found.");
        Room room = roomOpt.get();
        if (room.getStatus() != Room.Status.AVAILABLE) {
            throw new IllegalStateException("Room " + room.getRoomNumber() + " is not available.");
        }

        // 3. Calculate charges
        long nights = booking.getNumberOfNights();
        double roomCharges   = nights * room.getPricePerNight();
        double gst           = GSTCalculator.calculateGST(roomCharges, room.getPricePerNight());
        double total         = roomCharges + gst;
        double balance       = total - advanceAmount;

        booking.setRoomCharges(roomCharges);
        booking.setServiceCharges(0);
        booking.setGstAmount(gst);
        booking.setTotalAmount(total);
        booking.setAdvancePaid(advanceAmount);
        booking.setBalanceDue(balance);
        booking.setStatus(Booking.Status.CONFIRMED);
        booking.setUserId(SessionManager.getInstance().getCurrentUser().getUserId());
        booking.setBookingReference(bookingDAO.generateBookingReference());

        // 4. Persist booking
        int bookingId = bookingDAO.save(booking);
        if (bookingId < 0) {
            logger.error("Failed to persist booking.");
            return -1;
        }

        // 5. Record advance payment
        if (advanceAmount > 0) {
            Payment payment = new Payment(bookingId, advanceAmount, paymentMode, Payment.Type.ADVANCE);
            payment.setTransactionId("ADV-" + bookingId);
            paymentDAO.save(payment);
        }

        logger.info("Booking {} created successfully.", booking.getBookingReference());
        return bookingId;
    }

    // ── Check-In ──────────────────────────────────────────────────────────

    /**
     * Perform check-in: validate booking, mark room OCCUPIED.
     */
    public boolean checkIn(int bookingId) {
        Optional<Booking> opt = bookingDAO.findById(bookingId);
        if (opt.isEmpty()) throw new IllegalStateException("Booking not found.");

        Booking booking = opt.get();
        if (booking.getStatus() != Booking.Status.CONFIRMED) {
            throw new IllegalStateException("Booking is not in CONFIRMED state.");
        }

        booking.setStatus(Booking.Status.CHECKED_IN);
        booking.setActualCheckIn(LocalDateTime.now());
        boolean ok = bookingDAO.update(booking);

        if (ok) {
            roomDAO.updateStatus(booking.getRoomId(), Room.Status.OCCUPIED);
            logger.info("Check-in successful for booking {}", booking.getBookingReference());
        }
        return ok;
    }

    // ── Check-Out ─────────────────────────────────────────────────────────

    /**
     * Perform check-out: record final payment, mark room HOUSEKEEPING.
     */
    public boolean checkOut(int bookingId, double finalPayment, Payment.Mode paymentMode) {
        Optional<Booking> opt = bookingDAO.findById(bookingId);
        if (opt.isEmpty()) throw new IllegalStateException("Booking not found.");

        Booking booking = opt.get();
        if (booking.getStatus() != Booking.Status.CHECKED_IN) {
            throw new IllegalStateException("Booking is not in CHECKED_IN state.");
        }

        // Record final payment
        if (finalPayment > 0) {
            Payment payment = new Payment(bookingId, finalPayment, paymentMode, Payment.Type.FULL);
            payment.setTransactionId("CHK-" + bookingId);
            paymentDAO.save(payment);
        }

        booking.setStatus(Booking.Status.CHECKED_OUT);
        booking.setActualCheckOut(LocalDateTime.now());
        booking.setBalanceDue(0);
        boolean ok = bookingDAO.update(booking);

        if (ok) {
            roomDAO.updateStatus(booking.getRoomId(), Room.Status.HOUSEKEEPING);
            logger.info("Check-out successful for booking {}", booking.getBookingReference());
        }
        return ok;
    }

    // ── Cancel ────────────────────────────────────────────────────────────

    public boolean cancelBooking(int bookingId) {
        Optional<Booking> opt = bookingDAO.findById(bookingId);
        if (opt.isEmpty()) throw new IllegalStateException("Booking not found.");

        Booking booking = opt.get();
        if (booking.getStatus() == Booking.Status.CHECKED_IN ||
            booking.getStatus() == Booking.Status.CHECKED_OUT) {
            throw new IllegalStateException("Cannot cancel a booking in state: " + booking.getStatus());
        }

        boolean ok = bookingDAO.delete(bookingId);
        if (ok) {
            roomDAO.updateStatus(booking.getRoomId(), Room.Status.AVAILABLE);
            logger.info("Booking {} cancelled.", booking.getBookingReference());
        }
        return ok;
    }

    // ── Dashboard Stats ───────────────────────────────────────────────────

    public int getTotalRooms()        { return roomDAO.findAll().size(); }
    public int getAvailableRoomCount(){ return roomDAO.countByStatus(Room.Status.AVAILABLE); }
    public int getOccupiedRoomCount() { return roomDAO.countByStatus(Room.Status.OCCUPIED); }
    public int getCheckedInCount()    { return bookingDAO.countByStatus(Booking.Status.CHECKED_IN); }
    public double getMonthlyRevenue() { return bookingDAO.getMonthlyRevenue(); }
}
