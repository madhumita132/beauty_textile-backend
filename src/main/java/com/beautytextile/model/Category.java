package com.beautytextile.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "categories", indexes = {
    @Index(name = "idx_category_name",   columnList = "name",     unique = true),
    @Index(name = "idx_category_parent", columnList = "parent_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String name;

    /** Null for top-level (root) categories. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    @JsonIgnore
    private Category parent;

    /** Convenience field — same column as FK, read-only. */
    @Column(name = "parent_id", insertable = false, updatable = false)
    private Long parentId;

    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @Builder.Default
    @JsonIgnore
    private List<Category> children = new ArrayList<>();

    /** Static image path served from /images/categories/... e.g. /images/categories/saree/saree.jpg */
    @Column(name = "image_path", length = 255)
    private String imagePath;

    /** Category-level discount applied to all products in this category. */
    @Column(name = "discount_type", length = 10)
    @Builder.Default
    private String discountType = "NONE";  // NONE | PERCENTAGE | FIXED

    @Column(name = "discount_value", precision = 10, scale = 2)
    @Builder.Default
    private java.math.BigDecimal discountValue = java.math.BigDecimal.ZERO;
}
