package com.worldcup.hotelbooking.booking.booking;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.worldcup.hotelbooking.booking.bookingroom.BookingRoom;
import com.worldcup.hotelbooking.catalog.hotel.Hotel;
import com.worldcup.hotelbooking.user.user.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@Entity
public class Booking {
@Id @GeneratedValue private Long id;
@NotNull
private String bookingReference;

private int matchId;

private LocalDate checkInDate;
private LocalDate checkOutDate;

@NotNull
private int numberOfGuests;
private int numberOfAdults;
private int numberOfChildren;

@NotNull
private double totalPrice;

@NotNull
private String status="PENDING";
private LocalDate createdAt;
private LocalDate updatedAt;
private LocalDate confirmedAt;

private String cancelReason;
private String cancelledBy;
private LocalDate cancelledAt;

    @ManyToOne()
    @JoinColumn(name = "user_id", nullable = false)
    @JsonBackReference
    private User user;

    @ManyToOne()
    @JoinColumn(name = "hotel_id", nullable = false)
    @JsonBackReference
    private Hotel hotel;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL)
    @JsonManagedReference
    private List<BookingRoom> bookingRooms = new ArrayList<>();


Booking(String bookingReference, int matchId, LocalDate checkInDate,LocalDate checkOutDate, int numberOfGuests, int numberOfAdults, int numberOfChildren, double totalPrice, String status, User user, Hotel hotel) {
    this.bookingReference = bookingReference;
    this.matchId = matchId;
    this.checkInDate = checkInDate;
    this.checkOutDate = checkOutDate;
    this.numberOfGuests = numberOfGuests;
    this.numberOfAdults = numberOfAdults;
    this.numberOfChildren = numberOfChildren;
    this.totalPrice = totalPrice;
    this.status = status;
    this.user = user;
    this.hotel = hotel;
}


public void addBookingRoom(BookingRoom bookingRoom) {
    bookingRooms.add(bookingRoom);
    bookingRoom.setBooking(this);
}

public void removeBookingRoom(BookingRoom bookingRoom) {
    bookingRooms.remove(bookingRoom);
    bookingRoom.setBooking(null);
}


    public Booking() {

    }


}
