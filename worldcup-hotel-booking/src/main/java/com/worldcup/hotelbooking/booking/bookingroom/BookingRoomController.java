package com.worldcup.hotelbooking.booking.bookingroom;

import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
public class BookingRoomController {
    //I am not sure if we need this controller, as the booking process is handled by the BookingController, and the BookingRoom entity is just a join table between Booking and Room. We can implement any necessary endpoints for managing the BookingRoom entity here if needed in the future.

    private final BookingRoomServiceImp bookingRoomService;
    BookingRoomController(BookingRoomServiceImp bookingRoomService) {
        this.bookingRoomService = bookingRoomService;
    }


}
