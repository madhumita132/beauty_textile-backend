package com.beautytextile.repository;

import com.beautytextile.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCase(String name);

    /** All root categories (no parent). */
    List<Category> findByParentIsNullOrderByName();

    /** Direct children of a given parent. */
    List<Category> findByParentIdOrderByName(Long parentId);

    /** All categories eagerly with their children (for tree building). */
    @Query("SELECT c FROM Category c LEFT JOIN FETCH c.children WHERE c.parent IS NULL ORDER BY c.name")
    List<Category> findRootsWithChildren();
}
