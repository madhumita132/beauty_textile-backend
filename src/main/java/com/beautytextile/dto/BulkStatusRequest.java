package com.beautytextile.dto;

import java.util.List;

/** Request to update status for multiple products. */
public record BulkStatusRequest(List<Long> productIds, String status) {}
