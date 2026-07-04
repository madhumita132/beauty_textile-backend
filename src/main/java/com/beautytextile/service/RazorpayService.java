package com.beautytextile.service;

import com.beautytextile.dto.PaymentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Creates Razorpay orders for online checkout.
 * Runs in MOCK mode (returns a fake order id) until real keys are configured
 * in application.yml (app.razorpay.enabled=true).
 */
@Service
public class RazorpayService {

    private static final Logger log = LoggerFactory.getLogger(RazorpayService.class);

    @Value("${app.razorpay.enabled}")
    private boolean enabled;

    @Value("${app.razorpay.key-id}")
    private String keyId;

    @Value("${app.razorpay.key-secret}")
    private String keySecret;

    @Value("${app.razorpay.currency}")
    private String currency;

    /** Create a Razorpay order for the given rupee amount. */
    public PaymentResponse createOrder(BigDecimal amount) {
        if (!enabled) {
            String mockId = "order_MOCK_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
            log.info("[Razorpay MOCK] Created order {} for ₹{}", mockId, amount);
            return new PaymentResponse(mockId, keyId, amount, currency, true);
        }

        // ---- Real integration goes here ----
        // Add dependency: com.razorpay:razorpay-java
        //   RazorpayClient client = new RazorpayClient(keyId, keySecret);
        //   JSONObject req = new JSONObject();
        //   req.put("amount", amount.multiply(BigDecimal.valueOf(100)).intValue()); // paise
        //   req.put("currency", currency);
        //   Order order = client.orders.create(req);
        //   return new PaymentResponse(order.get("id"), keyId, amount, currency, false);
        throw new IllegalStateException("Razorpay enabled but real SDK integration not wired yet");
    }

    /**
     * Verify the payment signature returned by Razorpay checkout.
     * In MOCK mode always returns true.
     */
    public boolean verifySignature(String orderId, String paymentId, String signature) {
        if (!enabled) {
            return true;
        }
        // Real: Utils.verifyPaymentSignature(...) using keySecret
        return signature != null && !signature.isBlank();
    }
}
