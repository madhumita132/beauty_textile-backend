package com.beautytextile.service;

import com.beautytextile.dto.ExchangeRequest;
import com.beautytextile.dto.ReturnItemRequest;
import com.beautytextile.dto.ReturnRequest;
import com.beautytextile.exception.BusinessException;
import com.beautytextile.exception.ResourceNotFoundException;
import com.beautytextile.model.*;
import com.beautytextile.repository.BillingRepository;
import com.beautytextile.repository.ExchangeRepository;
import com.beautytextile.repository.ReturnRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReturnExchangeService {

    private final BillingRepository billingRepo;
    private final ReturnRepository returnRepo;
    private final ExchangeRepository exchangeRepo;
    private final ProductService productService;
    private final InventoryService inventoryService;

    public ReturnExchangeService(BillingRepository billingRepo,
                                  ReturnRepository returnRepo,
                                  ExchangeRepository exchangeRepo,
                                  ProductService productService,
                                  @Lazy InventoryService inventoryService) {
        this.billingRepo      = billingRepo;
        this.returnRepo       = returnRepo;
        this.exchangeRepo     = exchangeRepo;
        this.productService   = productService;
        this.inventoryService = inventoryService;
    }

    // ── Search bills ────────────────────────────────────────────────────────

    public List<Billing> findBillsByPhone(String phone) {
        return billingRepo.findByPhoneOrderByCreatedAtDesc(phone.trim());
    }

    public Billing findBillById(Long id) {
        return billingRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found: " + id));
    }

    public List<Billing> findBillsByDate(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end   = date.plusDays(1).atStartOfDay();
        return billingRepo.findByCreatedAtBetweenOrderByCreatedAtDesc(start, end);
    }

    // ── Returns ─────────────────────────────────────────────────────────────

    /** Full or partial return. Updates stock and bill status. */
    @Transactional
    public Return processReturn(ReturnRequest req) {
        Billing bill = findBillById(req.billId());
        if ("RETURNED".equals(bill.getStatus())) {
            throw new BusinessException("Bill #" + bill.getId() + " is already fully returned.");
        }

        List<ReturnItemRequest> itemsToReturn = resolveItems(bill, req);

        // Validate quantities don't exceed original purchase
        Map<Long, Integer> origQty = bill.getItems().stream()
                .collect(Collectors.toMap(BillingItem::getProductId, BillingItem::getQuantity));

        BigDecimal refundTotal = BigDecimal.ZERO;
        Return returnRecord = Return.builder()
                .billId(bill.getId())
                .customerName(bill.getCustomerName())
                .customerPhone(bill.getPhone())
                .returnType(req.returnType() != null ? req.returnType() : "FULL")
                .returnReason(req.returnReason())
                .refundMethod(req.refundMethod() != null ? req.refundMethod() : "CASH")
                .processedBy(req.processedBy())
                .build();

        for (ReturnItemRequest item : itemsToReturn) {
            Integer orig = origQty.get(item.productId());
            if (orig == null) {
                throw new BusinessException("Product #" + item.productId() + " not found in bill #" + bill.getId());
            }
            if (item.quantity() > orig) {
                throw new BusinessException("Cannot return more than purchased. Purchased: " + orig
                        + ", Returning: " + item.quantity() + " for product " + item.productName());
            }

            // Restore stock
            Product p = productService.findById(item.productId());
            productService.addStock(item.productId(), item.quantity());
            // Audit trail
            inventoryService.recordAdjustment(p, item.quantity(),
                    p.getStock() - item.quantity(), "RETURN", returnRecord.getId(),
                    "Return of bill #" + bill.getId(), req.processedBy());

            refundTotal = refundTotal.add(item.price().multiply(BigDecimal.valueOf(item.quantity())));
            returnRecord.addItem(ReturnItem.builder()
                    .productId(item.productId())
                    .productName(item.productName())
                    .quantity(item.quantity())
                    .price(item.price())
                    .build());
        }

        returnRecord.setRefundAmount(refundTotal);

        // Update bill status
        boolean allItemsReturned = isAllItemsReturned(bill, itemsToReturn);
        bill.setStatus(allItemsReturned ? "RETURNED" : "PARTIALLY_RETURNED");
        billingRepo.save(bill);

        return returnRepo.save(returnRecord);
    }

    // ── Exchanges ────────────────────────────────────────────────────────────

    /** Exchange old product for new product. Adjusts stock + returns exchange record. */
    @Transactional
    public Exchange processExchange(ExchangeRequest req) {
        Billing bill = findBillById(req.oldBillId());
        if ("RETURNED".equals(bill.getStatus())) {
            throw new BusinessException("Bill #" + bill.getId() + " is already fully returned; cannot exchange.");
        }

        // Validate old product is in the bill
        BillingItem oldLine = bill.getItems().stream()
                .filter(i -> i.getProductId().equals(req.oldProductId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException("Product #" + req.oldProductId()
                        + " not found in bill #" + bill.getId()));

        int returnQty = req.oldQuantity() != null ? req.oldQuantity() : 1;
        if (returnQty > oldLine.getQuantity()) {
            throw new BusinessException("Cannot exchange more than purchased quantity ("
                    + oldLine.getQuantity() + ").");
        }

        Product oldProduct = productService.findById(req.oldProductId());
        Product newProduct = productService.findById(req.newProductId());

        // Validate new product has stock
        int newQty = req.newQuantity() != null ? req.newQuantity() : 1;
        if (newProduct.getStock() < newQty) {
            throw new BusinessException("Insufficient stock for new product '" + newProduct.getName()
                    + "' (available: " + newProduct.getStock() + ")");
        }

        // Stock adjustments
        productService.addStock(oldProduct.getId(), returnQty);      // returned item back to shelf
        productService.reduceStock(newProduct.getId(), newQty);      // new item out to customer

        // Audit trail
        inventoryService.recordAdjustment(oldProduct, returnQty, oldProduct.getStock() - returnQty,
                "EXCHANGE", null, "Exchange: returned " + oldProduct.getName(), req.processedBy());
        inventoryService.recordAdjustment(newProduct, -newQty, newProduct.getStock() + newQty,
                "EXCHANGE", null, "Exchange: new " + newProduct.getName(), req.processedBy());

        BigDecimal oldTotal = oldProduct.getPrice().multiply(BigDecimal.valueOf(returnQty));
        BigDecimal newTotal = newProduct.getPrice().multiply(BigDecimal.valueOf(newQty));
        BigDecimal diff     = newTotal.subtract(oldTotal); // +ve: customer pays; -ve: shop owes

        Exchange exchange = Exchange.builder()
                .oldBillId(bill.getId())
                .oldProductId(oldProduct.getId())
                .oldProductName(oldProduct.getName())
                .oldQuantity(returnQty)
                .oldPrice(oldProduct.getPrice())
                .newProductId(newProduct.getId())
                .newProductName(newProduct.getName())
                .newQuantity(newQty)
                .newPrice(newProduct.getPrice())
                .priceDifference(diff)
                .refundMethod(req.refundMethod() != null ? req.refundMethod() : "CASH")
                .customerName(bill.getCustomerName())
                .customerPhone(bill.getPhone())
                .exchangeReason(req.exchangeReason())
                .processedBy(req.processedBy())
                .build();

        return exchangeRepo.save(exchange);
    }

    // ── History & Reports ───────────────────────────────────────────────────

    public List<Return>   allReturns()   { return returnRepo.findAllByOrderByReturnDateDesc(); }
    public List<Exchange> allExchanges() { return exchangeRepo.findAllByOrderByExchangeDateDesc(); }

    public List<Return>   returnsByBill(Long billId)   { return returnRepo.findByBillId(billId); }
    public List<Exchange> exchangesByBill(Long billId) { return exchangeRepo.findByOldBillId(billId); }

    /** Monthly stats for reports. month = "YYYY-MM" */
    public Map<String, Object> monthlyStats(String month) {
        LocalDate start = LocalDate.parse(month + "-01");
        LocalDate end   = start.plusMonths(1);
        LocalDateTime dtStart = start.atStartOfDay();
        LocalDateTime dtEnd   = end.atStartOfDay();

        long returnCount   = returnRepo.countBetween(dtStart, dtEnd);
        long exchangeCount = exchangeRepo.countBetween(dtStart, dtEnd);
        BigDecimal refunds = returnRepo.sumRefundsBetween(dtStart, dtEnd);

        List<Return>   returns   = returnRepo.findByDateRange(dtStart, dtEnd);
        List<Exchange> exchanges = exchangeRepo.findByDateRange(dtStart, dtEnd);

        return Map.of(
                "totalReturns",   returnCount,
                "totalExchanges", exchangeCount,
                "totalRefunds",   refunds,
                "returns",        returns,
                "exchanges",      exchanges
        );
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /** If request has no items and it's a FULL return, use all bill items. */
    private List<ReturnItemRequest> resolveItems(Billing bill, ReturnRequest req) {
        if (req.itemsToReturn() != null && !req.itemsToReturn().isEmpty()) {
            return req.itemsToReturn();
        }
        return bill.getItems().stream()
                .map(i -> new ReturnItemRequest(i.getProductId(), i.getProductName(),
                        i.getQuantity(), i.getPrice()))
                .collect(Collectors.toList());
    }

    private boolean isAllItemsReturned(Billing bill, List<ReturnItemRequest> returned) {
        Map<Long, Integer> returnedMap = returned.stream()
                .collect(Collectors.toMap(ReturnItemRequest::productId,
                        ReturnItemRequest::quantity, Integer::sum));
        return bill.getItems().stream()
                .allMatch(i -> returnedMap.getOrDefault(i.getProductId(), 0) >= i.getQuantity());
    }
}
