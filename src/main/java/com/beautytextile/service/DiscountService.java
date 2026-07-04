package com.beautytextile.service;

import com.beautytextile.dto.*;
import com.beautytextile.model.Category;
import com.beautytextile.model.Offer;
import com.beautytextile.model.Product;
import com.beautytextile.repository.CategoryRepository;
import com.beautytextile.repository.OfferRepository;
import com.beautytextile.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

/**
 * Central discount engine.
 *
 * Priority (highest wins):
 *   1. Product-level discount (if set)
 *   2. Active Offer targeting this product (PRODUCT scope)
 *   3. Active Offer targeting this product's category (CATEGORY scope)
 *   4. Category-level discount (stored on Category entity)
 *   5. Active GLOBAL offer
 *   6. No discount
 */
@Service
public class DiscountService {

    private final ProductRepository   productRepo;
    private final CategoryRepository  categoryRepo;
    private final OfferRepository     offerRepo;

    public DiscountService(ProductRepository productRepo,
                           CategoryRepository categoryRepo,
                           OfferRepository offerRepo) {
        this.productRepo  = productRepo;
        this.categoryRepo = categoryRepo;
        this.offerRepo    = offerRepo;
    }

    // ── Compute discount amount for a single product ────────────────────────

    public BigDecimal computeDiscount(Product p) {
        List<Offer> activeOffers = offerRepo.findActiveOffers(LocalDate.now());

        // 1. Product-level discount
        if (hasDiscount(p.getDiscountType(), p.getDiscountValue())) {
            return calcAmount(p.getPrice(), p.getDiscountType(), p.getDiscountValue());
        }

        // 2. Active offer – product scope
        for (Offer o : activeOffers) {
            if ("PRODUCT".equals(o.getOfferScope()) && p.getId().equals(o.getProductId())) {
                return calcAmount(p.getPrice(), o.getDiscountType(), o.getDiscountValue());
            }
        }

        // 3. Active offer – category scope
        for (Offer o : activeOffers) {
            if ("CATEGORY".equals(o.getOfferScope())
                    && p.getCategory() != null
                    && p.getCategory().equalsIgnoreCase(o.getCategoryName())) {
                return calcAmount(p.getPrice(), o.getDiscountType(), o.getDiscountValue());
            }
        }

        // 4. Category-level discount
        Category cat = categoryRepo.findByNameIgnoreCase(p.getCategory()).orElse(null);
        if (cat != null && hasDiscount(cat.getDiscountType(), cat.getDiscountValue())) {
            return calcAmount(p.getPrice(), cat.getDiscountType(), cat.getDiscountValue());
        }

        // 5. Active global offer
        for (Offer o : activeOffers) {
            if ("GLOBAL".equals(o.getOfferScope())) {
                return calcAmount(p.getPrice(), o.getDiscountType(), o.getDiscountValue());
            }
        }

        return BigDecimal.ZERO;
    }

    public PricedProductResponse priced(Product p) {
        return new PricedProductResponse(p, computeDiscount(p));
    }

    // ── Apply product-level discounts ────────────────────────────────────────

    @Transactional
    public void applyProductDiscount(ProductDiscountRequest req) {
        for (Long pid : req.productIds()) {
            productRepo.findById(pid).ifPresent(p -> {
                p.setDiscountType(req.discountType() == null ? "NONE" : req.discountType());
                p.setDiscountValue(req.discountValue() == null ? BigDecimal.ZERO : req.discountValue());
                productRepo.save(p);
            });
        }
    }

    // ── Apply category-level discounts ───────────────────────────────────────

    @Transactional
    public void applyCategoryDiscount(CategoryDiscountRequest req) {
        categoryRepo.findByNameIgnoreCase(req.categoryName()).ifPresent(cat -> {
            cat.setDiscountType(req.discountType() == null ? "NONE" : req.discountType());
            cat.setDiscountValue(req.discountValue() == null ? BigDecimal.ZERO : req.discountValue());
            categoryRepo.save(cat);
        });
    }

    // ── Offer CRUD ───────────────────────────────────────────────────────────

    @Transactional
    public Offer createOffer(OfferRequest req) {
        Offer o = Offer.builder()
                .offerName(req.offerName())
                .offerScope(req.offerScope())
                .discountType(req.discountType())
                .discountValue(req.discountValue())
                .categoryName(req.categoryName())
                .productId(req.productId())
                .startDate(req.startDate())
                .endDate(req.endDate())
                .active(req.active())
                .build();
        return offerRepo.save(o);
    }

    @Transactional
    public Offer updateOffer(Long id, OfferRequest req) {
        Offer o = offerRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Offer not found: " + id));
        o.setOfferName(req.offerName());
        o.setOfferScope(req.offerScope());
        o.setDiscountType(req.discountType());
        o.setDiscountValue(req.discountValue());
        o.setCategoryName(req.categoryName());
        o.setProductId(req.productId());
        o.setStartDate(req.startDate());
        o.setEndDate(req.endDate());
        o.setActive(req.active());
        return offerRepo.save(o);
    }

    @Transactional
    public void toggleOffer(Long id) {
        offerRepo.findById(id).ifPresent(o -> {
            o.setActive(!o.isActive());
            offerRepo.save(o);
        });
    }

    @Transactional
    public void deleteOffer(Long id) { offerRepo.deleteById(id); }

    public List<Offer> getAllOffers() { return offerRepo.findAll(); }

    public List<Offer> getActiveOffers() {
        return offerRepo.findActiveOffers(LocalDate.now());
    }

    // ── Billing discount ─────────────────────────────────────────────────────

    public BigDecimal computeBillingDiscount(BigDecimal subtotal, String type, BigDecimal value) {
        if (value == null || value.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return calcAmount(subtotal, type, value);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private boolean hasDiscount(String type, BigDecimal value) {
        return type != null && !"NONE".equalsIgnoreCase(type)
                && value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    private BigDecimal calcAmount(BigDecimal price, String type, BigDecimal value) {
        if ("PERCENTAGE".equalsIgnoreCase(type)) {
            return price.multiply(value).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }
        if ("FIXED".equalsIgnoreCase(type)) {
            return value.min(price);  // can't discount more than the price
        }
        return BigDecimal.ZERO;
    }
}
