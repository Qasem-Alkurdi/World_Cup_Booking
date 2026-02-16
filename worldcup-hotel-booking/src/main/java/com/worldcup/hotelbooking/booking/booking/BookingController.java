package com.worldcup.hotelbooking.booking.booking;

import com.worldcup.hotelbooking.booking.bookingroom.BookingRoom;
import com.worldcup.hotelbooking.booking.bookingroom.BookingRoomMapper;
import com.worldcup.hotelbooking.booking.bookingroom.BookingRoomRequestDto;
import com.worldcup.hotelbooking.booking.bookingroom.BookingRoomResponseDto;
import com.worldcup.hotelbooking.catalog.hotel.HotelService;
import com.worldcup.hotelbooking.catalog.roomtype.RoomTypeService;
import com.worldcup.hotelbooking.user.user.AppUserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/bookings")
public class BookingController {
    private final BookingServiceImp bookingService;
    private final AppUserService appUserService;
    private final HotelService hotelService;
    private final RoomTypeService roomTypeService;

    BookingController(BookingServiceImp bookingService, AppUserService appUserService, HotelService hotelService, RoomTypeService roomTypeService) {
        this.roomTypeService = roomTypeService;
        this.hotelService = hotelService;
        this.appUserService = appUserService;
        this.bookingService = bookingService;
    }

    //get

    @Operation(summary = "Get booking by ID", description = "Retrieve a booking by its unique ID.")
    @GetMapping("/{id}")
    public BookingResponseDto getBookingById(@PathVariable Long id) {
        return BookingMapper.toDto(bookingService.getBookingById(id));
    }

    @Operation(summary = "Get user's bookings by status", description = "Retrieve all bookings for a specific user filtered by booking status.")
    @GetMapping("/user/{userId}/status/{status}")
    public List<BookingResponseDto> getUserBookingsByStatus(@PathVariable Long userId, @PathVariable Booking.BookingStatus status) {
        return bookingService.getUserBookings(userId, status).stream().map(BookingMapper::toDto).collect(Collectors.toList());
    }

    @Operation(summary = "Get all user's bookings", description = "Retrieve all bookings for a specific user regardless of booking status.")
    @GetMapping("/user/{userId}")
    public List<BookingResponseDto> getUserBookings(@PathVariable Long userId) {
        return bookingService.getUserBookings(userId).stream().map(BookingMapper::toDto).collect(Collectors.toList());
    }

    @Operation(summary = "Get hotel bookings by status", description = "Retrieve all bookings for a specific hotel filtered by booking status.")
    @GetMapping("/hotel/{hotelId}/status/{status}")
    public List<BookingResponseDto> getHotelBookingsByStatus(@PathVariable Long hotelId, @PathVariable Booking.BookingStatus status) {
        return bookingService.getHotelBookings(hotelId, status).stream().map(BookingMapper::toDto).collect(Collectors.toList());
    }

    @Operation(summary = "Get all hotel bookings", description = "Retrieve all bookings for a specific hotel regardless of booking status.")
    @GetMapping("/hotel/{hotelId}")
    public List<BookingResponseDto> getHotelBookings(@PathVariable Long hotelId) {
        return bookingService.getHotelBookings(hotelId).stream().map(BookingMapper::toDto).collect(Collectors.toList());
    }

    @Operation(summary = "Create a new booking", description = "Create a new booking with the provided details. The request must include user ID, hotel ID, check-in and check-out dates, and room details.")
    @PostMapping
    public ResponseEntity<BookingResponseDto> createBooking(@Valid @RequestBody BookingRequestDto bookingRequest, UriComponentsBuilder uriBuilder) {
        Booking booking = BookingMapper.toEntity(bookingRequest, appUserService.getUserById(bookingRequest.getUserId()), hotelService.findById(bookingRequest.getHotelId()));
        for (BookingRoomRequestDto roomRequest : bookingRequest.getRooms()) {
            bookingService.addBookingRoom(
                    BookingRoomMapper.toEntity(
                            roomRequest, booking, roomTypeService.findById(bookingRequest.getHotelId(), roomRequest.getRoomTypeId())
                    )
            );
        }

        Booking createdBooking = bookingService.createBooking(booking);

        BookingResponseDto responseDto = BookingMapper.toDto(createdBooking);
        for (BookingRoom bookingRoom : createdBooking.getBookingRooms()) {
            responseDto.getRooms().add(BookingRoomMapper.toDto(bookingRoom));
        }
        return ResponseEntity.created(uriBuilder.path("/bookings/{id}").buildAndExpand(createdBooking.getId()).toUri()).body(responseDto);
    }

    @Operation(summary = "Confirm a booking", description = "Confirm a pending booking after successful payment. This will change the booking status to CONFIRMED.")
    @PutMapping("/{id}/confirm")
    public ResponseEntity<BookingResponseDto> updateBookingStatus(@PathVariable Long id) {
        Booking updatedBooking = bookingService.confirmBooking(id);
        return ResponseEntity.ok(BookingMapper.toDto(updatedBooking));
    }

    @Operation(summary = "Cancel a booking", description = "Cancel a booking by providing the booking ID and a reason for cancellation. This will change the booking status to CANCELLED.")
    @PutMapping("/{id}/cancel/{reason}")
    public ResponseEntity<BookingResponseDto> cancelBooking(@PathVariable Long id, @PathVariable String reason) {
        Booking updatedBooking = bookingService.cancelBooking(id, reason);
        return ResponseEntity.ok(BookingMapper.toDto(updatedBooking));
    }

    @Operation(summary = "Get booking rooms", description = "Retrieve the list of rooms associated with a specific booking by its ID.")
    @GetMapping("/{id}/rooms")
    public List<BookingRoomResponseDto> getBookingRooms(@PathVariable Long id) {
        return bookingService.getBookingById(id).getBookingRooms().stream().map(BookingRoomMapper::toDto).collect(Collectors.toList());
    }

    @Operation(summary = "Get booking by reference", description = "Retrieve a booking using its unique booking reference code. This allows users to find their booking without needing the booking ID.")
    @GetMapping("/reference/{reference}")
    public BookingResponseDto getBookingByReference(@PathVariable String reference) {
        return BookingMapper.toDto(bookingService.findBookingByReference(reference));
    }


}
