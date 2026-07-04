package com.beautytextile.controller;

import com.beautytextile.model.HeroSlide;
import com.beautytextile.service.FileStorageService;
import com.beautytextile.service.HeroSlideService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * CRUD for the customer home page hero banner slides.
 * GET is public (renders the storefront); writes require an authenticated admin session.
 */
@RestController
@RequestMapping("/api/hero-slides")
public class HeroSlideController {

    private final HeroSlideService service;
    private final FileStorageService fileStorageService;

    public HeroSlideController(HeroSlideService service, FileStorageService fileStorageService) {
        this.service = service;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping
    public List<HeroSlide> getAll() {
        return service.findAll();
    }

    @PostMapping
    public HeroSlide create(@RequestBody Map<String, String> body) {
        return service.create(body.get("kicker"), body.get("title"), body.get("text"));
    }

    @PutMapping("/{id}")
    public HeroSlide update(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return service.update(id, body.get("kicker"), body.get("title"), body.get("text"));
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        service.delete(id);
        return Map.of("deleted", true, "id", id);
    }

    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, String> uploadImage(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        String imagePath = fileStorageService.store(file);
        service.updateImagePath(id, imagePath);
        return Map.of("imagePath", imagePath);
    }
}
