package com.beautytextile.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.math.BigDecimal;
import java.util.List;

public record BillingRequest(
        String customerName,
        String phone,
        String paymentMode,           // CASH, UPI, CARD
        boolean sendWhatsApp,
        String discountType,          // NONE | PERCENTAGE | FIXED
        BigDecimal discountValue,
        @NotEmpty @Valid List<ItemRequest> items
) {}
