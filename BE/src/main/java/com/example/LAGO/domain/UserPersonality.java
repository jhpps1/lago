package com.example.LAGO.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 사용자 성격 테스트 엔티티
 * user_personality 테이블과 매핑
 */
@Entity
@Table(name = "user_personality")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPersonality {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "test_id")
    private Integer testId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "personality")
    private String personality;

    @Column(name = "tested_at", nullable = false)
    private LocalDateTime testedAt;

    // 연관관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @PrePersist
    protected void onCreate() {
        if (testedAt == null) {
            testedAt = LocalDateTime.now();
        }
    }
}