package com.beautytextile.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for creating or updating a product via bulk import or API.
 * All fields null-safe so partial updates work.
 */
public record InventoryProductRequest(
    Long id,                    // null = create; non-null = update
    String name,
    String description,
    String category,
    BigDecimal price,
    BigDecimal costPrice,
    Integer stock,
    Integer reorderLevel,
    String imageUrl,
    String barcode,             // null = auto-generate
    String sku,                 // null = auto-generate from barcode
    String status,              // ACTIVE | INACTIVE | DISCONTINUED
    String supplier,
    List<String> extraImages,
    String discountType,
    BigDecimal discountValue
) {}
