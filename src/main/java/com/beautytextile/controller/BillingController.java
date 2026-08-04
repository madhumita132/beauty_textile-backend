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

    /** Search past bills by customer name and/or phone and/or exact date (yyyy-MM-dd). */
    @GetMapping("/search")
    public List<Billing> search(@RequestParam(required = false) String name,
                                 @RequestParam(required = false) String phone,
                                 @RequestParam(required = false) String date) {
        return service.search(name, phone, date);
    }

    /** Edit a previously-saved bill (items, customer info, discount) after billing. */
    @PutMapping("/{id}")
    public Billing update(@PathVariable Long id, @Valid @RequestBody BillingRequest req) {
        return service.updateBill(id, req);
    }
}
