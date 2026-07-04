package com.beautytextile.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Forwards all non-API, non-static routes to Angular's index.html.
 *
 * This is required for Angular's client-side routing to work when
 * the user refreshes a page or navigates directly to a URL like
 * /products/5 or /admin/billing.
 *
 * Spring Boot serves static files from src/main/resources/static/
 * (Angular build output copied there by build-and-run.bat).
 *
 * The pattern excludes:
 *  - /api/**      → handled by REST controllers
 *  - /images/**   → static uploads served by ResourceHttpRequestHandler
 *  - /actuator/** → Spring Boot Actuator
 *  - /error       → Spring error page
 */
@Controller
public class SpaController {

    @RequestMapping(value = {
        "/",
        "/products",
        "/products/{id:[\\d]+}",
        "/cart",
        "/checkout",
        "/order-confirmation/{id:[\\d]+}",
        "/admin",
        "/admin/login",
        "/admin/dashboard",
        "/admin/products",
        "/admin/billing",
        "/admin/orders",
        "/admin/reports",
        "/admin/discounts",
        "/admin/returns",
        "/admin/inventory",
        "/admin/reviews",
        "/admin/settings"
    })
    public String forwardToAngular() {
        return "forward:/index.html";
    }
}
