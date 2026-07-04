package com.beautytextile.service;

import com.beautytextile.model.AppSettings;
import com.beautytextile.repository.AppSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppSettingsService {

    private final AppSettingsRepository repo;

    public AppSettingsService(AppSettingsRepository repo) {
        this.repo = repo;
    }

    /** Returns the singleton settings row, creating defaults if absent. */
    public AppSettings getSettings() {
        return repo.findById(1L).orElseGet(() -> {
            AppSettings s = new AppSettings();
            s.setId(1L);
            s.setGstEnabled(false);
            s.setGstPercentage(0);
            return repo.save(s);
        });
    }

    /** Update GST settings. */
    @Transactional
    public AppSettings updateGst(boolean gstEnabled, int gstPercentage) {
        AppSettings s = getSettings();
        s.setGstEnabled(gstEnabled);
        s.setGstPercentage(gstPercentage);
        return repo.save(s);
    }
}
