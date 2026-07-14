package com.beautytextile.controller;

import com.beautytextile.repository.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Backup endpoint: streams an Excel workbook containing the key database tables.
 * Admin-only (secured in SecurityConfig).
 *
 * GET /api/admin/backup   → triggers workbook generation and sends as .xlsx download
 */
@RestController
@RequestMapping("/api/admin")
public class BackupController {

    private final CategoryRepository categoryRepo;
    private final ProductRepository productRepo;
    private final OrderRepository orderRepo;
    private final BillingRepository billingRepo;
    private final ReviewRepository reviewRepo;
    private final AppSettingsRepository appSettingsRepo;
    private final AdminUserRepository adminUserRepo;
    private final HeroSlideRepository heroSlideRepo;

    public BackupController(CategoryRepository categoryRepo,
                            ProductRepository productRepo,
                            OrderRepository orderRepo,
                            BillingRepository billingRepo,
                            ReviewRepository reviewRepo,
                            AppSettingsRepository appSettingsRepo,
                            AdminUserRepository adminUserRepo,
                            HeroSlideRepository heroSlideRepo) {
        this.categoryRepo = categoryRepo;
        this.productRepo = productRepo;
        this.orderRepo = orderRepo;
        this.billingRepo = billingRepo;
        this.reviewRepo = reviewRepo;
        this.appSettingsRepo = appSettingsRepo;
        this.adminUserRepo = adminUserRepo;
        this.heroSlideRepo = heroSlideRepo;
    }

    @GetMapping("/backup")
    public ResponseEntity<byte[]> downloadBackup() throws IOException {
        String filename = "beauty-textile-backup-"
                + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
                + ".xlsx";

        byte[] data;
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            writeSheet(wb, "Categories", List.of("ID", "Name", "Parent ID", "Image Path"), categoryRepo.findAll(), row -> new Object[]{
                    row.getId(), row.getName(), row.getParent() != null ? row.getParent().getId() : "", row.getImagePath() != null ? row.getImagePath() : ""
            });
            writeSheet(wb, "Products", List.of("ID", "Name", "Category", "Price", "Stock", "Barcode", "SKU", "Status"), productRepo.findAll(), row -> new Object[]{
                    row.getId(), row.getName(), row.getCategory(), row.getPrice() != null ? row.getPrice() : BigDecimal.ZERO,
                    row.getStock(), row.getBarcode() != null ? row.getBarcode() : "", row.getSku() != null ? row.getSku() : "",
                    row.getStatus() != null ? row.getStatus() : "ACTIVE"
            });
            writeSheet(wb, "Orders", List.of("ID", "Customer Name", "Phone", "Amount", "Payment Status", "Fulfillment Status", "Created At"), orderRepo.findAll(), row -> new Object[]{
                    row.getId(), row.getCustomerName(), row.getPhone(), row.getTotalAmount(), row.getPaymentStatus(), row.getFulfillmentStatus(), row.getCreatedAt()
            });
            writeSheet(wb, "Billing", List.of("ID", "Customer Name", "Phone", "Total Amount", "Discount Amount", "GST Amount", "Grand Total", "Status", "Created At"), billingRepo.findAll(), row -> new Object[]{
                    row.getId(), row.getCustomerName(), row.getPhone(), row.getTotalAmount(), row.getDiscountAmount(), row.getGstAmount(), row.getGrandTotal(), row.getStatus(), row.getCreatedAt()
            });
            writeSheet(wb, "Reviews", List.of("ID", "Product ID", "Customer Name", "Mobile", "Rating", "Status", "Created At"), reviewRepo.findAll(), row -> new Object[]{
                    row.getId(), row.getProductId(), row.getCustomerName(), row.getMobileNumber(), row.getRating(), row.getStatus(), row.getCreatedAt()
            });
            writeSheet(wb, "Settings", List.of("ID", "GST Enabled", "GST Percentage"), appSettingsRepo.findAll(), row -> new Object[]{
                    row.getId(), row.isGstEnabled(), row.getGstPercentage()
            });
            writeSheet(wb, "Admins", List.of("ID", "Username", "Role"), adminUserRepo.findAll(), row -> new Object[]{
                    row.getId(), row.getUsername(), row.getRole()
            });
            writeSheet(wb, "Hero Slides", List.of("ID", "Kicker", "Title", "Text", "Image Path", "Sort Order"), heroSlideRepo.findAll(), row -> new Object[]{
                    row.getId(), row.getKicker(), row.getTitle(), row.getText(), row.getImagePath(), row.getSortOrder()
            });

            wb.write(out);
            data = out.toByteArray();
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    private <T> void writeSheet(XSSFWorkbook wb, String sheetName, List<String> headers, List<T> rows, RowMapper<T> mapper) {
        Sheet sheet = wb.createSheet(sheetName);
        CellStyle headerStyle = wb.createCellStyle();
        Font headerFont = wb.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);

        Row header = sheet.createRow(0);
        for (int i = 0; i < headers.size(); i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(headers.get(i));
            cell.setCellStyle(headerStyle);
            sheet.setColumnWidth(i, 20 * 256);
        }

        int rowIndex = 1;
        for (T rowData : rows) {
            Row row = sheet.createRow(rowIndex++);
            Object[] values = mapper.map(rowData);
            for (int i = 0; i < values.length; i++) {
                Cell cell = row.createCell(i);
                Object value = values[i];
                if (value == null) {
                    cell.setBlank();
                } else if (value instanceof Number number) {
                    cell.setCellValue(number.doubleValue());
                } else if (value instanceof Boolean bool) {
                    cell.setCellValue(bool);
                } else if (value instanceof LocalDateTime dt) {
                    cell.setCellValue(dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                } else {
                    cell.setCellValue(String.valueOf(value));
                }
            }
        }
    }

    @FunctionalInterface
    private interface RowMapper<T> {
        Object[] map(T row);
    }
}
