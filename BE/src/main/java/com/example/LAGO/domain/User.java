package com.example.LAGO.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 사용자 엔티티
 * 지침서 명세 USER 테이블과 완전 일치
 * EC2 데이터베이스 테이블명 USERS (대문자) 사용
 */
@Entity
@Table(name = "users")
@Getter 
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "email")
    private String email;

    @Column(name = "social_login_id")
    private String socialLoginId;

    @Column(name = "login_type")
    private String loginType;

    @Column(name = "nickname", nullable = false)
    private String nickname;

    @Column(name = "personality")
    private String personality; // 투자 성향

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "frame_id")
    private Integer frameId; // 프로필 테두리 FK

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt; // 탈퇴 일시

    @Column(name = "is_ai", nullable = false)
    private Boolean isAi; // AI 봇 계정여부

    @Column(name = "ai_id")
    private Integer aiId; // AI ENUM 식별값

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (isAi == null) {
            isAi = false;
        }
    }
}
