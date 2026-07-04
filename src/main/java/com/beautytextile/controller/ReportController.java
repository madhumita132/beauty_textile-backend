package com.beautytextile.controller;

import com.beautytextile.dto.report.*;
import com.beautytextile.service.ReportService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/monthly")
    public MonthlyReport monthly(@RequestParam String month) {
        return reportService.monthly(month);
    }

    @GetMapping("/daily")
    public DailySales daily(@RequestParam String date) {
        return reportService.daily(date);
    }

    @GetMapping("/product-wise")
    public List<ProductSales> productWise(@RequestParam(required = false) String from,
                                          @RequestParam(required = false) String to) {
        LocalDate f = from == null ? LocalDate.now().withDayOfMonth(1) : LocalDate.parse(from);
        LocalDate t = to == null ? LocalDate.now() : LocalDate.parse(to);
        return reportService.productWise(f, t);
    }

    @GetMapping("/category-wise")
    public List<CategorySales> categoryWise(@RequestParam(required = false) String from,
                                            @RequestParam(required = false) String to) {
        LocalDate f = from == null ? LocalDate.now().withDayOfMonth(1) : LocalDate.parse(from);
        LocalDate t = to == null ? LocalDate.now() : LocalDate.parse(to);
        return reportService.categoryWise(f, t);
    }

    @GetMapping("/dashboard")
    public DashboardSummary dashboard() {
        return reportService.dashboardCurrentMonth();
    }
}
