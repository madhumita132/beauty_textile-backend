package com.beautytextile.controller;

import com.beautytextile.dto.OfferRequest;
import com.beautytextile.model.Offer;
import com.beautytextile.service.DiscountService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * CRUD for festival/promotional offers.
 * GET  /api/offers       — public (customer sees active offers banner)
 * All write ops require JWT.
 */
@RestController
@RequestMapping("/api/offers")
public class OfferController {

    private final DiscountService discountService;

    public OfferController(DiscountService discountService) {
        this.discountService = discountService;
    }

    @GetMapping
    public List<Offer> all() { return discountService.getAllOffers(); }

    @GetMapping("/active")
    public List<Offer> active() { return discountService.getActiveOffers(); }

    @PostMapping
    public Offer create(@RequestBody OfferRequest req) {
        return discountService.createOffer(req);
    }

    @PutMapping("/{id}")
    public Offer update(@PathVariable Long id, @RequestBody OfferRequest req) {
        return discountService.updateOffer(id, req);
    }

    @PatchMapping("/{id}/toggle")
    public Map<String, Object> toggle(@PathVariable Long id) {
        discountService.toggleOffer(id);
        return Map.of("success", true);
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        discountService.deleteOffer(id);
        return Map.of("deleted", true);
    }
}
