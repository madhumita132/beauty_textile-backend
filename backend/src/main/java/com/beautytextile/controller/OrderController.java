package com.beautytextile.controller;

import com.beautytextile.dto.OrderRequest;
import com.beautytextile.model.Order;
import com.beautytextile.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public Order create(@Valid @RequestBody OrderRequest req) {
        return orderService.createOrder(req);
    }

    @GetMapping
    public List<Order> all() {
        return orderService.findAll();
    }

    @GetMapping("/{id}")
    public Order byId(@PathVariable Long id) {
        return orderService.findById(id);
    }

    /** Admin updates fulfilment status: CONFIRMED → PACKED → SHIPPED → DELIVERED */
    @PatchMapping("/{id}/fulfillment")
    public Order updateFulfillment(@PathVariable Long id,
                                   @RequestBody Map<String, String> body) {
        return orderService.updateFulfillmentStatus(id, body.get("status"));
    }
}
