package com.beautytextile.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** In-shop POS bill. */
@Entity
@Table(name = "billing")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Billing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_name", length = 150)
    private String customerName;

    @Column(length = 20)
    private String phone;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    /** Discount applied at billing time. */
    @Column(name = "discount_type", length = 15)
    @Builder.Default
    private String discountType = "NONE";   // NONE | PERCENTAGE | FIXED

    @Column(name = "discount_value", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal discountValue = BigDecimal.ZERO;

    @Column(name = "discount_amount", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;  // computed, stored for reports

    @Column(name = "final_amount", precision = 10, scale = 2)
    private BigDecimal finalAmount;   // totalAmount - discountAmount

    @Column(name = "payment_mode", length = 20)
    private String paymentMode;   // CASH, UPI, CARD

    /** GST percentage applied to this bill (snapshot from settings at billing time). */
    @Column(name = "gst_percentage")
    @Builder.Default
    private Integer gstPercentage = 0;

    /** GST amount = finalAmount × gstPercentage / 100 */
    @Column(name = "gst_amount", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal gstAmount = BigDecimal.ZERO;

    /** Grand total = finalAmount + gstAmount (the amount the customer actually pays) */
    @Column(name = "grand_total", precision = 10, scale = 2)
    private BigDecimal grandTotal;

    /** ACTIVE | RETURNED | PARTIALLY_RETURNED */
    @Column(name = "status", length = 25)
    @Builder.Default
    private String status = "ACTIVE";

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "billing", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<BillingItem> items = new ArrayList<>();

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    public void addItem(BillingItem item) {
        item.setBilling(this);
        items.add(item);
    }
}
