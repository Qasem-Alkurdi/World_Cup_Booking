package com.worldcup.hotelbooking.review.dto;

import lombok.AllArgsConstructor;

import java.time.OffsetDateTime;

@AllArgsConstructor
public record ReviewResponseDto(Long id, Long hotelId, Long userId, Long bookingId, Integer rating, String comment,
                                boolean visible, OffsetDateTime createdAt, OffsetDateTime updatedAt) {

}