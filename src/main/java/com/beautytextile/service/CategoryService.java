package com.beautytextile.service;

import com.beautytextile.exception.BusinessException;
import com.beautytextile.exception.ResourceNotFoundException;
import com.beautytextile.model.Category;
import com.beautytextile.repository.CategoryRepository;
import com.beautytextile.service.storage.ImageStorageService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    private final CategoryRepository repo;
    private final ImageStorageService imageStorage;

    public CategoryService(CategoryRepository repo, ImageStorageService imageStorage) {
        this.repo = repo;
        this.imageStorage = imageStorage;
    }

    /** Flat list — every category with parentId. */
    public List<Category> findAll() {
        return repo.findAll();
    }

    public List<Category> search(String query) {
        return repo.findByNameContainingIgnoreCase(query == null ? "" : query.trim());
    }

    public Page<Category> findAllPaged(int page, int size) {
        return repo.findAll(PageRequest.of(page, size));
    }

    public Page<Category> searchPaged(String query, int page, int size) {
        return repo.findByNameContainingIgnoreCase(query == null ? "" : query.trim(), PageRequest.of(page, size));
    }

    /** Hierarchical tree of root categories → children → grandchildren. Includes inactive categories (admin use). */
    public List<CategoryNode> findTree() {
        List<Category> all = repo.findAll();
        Map<Long, CategoryNode> nodes = new LinkedHashMap<>();
        for (Category c : all) {
            nodes.put(c.getId(), new CategoryNode(c.getId(), c.getName(), c.getParentId(), c.getImagePath(), c.isActive(), new ArrayList<>()));
        }
        List<CategoryNode> roots = new ArrayList<>();
        for (Category c : all) {
            CategoryNode node = nodes.get(c.getId());
            if (c.getParentId() == null) {
                roots.add(node);
            } else {
                CategoryNode parent = nodes.get(c.getParentId());
                if (parent != null) parent.children().add(node);
            }
        }
        roots.sort(Comparator.comparing(CategoryNode::name));
        return roots;
    }

    /**
     * Hierarchical tree pruned to only active categories (customer-facing use).
     * An inactive root hides its whole subtree; an inactive child is simply omitted
     * from its (active) parent's children.
     */
    public List<CategoryNode> findActiveTree() {
        return findTree().stream()
                .filter(CategoryNode::active)
                .map(this::pruneInactive)
                .collect(Collectors.toList());
    }

    private CategoryNode pruneInactive(CategoryNode node) {
        List<CategoryNode> activeChildren = node.children().stream()
                .filter(CategoryNode::active)
                .map(this::pruneInactive)
                .collect(Collectors.toList());
        return new CategoryNode(node.id(), node.name(), node.parentId(), node.imagePath(), node.active(), activeChildren);
    }

    /** Create a new category. parentId is optional. */
    public Category create(String name, Long parentId) {
        if (name == null || name.isBlank()) {
            throw new BusinessException("Category name is required");
        }
        if (repo.existsByNameIgnoreCase(name.trim())) {
            throw new BusinessException("Category already exists: " + name);
        }
        Category parent = null;
        if (parentId != null) {
            parent = repo.findById(parentId)
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category not found: " + parentId));
        }
        return repo.save(Category.builder().name(name.trim()).parent(parent).build());
    }

    public void delete(Long id) {
        Category category = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
        if (category.getImagePath() != null) {
            imageStorage.delete(category.getImagePath());
        }
        repo.deleteById(id);
    }

    @Transactional
    public Category updateImagePath(Long id, String imagePath) {
        if (imagePath == null || imagePath.isBlank()) {
            throw new BusinessException("Category image path is required");
        }
        Category category = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
        String oldPath = category.getImagePath();
        category.setImagePath(imagePath);
        Category saved = repo.save(category);
        if (oldPath != null && !oldPath.equals(saved.getImagePath())) {
            imageStorage.delete(oldPath);
        }
        return saved;
    }

    @Transactional
    public Category setActive(Long id, boolean active) {
        Category category = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
        category.setActive(active);
        return repo.save(category);
    }

    /** DTO for tree response. */
    public record CategoryNode(Long id, String name, Long parentId, String imagePath, boolean active, List<CategoryNode> children) {}
}
