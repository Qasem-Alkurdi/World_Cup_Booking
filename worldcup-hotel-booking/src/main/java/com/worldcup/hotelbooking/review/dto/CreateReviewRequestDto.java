package com.worldcup.hotelbooking.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public record CreateReviewRequestDto(@NotNull @Min(1) @Max(5) Integer rating, String comment) {

}