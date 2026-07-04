package com.beautytextile.service;

import com.beautytextile.dto.ProductRequest;
import com.beautytextile.exception.BusinessException;
import com.beautytextile.exception.ResourceNotFoundException;
import com.beautytextile.model.Product;
import com.beautytextile.repository.ProductRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ProductService {

    private static final int LOW_STOCK_THRESHOLD = 5;

    private final ProductRepository repo;
    private final BarcodeService barcodeService;

    public ProductService(ProductRepository repo, BarcodeService barcodeService) {
        this.repo = repo;
        this.barcodeService = barcodeService;
    }

    public List<Product> findAll() {
        return repo.findAll();
    }

    public List<Product> findByCategory(String category) {
        return repo.findByCategoryIgnoreCase(category);
    }

    public List<Product> search(String name) {
        return repo.findByNameContainingIgnoreCase(name == null ? "" : name);
    }

    public Product findById(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
    }

    public Product findByBarcode(String barcode) {
        return repo.findByBarcode(barcode)
                .orElseThrow(() -> new ResourceNotFoundException("No product for barcode: " + barcode));
    }

    public List<Product> lowStock() {
        return repo.findByStockLessThan(LOW_STOCK_THRESHOLD);
    }

    public Product create(ProductRequest req) {
        String barcode = (req.barcode() == null || req.barcode().isBlank())
                ? generateUniqueBarcode()
                : req.barcode().trim();

        if (repo.existsByBarcode(barcode)) {
            throw new BusinessException("Barcode already exists: " + barcode);
        }

        Product p = Product.builder()
                .name(req.name())
                .description(req.description())
                .category(req.category())
                .price(req.price())
                .stock(req.stock())
                .imageUrl(req.imageUrl())
                .barcode(barcode)
                .extraImages(req.extraImages() != null ? new ArrayList<>(req.extraImages()) : new ArrayList<>())
                .build();
        return repo.save(p);
    }

    public Product update(Long id, ProductRequest req) {
        Product p = findById(id);
        p.setName(req.name());
        p.setDescription(req.description());
        p.setCategory(req.category());
        p.setPrice(req.price());
        p.setStock(req.stock());
        if (req.imageUrl() != null && !req.imageUrl().isBlank()) {
            p.setImageUrl(req.imageUrl());
        }
        if (req.extraImages() != null) {
            p.getExtraImages().clear();
            p.getExtraImages().addAll(req.extraImages());
        }
        if (req.barcode() != null && !req.barcode().isBlank()) {
            p.setBarcode(req.barcode().trim());
        }
        return repo.save(p);
    }

    public void delete(Long id) {
        if (!repo.existsById(id)) {
            throw new ResourceNotFoundException("Product not found: " + id);
        }
        repo.deleteById(id);
    }

    /** Reduce stock, validating availability. Used by orders + billing. */
    public void reduceStock(Long productId, int quantity) {
        Product p = findById(productId);
        if (p.getStock() < quantity) {
            throw new BusinessException("Insufficient stock for " + p.getName()
                    + " (available " + p.getStock() + ", requested " + quantity + ")");
        }
        p.setStock(p.getStock() - quantity);
        repo.save(p);
    }

    /** Increase stock. Used by returns and exchanges. */
    public void addStock(Long productId, int quantity) {
        Product p = findById(productId);
        p.setStock(p.getStock() + quantity);
        repo.save(p);
    }

    public String generateUniqueBarcode() {
        List<String> top = repo.findTopBtBarcodes(PageRequest.of(0, 1));
        String last = top.isEmpty() ? null : top.get(0);
        String candidate = barcodeService.nextBarcode(last);
        while (repo.existsByBarcode(candidate)) {
            candidate = barcodeService.nextBarcode(candidate);
        }
        return candidate;
    }
}
