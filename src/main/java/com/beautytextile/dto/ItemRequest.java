package com.beautytextile.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ItemRequest(
        @NotNull Long productId,
        @NotNull @Min(1) Integer quantity
) {}
