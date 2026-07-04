package com.beautytextile.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products", indexes = {
    @Index(name = "idx_product_barcode",  columnList = "barcode",  unique = true),
    @Index(name = "idx_product_sku",      columnList = "sku",      unique = true),
    @Index(name = "idx_product_category", columnList = "category"),
    @Index(name = "idx_product_name",     columnList = "name"),
    @Index(name = "idx_product_stock",    columnList = "stock"),
    @Index(name = "idx_product_created",  columnList = "created_at")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 1000)
    private String description;

    /** Category name (Women, Men, Kids, Girls, Boys, Kurthi). */
    @Column(nullable = false, length = 60)
    private String category;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer stock;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    /** Additional images (gallery). Stored in product_images table. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "product_images", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "image_url", length = 500)
    @Builder.Default
    private List<String> extraImages = new ArrayList<>();

    /** Product-level discount. NONE means no discount override. */
    @Column(name = "discount_type", length = 10)
    @Builder.Default
    private String discountType = "NONE";  // NONE | PERCENTAGE | FIXED

    @Column(name = "discount_value", precision = 10, scale = 2)
    @Builder.Default
    private java.math.BigDecimal discountValue = java.math.BigDecimal.ZERO;

    /** Unique barcode value, e.g. BT1001 (Code 128). */
    @Column(nullable = false, unique = true, length = 50)
    private String barcode;

    /** SKU (Stock Keeping Unit) — optional short code for internal reference. */
    @Column(length = 80, unique = true)
    private String sku;

    /** Status: ACTIVE | INACTIVE | DISCONTINUED */
    @Column(length = 15)
    @Builder.Default
    private String status = "ACTIVE";

    @Column(name = "reorder_level")
    @Builder.Default
    private Integer reorderLevel = 5;

    @Column(name = "cost_price", precision = 10, scale = 2)
    private java.math.BigDecimal costPrice;

    @Column(name = "supplier", length = 150)
    private String supplier;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (stock == null) stock = 0;
    }
}
