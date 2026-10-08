package com.example.LAGO.repository;

import com.example.LAGO.domain.UserToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserTokenRepository extends JpaRepository<UserToken, Integer> {

    Optional<UserToken> findByRefreshToken(String refreshToken);

    Optional<UserToken> findByUserId(Long userId);

    @Modifying
    @Query("DELETE FROM UserToken ut WHERE ut.userId = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM UserToken ut WHERE ut.expiredAt < :currentTime")
    void deleteExpiredTokens(@Param("currentTime") LocalDateTime currentTime);

    boolean existsByRefreshToken(String refreshToken);
}