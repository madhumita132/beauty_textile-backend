package com.beautytextile.dto.report;

import java.math.BigDecimal;

public record CategorySales(
        String category,
        long quantitySold,
        BigDecimal revenue
) {}
