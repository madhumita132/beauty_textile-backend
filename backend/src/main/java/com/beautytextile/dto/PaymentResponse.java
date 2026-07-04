package com.beautytextile.dto;

import java.math.BigDecimal;

/** Razorpay order details returned to the frontend checkout. */
public record PaymentResponse(
        String razorpayOrderId,
        String keyId,
        BigDecimal amount,
        String currency,
        boolean mock
) {}
