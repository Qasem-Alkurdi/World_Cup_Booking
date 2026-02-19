package com.worldcup.hotelbooking.availability_pricing.stadium;

import com.worldcup.hotelbooking.availability_pricing.match.Match;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Entity
@Getter
@Setter
public class Stadium {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) long id;
    // Stadium location
    private Double stadiumLatitude;
    private Double stadiumLongitude;

    @OneToMany(mappedBy = "stadium")
    private List<Match> matches;
}
