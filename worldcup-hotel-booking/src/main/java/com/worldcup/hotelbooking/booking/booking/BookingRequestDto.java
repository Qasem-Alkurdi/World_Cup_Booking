package com.worldcup.hotelbooking.booking.booking;

import com.worldcup.hotelbooking.booking.bookingroom.BookingRoomRequestDto;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

    @Data
    public class BookingRequestDto {

        @NotNull(message = "Hotel ID cannot be null")
        private Long hotelId;

        private Long matchId;

        @NotNull(message = "Check-in date cannot be null")
        private LocalDate checkInDate;

        @NotNull(message = "Check-out date cannot be null")
        private LocalDate checkOutDate;

        @Min(1)
        private int numberOfGuests;
        private int numberOfAdults;
        private int numberOfChildren;

        private List<BookingRoomRequestDto> rooms;
    }


