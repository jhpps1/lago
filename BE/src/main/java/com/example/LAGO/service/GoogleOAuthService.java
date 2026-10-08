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
public class GoogleOAuthService {

    @Value("${oauth2.google.client-id}")
    private String clientId;

    @Value("${oauth2.google.client-secret}")
    private String clientSecret;

    @Value("${oauth2.google.redirect-uri}")
    private String redirectUri;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String GOOGLE_USER_INFO_URL = "https://www.googleapis.com/oauth2/v2/userinfo";

    // 안드로이드에서 이미 Access Token을 받아서 전달하므로 이 메서드는 더 이상 사용하지 않음
    @Deprecated
    public String getGoogleAccessToken(String authorizationCode) {
        throw new UnsupportedOperationException("This method is deprecated. Use Android SDK Access Token directly.");
    }

    public Map<String, String> getGoogleUserInfo(String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);

            HttpEntity<String> request = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    GOOGLE_USER_INFO_URL,
                    HttpMethod.GET,
                    request,
                    String.class
            );

            JsonNode jsonNode = objectMapper.readTree(response.getBody());

            Map<String, String> userInfo = new HashMap<>();
            userInfo.put("id", jsonNode.get("id").asText());
            userInfo.put("email", jsonNode.get("email").asText());
            userInfo.put("name", jsonNode.get("name").asText());
            userInfo.put("picture", jsonNode.has("picture") ? jsonNode.get("picture").asText() : null);

            log.info("Retrieved Google user info for: {}", userInfo.get("email"));
            return userInfo;

        } catch (Exception e) {
            log.error("Error getting Google user info: {}", e.getMessage());
            throw new RuntimeException("Failed to get Google user info", e);
        }
    }

    // 안드로이드에서 직접 소셜 로그인을 처리하므로 Auth URL 생성이 불필요
    @Deprecated
    public String generateGoogleAuthUrl() {
        throw new UnsupportedOperationException("This method is deprecated. Use Android Google SDK directly.");
    }
}