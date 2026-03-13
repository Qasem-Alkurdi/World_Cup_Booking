package com.worldcup.hotelbooking.payment.payment;

import com.worldcup.hotelbooking.booking.booking.Booking;
import com.worldcup.hotelbooking.booking.booking.BookingServiceImp;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/payments")
public class PaymentController {
    private final PaymentServiceImpl paymentService;
    private final BookingServiceImp bookingService;

    PaymentController(PaymentServiceImpl paymentService, BookingServiceImp bookingService) {
        this.paymentService = paymentService;
        this.bookingService = bookingService;
    }

    @PostMapping("/intent")
    public ResponseEntity<PaymentResponseDto> createPaymentIntent(
            @Valid @RequestBody PaymentRequestDto paymentRequest,
            UriComponentsBuilder uriBuilder) {
        Booking booking = bookingService.getBookingById(paymentRequest.getBookingId());
        Payment response = paymentService.createPaymentIntent(booking, paymentRequest.getPaymentMethod());

        return ResponseEntity.created(uriBuilder.path("/payments/{id}").buildAndExpand(response.getId()).toUri())
                .body(PaymentMapper.toPaymentResponseDto(response));
    }

    @GetMapping("/{id}")
    public PaymentResponseDto getPaymentById(@PathVariable Long id) {
        return PaymentMapper.toPaymentResponseDto(paymentService.getPaymentById(id));
    }

    @GetMapping("/booking/{bookingId}")
    public PaymentResponseDto getPaymentByBookingId(@PathVariable Long bookingId) {
        return PaymentMapper.toPaymentResponseDto(paymentService.getPaymentByBookingId(bookingId));
    }

    @PostMapping("/{id}/simulate-success")
    public PaymentResponseDto simulateSuccess(@PathVariable Long id) {
        return PaymentMapper.toPaymentResponseDto(paymentService.simulateSuccess(id));
    }

    @PostMapping("/{id}/simulate-failure")
    public PaymentResponseDto simulateFailure(
            @PathVariable Long id,
            @RequestBody(required = false) PaymentFailureRequestDto request) {
        String reason = request == null ? null : request.getFailureReason();
        return PaymentMapper.toPaymentResponseDto(paymentService.simulateFailure(id, reason));
    }

    @PostMapping("/{id}/refund")
    public PaymentResponseDto refundPayment(
            @PathVariable Long id,
            @Valid @RequestBody PaymentRefundRequestDto request) {
        return PaymentMapper.toPaymentResponseDto(
                paymentService.refundPayment(id, request.getRefundAmount(), request.getRefundReason())
        );
    }
}
