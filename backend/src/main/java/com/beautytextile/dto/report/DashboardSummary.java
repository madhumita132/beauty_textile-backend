package com.beautytextile.dto.report;

import java.math.BigDecimal;
import java.util.List;

/** Admin dashboard summary for the current month. */
public record DashboardSummary(
        BigDecimal currentMonthRevenue,
        long totalOrders,
        long totalProductsSold,
        long lowStockCount,
        List<ProductSales> topProducts,
        List<CategorySales> categorySales,
        List<DailySales> dailySales
) {}
