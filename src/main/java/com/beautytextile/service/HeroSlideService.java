package com.beautytextile.service;

import com.beautytextile.exception.BusinessException;
import com.beautytextile.exception.ResourceNotFoundException;
import com.beautytextile.model.HeroSlide;
import com.beautytextile.repository.HeroSlideRepository;
import com.beautytextile.service.storage.ImageStorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class HeroSlideService {

    private final HeroSlideRepository repo;
    private final ImageStorageService imageStorage;

    public HeroSlideService(HeroSlideRepository repo, ImageStorageService imageStorage) {
        this.repo = repo;
        this.imageStorage = imageStorage;
    }

    public List<HeroSlide> findAll() {
        return repo.findAllByOrderBySortOrderAscIdAsc();
    }

    @Transactional
    public HeroSlide create(String kicker, String title, String text) {
        if (title == null || title.isBlank()) {
            throw new BusinessException("Slide title is required");
        }
        int nextOrder = repo.findAllByOrderBySortOrderAscIdAsc().size();
        return repo.save(HeroSlide.builder()
                .kicker(kicker)
                .title(title.trim())
                .text(text)
                .sortOrder(nextOrder)
                .build());
    }

    @Transactional
    public HeroSlide update(Long id, String kicker, String title, String text) {
        if (title == null || title.isBlank()) {
            throw new BusinessException("Slide title is required");
        }
        HeroSlide slide = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hero slide not found: " + id));
        slide.setKicker(kicker);
        slide.setTitle(title.trim());
        slide.setText(text);
        return repo.save(slide);
    }

    @Transactional
    public HeroSlide updateImagePath(Long id, String imagePath) {
        if (imagePath == null || imagePath.isBlank()) {
            throw new BusinessException("Slide image path is required");
        }
        HeroSlide slide = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hero slide not found: " + id));
        String oldPath = slide.getImagePath();
        slide.setImagePath(imagePath);
        HeroSlide saved = repo.save(slide);
        if (oldPath != null && !oldPath.equals(saved.getImagePath())) {
            imageStorage.delete(oldPath);
        }
        return saved;
    }

    public void delete(Long id) {
        HeroSlide slide = repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hero slide not found: " + id));
        if (slide.getImagePath() != null) {
            imageStorage.delete(slide.getImagePath());
        }
        repo.deleteById(id);
    }
}
