package com.beautytextile.controller;

import com.beautytextile.dto.BillingRequest;
import com.beautytextile.model.Billing;
import com.beautytextile.service.BillingService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/billing")
public class BillingController {

    private final BillingService service;

    public BillingController(BillingService service) {
        this.service = service;
    }

    @PostMapping
    public Billing create(@Valid @RequestBody BillingRequest req) {
        return service.createBill(req);
    }

    @GetMapping
    public List<Billing> all() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public Billing byId(@PathVariable Long id) {
        return service.findById(id);
    }
}
