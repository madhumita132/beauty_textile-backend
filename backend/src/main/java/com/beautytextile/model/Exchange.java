package com.beautytextile.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Records a product exchange: customer swaps old product for a new one,
 * paying or receiving the price difference.
 */
@Entity
@Table(name = "exchanges")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Exchange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "old_bill_id", nullable = false)
    private Long oldBillId;

    @Column(name = "old_product_id", nullable = false)
    private Long oldProductId;

    @Column(name = "old_product_name", length = 150)
    private String oldProductName;

    @Column(name = "old_quantity", nullable = false)
    @Builder.Default
    private Integer oldQuantity = 1;

    @Column(name = "old_price", precision = 10, scale = 2)
    private BigDecimal oldPrice;

    @Column(name = "new_product_id", nullable = false)
    private Long newProductId;

    @Column(name = "new_product_name", length = 150)
    private String newProductName;

    @Column(name = "new_quantity", nullable = false)
    @Builder.Default
    private Integer newQuantity = 1;

    @Column(name = "new_price", precision = 10, scale = 2)
    private BigDecimal newPrice;

    /**
     * Positive  → customer pays extra.
     * Negative  → shop refunds / issues store credit.
     */
    @Column(name = "price_difference", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal priceDifference = BigDecimal.ZERO;

    /**
     * How the difference is handled when shop owes money.
     * CASH | STORE_CREDIT
     */
    @Column(name = "refund_method", length = 20)
    @Builder.Default
    private String refundMethod = "CASH";

    @Column(name = "customer_name", length = 150)
    private String customerName;

    @Column(name = "customer_phone", length = 20)
    private String customerPhone;

    @Column(name = "exchange_reason", length = 500)
    private String exchangeReason;

    /** Who processed the exchange (admin username). */
    @Column(name = "processed_by", length = 100)
    private String processedBy;

    @Column(name = "exchange_date")
    private LocalDateTime exchangeDate;

    @PrePersist
    void onCreate() {
        if (exchangeDate == null) exchangeDate = LocalDateTime.now();
    }
}
