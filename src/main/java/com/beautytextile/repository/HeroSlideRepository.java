package com.beautytextile.repository;

import com.beautytextile.model.HeroSlide;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HeroSlideRepository extends JpaRepository<HeroSlide, Long> {
    List<HeroSlide> findAllByOrderBySortOrderAscIdAsc();
}
