package com.beautytextile.controller;

import com.beautytextile.dto.ExchangeRequest;
import com.beautytextile.dto.ReturnRequest;
import com.beautytextile.model.Billing;
import com.beautytextile.model.Exchange;
import com.beautytextile.model.Return;
import com.beautytextile.service.ReturnExchangeService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/returns")
public class ReturnExchangeController {

    private final ReturnExchangeService svc;

    public ReturnExchangeController(ReturnExchangeService svc) { this.svc = svc; }

    // ── Bill search ──────────────────────────────────────────────────────────

    @GetMapping("/search/bill/{id}")
    public ResponseEntity<Billing> getBill(@PathVariable Long id) {
        return ResponseEntity.ok(svc.findBillById(id));
    }

    @GetMapping("/search/phone/{phone}")
    public ResponseEntity<List<Billing>> byPhone(@PathVariable String phone) {
        return ResponseEntity.ok(svc.findBillsByPhone(phone));
    }

    @GetMapping("/search/date")
    public ResponseEntity<List<Billing>> byDate(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(svc.findBillsByDate(date));
    }

    // ── Return ───────────────────────────────────────────────────────────────

    @PostMapping("/return")
    public ResponseEntity<Return> doReturn(@RequestBody ReturnRequest req) {
        return ResponseEntity.ok(svc.processReturn(req));
    }

    @GetMapping("/return/bill/{billId}")
    public ResponseEntity<List<Return>> returnsByBill(@PathVariable Long billId) {
        return ResponseEntity.ok(svc.returnsByBill(billId));
    }

    @GetMapping("/return/all")
    public ResponseEntity<List<Return>> allReturns() {
        return ResponseEntity.ok(svc.allReturns());
    }

    // ── Exchange ─────────────────────────────────────────────────────────────

    @PostMapping("/exchange")
    public ResponseEntity<Exchange> doExchange(@RequestBody ExchangeRequest req) {
        return ResponseEntity.ok(svc.processExchange(req));
    }

    @GetMapping("/exchange/bill/{billId}")
    public ResponseEntity<List<Exchange>> exchangesByBill(@PathVariable Long billId) {
        return ResponseEntity.ok(svc.exchangesByBill(billId));
    }

    @GetMapping("/exchange/all")
    public ResponseEntity<List<Exchange>> allExchanges() {
        return ResponseEntity.ok(svc.allExchanges());
    }

    // ── Monthly stats ────────────────────────────────────────────────────────

    @GetMapping("/stats/monthly/{month}")
    public ResponseEntity<Map<String, Object>> monthlyStats(@PathVariable String month) {
        return ResponseEntity.ok(svc.monthlyStats(month));
    }
}
