package com.beautytextile.controller;

import com.beautytextile.dto.CategoryDiscountRequest;
import com.beautytextile.dto.ProductDiscountRequest;
import com.beautytextile.service.DiscountService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Admin endpoints for product and category level discounts.
 * All require JWT authentication (covered by SecurityConfig /api/admin/**).
 */
@RestController
@RequestMapping("/api/admin/discounts")
public class DiscountController {

    private final DiscountService discountService;

    public DiscountController(DiscountService discountService) {
        this.discountService = discountService;
    }

    /** Apply discount to one or more products. */
    @PostMapping("/products")
    public Map<String, Object> applyProductDiscount(@RequestBody ProductDiscountRequest req) {
        discountService.applyProductDiscount(req);
        return Map.of("success", true);
    }

    /** Apply discount to an entire category. */
    @PostMapping("/category")
    public Map<String, Object> applyCategoryDiscount(@RequestBody CategoryDiscountRequest req) {
        discountService.applyCategoryDiscount(req);
        return Map.of("success", true);
    }

}
