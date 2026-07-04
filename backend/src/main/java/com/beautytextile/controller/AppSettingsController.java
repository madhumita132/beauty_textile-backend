package com.beautytextile.controller;

import com.beautytextile.dto.GstSettingsRequest;
import com.beautytextile.model.AppSettings;
import com.beautytextile.service.AppSettingsService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings")
public class AppSettingsController {

    private final AppSettingsService svc;

    public AppSettingsController(AppSettingsService svc) {
        this.svc = svc;
    }

    /** Public — billing component loads this on page init */
    @GetMapping("/gst")
    public AppSettings getGst() {
        return svc.getSettings();
    }

    /** Admin-only — save GST configuration */
    @PutMapping("/gst")
    public AppSettings updateGst(@RequestBody GstSettingsRequest req) {
        return svc.updateGst(req.gstEnabled(), req.gstPercentage());
    }
}
