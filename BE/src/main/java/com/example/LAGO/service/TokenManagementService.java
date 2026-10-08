package com.example.LAGO.service;

import com.example.LAGO.domain.User;
import com.example.LAGO.domain.UserToken;
import com.example.LAGO.repository.UserTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TokenManagementService {

    private final JwtTokenService jwtTokenService;
    private final UserTokenRepository userTokenRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    public Map<String, String> generateTokenPair(User user) {
        String accessToken = jwtTokenService.generateAccessToken(
                user.getUserId(), 
                user.getEmail(), 
                user.getLoginType()
        );
        String refreshToken = jwtTokenService.generateRefreshToken(user.getUserId());

        saveRefreshToken(user.getUserId(), refreshToken);

        Map<String, String> tokens = new HashMap<>();
        tokens.put("accessToken", accessToken);
        tokens.put("refreshToken", refreshToken);
        tokens.put("tokenType", "Bearer");
        tokens.put("expiresIn", String.valueOf(jwtTokenService.accessTokenExpiry / 1000));

        return tokens;
    }

    public void saveRefreshToken(Long userId, String refreshToken) {
        userTokenRepository.deleteByUserId(userId);

        LocalDateTime expiredAt = jwtTokenService.getExpirationFromToken(refreshToken);
        
        UserToken userToken = UserToken.builder()
                .userId(userId)
                .refreshToken(refreshToken)
                .expiredAt(expiredAt)
                .build();

        userTokenRepository.save(userToken);

        String redisKey = "refresh_token:" + userId;
        redisTemplate.opsForValue().set(redisKey, refreshToken, Duration.ofDays(30));
        
        log.info("Refresh token saved for user: {} (DB + Redis)", userId);
    }

    public Optional<String> refreshAccessToken(String refreshToken) {
        try {
            if (!jwtTokenService.isTokenValid(refreshToken)) {
                log.warn("Invalid refresh token provided");
                return Optional.empty();
            }

            Optional<UserToken> userTokenOpt = userTokenRepository.findByRefreshToken(refreshToken);
            if (userTokenOpt.isEmpty() || userTokenOpt.get().isExpired()) {
                log.warn("Refresh token not found in database or expired");
                return Optional.empty();
            }

            Long userId = jwtTokenService.getUserIdFromToken(refreshToken);
            UserToken userToken = userTokenOpt.get();
            User user = userToken.getUser();

            String newAccessToken = jwtTokenService.generateAccessToken(
                    userId, 
                    user.getEmail(), 
                    user.getLoginType()
            );

            log.info("New access token generated for user: {}", userId);
            return Optional.of(newAccessToken);

        } catch (Exception e) {
            log.error("Error refreshing access token: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public Optional<Map<String, String>> refreshTokenPair(String refreshToken) {
        try {
            if (!jwtTokenService.isTokenValid(refreshToken)) {
                log.warn("Invalid refresh token provided for token pair refresh");
                return Optional.empty();
            }

            Optional<UserToken> userTokenOpt = userTokenRepository.findByRefreshToken(refreshToken);
            if (userTokenOpt.isEmpty() || userTokenOpt.get().isExpired()) {
                log.warn("Refresh token not found in database or expired for token pair refresh");
                return Optional.empty();
            }

            Long userId = jwtTokenService.getUserIdFromToken(refreshToken);
            UserToken userToken = userTokenOpt.get();
            User user = userToken.getUser();

            // 새로운 토큰 페어 생성
            String newAccessToken = jwtTokenService.generateAccessToken(
                    userId, 
                    user.getEmail(), 
                    user.getLoginType()
            );
            String newRefreshToken = jwtTokenService.generateRefreshToken(userId);

            // 기존 리프레시 토큰 무효화하고 새로운 리프레시 토큰 저장
            userTokenRepository.delete(userToken);
            saveRefreshToken(userId, newRefreshToken);

            Map<String, String> tokens = new HashMap<>();
            tokens.put("accessToken", newAccessToken);
            tokens.put("refreshToken", newRefreshToken);
            tokens.put("tokenType", "Bearer");
            tokens.put("expiresIn", String.valueOf(jwtTokenService.accessTokenExpiry / 1000));

            log.info("New token pair generated for user: {} (Rolling Refresh)", userId);
            return Optional.of(tokens);

        } catch (Exception e) {
            log.error("Error refreshing token pair: {}", e.getMessage());
            return Optional.empty();
        }
    }

    public void revokeToken(Long userId) {
        userTokenRepository.deleteByUserId(userId);
        
        String redisKey = "refresh_token:" + userId;
        redisTemplate.delete(redisKey);
        
        log.info("All tokens revoked for user: {} (DB + Redis)", userId);
    }

    public void revokeRefreshToken(String refreshToken) {
        Optional<UserToken> userTokenOpt = userTokenRepository.findByRefreshToken(refreshToken);
        if (userTokenOpt.isPresent()) {
            Long userId = userTokenOpt.get().getUserId();
            userTokenRepository.delete(userTokenOpt.get());
            
            String redisKey = "refresh_token:" + userId;
            redisTemplate.delete(redisKey);
            
            log.info("Refresh token revoked for user: {} (DB + Redis)", userId);
        }
    }

    public boolean isRefreshTokenValid(String refreshToken) {
        if (!jwtTokenService.isTokenValid(refreshToken)) {
            return false;
        }

        Optional<UserToken> userToken = userTokenRepository.findByRefreshToken(refreshToken);
        return userToken.isPresent() && !userToken.get().isExpired();
    }

    @Scheduled(fixedRate = 3600000) // 1시간마다 실행
    public void cleanupExpiredTokens() {
        try {
            userTokenRepository.deleteExpiredTokens(LocalDateTime.now());
            log.info("Expired tokens cleanup completed");
        } catch (Exception e) {
            log.error("Error during token cleanup: {}", e.getMessage());
        }
    }
}