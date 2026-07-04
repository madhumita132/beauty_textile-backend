package com.beautytextile.repository;

import com.beautytextile.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    /** All approved reviews for a product, newest first */
    List<Review> findByProductIdAndStatusOrderByCreatedAtDesc(Long productId, String status);

    /** All reviews for admin management, newest first */
    @Query("SELECT r FROM Review r ORDER BY r.createdAt DESC")
    List<Review> findAllByOrderByCreatedAtDesc();

    /** Pending reviews count */
    long countByStatus(String status);

    /** Approved reviews for home page testimonials (any product, limit applied in service) */
    List<Review> findByStatusOrderByCreatedAtDesc(String status);

    /** Average rating for a product (approved only) */
    @Query("SELECT COALESCE(AVG(r.rating), 0) FROM Review r WHERE r.productId = :pid AND r.status = 'APPROVED'")
    double avgRatingByProduct(@Param("pid") Long productId);

    /** Review count for a product (approved only) */
    @Query("SELECT COUNT(r) FROM Review r WHERE r.productId = :pid AND r.status = 'APPROVED'")
    long countApprovedByProduct(@Param("pid") Long productId);

    /** Top-rated products — returns [productId, avgRating, reviewCount] */
    @Query("SELECT r.productId, AVG(r.rating) AS avg, COUNT(r) AS cnt " +
           "FROM Review r WHERE r.status = 'APPROVED' " +
           "GROUP BY r.productId ORDER BY avg DESC")
    List<Object[]> topRatedProducts();

    /** Global average rating across all approved reviews */
    @Query("SELECT COALESCE(AVG(r.rating), 0) FROM Review r WHERE r.status = 'APPROVED'")
    double globalAvgRating();

    /** Reviews by star rating for filter */
    List<Review> findByProductIdAndRatingAndStatusOrderByCreatedAtDesc(
            Long productId, int rating, String status);
}
