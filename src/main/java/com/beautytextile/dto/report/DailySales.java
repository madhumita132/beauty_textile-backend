package com.beautytextile.dto.report;

import java.math.BigDecimal;

public record DailySales(
        String date,                  // YYYY-MM-DD
        BigDecimal revenue,
        long orders
) {}
