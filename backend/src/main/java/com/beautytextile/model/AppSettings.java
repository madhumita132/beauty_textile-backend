package com.beautytextile.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * Singleton settings row (id always = 1).
 * Holds shop-wide configuration like GST.
 */
@Entity
@Table(name = "app_settings")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AppSettings {

    @Id
    private Long id;

    /** GST toggle — admin can enable/disable */
    @Column(name = "gst_enabled", nullable = false)
    @Builder.Default
    private boolean gstEnabled = false;

    /**
     * GST percentage applied to entire bill.
     * Allowed values: 0, 1, 2, 3, 5, 12, 18
     */
    @Column(name = "gst_percentage", nullable = false)
    @Builder.Default
    private int gstPercentage = 0;

    /** Ensures only one row exists */
    @PrePersist
    void ensureSingleton() {
        this.id = 1L;
    }
}
