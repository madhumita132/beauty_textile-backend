package com.beautytextile.service;

import com.beautytextile.dto.report.*;
import com.beautytextile.model.Billing;
import com.beautytextile.model.BillingItem;
import com.beautytextile.model.Order;
import com.beautytextile.model.OrderItem;
import com.beautytextile.model.Product;
import com.beautytextile.repository.BillingRepository;
import com.beautytextile.repository.OrderRepository;
import com.beautytextile.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private final OrderRepository orderRepo;
    private final BillingRepository billingRepo;
    private final ProductRepository productRepo;

    public ReportService(OrderRepository orderRepo,
                         BillingRepository billingRepo,
                         ProductRepository productRepo) {
        this.orderRepo = orderRepo;
        this.billingRepo = billingRepo;
        this.productRepo = productRepo;
    }

    public MonthlyReport monthly(String yyyyMm) {
        YearMonth ym = YearMonth.parse(yyyyMm);
        LocalDateTime start = ym.atDay(1).atStartOfDay();
        LocalDateTime end = ym.atEndOfMonth().atTime(23, 59, 59);

        List<Order> orders = orderRepo.findByCreatedAtBetween(start, end);
        List<Billing> bills = billingRepo.findByCreatedAtBetween(start, end);

        BigDecimal totalSales = sumOrderTotal(orders).add(sumBillingTotal(bills));
        long totalOrders = orders.size() + bills.size();

        Map<Long, ProductAccumulator> acc = aggregateProductSales(orders, bills);
        long totalProductsSold = acc.values().stream().mapToLong(a -> a.qty).sum();

        List<ProductSales> ranked = acc.entrySet().stream()
                .map(e -> new ProductSales(e.getKey(), e.getValue().name, e.getValue().qty, e.getValue().revenue))
                .sorted(Comparator.comparing(ProductSales::quantitySold).reversed())
                .toList();

        List<ProductSales> top = ranked.stream().limit(5).toList();
        List<ProductSales> low = ranked.stream()
                .sorted(Comparator.comparing(ProductSales::quantitySold))
                .limit(5)
                .toList();

        return new MonthlyReport(yyyyMm, totalSales, totalOrders, totalProductsSold, top, low);
    }

    public DailySales daily(String yyyyMmDd) {
        LocalDate date = LocalDate.parse(yyyyMmDd);
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(23, 59, 59);

        List<Order> orders = orderRepo.findByCreatedAtBetween(start, end);
        List<Billing> bills = billingRepo.findByCreatedAtBetween(start, end);

        BigDecimal revenue = sumOrderTotal(orders).add(sumBillingTotal(bills));
        long count = orders.size() + bills.size();
        return new DailySales(yyyyMmDd, revenue, count);
    }

    public List<ProductSales> productWise(LocalDate from, LocalDate to) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.atTime(23, 59, 59);

        Map<Long, ProductAccumulator> acc = aggregateProductSales(
                orderRepo.findByCreatedAtBetween(start, end),
                billingRepo.findByCreatedAtBetween(start, end)
        );

        return acc.entrySet().stream()
                .map(e -> new ProductSales(e.getKey(), e.getValue().name, e.getValue().qty, e.getValue().revenue))
                .sorted(Comparator.comparing(ProductSales::quantitySold).reversed())
                .toList();
    }

    public List<CategorySales> categoryWise(LocalDate from, LocalDate to) {
        Map<String, CategoryAccumulator> byCategory = new HashMap<>();

        for (ProductSales ps : productWise(from, to)) {
            Product p = productRepo.findById(ps.productId()).orElse(null);
            String category = p == null ? "Unknown" : p.getCategory();
            CategoryAccumulator acc = byCategory.computeIfAbsent(category, c -> new CategoryAccumulator());
            acc.qty += ps.quantitySold();
            acc.revenue = acc.revenue.add(ps.revenue());
        }

        return byCategory.entrySet().stream()
                .map(e -> new CategorySales(e.getKey(), e.getValue().qty, e.getValue().revenue))
                .sorted(Comparator.comparing(CategorySales::revenue).reversed())
                .toList();
    }

    public DashboardSummary dashboardCurrentMonth() {
        YearMonth ym = YearMonth.now();
        LocalDateTime start = ym.atDay(1).atStartOfDay();
        LocalDateTime end = ym.atEndOfMonth().atTime(23, 59, 59);

        // Single DB query per entity for the whole month
        List<Order> orders = orderRepo.findByCreatedAtBetween(start, end);
        List<Billing> bills = billingRepo.findByCreatedAtBetween(start, end);

        BigDecimal totalSales = sumOrderTotal(orders).add(sumBillingTotal(bills));
        long totalOrders = orders.size() + bills.size();
        Map<Long, ProductAccumulator> acc = aggregateProductSales(orders, bills);
        long totalProductsSold = acc.values().stream().mapToLong(a -> a.qty).sum();

        List<ProductSales> ranked = acc.entrySet().stream()
                .map(e -> new ProductSales(e.getKey(), e.getValue().name, e.getValue().qty, e.getValue().revenue))
                .sorted(Comparator.comparing(ProductSales::quantitySold).reversed())
                .toList();
        List<ProductSales> top = ranked.stream().limit(5).toList();

        // Category breakdown (in-memory, no extra queries)
        Map<String, CategoryAccumulator> byCat = new HashMap<>();
        for (ProductSales ps : ranked) {
            Product p = productRepo.findById(ps.productId()).orElse(null);
            String cat = p == null ? "Unknown" : p.getCategory();
            CategoryAccumulator ca = byCat.computeIfAbsent(cat, c -> new CategoryAccumulator());
            ca.qty += ps.quantitySold();
            ca.revenue = ca.revenue.add(ps.revenue());
        }
        List<CategorySales> category = byCat.entrySet().stream()
                .map(e -> new CategorySales(e.getKey(), e.getValue().qty, e.getValue().revenue))
                .sorted(Comparator.comparing(CategorySales::revenue).reversed())
                .toList();

        // Daily breakdown — group already-fetched orders+bills by date (no extra DB calls)
        Map<String, BigDecimal[]> byDay = new LinkedHashMap<>();
        for (int d = 1; d <= ym.lengthOfMonth(); d++) {
            byDay.put(ym.atDay(d).toString(), new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
        }
        for (Order o : orders) {
            String key = o.getCreatedAt().toLocalDate().toString();
            byDay.computeIfPresent(key, (k, v) -> { v[0] = v[0].add(o.getTotalAmount()); v[1] = v[1].add(BigDecimal.ONE); return v; });
        }
        for (Billing b : bills) {
            String key = b.getCreatedAt().toLocalDate().toString();
            byDay.computeIfPresent(key, (k, v) -> { v[0] = v[0].add(b.getTotalAmount()); v[1] = v[1].add(BigDecimal.ONE); return v; });
        }
        List<DailySales> daily = byDay.entrySet().stream()
                .map(e -> new DailySales(e.getKey(), e.getValue()[0], e.getValue()[1].longValue()))
                .toList();

        long lowStock = productRepo.findByStockLessThan(5).size();

        return new DashboardSummary(totalSales, totalOrders, totalProductsSold, lowStock, top, category, daily);
    }

    private Map<Long, ProductAccumulator> aggregateProductSales(List<Order> orders, List<Billing> bills) {
        Map<Long, ProductAccumulator> map = new HashMap<>();

        for (Order o : orders) {
            for (OrderItem i : o.getItems()) {
                ProductAccumulator a = map.computeIfAbsent(i.getProductId(), x -> new ProductAccumulator(i.getProductName()));
                a.qty += i.getQuantity();
                a.revenue = a.revenue.add(i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())));
            }
        }
        for (Billing b : bills) {
            for (BillingItem i : b.getItems()) {
                ProductAccumulator a = map.computeIfAbsent(i.getProductId(), x -> new ProductAccumulator(i.getProductName()));
                a.qty += i.getQuantity();
                a.revenue = a.revenue.add(i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())));
            }
        }
        return map;
    }

    private BigDecimal sumOrderTotal(List<Order> orders) {
        return orders.stream().map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumBillingTotal(List<Billing> bills) {
        return bills.stream().map(Billing::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static class ProductAccumulator {
        String name;
        long qty;
        BigDecimal revenue = BigDecimal.ZERO;

        ProductAccumulator(String name) {
            this.name = name;
        }
    }

    private static class CategoryAccumulator {
        long qty;
        BigDecimal revenue = BigDecimal.ZERO;
    }
}
