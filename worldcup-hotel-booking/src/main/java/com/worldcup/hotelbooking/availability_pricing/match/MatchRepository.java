package com.worldcup.hotelbooking.availability_pricing.match;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface MatchRepository extends JpaRepository<Match, Long> {
    /**
     * Find all matches happening between two dates
     */
    @Query("""
        SELECT m FROM Match m 
        WHERE m.matchDateTime >= :startDate 
        AND m.matchDateTime <= :endDate
        ORDER BY m.matchDateTime
    """)
    List<Match> findMatchesBetweenDates(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Find matches near a hotel during a date range
     */
    @Query("""
        SELECT m FROM Match m 
        WHERE m.matchDateTime >= :startDate 
        AND m.matchDateTime <= :endDate
        AND m.city = :city
        ORDER BY m.stage DESC, m.matchDateTime
    """)
    List<Match> findMatchesInCityBetweenDates(
            @Param("city") String city,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}
