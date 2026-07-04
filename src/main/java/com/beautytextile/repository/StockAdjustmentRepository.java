package com.beautytextile.repository;

import com.beautytextile.model.StockAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface StockAdjustmentRepository extends JpaRepository<StockAdjustment, Long> {

    List<StockAdjustment> findByProductIdOrderByAdjustedAtDesc(Long productId);

    List<StockAdjustment> findByAdjustedAtBetweenOrderByAdjustedAtDesc(
            LocalDateTime start, LocalDateTime end);

    List<StockAdjustment> findByReasonAndAdjustedAtBetween(
            String reason, LocalDateTime start, LocalDateTime end);

    @Query("SELECT s FROM StockAdjustment s WHERE s.adjustedAt BETWEEN :start AND :end ORDER BY s.adjustedAt DESC")
    List<StockAdjustment> findByDateRange(LocalDateTime start, LocalDateTime end);
}
