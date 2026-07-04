package com.beautytextile.service;

import com.beautytextile.exception.BusinessException;
import com.beautytextile.exception.ResourceNotFoundException;
import com.beautytextile.model.Category;
import com.beautytextile.repository.CategoryRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    private final CategoryRepository repo;

    public CategoryService(CategoryRepository repo) {
        this.repo = repo;
    }

    /** Flat list — every category with parentId. */
    public List<Category> findAll() {
        return repo.findAll();
    }

    /** Hierarchical tree of root categories → children → grandchildren. */
    public List<CategoryNode> findTree() {
        List<Category> all = repo.findAll();
        Map<Long, CategoryNode> nodes = new LinkedHashMap<>();
        for (Category c : all) {
            nodes.put(c.getId(), new CategoryNode(c.getId(), c.getName(), c.getParentId(), c.getImagePath(), new ArrayList<>()));
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
        repo.deleteById(id);
    }

    /** DTO for tree response. */
    public record CategoryNode(Long id, String name, Long parentId, String imagePath, List<CategoryNode> children) {}
}
