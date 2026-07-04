package com.beautytextile.dto.report;

import java.math.BigDecimal;
import java.util.List;

/** Aggregated monthly report combining online orders + shop billing. */
public record MonthlyReport(
        String month,                 // YYYY-MM
        BigDecimal totalSales,
        long totalOrders,
        long totalProductsSold,
        List<ProductSales> topProducts,
        List<ProductSales> lowProducts
) {}
