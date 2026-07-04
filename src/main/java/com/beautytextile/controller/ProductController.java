package com.beautytextile.controller;

import com.beautytextile.dto.PricedProductResponse;
import com.beautytextile.dto.ProductRequest;
import com.beautytextile.model.Product;
import com.beautytextile.service.BarcodeService;
import com.beautytextile.service.DiscountService;
import com.beautytextile.service.FileStorageService;
import com.beautytextile.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;
    private final BarcodeService barcodeService;
    private final FileStorageService fileStorageService;
    private final DiscountService discountService;

    public ProductController(ProductService productService,
                             BarcodeService barcodeService,
                             FileStorageService fileStorageService,
                             DiscountService discountService) {
        this.productService  = productService;
        this.barcodeService  = barcodeService;
        this.fileStorageService = fileStorageService;
        this.discountService = discountService;
    }

    @PostMapping
    public Product create(@Valid @RequestBody ProductRequest req) {
        return productService.create(req);
    }

    @GetMapping
    public List<PricedProductResponse> getAll(@RequestParam(required = false) String category,
                                              @RequestParam(required = false) String search) {
        List<Product> products;
        if (search != null && !search.isBlank()) {
            products = productService.search(search);
        } else if (category != null && !category.isBlank()) {
            products = productService.findByCategory(category);
        } else {
            products = productService.findAll();
        }
        return products.stream().map(discountService::priced).toList();
    }

    @GetMapping("/{id}")
    public PricedProductResponse getById(@PathVariable Long id) {
        return discountService.priced(productService.findById(id));
    }

    @GetMapping("/barcode/{barcode}")
    public PricedProductResponse getByBarcode(@PathVariable String barcode) {
        return discountService.priced(productService.findByBarcode(barcode));
    }

    @PutMapping("/{id}")
    public Product update(@PathVariable Long id, @Valid @RequestBody ProductRequest req) {
        return productService.update(id, req);
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        productService.delete(id);
        return Map.of("deleted", true, "id", id);
    }

    @GetMapping("/next-barcode")
    public Map<String, String> nextBarcode() {
        return Map.of("barcode", productService.generateUniqueBarcode());
    }

    @GetMapping("/{id}/barcode.png")
    public ResponseEntity<byte[]> barcodeImage(@PathVariable Long id) {
        Product p = productService.findById(id);
        byte[] png = barcodeService.generatePng(p.getBarcode(), 380, 120);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=barcode-" + p.getBarcode() + ".png")
                .contentType(MediaType.IMAGE_PNG)
                .body(png);
    }

    @PostMapping("/upload-image")
    public Map<String, String> uploadImage(@RequestParam("file") MultipartFile file) {
        String path = fileStorageService.store(file);
        return Map.of("imageUrl", path);
    }
}
