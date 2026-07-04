package com.beautytextile.dto;

import com.beautytextile.model.Product;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Wraps a Product and adds computed discount/final-price fields.
 * Returned by GET /api/products to allow the frontend to show
 * original price, discount, and final price everywhere.
 */
public class PricedProductResponse {

    public final long    id;
    public final String  name;
    public final String  description;
    public final String  category;
    public final String  imageUrl;
    public final java.util.List<String> extraImages;
    public final String  barcode;
    public final int     stock;
    public final String  createdAt;

    // Pricing
    public final BigDecimal originalPrice;
    public final BigDecimal discountAmount;
    public final BigDecimal finalPrice;
    public final BigDecimal price;          // alias for finalPrice — used by cart/checkout/legacy
    public final String     discountLabel;   // "10% OFF", "₹100 OFF", ""

    public PricedProductResponse(Product p, BigDecimal discountAmt) {
        this.id          = p.getId();
        this.name        = p.getName();
        this.description = p.getDescription();
        this.category    = p.getCategory();
        this.imageUrl    = p.getImageUrl();
        this.extraImages = p.getExtraImages();
        this.barcode     = p.getBarcode();
        this.stock       = p.getStock() == null ? 0 : p.getStock();
        this.createdAt   = p.getCreatedAt() == null ? null : p.getCreatedAt().toString();

        this.originalPrice  = p.getPrice();
        this.discountAmount = discountAmt.setScale(2, RoundingMode.HALF_UP);
        this.finalPrice     = p.getPrice().subtract(discountAmt).max(BigDecimal.ZERO)
                               .setScale(2, RoundingMode.HALF_UP);
        this.price          = this.finalPrice;   // alias
        this.discountLabel  = discountAmt.compareTo(BigDecimal.ZERO) > 0
                ? "₹" + this.discountAmount.stripTrailingZeros().toPlainString() + " OFF"
                : "";
    }
}
