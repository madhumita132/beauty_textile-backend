package com.beautytextile.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;

public record ProductRequest(
        @NotBlank String name,
        String description,
        @NotBlank String category,
        @NotNull @DecimalMin("0.0") BigDecimal price,
        @NotNull @Min(0) Integer stock,
        String imageUrl,
        String barcode,
        List<String> extraImages
) {}
