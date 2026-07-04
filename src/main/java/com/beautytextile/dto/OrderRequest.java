package com.beautytextile.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record OrderRequest(
        @NotBlank String customerName,
        @NotBlank String phone,
        String address,
        @NotEmpty @Valid List<ItemRequest> items
) {}
