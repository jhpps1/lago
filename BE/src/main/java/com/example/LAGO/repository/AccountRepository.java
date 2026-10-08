package com.example.LAGO.repository;

import com.example.LAGO.domain.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Account 엔티티 Repository
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    /**
     * 사용자 ID와 계좌 타입으로 조회 (기존 메서드)
     */
    Optional<Account> findByUserIdAndType(Long userId, Integer type);

    /**
     * 사용자 ID로 계좌 목록 조회
     * @param userId 사용자 ID
     * @return 사용자의 계좌 목록
     */
    List<Account> findByUserId(Long userId);

    /**
     * 사용자 ID로 첫 번째 계좌 조회 (기본 계좌)
     * @param userId 사용자 ID
     * @return 사용자의 첫 번째 계좌
     */
    Optional<Account> findFirstByUserId(Long userId);

    /**
     * 계좌 ID와 사용자 ID로 계좌 조회
     * @param accountId 계좌 ID
     * @param userId 사용자 ID
     * @return 계좌
     */
    Optional<Account> findByAccountIdAndUserId(Long accountId, Long userId);
}
