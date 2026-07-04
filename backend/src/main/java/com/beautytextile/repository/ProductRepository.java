package com.beautytextile.repository;

import com.beautytextile.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findByBarcode(String barcode);
    Optional<Product> findBySku(String sku);

    boolean existsByBarcode(String barcode);
    boolean existsBySku(String sku);

    // ── Paginated queries (use for large datasets) ─────────────────────────

    Page<Product> findAll(Pageable pageable);

    Page<Product> findByCategoryIgnoreCase(String category, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE " +
           "LOWER(p.name) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(p.barcode) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(p.sku) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(p.category) LIKE LOWER(CONCAT('%',:q,'%'))")
    Page<Product> search(@Param("q") String query, Pageable pageable);

    Page<Product> findByStatus(String status, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.stock <= p.reorderLevel")
    Page<Product> findLowStock(Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.stock = 0")
    Page<Product> findOutOfStock(Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.stock <= :threshold")
    Page<Product> findByStockLessThanPage(@Param("threshold") int threshold, Pageable pageable);

    // ── Non-paged (used internally / small datasets) ──────────────────────

    List<Product> findByCategoryIgnoreCase(String category);
    List<Product> findByNameContainingIgnoreCase(String name);
    List<Product> findByStockLessThan(Integer threshold);

    @Query("SELECT p FROM Product p WHERE p.stock <= p.reorderLevel ORDER BY p.stock ASC")
    List<Product> findLowStockList();

    /** Sorted longest-first then lexicographic — gives the numerically largest BT barcode first. */
    @Query("SELECT p.barcode FROM Product p WHERE p.barcode LIKE 'BT%' ORDER BY LENGTH(p.barcode) DESC, p.barcode DESC")
    List<String> findTopBtBarcodes(Pageable pageable);

    // ── Bulk operations ───────────────────────────────────────────────────

    @Modifying
    @Query("UPDATE Product p SET p.status = :status WHERE p.id IN :ids")
    int bulkUpdateStatus(@Param("status") String status, @Param("ids") List<Long> ids);

    @Modifying
    @Query("UPDATE Product p SET p.discountType = :dtype, p.discountValue = :dval WHERE p.category = :cat")
    int bulkUpdateCategoryDiscount(@Param("cat") String cat,
                                   @Param("dtype") String dtype,
                                   @Param("dval") BigDecimal dval);

    // ── Reports ───────────────────────────────────────────────────────────

    @Query("SELECT COUNT(p) FROM Product p WHERE p.status = 'ACTIVE'")
    long countActive();

    @Query("SELECT COUNT(p) FROM Product p WHERE p.stock = 0")
    long countOutOfStock();

    @Query("SELECT COUNT(p) FROM Product p WHERE p.stock <= p.reorderLevel AND p.stock > 0")
    long countLowStock();

    @Query("SELECT SUM(p.stock * p.price) FROM Product p WHERE p.status = 'ACTIVE'")
    BigDecimal totalInventoryValue();

    @Query("SELECT SUM(p.stock * p.costPrice) FROM Product p WHERE p.costPrice IS NOT NULL")
    BigDecimal totalCostValue();

    @Query("SELECT DISTINCT p.category FROM Product p ORDER BY p.category")
    List<String> findAllCategories();

    @Query("SELECT DISTINCT p.supplier FROM Product p WHERE p.supplier IS NOT NULL ORDER BY p.supplier")
    List<String> findAllSuppliers();
}

