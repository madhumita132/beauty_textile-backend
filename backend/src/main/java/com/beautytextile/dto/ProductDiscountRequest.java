package com.beautytextile.dto;

import java.math.BigDecimal;
import java.util.List;

/** Request to set product-level discount. */
public record ProductDiscountRequest(
        List<Long> productIds,
        String discountType,      // NONE | PERCENTAGE | FIXED
        BigDecimal discountValue
) {}
