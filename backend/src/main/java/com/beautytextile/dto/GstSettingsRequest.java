package com.beautytextile.dto;

/** Request body for updating GST settings. */
public record GstSettingsRequest(
        boolean gstEnabled,
        int gstPercentage   // 0, 1, 2, 3, 5, 12, 18
) {}
