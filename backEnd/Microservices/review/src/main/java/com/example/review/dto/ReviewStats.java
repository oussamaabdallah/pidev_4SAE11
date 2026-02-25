package com.example.review.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewStats {
    private long totalCount;
    private double averageRating;
    private Map<Integer, Long> countByRating;
}
