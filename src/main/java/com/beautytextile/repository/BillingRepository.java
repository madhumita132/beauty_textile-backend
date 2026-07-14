package com.beautytextile.repository;

import com.beautytextile.model.Billing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface BillingRepository extends JpaRepository<Billing, Long> {
    List<Billing> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT b FROM Billing b ORDER BY b.createdAt DESC")
    List<Billing> findAllByOrderByCreatedAtDesc();

    List<Billing> findByPhoneOrderByCreatedAtDesc(String phone);
    List<Billing> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime start, LocalDateTime end);

    @Query("SELECT DISTINCT b FROM Billing b JOIN b.items i JOIN Product p ON p.id = i.productId " +
           "WHERE LOWER(TRIM(p.barcode)) = LOWER(TRIM(:barcode)) ORDER BY b.createdAt DESC")
    List<Billing> findByProductBarcodeOrderByCreatedAtDesc(@Param("barcode") String barcode);

    @Query("SELECT DISTINCT b FROM Billing b JOIN b.items i JOIN Product p ON p.id = i.productId " +
           "WHERE LOWER(TRIM(p.barcode)) LIKE LOWER(CONCAT('%', TRIM(:barcode), '%')) ORDER BY b.createdAt DESC")
    List<Billing> searchByProductBarcodeOrderByCreatedAtDesc(@Param("barcode") String barcode);

       @Query("SELECT b FROM Billing b LEFT JOIN FETCH b.items WHERE b.id = :id")
       Optional<Billing> findById(@Param("id") Long id);

       @Query("SELECT DISTINCT b FROM Billing b LEFT JOIN FETCH b.items ORDER BY b.createdAt DESC")
       List<Billing> findAllWithItemsOrderByCreatedAtDesc();

       @Query("SELECT DISTINCT b FROM Billing b LEFT JOIN FETCH b.items WHERE b.phone = :phone ORDER BY b.createdAt DESC")
       List<Billing> findByPhoneOrderByCreatedAtDescWithItems(@Param("phone") String phone);

       @Query("SELECT DISTINCT b FROM Billing b LEFT JOIN FETCH b.items WHERE b.createdAt BETWEEN :start AND :end ORDER BY b.createdAt DESC")
       List<Billing> findByCreatedAtBetweenOrderByCreatedAtDescWithItems(@Param("start") LocalDateTime start,
                                                                                                                         @Param("end") LocalDateTime end);
}
