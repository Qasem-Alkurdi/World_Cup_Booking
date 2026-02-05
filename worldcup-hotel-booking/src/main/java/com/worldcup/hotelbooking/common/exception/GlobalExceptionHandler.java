package com.worldcup.hotelbooking.common.exception;

import com.worldcup.hotelbooking.booking.booking.BookingNotFoundException;
import com.worldcup.hotelbooking.booking.bookingroom.BookingRoomNotFoundException;
import com.worldcup.hotelbooking.payment.payment.PaymentNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.Instant;

@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(PaymentNotFoundException.class)
    public ResponseEntity<ApiError> handlePaymentNotFound(PaymentNotFoundException ex,  HttpServletRequest request) {
        ApiError body = new ApiError(
                Instant.now().toString(),
                404,
                "Not Found",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(BookingNotFoundException.class)
    public ResponseEntity<ApiError> handleBookingNotFound(BookingNotFoundException ex,  HttpServletRequest request) {
        ApiError body = new ApiError(
                Instant.now().toString(),
                404,
                "Not Found",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(BookingRoomNotFoundException.class)
    public ResponseEntity<ApiError> handleBookingRoomNotFound(BookingRoomNotFoundException ex,  HttpServletRequest request) {
        ApiError body = new ApiError(
                Instant.now().toString(),
                404,
                "Not Found",
                ex.getMessage(),
                request.getRequestURI()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

}
