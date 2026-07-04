package com.beautytextile.service;

import com.beautytextile.dto.BillingRequest;
import com.beautytextile.dto.ItemRequest;
import com.beautytextile.exception.ResourceNotFoundException;
import com.beautytextile.model.AppSettings;
import com.beautytextile.model.Billing;
import com.beautytextile.model.BillingItem;
import com.beautytextile.model.Product;
import com.beautytextile.repository.BillingRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class BillingService {

    private final BillingRepository billingRepo;
    private final ProductService productService;
    private final WhatsAppService whatsAppService;
    private final DiscountService discountService;
    private final AppSettingsService appSettingsService;

    @Value("${app.shop.name}")
    private String shopName;

    public BillingService(BillingRepository billingRepo,
                          ProductService productService,
                          WhatsAppService whatsAppService,
                          DiscountService discountService,
                          AppSettingsService appSettingsService) {
        this.billingRepo        = billingRepo;
        this.productService     = productService;
        this.whatsAppService    = whatsAppService;
        this.discountService    = discountService;
        this.appSettingsService = appSettingsService;
    }

    public List<Billing> findAll() {
        return billingRepo.findAllByOrderByCreatedAtDesc();
    }

    public Billing findById(Long id) {
        return billingRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found: " + id));
    }

    /** Create a POS bill, reduce stock, optionally send WhatsApp. */
    @Transactional
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
}
