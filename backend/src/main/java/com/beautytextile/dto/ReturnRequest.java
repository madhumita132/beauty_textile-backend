package com.beautytextile.dto;

import java.util.List;

/**
 * Request to return one or more items from a bill.
 * For a FULL return, supply all bill items (or leave itemsToReturn empty — backend will fill).
 */
public record ReturnRequest(
    Long billId,
    String returnType,        // FULL | PARTIAL
    String returnReason,
    String refundMethod,      // CASH | STORE_CREDIT
    String processedBy,
    List<ReturnItemRequest> itemsToReturn
) {}
