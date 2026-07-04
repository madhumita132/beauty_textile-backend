package com.beautytextile.dto;

/**
 * Request to exchange old product(s) from a bill for a new product.
 */
public record ExchangeRequest(
    Long oldBillId,
    Long oldProductId,
    Integer oldQuantity,
    Long newProductId,
    Integer newQuantity,
    String refundMethod,   // CASH | STORE_CREDIT  (used when new < old)
    String exchangeReason,
    String processedBy
) {}
