package com.beautytextile.dto.report;

import java.math.BigDecimal;

public record ProductSales(
        Long productId,
        String productName,
        long quantitySold,
        BigDecimal revenue
) {}
