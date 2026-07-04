package com.beautytextile.controller;

import com.beautytextile.dto.AdminReviewAction;
import com.beautytextile.dto.ReviewRequest;
import com.beautytextile.model.Review;
import com.beautytextile.service.ReviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService svc;

    public ReviewController(ReviewService svc) {
        this.svc = svc;
    }

    // ── Public ────────────────────────────────────────────────────────────────

    /** Customer submits a review — stored as PENDING */
    @PostMapping
    public Review submitReview(@RequestBody ReviewRequest req) {
        return svc.submitReview(req);
    }

    /** Approved reviews for a product page; optional ?star=4 filter */
    @GetMapping("/product/{productId}")
    public List<Review> getProductReviews(
            @PathVariable Long productId,
            @RequestParam(required = false) Integer star) {
        return svc.getApprovedReviews(productId, star);
    }

    /** Rating summary (avgRating + totalReviews) for a product */
    @GetMapping("/product/{productId}/summary")
    public Map<String, Object> getProductSummary(@PathVariable Long productId) {
        return svc.getProductRatingSummary(productId);
    }

    /** Latest approved reviews for home-page testimonials */
    @GetMapping("/testimonials")
    public List<Review> getTestimonials(
            @RequestParam(defaultValue = "6") int limit) {
        return svc.getTestimonials(limit);
    }

    // ── Admin ─────────────────────────────────────────────────────────────────

    @GetMapping("/admin/all")
    public List<Review> getAllReviews() {
        return svc.getAllReviews();
    }

    @GetMapping("/admin/stats")
    public Map<String, Object> getStats() {
        return svc.getStats();
    }

    @GetMapping("/admin/top-rated")
    public List<Map<String, Object>> getTopRated() {
        return svc.getTopRatedProducts();
    }

    @PutMapping("/admin/{id}/approve")
    public Review approve(@PathVariable Long id) {
        return svc.approveReview(id);
    }

    @PutMapping("/admin/{id}/reject")
    public Review reject(@PathVariable Long id) {
        return svc.rejectReview(id);
    }

    @PutMapping("/admin/{id}/reply")
    public Review reply(@PathVariable Long id, @RequestBody AdminReviewAction req) {
        return svc.addReply(id, req.reply());
    }

    @DeleteMapping("/admin/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        svc.deleteReview(id);
        return ResponseEntity.noContent().build();
    }
}
