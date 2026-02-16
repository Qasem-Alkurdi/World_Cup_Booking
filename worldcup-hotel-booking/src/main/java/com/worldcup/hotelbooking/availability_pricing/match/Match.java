package com.worldcup.hotelbooking.availability_pricing.match;

import com.worldcup.hotelbooking.availability_pricing.stadium.Stadium;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Table(name = "matches")
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String homeTeam;

    @Column(nullable = false)
    private String awayTeam;

    @Column(nullable = false)
    private LocalDateTime matchDateTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MatchStage stage;

    @Column(nullable = false)
    private String venue;  // Stadium name

    @Column(nullable = false)
    private String city;

    @ManyToOne
    @JoinColumn(name = "Stadium_id")
    private Stadium stadium;

    // Match importance factors
    private boolean isOpeningMatch = false;
    private boolean isDerby = false;

    @ElementCollection
    @CollectionTable(name = "match_popular_teams")
    private List<String> popularTeams = new ArrayList<>();

    public enum MatchStage {
        GROUP_STAGE_1,
        GROUP_STAGE_2,
        GROUP_STAGE_3,
        ROUND_OF_32,
        ROUND_OF_16,
        QUARTER_FINAL,
        SEMI_FINAL,
        THIRD_PLACE,
        FINAL
    }
}
