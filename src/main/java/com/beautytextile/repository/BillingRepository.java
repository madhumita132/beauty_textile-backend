package com.beautytextile.repository;

import com.beautytextile.model.Billing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BillingRepository extends JpaRepository<Billing, Long> {
    List<Billing> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT b FROM Billing b ORDER BY b.createdAt DESC")
    List<Billing> findAllByOrderByCreatedAtDesc();

    List<Billing> findByPhoneOrderByCreatedAtDesc(String phone);
    List<Billing> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime start, LocalDateTime end);
    Optional<Billing> findById(Long id);
}
