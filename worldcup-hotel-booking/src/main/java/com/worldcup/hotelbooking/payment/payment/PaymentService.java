package com.worldcup.hotelbooking.payment.payment;

import com.worldcup.hotelbooking.booking.booking.Booking;

import java.math.BigDecimal;

public interface PaymentService {
    Payment createPaymentIntent(Booking booking, Payment.PaymentMethod paymentMethod);

    Payment simulateSuccess(Long paymentId);

    Payment simulateFailure(Long paymentId, String failureReason);

    Payment refundPayment(Long paymentId, BigDecimal refundAmount, String refundReason);

    Payment refundPaymentForBooking(Long bookingId, BigDecimal refundAmount, String refundReason);

    Payment getPaymentById(Long paymentId);

    Payment getPaymentByBookingId(Long bookingId);
}
