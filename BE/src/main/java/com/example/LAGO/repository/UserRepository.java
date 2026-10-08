package com.example.LAGO.repository;

import com.example.LAGO.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * User 엔티티 Repository
 * 지침서 명세: AI 봇은 is_ai=true로 구분
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * AI ID와 AI 여부로 조회 (기존 메서드)
     */
    Optional<User> findByAiIdAndIsAi(Integer aiId, Boolean isAi);

    /**
     * AI 봇 목록 조회 (is_ai = true)
     * @return AI 봇 사용자 목록 (AI ID 순서로 정렬)
     */
    List<User> findByIsAiTrueOrderByAiIdAsc();

    /**
     * AI ID로 AI 봇 조회 (간편 메서드)
     * @param aiId AI 봇 식별자
     * @return AI 봇 사용자
     */
    Optional<User> findByAiIdAndIsAiTrue(Integer aiId);

    /**
     * 일반 사용자 조회 (is_ai = false)
     * @param userId 사용자 ID
     * @return 일반 사용자
     */
    Optional<User> findByUserIdAndIsAiFalse(Long userId);

    /**
     * 사용자 ID로 조회 (AI 봇 여부 상관없이)
     * @param userId 사용자 ID
     * @return 사용자
     */
    Optional<User> findByUserId(Long userId);
    
    /**
     * 활성화된 AI 봇 목록 조회 (is_ai = true, deleted_at IS NULL)
     * @return 삭제되지 않은 AI 봇 사용자 목록
     */
    List<User> findByIsAiTrueAndDeletedAtIsNull();

    /**
     * 소셜 로그인 ID와 로그인 타입으로 사용자 조회
     * @param socialLoginId 소셜 로그인 ID
     * @param loginType 로그인 타입 (GOOGLE, KAKAO)
     * @return 사용자
     */
    Optional<User> findBySocialLoginIdAndLoginType(String socialLoginId, String loginType);

    /**
     * 이메일로 사용자 조회
     * @param email 이메일
     * @return 사용자
     */
    Optional<User> findByEmail(String email);
}
