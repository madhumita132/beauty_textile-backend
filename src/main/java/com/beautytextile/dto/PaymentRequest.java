package com.beautytextile.dto;

import java.math.BigDecimal;

/** Request to create a Razorpay order (amount in rupees). */
public record PaymentRequest(BigDecimal amount, Long orderId) {}
