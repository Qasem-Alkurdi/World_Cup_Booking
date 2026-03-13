package com.worldcup.hotelbooking.payment.payment;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaymentRequestDto {

    @NotNull
    private Long bookingId;

    @NotNull(message = "Payment method is required")
    private Payment.PaymentMethod paymentMethod;
}
