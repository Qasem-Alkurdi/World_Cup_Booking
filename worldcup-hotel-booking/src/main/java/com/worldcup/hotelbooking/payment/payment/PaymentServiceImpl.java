package com.worldcup.hotelbooking.payment.payment;

import com.worldcup.hotelbooking.booking.booking.Booking;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Transactional
public class PaymentServiceImpl implements PaymentService {
    private final PaymentRepository paymentRepository;

    PaymentServiceImpl(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Override
    public Payment createPaymentIntent(Booking booking, Payment.PaymentMethod paymentMethod) {
        if (paymentRepository.existsByBooking_Id(booking.getId())) {
            throw new IllegalStateException("Payment already exists for booking id: " + booking.getId());
        }

        if (booking.getStatus() != Booking.BookingStatus.PENDING) {
            throw new IllegalStateException("Payment intent can only be created for pending bookings.");
        }

        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setPaymentMethod(paymentMethod);
        payment.setAmount(booking.getTotalPrice());
        payment.setCurrency("USD");
        payment.setStatus(Payment.PaymentStatus.PENDING);
        payment.setPaymentIntentReference(generatePaymentIntentRef());
        payment.setTransactionReference(null);
        payment.setPaidAt(null);
        payment.setFailedAt(null);
        payment.setFailureReason(null);
        payment.setRefundAmount(BigDecimal.ZERO);
        payment.setRefundReason(null);
        payment.setRefundedAt(null);

        return paymentRepository.save(payment);
    }

    @Override
    public Payment simulateSuccess(Long paymentId) {
        Payment payment = getPaymentById(paymentId);

        if (payment.getStatus() != Payment.PaymentStatus.PENDING && payment.getStatus() != Payment.PaymentStatus.FAILED) {
            throw new IllegalStateException("Only pending or failed payments can be completed.");
        }

        Booking booking = payment.getBooking();
        if (booking.getStatus() == Booking.BookingStatus.CANCELLED) {
            throw new IllegalStateException("Cannot complete payment for a cancelled booking.");
        }

        payment.setStatus(Payment.PaymentStatus.COMPLETED);
        payment.setTransactionReference(generateTransactionRef());
        payment.setPaidAt(LocalDateTime.now());
        payment.setFailedAt(null);
        payment.setFailureReason(null);

        booking.setStatus(Booking.BookingStatus.CONFIRMED);
        booking.setConfirmedAt(LocalDateTime.now());

        return paymentRepository.save(payment);
    }

    @Override
    public Payment simulateFailure(Long paymentId, String failureReason) {
        Payment payment = getPaymentById(paymentId);

        if (payment.getStatus() != Payment.PaymentStatus.PENDING) {
            throw new IllegalStateException("Only pending payments can be marked as failed.");
        }

        payment.setStatus(Payment.PaymentStatus.FAILED);
        payment.setFailureReason((failureReason == null || failureReason.isBlank())
                ? "Mock payment failure"
                : failureReason.trim());
        payment.setFailedAt(LocalDateTime.now());
        payment.setPaidAt(null);
        payment.setTransactionReference(null);

        return paymentRepository.save(payment);
    }

    @Override
    public Payment refundPayment(Long paymentId, BigDecimal refundAmount, String refundReason) {
        Payment payment = getPaymentById(paymentId);
        return refundCompletedPayment(payment, refundAmount, refundReason);
    }

    @Override
    public Payment refundPaymentForBooking(Long bookingId, BigDecimal refundAmount, String refundReason) {
        Payment payment = getPaymentByBookingId(bookingId);
        return refundCompletedPayment(payment, refundAmount, refundReason);
    }

    @Override
    @Transactional(readOnly = true)
    public Payment getPaymentById(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found with id: " + paymentId));
    }

    @Override
    @Transactional(readOnly = true)
    public Payment getPaymentByBookingId(Long bookingId) {
        return paymentRepository.findByBooking_Id(bookingId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found for booking id: " + bookingId));
    }

    private Payment refundCompletedPayment(Payment payment, BigDecimal refundAmount, String refundReason) {
        BigDecimal normalizedRefund = refundAmount == null ? BigDecimal.ZERO : refundAmount.max(BigDecimal.ZERO);

        if (payment.getStatus() != Payment.PaymentStatus.COMPLETED) {
            throw new IllegalStateException("Only completed payments can be refunded.");
        }
        if (normalizedRefund.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Refund amount must be greater than zero.");
        }
        if (normalizedRefund.compareTo(payment.getAmount()) > 0) {
            throw new IllegalArgumentException("Refund amount cannot exceed paid amount.");
        }

        payment.setStatus(Payment.PaymentStatus.REFUNDED);
        payment.setRefundAmount(normalizedRefund);
        payment.setRefundReason((refundReason == null || refundReason.isBlank())
                ? "Refund processed"
                : refundReason.trim());
        payment.setRefundedAt(LocalDateTime.now());

        return paymentRepository.save(payment);
    }

    private String generatePaymentIntentRef() {
        return "PI-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String generateTransactionRef() {
        return "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
