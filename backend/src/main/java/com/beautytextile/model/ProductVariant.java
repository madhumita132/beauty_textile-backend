package com.beautytextile.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * One colour variant of a product.
 * A product can have many colour variants, each with their own image and sizes.
 */
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "product_variants")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @JsonIgnore
    private Product product;

    /** Display name of the colour, e.g. "Red", "Navy Blue" */
    @Column(name = "color_name", length = 60, nullable = false)
    private String colorName;

    /** CSS hex code, e.g. "#C0392B" */
    @Column(name = "color_hex", length = 10)
    private String colorHex;

    /** Image URL for this colour variant */
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @OneToMany(mappedBy = "variant", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProductVariantSize> sizes = new ArrayList<>();
}
