package com.beautytextile.repository;

import com.beautytextile.model.Exchange;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface ExchangeRepository extends JpaRepository<Exchange, Long> {

    List<Exchange> findByOldBillId(Long billId);

    @Query("SELECT e FROM Exchange e ORDER BY e.exchangeDate DESC")
    List<Exchange> findAllByOrderByExchangeDateDesc();

    @Query("SELECT e FROM Exchange e WHERE e.exchangeDate BETWEEN :start AND :end ORDER BY e.exchangeDate DESC")
    List<Exchange> findByDateRange(LocalDateTime start, LocalDateTime end);

    @Query("SELECT COUNT(e) FROM Exchange e WHERE e.exchangeDate BETWEEN :start AND :end")
    long countBetween(LocalDateTime start, LocalDateTime end);
}
