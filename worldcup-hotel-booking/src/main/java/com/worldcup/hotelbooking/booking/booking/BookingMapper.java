package com.worldcup.hotelbooking.booking.booking;

import com.worldcup.hotelbooking.booking.bookingroom.BookingRoomMapper;
import com.worldcup.hotelbooking.booking.bookingroom.BookingRoomRequestDto;
import com.worldcup.hotelbooking.catalog.hotel.Hotel;
import com.worldcup.hotelbooking.catalog.hotel.HotelRepository;
import com.worldcup.hotelbooking.user.user.User;
import com.worldcup.hotelbooking.user.user.UserRepository;

import java.math.BigDecimal;

public class BookingMapper {

    public static Booking toEntity(
            BookingRequestDto dto,
            User user,
            Hotel hotel) {

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setHotel(hotel);
        booking.setCheckInDate(dto.getCheckInDate());
        booking.setCheckOutDate(dto.getCheckOutDate());
        booking.setStatus(Booking.BookingStatus.PENDING);

        return booking;
    }

    public static BookingResponseDto toDto(Booking booking) {
        BookingResponseDto dto = new BookingResponseDto(
                booking.getBookingReference(),
                booking.getStatus(),
                booking.getCheckInDate(),
                booking.getCheckOutDate(),
                booking.getTotalPrice(),
                booking.getBookingRooms().stream()
                        .map(BookingRoomMapper::toDto)
                        .toList()
        );

        return dto;
    }

}
