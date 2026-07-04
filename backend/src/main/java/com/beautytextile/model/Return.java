package com.beautytextile.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Tracks a full or partial return of items from a bill.
 */
@Entity
@Table(name = "returns")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Return {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bill_id", nullable = false)
    private Long billId;

    @Column(name = "customer_name", length = 150)
    private String customerName;

    @Column(name = "customer_phone", length = 20)
    private String customerPhone;

    /** FULL | PARTIAL */
    @Column(name = "return_type", length = 10)
    @Builder.Default
    private String returnType = "FULL";

    @Column(name = "return_reason", length = 500)
    private String returnReason;

    @Column(name = "return_date")
    private LocalDateTime returnDate;

    @Column(name = "refund_amount", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal refundAmount = BigDecimal.ZERO;

    /** CASH | STORE_CREDIT */
    @Column(name = "refund_method", length = 20)
    @Builder.Default
    private String refundMethod = "CASH";

    /** Who processed the return (admin username). */
    @Column(name = "processed_by", length = 100)
    private String processedBy;

    @OneToMany(mappedBy = "returnRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ReturnItem> items = new ArrayList<>();

    @PrePersist
    void onCreate() {
        if (returnDate == null) returnDate = LocalDateTime.now();
    }

    public void addItem(ReturnItem item) {
        item.setReturnRecord(this);
        items.add(item);
    }
}
