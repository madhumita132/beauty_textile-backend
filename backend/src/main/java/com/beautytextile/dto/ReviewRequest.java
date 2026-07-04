package com.beautytextile.dto;

/** Customer submits a review for a product. */
public record ReviewRequest(
        Long productId,
        String customerName,
        String mobileNumber,   // optional
        int rating,            // 1–5
        String reviewComment
) {}
