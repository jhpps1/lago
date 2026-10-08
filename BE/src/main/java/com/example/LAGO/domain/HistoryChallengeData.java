package com.example.LAGO.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 역사챌린지 엔티티
 */
@Entity
@Table(name = "history_challenge_data")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistoryChallengeData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "challenge_data_id")
    private Integer challengeDataId;

    @Column(name = "challenge_id")
    private Integer challengeId;

    @Column(name = "event_date_time")
    private LocalDateTime eventDateTime;

    @Column(name = "origin_date_time")
    private LocalDateTime originDateTime;

    @Column(name = "open_price", nullable = false)
    private Integer openPrice;

    @Column(name = "high_price", nullable = false)
    private Integer highPrice;

    @Column(name = "low_price", nullable = false)
    private Integer lowPrice;

    @Column(name = "close_price", nullable = false)
    private Integer closePrice;

    @Column(name = "volume", nullable = false)
    private Integer volume;
}
