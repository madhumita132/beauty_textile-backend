package com.beautytextile.dto;

import java.math.BigDecimal;

/** Request to set category-level discount. */
public record CategoryDiscountRequest(
        String categoryName,
        String discountType,   // NONE | PERCENTAGE | FIXED
        BigDecimal discountValue
) {}
