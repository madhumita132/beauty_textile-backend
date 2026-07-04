package com.beautytextile.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A time-bound promotional offer (Diwali Sale, Pongal Sale, etc.).
 *
 * offer_scope: GLOBAL | CATEGORY | PRODUCT
 * discount_type: PERCENTAGE | FIXED
 */
@Entity
@Table(name = "offers")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Offer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "offer_name", nullable = false, length = 100)
    private String offerName;

    /** GLOBAL, CATEGORY, PRODUCT */
    @Column(name = "offer_scope", nullable = false, length = 20)
    private String offerScope;

    /** PERCENTAGE or FIXED */
    @Column(name = "discount_type", nullable = false, length = 15)
    private String discountType;

    @Column(name = "discount_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountValue;

    /** Null when scope=GLOBAL */
    @Column(name = "category_name", length = 80)
    private String categoryName;

    /** Null when scope≠PRODUCT */
    @Column(name = "product_id")
    private Long productId;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    /** Manual override – false = always respect dates */
    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() { if (createdAt == null) createdAt = LocalDateTime.now(); }

    /** True when offer is active and current date is within the date range. */
    public boolean isCurrentlyActive() {
        if (!active) return false;
        LocalDate today = LocalDate.now();
        return !today.isBefore(startDate) && !today.isAfter(endDate);
    }
}
