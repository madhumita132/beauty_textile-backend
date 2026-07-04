package com.beautytextile.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * A single slide shown in the customer home page hero banner.
 * Admin can create/update/delete slides and upload the background image for each.
 */
@Entity
@Table(name = "hero_slides")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class HeroSlide {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Small pill text shown above the title, e.g. "New Collection". */
    @Column(length = 60)
    private String kicker;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(length = 400)
    private String text;

    /** Background image path served from /images/... */
    @Column(name = "image_path", length = 255)
    private String imagePath;

    /** Controls display order on the home page (ascending). */
    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private int sortOrder = 0;
}
