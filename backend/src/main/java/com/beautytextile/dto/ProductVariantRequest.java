package com.beautytextile.dto;

import java.util.List;

/**
 * Request body for creating / updating a product colour variant.
 *
 * Example JSON:
 * {
 *   "colorName": "Navy Blue",
 *   "colorHex": "#1B3A6B",
 *   "imageUrl": "/images/shirt-navy.jpg",
 *   "sizes": [
 *     { "size": "M",   "stock": 10 },
 *     { "size": "L",   "stock": 8  },
 *     { "size": "XL",  "stock": 5  },
 *     { "size": "XXL", "stock": 3  }
 *   ]
 * }
 */
public record ProductVariantRequest(
        String colorName,
        String colorHex,
        String imageUrl,
        List<SizeStock> sizes
) {
    public record SizeStock(String size, int stock, String barcode) {}
}
