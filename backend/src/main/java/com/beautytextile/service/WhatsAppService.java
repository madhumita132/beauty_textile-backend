package com.beautytextile.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * WhatsApp messaging — currently disabled.
 * To enable: add Twilio dependency, configure credentials in application.yml,
 * and implement sendMessage() with Twilio SDK.
 */
@Service
public class WhatsAppService {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppService.class);

    public boolean sendMessage(String phone, String message) {
        if (phone == null || phone.isBlank()) return false;
        log.debug("[WhatsApp disabled] Would send to {}", normalize(phone));
        return true;
    }

    public String buildBillMessage(String shopName, String customerName,
                                   String itemsBlock, String total) {
        String name = (customerName == null || customerName.isBlank()) ? "Customer" : customerName;
        return "Hello " + name + ",\n\nYour bill from " + shopName + ":\n\n"
                + itemsBlock + "\nTotal: \u20b9" + total + "\n\nThank you!";
    }

    private String normalize(String phone) {
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.length() == 10) digits = "91" + digits;
        return "+" + digits;
    }
}

