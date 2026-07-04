package com.beautytextile.service;

import com.beautytextile.dto.ReviewRequest;
import com.beautytextile.exception.BusinessException;
import com.beautytextile.exception.ResourceNotFoundException;
import com.beautytextile.model.Review;
import com.beautytextile.repository.ReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReviewService {

    private final ReviewRepository repo;

    public ReviewService(ReviewRepository repo) {
        this.repo = repo;
    }

    // ── Customer ──────────────────────────────────────────────────────────────

    /** Submit a new review — starts in PENDING status awaiting admin approval. */
    @Transactional
    public Review submitReview(ReviewRequest req) {
        if (req.rating() < 1 || req.rating() > 5) {
            throw new BusinessException("Rating must be between 1 and 5");
        }
        if (req.productId() == null) {
            throw new BusinessException("Product ID is required");
        }
        Review r = Review.builder()
                .productId(req.productId())
                .customerName(req.customerName() != null ? req.customerName().trim() : "Anonymous")
                .mobileNumber(req.mobileNumber())
                .rating(req.rating())
                .reviewComment(req.reviewComment())
                .status("PENDING")
                .build();
        return repo.save(r);
    }

    /** Approved reviews for a product page (optionally filtered by star). */
    public List<Review> getApprovedReviews(Long productId, Integer starFilter) {
        if (starFilter != null && starFilter >= 1 && starFilter <= 5) {
            return repo.findByProductIdAndRatingAndStatusOrderByCreatedAtDesc(productId, starFilter, "APPROVED");
        }
        return repo.findByProductIdAndStatusOrderByCreatedAtDesc(productId, "APPROVED");
    }

    /** Rating summary for a product: avgRating + reviewCount */
    public Map<String, Object> getProductRatingSummary(Long productId) {
        double avg = repo.avgRatingByProduct(productId);
        long count = repo.countApprovedByProduct(productId);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("averageRating", Math.round(avg * 10.0) / 10.0);
        m.put("totalReviews", count);
        return m;
    }

    /** Latest approved reviews for home-page testimonials. */
    public List<Review> getTestimonials(int limit) {
        return repo.findByStatusOrderByCreatedAtDesc("APPROVED")
                   .stream().limit(limit).toList();
    }

    // ── Admin ─────────────────────────────────────────────────────────────────

    public List<Review> getAllReviews() {
        return repo.findAllByOrderByCreatedAtDesc();
    }

    @Transactional
    public Review approveReview(Long id) {
        Review r = findById(id);
        r.setStatus("APPROVED");
        return repo.save(r);
    }

    @Transactional
    public Review rejectReview(Long id) {
        Review r = findById(id);
        r.setStatus("REJECTED");
        return repo.save(r);
    }

    @Transactional
    public Review addReply(Long id, String reply) {
        Review r = findById(id);
        r.setAdminReply(reply);
        return repo.save(r);
    }

    @Transactional
    public void deleteReview(Long id) {
        findById(id);
        repo.deleteById(id);
    }

    /** Dashboard stats: totalReviews, pendingCount, avgRating */
    public Map<String, Object> getStats() {
        long total   = repo.count();
        long pending = repo.countByStatus("PENDING");
        double avg   = repo.globalAvgRating();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("totalReviews",  total);
        m.put("pendingReviews", pending);
        m.put("averageRating", Math.round(avg * 10.0) / 10.0);
        return m;
    }

    /** Top-rated products report */
    public List<Map<String, Object>> getTopRatedProducts() {
        return repo.topRatedProducts().stream().map(row -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("productId",    row[0]);
            m.put("averageRating", Math.round(((Number) row[1]).doubleValue() * 10.0) / 10.0);
            m.put("reviewCount",  row[2]);
            return m;
        }).toList();
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private Review findById(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found: " + id));
    }
}
