package com.beautytextile.service;

import com.beautytextile.dto.*;
import com.beautytextile.exception.BusinessException;
import com.beautytextile.exception.ResourceNotFoundException;
import com.beautytextile.model.Product;
import com.beautytextile.model.StockAdjustment;
import com.beautytextile.repository.ProductRepository;
import com.beautytextile.repository.StockAdjustmentRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class InventoryService {

    private final ProductRepository productRepo;
    private final StockAdjustmentRepository adjustmentRepo;
    private final ProductService productService;
    private final BarcodeService barcodeService;

    public InventoryService(ProductRepository productRepo,
                            StockAdjustmentRepository adjustmentRepo,
                            ProductService productService,
                            BarcodeService barcodeService) {
        this.productRepo    = productRepo;
        this.adjustmentRepo = adjustmentRepo;
        this.productService = productService;
        this.barcodeService = barcodeService;
    }

    // ─────────────────────────────────────────────────────────────
    //  Paginated product queries
    // ─────────────────────────────────────────────────────────────

    public PagedResponse<Product> listProducts(int page, int size, String sort, String dir) {
        Sort.Direction direction = "desc".equalsIgnoreCase(dir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pg = PageRequest.of(page, size, Sort.by(direction, sort));
        return PagedResponse.of(productRepo.findAll(pg));
    }

    public PagedResponse<Product> searchProducts(String q, int page, int size) {
        Pageable pg = PageRequest.of(page, size, Sort.by("name"));
        return PagedResponse.of(productRepo.search(q, pg));
    }

    public PagedResponse<Product> listByCategory(String category, int page, int size) {
        Pageable pg = PageRequest.of(page, size, Sort.by("name"));
        return PagedResponse.of(productRepo.findByCategoryIgnoreCase(category, pg));
    }

    public PagedResponse<Product> listLowStock(int page, int size) {
        Pageable pg = PageRequest.of(page, size, Sort.by("stock"));
        return PagedResponse.of(productRepo.findLowStock(pg));
    }

    public PagedResponse<Product> listOutOfStock(int page, int size) {
        Pageable pg = PageRequest.of(page, size, Sort.by("name"));
        return PagedResponse.of(productRepo.findOutOfStock(pg));
    }

    // ─────────────────────────────────────────────────────────────
    //  Single product create / update (inventory-enriched)
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public Product createProduct(InventoryProductRequest req) {
        String barcode = resolveBarcode(req.barcode());
        String sku     = resolveSku(req.sku(), barcode);

        Product p = Product.builder()
                .name(req.name())
                .description(req.description())
                .category(req.category())
                .price(req.price())
                .costPrice(req.costPrice())
                .stock(req.stock() != null ? req.stock() : 0)
                .reorderLevel(req.reorderLevel() != null ? req.reorderLevel() : 5)
                .imageUrl(req.imageUrl())
                .barcode(barcode)
                .sku(sku)
                .status(req.status() != null ? req.status() : "ACTIVE")
                .supplier(req.supplier())
                .extraImages(req.extraImages() != null ? new ArrayList<>(req.extraImages()) : new ArrayList<>())
                .discountType(req.discountType() != null ? req.discountType() : "NONE")
                .discountValue(req.discountValue() != null ? req.discountValue() : BigDecimal.ZERO)
                .build();

        Product saved = productRepo.save(p);
        recordAdjustment(saved, saved.getStock(), 0, "BULK_IMPORT", null, "Initial stock", "system");
        return saved;
    }

    @Transactional
    public Product updateProduct(Long id, InventoryProductRequest req) {
        Product p = productRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));

        if (req.name() != null)        p.setName(req.name());
        if (req.description() != null) p.setDescription(req.description());
        if (req.category() != null)    p.setCategory(req.category());
        if (req.price() != null)       p.setPrice(req.price());
        if (req.costPrice() != null)   p.setCostPrice(req.costPrice());
        if (req.reorderLevel() != null) p.setReorderLevel(req.reorderLevel());
        if (req.imageUrl() != null && !req.imageUrl().isBlank()) p.setImageUrl(req.imageUrl());
        if (req.status() != null)      p.setStatus(req.status());
        if (req.supplier() != null)    p.setSupplier(req.supplier());
        if (req.discountType() != null)  p.setDiscountType(req.discountType());
        if (req.discountValue() != null) p.setDiscountValue(req.discountValue());
        if (req.extraImages() != null) {
            p.getExtraImages().clear();
            p.getExtraImages().addAll(req.extraImages());
        }
        if (req.sku() != null && !req.sku().isBlank()) {
            if (!req.sku().equals(p.getSku()) && productRepo.existsBySku(req.sku())) {
                throw new BusinessException("SKU already exists: " + req.sku());
            }
            p.setSku(req.sku());
        }

        if (req.stock() != null && !req.stock().equals(p.getStock())) {
            int before = p.getStock();
            int delta  = req.stock() - before;
            p.setStock(req.stock());
            recordAdjustment(p, delta, before, "AUDIT_CORRECTION", null, "Manual stock update", "admin");
        }

        return productRepo.save(p);
    }

    // ─────────────────────────────────────────────────────────────
    //  Stock Adjustment
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public StockAdjustment adjustStock(StockAdjustRequest req) {
        Product p = productRepo.findById(req.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + req.productId()));

        int before = p.getStock();
        int after  = before + req.quantityDelta();
        if (after < 0) throw new BusinessException("Stock cannot go negative. Current: " + before);

        p.setStock(after);
        productRepo.save(p);

        return recordAdjustment(p, req.quantityDelta(), before,
                req.reason(), null, req.note(),
                req.adjustedBy() != null ? req.adjustedBy() : "admin");
    }

    public List<StockAdjustment> getAdjustmentHistory(Long productId) {
        return adjustmentRepo.findByProductIdOrderByAdjustedAtDesc(productId);
    }

    public List<StockAdjustment> getAdjustmentsByDateRange(LocalDate from, LocalDate to) {
        return adjustmentRepo.findByDateRange(
                from.atStartOfDay(), to.plusDays(1).atStartOfDay());
    }

    // ─────────────────────────────────────────────────────────────
    //  Bulk Barcode Generation
    // ─────────────────────────────────────────────────────────────

    /** Ensure all active products without a barcode get one. Returns count of updated products. */
    @Transactional
    public int generateMissingBarcodes() {
        List<Product> all = productRepo.findAll();
        int count = 0;
        for (Product p : all) {
            if (p.getBarcode() == null || p.getBarcode().isBlank()) {
                p.setBarcode(productService.generateUniqueBarcode());
                if (p.getSku() == null || p.getSku().isBlank()) {
                    p.setSku(p.getBarcode());
                }
                productRepo.save(p);
                count++;
            }
        }
        return count;
    }

    /**
     * Build an Excel file with one row per product containing:
     * Barcode | SKU | Name | Category | Price | Stock
     * Used for bulk barcode printing (sheet can be mail-merged into label software).
     */
    public byte[] exportBarcodesExcel(List<Long> productIds) throws IOException {
        List<Product> products = productIds == null || productIds.isEmpty()
                ? productRepo.findAll(Sort.by("category", "name"))
                : productRepo.findAllById(productIds);

        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Barcodes");

            // Header
            String[] cols = {"Barcode", "SKU", "Name", "Category", "Price", "Stock", "Status"};
            Row header = sheet.createRow(0);
            CellStyle hs = wb.createCellStyle();
            Font hf = wb.createFont(); hf.setBold(true);
            hs.setFont(hf);
            for (int i = 0; i < cols.length; i++) {
                Cell c = header.createCell(i);
                c.setCellValue(cols[i]);
                c.setCellStyle(hs);
                sheet.setColumnWidth(i, 18 * 256);
            }

            int rowIdx = 1;
            for (Product p : products) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(p.getBarcode());
                row.createCell(1).setCellValue(p.getSku() != null ? p.getSku() : "");
                row.createCell(2).setCellValue(p.getName());
                row.createCell(3).setCellValue(p.getCategory());
                row.createCell(4).setCellValue(p.getPrice().doubleValue());
                row.createCell(5).setCellValue(p.getStock());
                row.createCell(6).setCellValue(p.getStatus() != null ? p.getStatus() : "ACTIVE");
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  Bulk Excel Import
    // ─────────────────────────────────────────────────────────────

    /**
     * Expected columns (case-insensitive, order matters):
     * name | category | price | cost_price | stock | reorder_level
     * | barcode | sku | supplier | status | description
     *
     * Returns a summary: {created, updated, errors, errorRows}
     */
    @Transactional
    public Map<String, Object> importFromExcel(MultipartFile file, String importedBy) throws IOException {
        int created = 0, updated = 0;
        List<String> errors = new ArrayList<>();

        try (Workbook wb = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) throw new BusinessException("Excel file has no header row");

            // Build column index map
            Map<String, Integer> cols = new HashMap<>();
            for (Cell cell : headerRow) {
                cols.put(cell.getStringCellValue().trim().toLowerCase().replace(" ", "_"), cell.getColumnIndex());
            }

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null || isRowEmpty(row)) continue;

                try {
                    String name     = getString(row, cols, "name");
                    String category = getString(row, cols, "category");
                    if (name == null || name.isBlank() || category == null || category.isBlank()) {
                        errors.add("Row " + (r + 1) + ": name and category are required");
                        continue;
                    }

                    BigDecimal price     = getBigDecimal(row, cols, "price");
                    BigDecimal costPrice = getBigDecimal(row, cols, "cost_price");
                    Integer stock        = getInt(row, cols, "stock");
                    Integer reorderLevel = getInt(row, cols, "reorder_level");
                    String barcode       = getString(row, cols, "barcode");
                    String sku           = getString(row, cols, "sku");
                    String supplier      = getString(row, cols, "supplier");
                    String status        = getString(row, cols, "status");
                    String description   = getString(row, cols, "description");
                    String imageUrl      = getString(row, cols, "image_url");

                    if (price == null) price = BigDecimal.ZERO;

                    // Check if update (barcode or sku match)
                    Optional<Product> existing = Optional.empty();
                    if (barcode != null && !barcode.isBlank()) {
                        existing = productRepo.findByBarcode(barcode);
                    }
                    if (existing.isEmpty() && sku != null && !sku.isBlank()) {
                        existing = productRepo.findBySku(sku);
                    }

                    if (existing.isPresent()) {
                        // Update existing
                        Product p = existing.get();
                        int before = p.getStock();
                        p.setName(name);
                        p.setCategory(category);
                        p.setPrice(price);
                        if (costPrice != null) p.setCostPrice(costPrice);
                        if (stock != null && !stock.equals(before)) {
                            p.setStock(stock);
                            recordAdjustment(p, stock - before, before, "BULK_IMPORT", null,
                                    "Excel import row " + (r + 1), importedBy);
                        }
                        if (reorderLevel != null) p.setReorderLevel(reorderLevel);
                        if (supplier != null)     p.setSupplier(supplier);
                        if (status != null)       p.setStatus(status);
                        if (description != null)  p.setDescription(description);
                        if (imageUrl != null)     p.setImageUrl(imageUrl);
                        if (sku != null && !sku.isBlank() && !sku.equals(p.getSku())) {
                            if (!productRepo.existsBySku(sku)) p.setSku(sku);
                        }
                        productRepo.save(p);
                        updated++;
                    } else {
                        // Create new
                        String resolvedBarcode = resolveBarcode(barcode);
                        String resolvedSku     = resolveSku(sku, resolvedBarcode);
                        Product p = Product.builder()
                                .name(name).category(category).price(price)
                                .costPrice(costPrice)
                                .stock(stock != null ? stock : 0)
                                .reorderLevel(reorderLevel != null ? reorderLevel : 5)
                                .barcode(resolvedBarcode).sku(resolvedSku)
                                .supplier(supplier)
                                .status(status != null ? status : "ACTIVE")
                                .description(description)
                                .imageUrl(imageUrl)
                                .build();
                        Product saved = productRepo.save(p);
                        if (saved.getStock() > 0) {
                            recordAdjustment(saved, saved.getStock(), 0, "BULK_IMPORT", null,
                                    "Excel import row " + (r + 1), importedBy);
                        }
                        created++;
                    }
                } catch (Exception ex) {
                    errors.add("Row " + (r + 1) + ": " + ex.getMessage());
                }
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("created", created);
        result.put("updated", updated);
        result.put("errorCount", errors.size());
        result.put("errors", errors);
        return result;
    }

    // ─────────────────────────────────────────────────────────────
    //  Excel Template Download
    // ─────────────────────────────────────────────────────────────

    public byte[] generateImportTemplate() throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Products");
            String[] cols = {
                "name", "category", "price", "cost_price", "stock", "reorder_level",
                "barcode", "sku", "supplier", "status", "description", "image_url"
            };
            Row header = sheet.createRow(0);
            CellStyle hs = wb.createCellStyle();
            Font hf = wb.createFont(); hf.setBold(true);
            hs.setFont(hf);
            for (int i = 0; i < cols.length; i++) {
                Cell c = header.createCell(i);
                c.setCellValue(cols[i]);
                c.setCellStyle(hs);
                sheet.setColumnWidth(i, 20 * 256);
            }
            // One sample row
            Row sample = sheet.createRow(1);
            sample.createCell(0).setCellValue("Cotton Saree");
            sample.createCell(1).setCellValue("Saree");
            sample.createCell(2).setCellValue(599.00);
            sample.createCell(3).setCellValue(350.00);
            sample.createCell(4).setCellValue(20);
            sample.createCell(5).setCellValue(5);
            sample.createCell(6).setCellValue("");       // auto-generated if blank
            sample.createCell(7).setCellValue("");
            sample.createCell(8).setCellValue("Sundaram Textiles");
            sample.createCell(9).setCellValue("ACTIVE");
            sample.createCell(10).setCellValue("100% cotton");
            sample.createCell(11).setCellValue("");

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  Bulk status update
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public int bulkUpdateStatus(BulkStatusRequest req) {
        return productRepo.bulkUpdateStatus(req.status(), req.productIds());
    }

    // ─────────────────────────────────────────────────────────────
    //  Inventory Reports / Dashboard summary
    // ─────────────────────────────────────────────────────────────

    public Map<String, Object> inventorySummary() {
        long total       = productRepo.countActive();
        long outOfStock  = productRepo.countOutOfStock();
        long lowStock    = productRepo.countLowStock();
        BigDecimal saleVal = productRepo.totalInventoryValue();
        BigDecimal costVal = productRepo.totalCostValue();
        List<String> categories = productRepo.findAllCategories();

        Map<String, Object> s = new LinkedHashMap<>();
        s.put("totalActiveProducts", total);
        s.put("outOfStockCount",     outOfStock);
        s.put("lowStockCount",       lowStock);
        s.put("totalInventoryValue", saleVal != null ? saleVal : BigDecimal.ZERO);
        s.put("totalCostValue",      costVal  != null ? costVal  : BigDecimal.ZERO);
        s.put("categoryCount",       categories.size());
        s.put("categories",          categories);
        return s;
    }

    // ─────────────────────────────────────────────────────────────
    //  Internal helpers
    // ─────────────────────────────────────────────────────────────

    StockAdjustment recordAdjustment(Product p, int delta, int before,
                                     String reason, Long refId, String note, String by) {
        StockAdjustment sa = StockAdjustment.builder()
                .productId(p.getId())
                .productName(p.getName())
                .productBarcode(p.getBarcode())
                .quantityDelta(delta)
                .stockBefore(before)
                .stockAfter(before + delta)
                .reason(reason)
                .referenceId(refId)
                .note(note)
                .adjustedBy(by)
                .build();
        return adjustmentRepo.save(sa);
    }

    private String resolveBarcode(String requested) {
        if (requested != null && !requested.isBlank()) {
            if (productRepo.existsByBarcode(requested.trim()))
                throw new BusinessException("Barcode already exists: " + requested.trim());
            return requested.trim();
        }
        return productService.generateUniqueBarcode();
    }

    private String resolveSku(String requested, String fallbackBarcode) {
        if (requested != null && !requested.isBlank()) {
            if (productRepo.existsBySku(requested.trim()))
                throw new BusinessException("SKU already exists: " + requested.trim());
            return requested.trim();
        }
        // Default SKU = same as barcode
        String sku = fallbackBarcode;
        int suffix = 1;
        while (productRepo.existsBySku(sku)) {
            sku = fallbackBarcode + "-" + suffix++;
        }
        return sku;
    }

    // ── Excel cell readers ────────────────────────────────────────

    private String getString(Row row, Map<String, Integer> cols, String key) {
        Integer idx = cols.get(key);
        if (idx == null) return null;
        Cell cell = row.getCell(idx);
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default      -> null;
        };
    }

    private BigDecimal getBigDecimal(Row row, Map<String, Integer> cols, String key) {
        Integer idx = cols.get(key);
        if (idx == null) return null;
        Cell cell = row.getCell(idx);
        if (cell == null) return null;
        try {
            return switch (cell.getCellType()) {
                case NUMERIC -> BigDecimal.valueOf(cell.getNumericCellValue());
                case STRING  -> new BigDecimal(cell.getStringCellValue().trim());
                default      -> null;
            };
        } catch (Exception e) { return null; }
    }

    private Integer getInt(Row row, Map<String, Integer> cols, String key) {
        BigDecimal bd = getBigDecimal(row, cols, key);
        return bd != null ? bd.intValue() : null;
    }

    private boolean isRowEmpty(Row row) {
        for (Cell cell : row) {
            if (cell.getCellType() != CellType.BLANK) return false;
        }
        return true;
    }
}
