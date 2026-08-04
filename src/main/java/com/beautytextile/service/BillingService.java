package com.beautytextile.service;

import com.beautytextile.dto.BillingRequest;
import com.beautytextile.dto.ItemRequest;
import com.beautytextile.exception.BusinessException;
import com.beautytextile.exception.ResourceNotFoundException;
import com.beautytextile.model.AppSettings;
import com.beautytextile.model.Billing;
import com.beautytextile.model.BillingItem;
import com.beautytextile.model.Product;
import com.beautytextile.repository.BillingRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class BillingService {

    private static final String STATUS_RETURNED = "RETURNED";

    private final BillingRepository billingRepo;
    private final ProductService productService;
    private final WhatsAppService whatsAppService;
    private final DiscountService discountService;
    private final AppSettingsService appSettingsService;
    private final InventoryService inventoryService;

    @Value("${app.shop.name}")
    private String shopName;

    public BillingService(BillingRepository billingRepo,
                          ProductService productService,
                          WhatsAppService whatsAppService,
                          DiscountService discountService,
                          AppSettingsService appSettingsService,
                          @Lazy InventoryService inventoryService) {
        this.billingRepo        = billingRepo;
        this.productService     = productService;
        this.whatsAppService    = whatsAppService;
        this.discountService    = discountService;
        this.appSettingsService = appSettingsService;
        this.inventoryService   = inventoryService;
    }

    @Cacheable(cacheNames = "billing", key = "'all'")
    public List<Billing> findAll() {
        return billingRepo.findAllWithItemsOrderByCreatedAtDesc();
    }

    @Cacheable(cacheNames = "billingById", key = "#id")
    public Billing findById(Long id) {
        return billingRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found: " + id));
    }

    /** Create a POS bill, reduce stock, optionally send WhatsApp. */
    @Transactional
    @CacheEvict(cacheNames = {"billing", "billingById", "products", "productById", "productByBarcode"}, allEntries = true)
    public Billing createBill(BillingRequest req) {
        Billing bill = Billing.builder()
                .customerName(req.customerName())
                .phone(req.phone())
                .paymentMode(req.paymentMode() == null ? "CASH" : req.paymentMode())
                .totalAmount(BigDecimal.ZERO)
                .build();

        BigDecimal total = BigDecimal.ZERO;
        StringBuilder itemsBlock = new StringBuilder();

        for (ItemRequest ir : req.items()) {
            Product p = productService.findById(ir.productId());
            productService.reduceStock(p.getId(), ir.quantity());

            BigDecimal lineTotal = p.getPrice().multiply(BigDecimal.valueOf(ir.quantity()));
            total = total.add(lineTotal);

            bill.addItem(BillingItem.builder()
                    .productId(p.getId())
                    .productName(p.getName())
                    .quantity(ir.quantity())
                    .price(p.getPrice())
                    .build());

            itemsBlock.append(p.getName())
                    .append(" x").append(ir.quantity())
                    .append(" = ₹").append(lineTotal).append("\n");
        }

        bill.setTotalAmount(total);

        // Apply billing-level discount
        String dtype = req.discountType() != null ? req.discountType() : "NONE";
        BigDecimal dvalue = req.discountValue() != null ? req.discountValue() : BigDecimal.ZERO;
        BigDecimal discountAmt = discountService.computeBillingDiscount(total, dtype, dvalue);
        bill.setDiscountType(dtype);
        bill.setDiscountValue(dvalue);
        bill.setDiscountAmount(discountAmt);
        BigDecimal finalAmt = total.subtract(discountAmt);
        bill.setFinalAmount(finalAmt);

        // Apply GST from shop settings
        AppSettings settings = appSettingsService.getSettings();
        int gstPct = settings.isGstEnabled() ? settings.getGstPercentage() : 0;
        BigDecimal gstAmt = gstPct > 0
                ? finalAmt.multiply(BigDecimal.valueOf(gstPct))
                          .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        bill.setGstPercentage(gstPct);
        bill.setGstAmount(gstAmt);
        bill.setGrandTotal(finalAmt.add(gstAmt));

        Billing saved = billingRepo.save(bill);

        if (req.sendWhatsApp() && req.phone() != null && !req.phone().isBlank()) {
            String message = whatsAppService.buildBillMessage(
                    shopName, req.customerName(), itemsBlock.toString(), total.toString());
            whatsAppService.sendMessage(req.phone(), message);
        }
        return saved;
    }

    /**
     * Search past bills by any combination of customer name, phone and date.
     * Whichever filter is most selective (date, then phone, then name) drives the
     * DB query; the remaining filters (if any) are applied in-memory on that
     * already-small result set — this shop runs 1-2 tills, so bill volume per
     * query is small and a dynamic-criteria query isn't worth the complexity.
     */
    @Transactional(readOnly = true)
    public List<Billing> search(String name, String phone, String date) {
        String n = name  == null ? "" : name.trim();
        String p = phone == null ? "" : phone.trim();
        String d = date  == null ? "" : date.trim();

        List<Billing> base;
        if (!d.isEmpty()) {
            LocalDate parsed = LocalDate.parse(d);
            base = billingRepo.findByCreatedAtBetweenOrderByCreatedAtDescWithItems(
                    parsed.atStartOfDay(), parsed.plusDays(1).atStartOfDay());
        } else if (!p.isEmpty()) {
            base = billingRepo.findByPhoneOrderByCreatedAtDescWithItems(p);
        } else if (!n.isEmpty()) {
            base = billingRepo.findByCustomerNameContainingIgnoreCaseOrderByCreatedAtDescWithItems(n);
        } else {
            base = findAll();
        }

        String nLower = n.toLowerCase();
        return base.stream()
                .filter(b -> p.isEmpty() || (b.getPhone() != null && b.getPhone().contains(p)))
                .filter(b -> n.isEmpty() || (b.getCustomerName() != null && b.getCustomerName().toLowerCase().contains(nLower)))
                .toList();
    }

    /**
     * Edit a previously-saved bill: replace its line items/customer/discount info,
     * recompute totals + GST, and reconcile stock for any quantity/product changes
     * (increase sold qty → reduce stock further, decrease/remove → return stock),
     * all logged to the stock-adjustment audit trail under reason BILLING_EDIT.
     */
    @Transactional
    @CacheEvict(cacheNames = {"billing", "billingById", "products", "productById", "productByBarcode"}, allEntries = true)
    public Billing updateBill(Long id, BillingRequest req) {
        Billing bill = billingRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found: " + id));

        if (STATUS_RETURNED.equals(bill.getStatus())) {
            throw new BusinessException("Bill #" + id + " is fully returned and cannot be edited.");
        }

        Map<Long, Integer> originalQty = new LinkedHashMap<>();
        for (BillingItem item : bill.getItems()) {
            originalQty.merge(item.getProductId(), item.getQuantity(), Integer::sum);
        }

        Map<Long, Integer> newQty = new LinkedHashMap<>();
        for (ItemRequest ir : req.items()) {
            newQty.merge(ir.productId(), ir.quantity(), Integer::sum);
        }

        // Reconcile stock for products present in the new item list
        for (Map.Entry<Long, Integer> e : newQty.entrySet()) {
            int oldQty = originalQty.getOrDefault(e.getKey(), 0);
            int delta  = e.getValue() - oldQty;   // +ve: more sold now, -ve: less sold now
            if (delta == 0) continue;

            Product p = productService.findById(e.getKey());
            int before = p.getStock();
            if (delta > 0) {
                productService.reduceStock(e.getKey(), delta);
            } else {
                productService.addStock(e.getKey(), -delta);
            }
            inventoryService.recordAdjustment(p, -delta, before, "BILLING_EDIT", bill.getId(),
                    "Bill #" + bill.getId() + " edited (quantity change)", "admin");
        }

        // Products removed entirely from the bill — return their stock
        for (Map.Entry<Long, Integer> e : originalQty.entrySet()) {
            if (newQty.containsKey(e.getKey())) continue;
            Product p = productService.findById(e.getKey());
            int before = p.getStock();
            productService.addStock(e.getKey(), e.getValue());
            inventoryService.recordAdjustment(p, e.getValue(), before, "BILLING_EDIT", bill.getId(),
                    "Bill #" + bill.getId() + " edited (item removed)", "admin");
        }

        // Rebuild line items from the request
        bill.getItems().clear();
        BigDecimal total = BigDecimal.ZERO;
        for (ItemRequest ir : req.items()) {
            Product p = productService.findById(ir.productId());
            BigDecimal lineTotal = p.getPrice().multiply(BigDecimal.valueOf(ir.quantity()));
            total = total.add(lineTotal);
            bill.addItem(BillingItem.builder()
                    .productId(p.getId())
                    .productName(p.getName())
                    .quantity(ir.quantity())
                    .price(p.getPrice())
                    .build());
        }

        bill.setCustomerName(req.customerName());
        bill.setPhone(req.phone());
        if (req.paymentMode() != null) bill.setPaymentMode(req.paymentMode());
        bill.setTotalAmount(total);

        String dtype = req.discountType() != null ? req.discountType() : "NONE";
        BigDecimal dvalue = req.discountValue() != null ? req.discountValue() : BigDecimal.ZERO;
        BigDecimal discountAmt = discountService.computeBillingDiscount(total, dtype, dvalue);
        bill.setDiscountType(dtype);
        bill.setDiscountValue(dvalue);
        bill.setDiscountAmount(discountAmt);
        BigDecimal finalAmt = total.subtract(discountAmt);
        bill.setFinalAmount(finalAmt);

        AppSettings settings = appSettingsService.getSettings();
        int gstPct = settings.isGstEnabled() ? settings.getGstPercentage() : 0;
        BigDecimal gstAmt = gstPct > 0
                ? finalAmt.multiply(BigDecimal.valueOf(gstPct))
                          .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        bill.setGstPercentage(gstPct);
        bill.setGstAmount(gstAmt);
        bill.setGrandTotal(finalAmt.add(gstAmt));

        return billingRepo.save(bill);
    }
}
