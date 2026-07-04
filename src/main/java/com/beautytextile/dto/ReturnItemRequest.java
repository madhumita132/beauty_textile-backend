package com.beautytextile.dto;

public record ReturnItemRequest(
    Long productId,
    String productName,
    Integer quantity,
    java.math.BigDecimal price
) {}
