package com.worldcup.hotelbooking.catalog.roomtype;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.worldcup.hotelbooking.booking.bookingroom.BookingRoom;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

import java.util.List;

@Entity
public class RoomType {

    @Id
    @GeneratedValue
    private Long id;
    private String name;

@OneToMany(mappedBy = "roomType")
@JsonManagedReference
    private List<BookingRoom> bookingRooms;

public void addBookingRoom(BookingRoom bookingRoom) {
    this.bookingRooms.add(bookingRoom);
    bookingRoom.setRoomType(this);
}

    public String getName() {
        return name;
    }
}
