package com.beautytextile.controller;

import com.beautytextile.dto.*;
import com.beautytextile.model.Product;
import com.beautytextile.model.StockAdjustment;
import com.beautytextile.service.BarcodeService;
import com.beautytextile.service.InventoryService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventorySvc;
    private final BarcodeService   barcodeSvc;

    public InventoryController(InventoryService inventorySvc, BarcodeService barcodeSvc) {
        this.inventorySvc = inventorySvc;
        this.barcodeSvc   = barcodeSvc;
    }

    // ── Paginated product listing ────────────────────────────────────────────

    @GetMapping("/products")
    public ResponseEntity<PagedResponse<Product>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(defaultValue = "name") String sort,
            @RequestParam(defaultValue = "asc") String dir) {
        return ResponseEntity.ok(inventorySvc.listProducts(page, size, sort, dir));
    }

    @GetMapping("/products/search")
    public ResponseEntity<PagedResponse<Product>> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(inventorySvc.searchProducts(q, page, size));
    }

    @GetMapping("/products/category/{category}")
    public ResponseEntity<PagedResponse<Product>> byCategory(
            @PathVariable String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(inventorySvc.listByCategory(category, page, size));
    }

    @GetMapping("/products/low-stock")
    public ResponseEntity<PagedResponse<Product>> lowStock(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(inventorySvc.listLowStock(page, size));
    }

    @GetMapping("/products/out-of-stock")
    public ResponseEntity<PagedResponse<Product>> outOfStock(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(inventorySvc.listOutOfStock(page, size));
    }

    // ── CRUD ─────────────────────────────────────────────────────────────────

    @PostMapping("/products")
    public ResponseEntity<Product> create(@RequestBody InventoryProductRequest req) {
        return ResponseEntity.ok(inventorySvc.createProduct(req));
    }

    @PutMapping("/products/{id}")
    public ResponseEntity<Product> update(@PathVariable Long id,
                                          @RequestBody InventoryProductRequest req) {
        return ResponseEntity.ok(inventorySvc.updateProduct(id, req));
    }

    // ── Stock Adjustment ─────────────────────────────────────────────────────

    @PostMapping("/stock/adjust")
    public ResponseEntity<StockAdjustment> adjustStock(@RequestBody StockAdjustRequest req) {
        return ResponseEntity.ok(inventorySvc.adjustStock(req));
    }

    @GetMapping("/stock/history/{productId}")
    public ResponseEntity<List<StockAdjustment>> history(@PathVariable Long productId) {
        return ResponseEntity.ok(inventorySvc.getAdjustmentHistory(productId));
    }

    @GetMapping("/stock/history/range")
    public ResponseEntity<List<StockAdjustment>> historyRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(inventorySvc.getAdjustmentsByDateRange(from, to));
    }

    // ── Bulk Barcode ──────────────────────────────────────────────────────────

    @PostMapping("/barcodes/generate-missing")
    public ResponseEntity<Map<String, Object>> generateMissing() {
        int count = inventorySvc.generateMissingBarcodes();
        return ResponseEntity.ok(Map.of("updatedCount", count));
    }

    /** Download Excel sheet of barcodes for printing (all or specified product IDs). */
    @GetMapping("/barcodes/export")
    public ResponseEntity<byte[]> exportBarcodes(
            @RequestParam(required = false) List<Long> ids) throws IOException {
        byte[] data = inventorySvc.exportBarcodesExcel(ids);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"barcodes.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    /** Generate a single barcode PNG for print/display. */
    @GetMapping("/barcodes/image/{value}")
    public ResponseEntity<byte[]> barcodeImage(
            @PathVariable String value,
            @RequestParam(defaultValue = "300") int width,
            @RequestParam(defaultValue = "80")  int height) {
        byte[] png = barcodeSvc.generatePng(value, width, height);
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(png);
    }

    // ── Bulk Excel Import ─────────────────────────────────────────────────────

    @GetMapping("/import/template")
    public ResponseEntity<byte[]> importTemplate() throws IOException {
        byte[] data = inventorySvc.generateImportTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"import-template.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> importExcel(
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "admin") String importedBy) throws IOException {
        return ResponseEntity.ok(inventorySvc.importFromExcel(file, importedBy));
    }

    // ── Bulk status update ────────────────────────────────────────────────────

    @PostMapping("/products/bulk-status")
    public ResponseEntity<Map<String, Object>> bulkStatus(@RequestBody BulkStatusRequest req) {
        int updated = inventorySvc.bulkUpdateStatus(req);
        return ResponseEntity.ok(Map.of("updatedCount", updated));
    }

    // ── Inventory Reports ─────────────────────────────────────────────────────

    @GetMapping("/reports/summary")
    public ResponseEntity<Map<String, Object>> summary() {
        return ResponseEntity.ok(inventorySvc.inventorySummary());
    }
}
