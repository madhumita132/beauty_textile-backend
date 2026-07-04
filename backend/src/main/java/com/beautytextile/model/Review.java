package com.beautytextile.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "reviews", indexes = {
    @Index(name = "idx_review_product_id", columnList = "product_id"),
    @Index(name = "idx_review_status",     columnList = "status"),
    @Index(name = "idx_review_rating",     columnList = "rating"),
    @Index(name = "idx_review_created",    columnList = "created_at")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "customer_name", length = 100)
    private String customerName;

    @Column(name = "mobile_number", length = 15)
    private String mobileNumber;

    @Column(name = "rating", nullable = false)
    private int rating;   // 1–5

    @Column(name = "review_comment", length = 1000)
    private String reviewComment;

    /**
     * Moderation status.
     * PENDING → awaiting admin approval
     * APPROVED → visible publicly
     * REJECTED → hidden
     */
    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private String status = "PENDING";

    /** Admin reply text shown below the review */
    @Column(name = "admin_reply", length = 500)
    private String adminReply;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }
}
