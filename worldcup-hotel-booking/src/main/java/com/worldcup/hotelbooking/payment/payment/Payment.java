package com.worldcup.hotelbooking.payment.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.worldcup.hotelbooking.booking.booking.Booking;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Data
@Table(
    name = "payments",
    indexes = {
        @Index(name = "idx_payment_booking", columnList = "booking_id"),
        @Index(name = "idx_payment_status", columnList = "status"),
        @Index(name = "idx_payment_transaction", columnList = "transaction_reference"),
        @Index(name = "idx_payment_intent", columnList = "payment_intent_reference")
    }
)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "payment_intent_reference", unique = true, length = 100)
    private String paymentIntentReference;

    @Column(name = "transaction_reference", unique = true, length = 100)
    private String transactionReference;

    @NotNull
    @Column(name = "amount")
    private BigDecimal amount;

    @Column(name = "currency")
    private String currency = "USD";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "refund_amount", precision = 10, scale = 2)
    private BigDecimal refundAmount = BigDecimal.ZERO;

    @Lob
    @Column(name = "refund_reason")
    private String refundReason;

    @Lob
    @Column(name = "failure_reason")
    private String failureReason;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "failed_at")
    private LocalDateTime failedAt;

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;

    public Payment() {}

    public Payment(Booking booking, String paymentIntentReference, String transactionReference, BigDecimal amount,
                   PaymentMethod paymentMethod, PaymentStatus status, BigDecimal refundAmount, String refundReason,
                   String failureReason, LocalDateTime paidAt, LocalDateTime failedAt, LocalDateTime refundedAt) {
        this.booking = booking;
        this.paymentIntentReference = paymentIntentReference;
        this.transactionReference = transactionReference;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.status = status;
        this.refundAmount = refundAmount;
        this.refundReason = refundReason;
        this.failureReason = failureReason;
        this.paidAt = paidAt;
        this.failedAt = failedAt;
        this.refundedAt = refundedAt;
    }

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    public enum PaymentStatus {
        PENDING,
        COMPLETED,
        FAILED,
        CANCELLED,
        REFUNDED
    }

    public enum PaymentMethod {
        CREDIT_CARD,
        DEBIT_CARD,
        PAYPAL,
        CASH

    }
}