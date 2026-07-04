package com.beautytextile.controller;

import com.beautytextile.model.Product;
import com.beautytextile.service.ProductService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Admin-only utilities used by dashboard. */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final ProductService productService;

    public AdminController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/low-stock")
    public List<Product> lowStock() {
        return productService.lowStock();
    }
}
