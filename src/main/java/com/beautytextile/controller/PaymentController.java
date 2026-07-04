package com.beautytextile.controller;

import com.beautytextile.dto.PaymentRequest;
import com.beautytextile.dto.PaymentResponse;
import com.beautytextile.dto.PaymentVerifyRequest;
import com.beautytextile.exception.BusinessException;
import com.beautytextile.model.Order;
import com.beautytextile.service.OrderService;
import com.beautytextile.service.RazorpayService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    private final RazorpayService razorpayService;
    private final OrderService orderService;

    public PaymentController(RazorpayService razorpayService, OrderService orderService) {
        this.razorpayService = razorpayService;
        this.orderService = orderService;
    }

    @PostMapping("/create")
    public PaymentResponse create(@RequestBody PaymentRequest req) {
        return razorpayService.createOrder(req.amount());
    }

    @PostMapping("/verify")
    public Map<String, Object> verify(@RequestBody PaymentVerifyRequest req) {
        boolean ok = razorpayService.verifySignature(
                req.razorpayOrderId(), req.razorpayPaymentId(), req.razorpaySignature());
        if (!ok) {
            throw new BusinessException("Payment verification failed");
        }
        Order order = orderService.markPaid(req.orderId(), req.razorpayOrderId(), req.razorpayPaymentId());
        return Map.of(
                "success", true,
                "orderId", order.getId(),
                "paymentStatus", order.getPaymentStatus()
        );
    }
}
