package com.beautytextile.controller;

import com.beautytextile.model.Category;
import com.beautytextile.service.CategoryService;
import com.beautytextile.service.CategoryService.CategoryNode;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService service;

    public CategoryController(CategoryService service) {
        this.service = service;
    }

    /** Flat list with parentId field. */
    @GetMapping
    public List<Category> getAll() {
        return service.findAll();
    }

    /** Hierarchical tree: roots → children → grandchildren. */
    @GetMapping("/tree")
    public List<CategoryNode> getTree() {
        return service.findTree();
    }

    @PostMapping
    public Category create(@RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");
        Long parentId = body.get("parentId") != null
                ? Long.valueOf(body.get("parentId").toString()) : null;
        return service.create(name, parentId);
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        service.delete(id);
        return Map.of("deleted", true, "id", id);
    }
}
