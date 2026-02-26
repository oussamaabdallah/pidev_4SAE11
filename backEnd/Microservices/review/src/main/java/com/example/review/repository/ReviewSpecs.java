package com.example.review.repository;

import com.example.review.entity.Review;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

public final class ReviewSpecs {

    private ReviewSpecs() {}

    public static Specification<Review> withSearch(String search, Integer rating, Long reviewerId, Long revieweeId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (search != null && !search.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("comment")), "%" + search.toLowerCase().trim() + "%"));
            }
            if (rating != null && rating >= 1 && rating <= 5) {
                predicates.add(cb.equal(root.get("rating"), rating));
            }
            if (reviewerId != null) {
                predicates.add(cb.equal(root.get("reviewerId"), reviewerId));
            }
            if (revieweeId != null) {
                predicates.add(cb.equal(root.get("revieweeId"), revieweeId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
