package com.beautytextile.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

/**
 * Stock quantity for a specific size within a colour variant.
 * e.g.  variant=Red  size=XL  stock=8
 */
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "product_variant_sizes",
       uniqueConstraints = @UniqueConstraint(columnNames = {"variant_id", "size"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductVariantSize {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id", nullable = false)
    @JsonIgnore
    private ProductVariant variant;

    /** Size label: M / L / XL / XXL (or kids sizes like 2Y / 4Y) */
    @Column(length = 10, nullable = false)
    private String size;

    @Column(nullable = false)
    private Integer stock;

    /** Optional per-variant-size barcode for scanning at POS */
    @Column(length = 100, unique = true)
    private String barcode;
}
