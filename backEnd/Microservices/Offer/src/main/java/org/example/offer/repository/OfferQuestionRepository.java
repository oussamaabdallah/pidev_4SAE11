package org.example.offer.repository;

import org.example.offer.entity.OfferQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OfferQuestionRepository extends JpaRepository<OfferQuestion, Long> {

    @Query("SELECT q FROM OfferQuestion q JOIN FETCH q.offer WHERE q.offer.id = :offerId ORDER BY q.askedAt DESC")
    List<OfferQuestion> findByOfferIdOrderByAskedAtDesc(@Param("offerId") Long offerId);

    @Modifying
    @Query("DELETE FROM OfferQuestion q WHERE q.offer.id = :offerId")
    void deleteByOfferId(@Param("offerId") Long offerId);
}
