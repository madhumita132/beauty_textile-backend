package com.beautytextile.service;

import com.beautytextile.dto.ItemRequest;
import com.beautytextile.dto.OrderRequest;
import com.beautytextile.exception.ResourceNotFoundException;
import com.beautytextile.model.*;
import com.beautytextile.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepo;
    private final ProductService productService;

    public OrderService(OrderRepository orderRepo, ProductService productService) {
        this.orderRepo = orderRepo;
        this.productService = productService;
    }

    public List<Order> findAll() {
        return orderRepo.findAllByOrderByCreatedAtDesc();
    }

    public Order findById(Long id) {
        return orderRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
    }

    /**
     * Create an online order. Stock is reduced immediately (validated per item).
     * Payment status starts PENDING; updated after Razorpay verification.
     */
    @Transactional
    public Order createOrder(OrderRequest req) {
        Order order = Order.builder()
                .customerName(req.customerName())
                .phone(req.phone())
                .address(req.address())
                .paymentStatus(PaymentStatus.PENDING)
                .totalAmount(BigDecimal.ZERO)
                .build();

        BigDecimal total = BigDecimal.ZERO;
        for (ItemRequest ir : req.items()) {
            Product p = productService.findById(ir.productId());
            productService.reduceStock(p.getId(), ir.quantity());

            BigDecimal lineTotal = p.getPrice().multiply(BigDecimal.valueOf(ir.quantity()));
            total = total.add(lineTotal);

            order.addItem(OrderItem.builder()
                    .productId(p.getId())
                    .productName(p.getName())
                    .quantity(ir.quantity())
                    .price(p.getPrice())
                    .build());
        }
        order.setTotalAmount(total);
        return orderRepo.save(order);
    }

    @Transactional
    public Order markPaid(Long orderId, String razorpayOrderId, String razorpayPaymentId) {
        Order order = findById(orderId);
        order.setPaymentStatus(PaymentStatus.PAID);
        order.setRazorpayOrderId(razorpayOrderId);
        order.setRazorpayPaymentId(razorpayPaymentId);
        // Auto-confirm when payment is complete
        order.setFulfillmentStatus("CONFIRMED");
        return orderRepo.save(order);
    }

    /** Admin updates the fulfilment stage: CONFIRMED → PACKED → SHIPPED → DELIVERED */
    @Transactional
    public Order updateFulfillmentStatus(Long orderId, String status) {
        Order order = findById(orderId);
        order.setFulfillmentStatus(status.toUpperCase());
        return orderRepo.save(order);
    }
}
