package com.beautytextile.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OfferRequest(
        String offerName,
        String offerScope,       // GLOBAL | CATEGORY | PRODUCT
        String discountType,     // PERCENTAGE | FIXED
        BigDecimal discountValue,
        String categoryName,     // for CATEGORY scope
        Long productId,          // for PRODUCT scope
        LocalDate startDate,
        LocalDate endDate,
        boolean active
) {}
