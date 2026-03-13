package com.worldcup.hotelbooking.booking.booking;

import com.worldcup.hotelbooking.availability_pricing.availability.AvailabilityService;
import com.worldcup.hotelbooking.availability_pricing.pricing.EnhancedPricingService;
import com.worldcup.hotelbooking.booking.bookingroom.BookingRoom;
import com.worldcup.hotelbooking.booking.bookingroom.BookingRoomRepository;
import com.worldcup.hotelbooking.booking.cancellation.CancellationPolicyService;
import com.worldcup.hotelbooking.booking.cancellation.CancellationResult;
import com.worldcup.hotelbooking.catalog.hotel.HotelRepository;
import com.worldcup.hotelbooking.catalog.hotel.exceptions.HotelNotFoundException;
import com.worldcup.hotelbooking.catalog.roomtype.RoomTypeRepository;
import com.worldcup.hotelbooking.payment.payment.Payment;
import com.worldcup.hotelbooking.payment.payment.PaymentNotFoundException;
import com.worldcup.hotelbooking.payment.payment.PaymentService;
import com.worldcup.hotelbooking.user.user.AppUserNotFoundException;
import com.worldcup.hotelbooking.user.user.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;


@Service
@Transactional
public class BookingServiceImp implements BookingService {
    private static final Logger logger = LoggerFactory.getLogger(BookingServiceImp.class);
    private final BookingRepository bookingRepository;
    private final EnhancedPricingService enhancedPricingService;
    private final CancellationPolicyService cancellationPolicyService;
    private final AvailabilityService availabilityService;
    private final PaymentService paymentService;

    public BookingServiceImp(
            BookingRepository bookingRepository,
            EnhancedPricingService enhancedPricingService,
            CancellationPolicyService cancellationPolicyService,
            AvailabilityService availabilityService,
            PaymentService paymentService) {
        this.bookingRepository = bookingRepository;
        this.enhancedPricingService = enhancedPricingService;
        this.cancellationPolicyService = cancellationPolicyService;
        this.availabilityService = availabilityService;
        this.paymentService = paymentService;
    }

    //get
    @Transactional(readOnly = true)
    public Booking getBookingById(Long id) {
        return bookingRepository.findByIdWithRooms(id).orElseThrow(() -> new BookingNotFoundException("Booking not found with id: " + id));
    }

    /// //////////////////////////////////////////////////////////

    //create
    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
//to run all the code in this method as a single transaction and to prevent dirty reads, non-repeatable reads, and phantom reads, ensuring data integrity during the booking process.
    public Booking createBooking(Booking booking) {

        booking.setStatus(Booking.BookingStatus.PENDING);
        if (booking.getCheckOutDate().isBefore(booking.getCheckInDate())) {
            throw new IllegalArgumentException("Check-out date cannot be before check-in date");
        }
        if (booking.getBookingRooms() == null || booking.getBookingRooms().isEmpty()) {
            throw new IllegalArgumentException("At least one room must be booked");
        }
        if (!availabilityService.isNumberOfGuestsValid(booking)) {
            throw new IllegalArgumentException("Number of guests exceeds room capacity");
        }

        if(booking.getNumberOfGuests()!=booking.getNumberOfAdults()+booking.getNumberOfChildren()){
            throw new IllegalArgumentException("Total number of guests must equal the sum of adults and children");
        }
        for (BookingRoom room : booking.getBookingRooms()) {
            if (!availabilityService.checkAvailability(room.getRoomType().getId(), booking.getCheckInDate(), booking.getCheckOutDate(), room.getNumberOfRooms())) {
                throw new IllegalArgumentException("Not enough rooms available for room type: " + room.getRoomType().getName());
            }
        }

        booking.setTotalPrice(calculateTotalPrice(booking));


        return bookingRepository.save(booking);
    }

    public BigDecimal calculateTotalPrice(Booking booking) {
        BigDecimal totalPrice = BigDecimal.ZERO;
        for (BookingRoom room : booking.getBookingRooms()) {
            BigDecimal roomPrice = enhancedPricingService.calculateTotalStayPrice(booking, room.getRoomType().getHotel(), room.getRoomType(), room.getNumberOfRooms());
            totalPrice = totalPrice.add(roomPrice);
            room.setBasePricePerNightPerRoom(room.getRoomType().getBasePrice());
            room.setTotalPriceWithFees(roomPrice);

        }
        return totalPrice.setScale(2, RoundingMode.HALF_UP);
    }


    @Override
    @Transactional
    public Booking cancelBooking(Long id, String reason) {
        logger.info("Cancelling booking with id: {} for reason: {}", id, reason);

        Booking booking = bookingRepository.findByIdWithRooms(id)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found with id: " + id));

        CancellationResult cancellationResult = cancellationPolicyService.previewCancellation(booking);

        if (!cancellationResult.isCanCancel()) {
            throw new IllegalStateException(cancellationResult.getPolicyMessage());
        }

        logger.info("Cancellation approved: Refund ${} ({}%), Fee ${}",
                cancellationResult.getRefundAmount(),
                cancellationResult.getRefundPercentage(),
                cancellationResult.getCancellationFee());

        booking.setStatus(Booking.BookingStatus.CANCELLED);
        booking.setCancelReason(reason + " | " + cancellationResult.getPolicyMessage());
        booking.setCancelledAt(java.time.LocalDateTime.now());
        booking.setCancelledBy(booking.getAppUser().getUsername());

        Booking cancelled = bookingRepository.save(booking);
        autoRefundCompletedPaymentIfEligible(cancelled, cancellationResult);
        logger.info("Booking {} cancelled successfully - Refund: ${}",
                cancelled.getBookingReference(),
                cancellationResult.getRefundAmount());

        return cancelled;
    }

    /**
     * Preview cancellation without actually cancelling
     * Shows user what refund they would get
     */
    public CancellationResult previewCancellation(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found with id: " + bookingId));

        return cancellationPolicyService.previewCancellation(booking);
    }

    @Override
    public Booking confirmBooking(Long id) {
        Booking booking = bookingRepository.findByIdWithRooms(id).orElseThrow(() -> new BookingNotFoundException("Booking not found with id: " + id));
        if (booking.getStatus() == Booking.BookingStatus.CONFIRMED) {
            throw new IllegalStateException("Booking is already confirmed");
        }
        if (booking.getStatus() == Booking.BookingStatus.CANCELLED) {
            throw new IllegalStateException("Cancelled booking cannot be confirmed");
        }
        booking.setStatus(Booking.BookingStatus.CONFIRMED);
        booking.setConfirmedAt(java.time.LocalDateTime.now());
        return bookingRepository.save(booking);
    }

    public void addBookingRoom(BookingRoom bookingRoom) {
        Booking booking = bookingRoom.getBooking();
        booking.getBookingRooms().add(bookingRoom);
        bookingRoom.setBooking(booking);
    }

    public Booking findBookingByReference(String bookingReference) {
        return bookingRepository.findByBookingReference(bookingReference).orElseThrow(() -> new BookingNotFoundException("Booking not found with reference: " + bookingReference));
    }

    @Transactional
    public Booking updateExisting(long id, Booking requestBooking) {
        // 1. Fetch the MANAGED entity with its rooms
        Booking managedBooking = bookingRepository.findByIdWithRooms(id)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found: " + id));

        // 2. Business Rule Validations
        validateCanModify(managedBooking, requestBooking);

        // 3. Update top-level fields
        managedBooking.setCheckInDate(requestBooking.getCheckInDate());
        managedBooking.setCheckOutDate(requestBooking.getCheckOutDate());
        managedBooking.setNumberOfGuests(requestBooking.getNumberOfGuests());
        managedBooking.setNumberOfAdults(requestBooking.getNumberOfAdults());
        managedBooking.setNumberOfChildren(requestBooking.getNumberOfChildren());

        // 4. SMART ROOM UPDATE: Synchronize the collections
        // This avoids deleting and re-inserting the same rooms
        BigDecimal newTotal =updateBookingRoomsAndPrice(managedBooking, requestBooking.getBookingRooms());
        managedBooking.setTotalPrice(newTotal);
        // 5. DATA INTEGRITY: Validate logic (dates, capacity, etc.)
        performBookingValidations(managedBooking);

        // 6. CALCULATE PRICES: This fills the "price_per_night" columns before saving


        return bookingRepository.save(managedBooking);
    }

    private BigDecimal updateBookingRoomsAndPrice(Booking managed, List<BookingRoom> requestedRooms) {
        // Simple strategy: If the rooms are complex, clear is okay ONLY IF
        // you calculate prices IMMEDIATELY after adding them.
        managed.getBookingRooms().clear();
        BigDecimal totalPrice = BigDecimal.ZERO;
        for (BookingRoom room : requestedRooms) {
                BigDecimal roomPrice = enhancedPricingService.calculateTotalStayPrice(managed, room.getRoomType().getHotel(), room.getRoomType(), room.getNumberOfRooms());
                totalPrice = totalPrice.add(roomPrice);
                room.setBasePricePerNightPerRoom(room.getRoomType().getBasePrice());
                room.setTotalPriceWithFees(roomPrice);
                room.setBooking(managed);
                managed.getBookingRooms().add(room);
            }
            return totalPrice.setScale(2, RoundingMode.HALF_UP);
        }


    private void performBookingValidations(Booking booking) {
        if (booking.getCheckOutDate().isBefore(booking.getCheckInDate())) {
            throw new IllegalArgumentException("Check-out cannot be before check-in");
        }
        if (!availabilityService.isNumberOfGuestsValid(booking)) {
            throw new IllegalArgumentException("Guests exceed capacity");
        }
        // Check availability for the new dates/rooms
        for (BookingRoom room : booking.getBookingRooms()) {
            boolean available = availabilityService.checkAvailability(
                    room.getRoomType().getId(),
                    booking.getCheckInDate(),
                    booking.getCheckOutDate(),
                    room.getNumberOfRooms()
            );
            if (!available) {
                throw new IllegalArgumentException("Room type " + room.getRoomType().getName() + " is full for these dates.");
            }
        }
    }

    /**
     * Validate booking can be modified
     */
    private void validateCanModify(Booking booking, Booking request) {

        if (!booking.getHotel().getId().equals(request.getHotel().getId()))
            throw new ModificationNotAllowedException(
                    "Cannot modify the hotel"
            );

        if (booking.getStatus() == Booking.BookingStatus.CHECKED_IN) {
            throw new ModificationNotAllowedException(
                    "Cannot modify after check-in. Contact reception.");
        }

        if (booking.getStatus() == Booking.BookingStatus.CHECKED_OUT) {
            throw new ModificationNotAllowedException(
                    "Cannot modify completed booking");
        }

        if (booking.getStatus() == Booking.BookingStatus.CANCELLED) {
            throw new ModificationNotAllowedException(
                    "Cannot modify cancelled booking. Create new booking.");
        }

        if (booking.getCheckInDate().isBefore(LocalDate.now())) {
            throw new ModificationNotAllowedException(
                    "Cannot modify after check-in date passed");
        }
    }

    private List<String> analyzeChanges(Booking original, Booking request) {

        List<String> changes = new ArrayList<>();

        if (!original.getCheckInDate().equals(request.getCheckInDate())) {
            changes.add(String.format("Check-in: %s → %s",
                    original.getCheckInDate(), request.getCheckInDate()));
        }

        if (!original.getCheckOutDate().equals(request.getCheckOutDate())) {
            changes.add(String.format("Check-out: %s → %s",
                    original.getCheckOutDate(), request.getCheckOutDate()));
        }


        if (original.getBookingRooms().size() != request.getBookingRooms().size()) {
            changes.add(String.format("Rooms: %d → %d",
                    original.getBookingRooms().size(), request.getBookingRooms().size()));
        }

        if (original.getNumberOfGuests() != request.getNumberOfGuests()) {
            changes.add(String.format("Guests: %d → %d",
                    original.getNumberOfGuests(), request.getNumberOfGuests()));
        }

        return changes;
    }


    /**
     * Validate changes are allowed
     */
    private void validateChanges(List<String> analysis) {
        if (analysis.isEmpty()) {
            throw new ModificationNotAllowedException("No changes detected");
        }
    }



    public Page<Booking> getGuestHistory(
            Long userId,
            Pageable pageable
    ) {
        Specification<Booking> spec =
                Specification.where(BookingSpecifications.hasUser(userId))
                        .and(BookingSpecifications.isPast());

        return bookingRepository.findAll(spec, pageable);
    }

    public Page<Booking> getHotelUpcomingBookings(
            Long hotelId,
            Pageable pageable
    ) {
        Specification<Booking> spec =
                Specification.where(BookingSpecifications.hasHotel(hotelId))
                        .and(BookingSpecifications.isUpcoming())
                        .and(BookingSpecifications.hasStatus(Booking.BookingStatus.CONFIRMED));

        return bookingRepository.findAll(spec, pageable);
    }

    public Page<Booking> filterBookings(
            Long userId,
            Long hotelId,
            Booking.BookingStatus status,
            LocalDate fromDate,
            LocalDate toDate,
            Double minPrice,
            Double maxPrice,
            Pageable pageable
    ) {

        Specification<Booking> spec = Specification.where((root, query, criteriaBuilder) -> criteriaBuilder.conjunction());

        if (userId != null)
            spec = spec.and(BookingSpecifications.hasUser(userId));

        if (hotelId != null)
            spec = spec.and(BookingSpecifications.hasHotel(hotelId));

        spec = spec.and(BookingSpecifications.hasStatus(status))
                .and(BookingSpecifications.checkInAfter(fromDate))
                .and(BookingSpecifications.checkOutBefore(toDate))
                .and(BookingSpecifications.priceBetween(minPrice, maxPrice));

        return bookingRepository.findAll(spec, pageable);
    }


    public Booking checkInBooking(Long id) {
        Booking booking = bookingRepository.findByIdWithRooms(id)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found with id: " + id));

        if (booking.getStatus() != Booking.BookingStatus.CONFIRMED) {
            throw new IllegalStateException("Only confirmed bookings can be checked in");
        }

        if (booking.getCheckInDate().isAfter(LocalDate.now())) {
            throw new IllegalStateException("Cannot check in before check-in date");
        }

        booking.setStatus(Booking.BookingStatus.CHECKED_IN);
        return bookingRepository.save(booking);
    }

    public Booking checkOutBooking(Long id) {
        Booking booking = bookingRepository.findByIdWithRooms(id)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found with id: " + id));

        if (booking.getStatus() != Booking.BookingStatus.CHECKED_IN) {
            throw new IllegalStateException("Only checked-in bookings can be checked out");
        }


        booking.setStatus(Booking.BookingStatus.CHECKED_OUT);
        return bookingRepository.save(booking);
    }

    private void autoRefundCompletedPaymentIfEligible(Booking booking, CancellationResult cancellationResult) {
        if (cancellationResult.getRefundAmount() == null
                || cancellationResult.getRefundAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        try {
            Payment payment = paymentService.getPaymentByBookingId(booking.getId());
            if (payment.getStatus() == Payment.PaymentStatus.COMPLETED) {
                paymentService.refundPaymentForBooking(
                        booking.getId(),
                        cancellationResult.getRefundAmount(),
                        "Auto refund after booking cancellation"
                );
            }
        } catch (PaymentNotFoundException ex) {
            logger.info("No payment found for booking {}. Skipping refund.", booking.getId());
        }
    }
}
