package com.example.LAGO.service;

import com.example.LAGO.domain.User;
import com.example.LAGO.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
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
public class SocialLoginService {

    private final GoogleOAuthService googleOAuthService;
    private final KakaoOAuthService kakaoOAuthService;
    private final TokenManagementService tokenManagementService;
    private final JwtTokenService jwtTokenService;
    private final UserRepository userRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final AccountService accountService;

    public Map<String, Object> processSocialLogin(String provider, String accessToken) {
        try {
            Map<String, String> userInfo;
            
            if ("GOOGLE".equalsIgnoreCase(provider)) {
                userInfo = googleOAuthService.getGoogleUserInfo(accessToken);
            } else if ("KAKAO".equalsIgnoreCase(provider)) {
                userInfo = kakaoOAuthService.getKakaoUserInfo(accessToken);
            } else {
                throw new IllegalArgumentException("지원하지 않는 소셜 로그인 제공자입니다: " + provider);
            }

            String socialLoginId = userInfo.get("id");
            String email = userInfo.get("email");

            Optional<User> existingUser = userRepository.findBySocialLoginIdAndLoginType(socialLoginId, provider.toUpperCase());

            if (existingUser.isPresent()) {
                User user = existingUser.get();
                log.info("기존 사용자 로그인: {}", user.getEmail());
                
                Map<String, String> tokens = tokenManagementService.generateTokenPair(user);
                
                Map<String, Object> response = new HashMap<>();
                response.put("needsSignup", false);
                response.put("user", createUserResponse(user));
                response.putAll(tokens);
                
                return response;
            } else {
                log.info("신규 사용자, 회원가입 필요: {}", email);
                
                String tempToken = jwtTokenService.generateTempToken(socialLoginId, provider.toUpperCase());
                
                String redisKey = "temp_social_data:" + tempToken;
                Map<String, String> tempData = new HashMap<>();
                tempData.put("socialLoginId", socialLoginId);
                tempData.put("provider", provider.toUpperCase());
                tempData.put("email", email);
                tempData.put("name", userInfo.getOrDefault("name", ""));
                tempData.put("nickname", userInfo.getOrDefault("nickname", ""));
                tempData.put("profileImage", userInfo.getOrDefault("picture", userInfo.getOrDefault("profileImage", "")));
                
                redisTemplate.opsForHash().putAll(redisKey, tempData);
                redisTemplate.expire(redisKey, Duration.ofMinutes(10));
                
                Map<String, Object> response = new HashMap<>();
                response.put("needsSignup", true);
                response.put("tempToken", tempToken);
                response.put("email", email);
                response.put("suggestedNickname", generateSuggestedNickname(userInfo, provider));
                
                return response;
            }

        } catch (Exception e) {
            log.error("소셜 로그인 처리 중 오류: {}", e.getMessage(), e);
            throw new RuntimeException("소셜 로그인 처리 중 오류가 발생했습니다.", e);
        }
    }

    public Map<String, Object> completeSignup(String tempToken, String nickname, String personality) {
        try {
            if (!jwtTokenService.isTokenValid(tempToken) || !jwtTokenService.isTempToken(tempToken)) {
                throw new IllegalArgumentException("유효하지 않은 임시 토큰입니다.");
            }

            String redisKey = "temp_social_data:" + tempToken;
            Map<Object, Object> tempData = redisTemplate.opsForHash().entries(redisKey);
            
            if (tempData.isEmpty()) {
                throw new IllegalArgumentException("임시 토큰이 만료되었거나 존재하지 않습니다.");
            }

            String socialLoginId = (String) tempData.get("socialLoginId");
            String provider = (String) tempData.get("provider");
            String email = (String) tempData.get("email");
            String profileImage = (String) tempData.get("profileImage");

            // 캐릭터명을 설명형으로 변환 (조심이 -> 안정추구형)
            String personalityDescription = convertToDescription(personality);

            User newUser = User.builder()
                    .email(email)
                    .socialLoginId(socialLoginId)
                    .loginType(provider)
                    .nickname(nickname)
                    .personality(personalityDescription)
                    .createdAt(LocalDateTime.now())
                    .isAi(false)
                    .build();

            User savedUser = userRepository.save(newUser);
            log.info("신규 사용자 회원가입 완료: {}", savedUser.getEmail());

            // 신규 사용자용 계좌 생성 (모의투자 + 역사챌린지)
            accountService.createInitialAccountsForUser(savedUser.getUserId());
            log.info("신규 사용자 초기 계좌 생성 완료: userId={}", savedUser.getUserId());

            redisTemplate.delete(redisKey);

            Map<String, String> tokens = tokenManagementService.generateTokenPair(savedUser);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "회원가입이 완료되었습니다.");
            response.put("user", createUserResponse(savedUser));
            response.putAll(tokens);
            
            return response;

        } catch (Exception e) {
            log.error("회원가입 완료 처리 중 오류: {}", e.getMessage(), e);
            throw new RuntimeException("회원가입 완료 처리 중 오류가 발생했습니다.", e);
        }
    }

    private String generateSuggestedNickname(Map<String, String> userInfo, String provider) {
        String nickname = userInfo.get("nickname");
        String name = userInfo.get("name");

        if (nickname != null && !nickname.isEmpty()) {
            return nickname;
        }
        if (name != null && !name.isEmpty()) {
            return name;
        }
        
        return provider.toLowerCase() + "_user_" + System.currentTimeMillis();
    }

    private Map<String, Object> createUserResponse(User user) {
        Map<String, Object> userResponse = new HashMap<>();
        userResponse.put("userId", user.getUserId());
        userResponse.put("email", user.getEmail());
        userResponse.put("nickname", user.getNickname());
        userResponse.put("personality", user.getPersonality());
        userResponse.put("loginType", user.getLoginType());
        return userResponse;
    }

    /**
     * 캐릭터명을 설명형으로 변환
     */
    private String convertToDescription(String personality) {
        if (personality == null || personality.trim().isEmpty()) {
            return "위험중립형"; // 기본값
        }
        
        return switch (personality.trim()) {
            case "조심이" -> "안정추구형";
            case "균형이" -> "위험중립형";  
            case "적극이" -> "적극투자형";
            case "화끈이" -> "공격투자형";
            // 이미 설명형이면 그대로 반환
            case "안정추구형", "위험중립형", "적극투자형", "공격투자형" -> personality.trim();
            default -> {
                log.warn("알 수 없는 성향: {}, 기본값(위험중립형) 사용", personality);
                yield "위험중립형";
            }
        };
    }

    public Map<String, Object> refreshToken(String refreshToken) {
        // Rolling Refresh: 새로운 액세스 토큰과 리프레시 토큰을 모두 발급
        Optional<Map<String, String>> newTokenPair = tokenManagementService.refreshTokenPair(refreshToken);
        
        if (newTokenPair.isEmpty()) {
            throw new RuntimeException("유효하지 않은 리프레시 토큰입니다. 다시 로그인해주세요.");
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "토큰 갱신 성공");
        response.put("data", newTokenPair.get());
        
        return response;
    }

    public void logout(Long userId, String refreshToken) {
        if (refreshToken != null) {
            tokenManagementService.revokeRefreshToken(refreshToken);
        }
        tokenManagementService.revokeToken(userId);
        
        log.info("사용자 {} 로그아웃 완료", userId);
    }
}