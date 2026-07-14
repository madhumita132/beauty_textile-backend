package com.beautytextile.service;

import com.beautytextile.dto.ProductRequest;
import com.beautytextile.exception.BusinessException;
import com.beautytextile.exception.ResourceNotFoundException;
import com.beautytextile.model.Product;
import com.beautytextile.repository.ProductRepository;
import com.beautytextile.service.storage.ImageStorageService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ProductService {

    private static final int LOW_STOCK_THRESHOLD = 5;

    private final ProductRepository repo;
    private final BarcodeService barcodeService;
    private final ImageStorageService imageStorage;

    public ProductService(ProductRepository repo, BarcodeService barcodeService, ImageStorageService imageStorage) {
        this.repo = repo;
        this.barcodeService = barcodeService;
        this.imageStorage = imageStorage;
    }

    @Cacheable(cacheNames = "products", key = "'all'")
    public List<Product> findAll() {
        return repo.findAll();
    }

    @Cacheable(cacheNames = "products", key = "'cat:' + #category")
    public List<Product> findByCategory(String category) {
        return repo.findByCategoryIgnoreCase(category);
    }

    @Cacheable(cacheNames = "products", key = "'search:' + (#name == null ? '' : #name)")
    public List<Product> search(String name) {
        return repo.findByNameContainingIgnoreCase(name == null ? "" : name);
    }

    @Cacheable(cacheNames = "products", key = "'paged:all:' + #page + ':' + #size")
    public Page<Product> findAllPaged(int page, int size) {
        return repo.findAll(PageRequest.of(page, size));
    }

    @Cacheable(cacheNames = "products", key = "'paged:cat:' + #category + ':' + #page + ':' + #size")
    public Page<Product> findByCategoryPaged(String category, int page, int size) {
        return repo.findByCategoryIgnoreCase(category, PageRequest.of(page, size));
    }

    @Cacheable(cacheNames = "products", key = "'paged:search:' + (#query == null ? '' : #query) + ':' + #page + ':' + #size")
    public Page<Product> searchPaged(String query, int page, int size) {
        return repo.search(query == null ? "" : query, PageRequest.of(page, size));
    }

    @Cacheable(cacheNames = "productById", key = "#id")
    public Product findById(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
    }

    @Cacheable(cacheNames = "productByBarcode", key = "#barcode")
    public Product findByBarcode(String barcode) {
        return repo.findByBarcode(barcode)
                .orElseThrow(() -> new ResourceNotFoundException("No product for barcode: " + barcode));
    }

    public List<Product> lowStock() {
        return repo.findByStockLessThan(LOW_STOCK_THRESHOLD);
    }

    @CacheEvict(cacheNames = {"products", "productById", "productByBarcode"}, allEntries = true)
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

    @CacheEvict(cacheNames = {"products", "productById", "productByBarcode"}, allEntries = true)
    public Product update(Long id, ProductRequest req) {
        Product p = findById(id);
        String oldMainImage = p.getImageUrl();
        List<String> oldExtraImages = new ArrayList<>(p.getExtraImages());
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
        Product saved = repo.save(p);

        if (req.imageUrl() != null && !req.imageUrl().isBlank() && oldMainImage != null && !oldMainImage.equals(saved.getImageUrl())) {
            imageStorage.delete(oldMainImage);
        }
        if (req.extraImages() != null) {
            for (String oldImage : oldExtraImages) {
                if (oldImage != null && !saved.getExtraImages().contains(oldImage)) {
                    imageStorage.delete(oldImage);
                }
            }
        }
        return saved;
    }

    @CacheEvict(cacheNames = {"products", "productById", "productByBarcode"}, allEntries = true)
    public void delete(Long id) {
        Product p = findById(id);
        if (p.getImageUrl() != null) {
            imageStorage.delete(p.getImageUrl());
        }
        for (String extraImage : p.getExtraImages()) {
            imageStorage.delete(extraImage);
        }
        repo.deleteById(id);
    }

    /** Reduce stock, validating availability. Used by orders + billing. */
    @CacheEvict(cacheNames = {"products", "productById", "productByBarcode"}, allEntries = true)
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
    @CacheEvict(cacheNames = {"products", "productById", "productByBarcode"}, allEntries = true)
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
