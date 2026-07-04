package com.beautytextile.dto;

/** Request to manually adjust stock for a single product. */
public record StockAdjustRequest(
    Long productId,
    Integer quantityDelta,      // +ve = add,  -ve = remove
    String reason,              // MANUAL_ADD | MANUAL_REMOVE | AUDIT_CORRECTION | PURCHASE
    String note,
    String adjustedBy
) {}
