package com.beautytextile.controller;

import com.beautytextile.model.Category;
import com.beautytextile.service.CategoryService;
import com.beautytextile.service.CategoryService.CategoryNode;
import com.beautytextile.service.FileStorageService;
import org.springframework.http.MediaType;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService service;
    private final FileStorageService fileStorageService;

    public CategoryController(CategoryService service, FileStorageService fileStorageService) {
        this.service = service;
        this.fileStorageService = fileStorageService;
    }

    /** Flat list with parentId field. */
    @GetMapping
    public Object getAll(@RequestParam(required = false) String search,
                         @RequestParam(required = false) Integer page,
                         @RequestParam(required = false) Integer size) {
        if (page != null || size != null) {
            int resolvedPage = page != null ? page : 0;
            int resolvedSize = size != null ? size : 50;
            return getPagedResult(search, resolvedPage, resolvedSize);
        }
        if (search != null && !search.isBlank()) {
            return service.search(search);
        }
        return service.findAll();
    }

    private Map<String, Object> getPagedResult(String search, int page, int size) {
        Page<Category> result;
        if (search != null && !search.isBlank()) {
            result = service.searchPaged(search, page, size);
        } else {
            result = service.findAllPaged(page, size);
        }
        return Map.of(
                "content", result.getContent(),
                "page", result.getNumber(),
                "size", result.getSize(),
                "totalElements", result.getTotalElements(),
                "totalPages", result.getTotalPages(),
                "last", result.isLast()
        );
    }

    /** Hierarchical tree: roots → children → grandchildren.
     *  Pass ?activeOnly=true for the customer-facing pruned tree (inactive categories hidden). */
    @GetMapping("/tree")
    public List<CategoryNode> getTree(@RequestParam(required = false, defaultValue = "false") boolean activeOnly) {
        return activeOnly ? service.findActiveTree() : service.findTree();
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

    @PatchMapping("/{id}/active")
    public Category setActive(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        boolean active = Boolean.TRUE.equals(body.get("active"));
        return service.setActive(id, active);
    }

    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, String> uploadImage(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        String imagePath = fileStorageService.store(file);
        service.updateImagePath(id, imagePath);
        return Map.of("imagePath", imagePath);
    }
}
