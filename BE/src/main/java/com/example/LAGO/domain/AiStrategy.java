package com.example.LAGO.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * AI 전략 엔티티
 * 
 * EC2 연동 DB AI_STRATEGY 테이블 구조:
 * - strategy_id: int (PK, AUTO_INCREMENT)
 * - user_id: int (FK → USERS.user_id)
 * - strategy: varchar(50) (전략명/캐릭터명)
 * - prompt: text (전략 설명/프롬프트)
 * - created_at: datetime (생성일시)
 * 
 * 캐릭터별 전략:
 * - 화끈이 (공격투자형): 긍정 신호에 강하게 반응
 * - 적극이 (적극투자형): 성장주 중심, 기술적 분석 중시
 * - 균형이 (위험중립형): 분산투자, 균형잡힌 포트폴리오
 * - 조심이 (안정추구형): 가치투자, 부정 신호에 민감
 * 
 * @author D203팀 백엔드 개발자
 * @since 2025-08-06
 */
@Entity
@Table(name = "ai_strategy")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiStrategy {

    /**
     * 전략 ID (PK)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "strategy_id")
    private Long strategyId;

    /**
     * 사용자 ID (FK)
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 전략명 (캐릭터명: 화끈이/적극이/균형이/조심이)
     */
    @Column(name = "strategy", length = 50, nullable = false)
    private String strategy;

    /**
     * 전략 설명/프롬프트 (매매 판단 근거)
     */
    @Column(name = "prompt", columnDefinition = "TEXT")
    private String prompt;

    /**
     * 생성일시
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 사용자 엔티티와의 관계 매핑 (지연 로딩)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    /**
     * AI 전략 정보를 문자열로 표현
     * 
     * @return 전략 정보 요약
     */
    @Override
    public String toString() {
        return String.format("AiStrategy{strategyId=%d, userId=%d, strategy='%s', createdAt=%s}", 
                strategyId, userId, strategy, createdAt);
    }
}
