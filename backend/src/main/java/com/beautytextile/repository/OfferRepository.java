package com.beautytextile.repository;

import com.beautytextile.model.Offer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface OfferRepository extends JpaRepository<Offer, Long> {

    /** All active offers whose date range includes today. */
    @Query("SELECT o FROM Offer o WHERE o.active = true AND o.startDate <= :today AND o.endDate >= :today")
    List<Offer> findActiveOffers(LocalDate today);
}
