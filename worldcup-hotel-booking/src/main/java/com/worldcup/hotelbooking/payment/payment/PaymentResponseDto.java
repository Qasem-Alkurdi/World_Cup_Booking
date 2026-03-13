package com.worldcup.hotelbooking.payment.payment;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PaymentResponseDto {

    private Long paymentId;
    private String bookingReference;

    private BigDecimal amount;
    private String currency;

    private Payment.PaymentMethod paymentMethod;
    private Payment.PaymentStatus status;

    private String paymentIntentReference;
    private String transactionReference;

    private BigDecimal refundAmount;
    private String refundReason;
    private String failureReason;

    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
    private LocalDateTime failedAt;
    private LocalDateTime refundedAt;
}
