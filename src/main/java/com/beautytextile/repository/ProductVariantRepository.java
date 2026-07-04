package com.beautytextile.repository;

import com.beautytextile.model.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    @Query("SELECT v FROM ProductVariant v LEFT JOIN FETCH v.sizes WHERE v.product.id = :productId ORDER BY v.id")
    List<ProductVariant> findByProductIdWithSizes(Long productId);

    void deleteByProductId(Long productId);
}
