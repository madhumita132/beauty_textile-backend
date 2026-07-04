package com.beautytextile.repository;

import com.beautytextile.model.Return;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface ReturnRepository extends JpaRepository<Return, Long> {

    List<Return> findByBillId(Long billId);

    @Query("SELECT r FROM Return r ORDER BY r.returnDate DESC")
    List<Return> findAllByOrderByReturnDateDesc();

    @Query("SELECT r FROM Return r WHERE r.returnDate BETWEEN :start AND :end ORDER BY r.returnDate DESC")
    List<Return> findByDateRange(LocalDateTime start, LocalDateTime end);

    @Query("SELECT COALESCE(SUM(r.refundAmount), 0) FROM Return r WHERE r.returnDate BETWEEN :start AND :end")
    java.math.BigDecimal sumRefundsBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT COUNT(r) FROM Return r WHERE r.returnDate BETWEEN :start AND :end")
    long countBetween(LocalDateTime start, LocalDateTime end);
}
