package com.worldcup.hotelbooking.review.dto;

import lombok.AllArgsConstructor;

import java.math.BigDecimal;

@AllArgsConstructor
public record HotelReviewSummaryDto(Long hotelId, BigDecimal averageRating, int reviewCount) {

}