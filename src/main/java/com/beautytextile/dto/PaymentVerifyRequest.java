package com.beautytextile.dto;

/** Confirm payment (called after Razorpay checkout success). */
public record PaymentVerifyRequest(
        Long orderId,
        String razorpayOrderId,
        String razorpayPaymentId,
        String razorpaySignature
) {}
