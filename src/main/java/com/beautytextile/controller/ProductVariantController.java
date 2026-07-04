package com.beautytextile.controller;

import com.beautytextile.dto.ProductVariantRequest;
import com.beautytextile.model.Product;
import com.beautytextile.model.ProductVariant;
import com.beautytextile.model.ProductVariantSize;
import com.beautytextile.repository.ProductVariantRepository;
import com.beautytextile.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API for product colour variants and sizes.
 *
 * GET    /api/products/{id}/variants           — public
 * POST   /api/products/{id}/variants           — admin (JWT required)
 * PUT    /api/products/{id}/variants/{vid}     — admin (JWT required)
 * DELETE /api/products/{id}/variants/{vid}     — admin (JWT required)
 */
@RestController
@RequestMapping("/api/products/{productId}/variants")
public class ProductVariantController {

    private final ProductVariantRepository variantRepo;
    private final ProductService productService;

    public ProductVariantController(ProductVariantRepository variantRepo,
                                    ProductService productService) {
        this.variantRepo = variantRepo;
        this.productService = productService;
    }

    @GetMapping
    public List<ProductVariant> getVariants(@PathVariable Long productId) {
        return variantRepo.findByProductIdWithSizes(productId);
    }

    @PostMapping
    public ProductVariant addVariant(@PathVariable Long productId,
                                     @RequestBody ProductVariantRequest req) {
        Product product = productService.findById(productId);

        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        variant.setColorName(req.colorName());
        variant.setColorHex(req.colorHex() != null ? req.colorHex() : "#888888");
        variant.setImageUrl(req.imageUrl());

        if (req.sizes() != null) {
            for (ProductVariantRequest.SizeStock s : req.sizes()) {
                ProductVariantSize vs = new ProductVariantSize();
                vs.setVariant(variant);
                vs.setSize(s.size());
                vs.setStock(Math.max(0, s.stock()));
                if (s.barcode() != null && !s.barcode().isBlank()) vs.setBarcode(s.barcode().trim());
                variant.getSizes().add(vs);
            }
        }
        return variantRepo.save(variant);
    }

    @PutMapping("/{variantId}")
    public ProductVariant updateVariant(@PathVariable Long productId,
                                        @PathVariable Long variantId,
                                        @RequestBody ProductVariantRequest req) {
        ProductVariant variant = variantRepo.findById(variantId)
                .orElseThrow(() -> new RuntimeException("Variant not found: " + variantId));

        variant.setColorName(req.colorName());
        if (req.colorHex() != null) variant.setColorHex(req.colorHex());
        if (req.imageUrl() != null) variant.setImageUrl(req.imageUrl());

        // Replace all sizes
        variant.getSizes().clear();
        if (req.sizes() != null) {
            for (ProductVariantRequest.SizeStock s : req.sizes()) {
                ProductVariantSize vs = new ProductVariantSize();
                vs.setVariant(variant);
                vs.setSize(s.size());
                vs.setStock(Math.max(0, s.stock()));
                if (s.barcode() != null && !s.barcode().isBlank()) vs.setBarcode(s.barcode().trim());
                variant.getSizes().add(vs);
            }
        }
        return variantRepo.save(variant);
    }

    @DeleteMapping("/{variantId}")
    public ResponseEntity<Map<String, Object>> deleteVariant(@PathVariable Long productId,
                                                              @PathVariable Long variantId) {
        if (!variantRepo.existsById(variantId)) {
            return ResponseEntity.notFound().build();
        }
        variantRepo.deleteById(variantId);
        return ResponseEntity.ok(Map.of("deleted", true, "variantId", variantId));
    }
}
