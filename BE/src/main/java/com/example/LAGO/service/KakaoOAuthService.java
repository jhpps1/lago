package com.example.LAGO.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class KakaoOAuthService {

    @Value("${oauth2.kakao.client-id}")
    private String clientId;

    @Value("${oauth2.kakao.client-secret}")
    private String clientSecret;

    @Value("${oauth2.kakao.redirect-uri}")
    private String redirectUri;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String KAKAO_TOKEN_URL = "https://kauth.kakao.com/oauth/token";
    private static final String KAKAO_USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";

    // 안드로이드에서 이미 Access Token을 받아서 전달하므로 이 메서드는 더 이상 사용하지 않음
    @Deprecated
    public String getKakaoAccessToken(String authorizationCode) {
        throw new UnsupportedOperationException("This method is deprecated. Use Android SDK Access Token directly.");
    }

    public Map<String, String> getKakaoUserInfo(String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);

            HttpEntity<String> request = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    KAKAO_USER_INFO_URL,
                    HttpMethod.GET,
                    request,
                    String.class
            );

            JsonNode jsonNode = objectMapper.readTree(response.getBody());

            Map<String, String> userInfo = new HashMap<>();
            userInfo.put("id", jsonNode.get("id").asText());

            // 카카오 계정 정보
            JsonNode kakaoAccount = jsonNode.get("kakao_account");
            if (kakaoAccount != null) {
                if (kakaoAccount.has("email")) {
                    userInfo.put("email", kakaoAccount.get("email").asText());
                }

                // 프로필 정보
                JsonNode profile = kakaoAccount.get("profile");
                if (profile != null) {
                    userInfo.put("nickname", profile.get("nickname").asText());
                    if (profile.has("profile_image_url")) {
                        userInfo.put("profileImage", profile.get("profile_image_url").asText());
                    }
                }
            }

            log.info("Retrieved Kakao user info for: {}", userInfo.get("email"));
            return userInfo;

        } catch (Exception e) {
            log.error("Error getting Kakao user info: {}", e.getMessage());
            throw new RuntimeException("Failed to get Kakao user info", e);
        }
    }

    // 안드로이드에서 직접 소셜 로그인을 처리하므로 Auth URL 생성이 불필요
    @Deprecated
    public String generateKakaoAuthUrl() {
        throw new UnsupportedOperationException("This method is deprecated. Use Android Kakao SDK directly.");
    }
}