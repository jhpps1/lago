package com.example.LAGO.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 역사챌린지 엔티티
 */

@EntityListeners(AuditingEntityListener.class)
@Entity
@Table(name = "history_challenge")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistoryChallenge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "challenge_id")
    private Integer challengeId;

    @Column(name = "theme")
    private String theme;

    @Column(name = "stock_name", nullable = false)
    private String stockName;

    @Column(name = "stock_code", nullable = false)
    private String stockCode;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Column(name = "origin_date")
    private LocalDateTime originDate;
}