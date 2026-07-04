package com.beautytextile.service;

import com.beautytextile.exception.BusinessException;
import com.beautytextile.exception.ResourceNotFoundException;
import com.beautytextile.model.HeroSlide;
import com.beautytextile.repository.HeroSlideRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class HeroSlideService {

    private final HeroSlideRepository repo;

    public HeroSlideService(HeroSlideRepository repo) {
        this.repo = repo;
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
        slide.setImagePath(imagePath);
        return repo.save(slide);
    }

    public void delete(Long id) {
        repo.deleteById(id);
    }
}
