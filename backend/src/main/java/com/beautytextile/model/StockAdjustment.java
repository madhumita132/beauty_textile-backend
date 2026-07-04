package com.beautytextile.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Immutable audit record for every stock movement.
 * Captured on: purchase, sale, return, exchange, manual adjustment, bulk import.
 */
@Entity
@Table(name = "stock_adjustments", indexes = {
    @Index(name = "idx_sa_product",  columnList = "product_id"),
    @Index(name = "idx_sa_date",     columnList = "adjusted_at"),
    @Index(name = "idx_sa_type",     columnList = "reason")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class StockAdjustment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_name", length = 150)
    private String productName;

    @Column(name = "product_barcode", length = 50)
    private String productBarcode;

    /**
     * Positive = stock added.  Negative = stock removed.
     */
    @Column(name = "quantity_delta", nullable = false)
    private Integer quantityDelta;

    @Column(name = "stock_before", nullable = false)
    private Integer stockBefore;

    @Column(name = "stock_after", nullable = false)
    private Integer stockAfter;

    /**
     * SALE | RETURN | EXCHANGE | MANUAL_ADD | MANUAL_REMOVE
     * | BULK_IMPORT | AUDIT_CORRECTION | PURCHASE
     */
    @Column(name = "reason", length = 30, nullable = false)
    private String reason;

    @Column(name = "reference_id")
    private Long referenceId;   // billId / orderId / returnId etc.

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "adjusted_by", length = 100)
    private String adjustedBy;

    @Column(name = "adjusted_at")
    private LocalDateTime adjustedAt;

    @PrePersist
    void onCreate() {
        if (adjustedAt == null) adjustedAt = LocalDateTime.now();
    }
}
