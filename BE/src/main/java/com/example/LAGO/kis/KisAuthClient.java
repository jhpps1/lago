package com.example.LAGO.kis;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * KIS 접근토큰(/oauth2/tokenP) & 웹소켓 접속키(/oauth2/Approval) 발급 클라이언트.

 * ✔ tokenP: { grant_type: "client_credentials", appkey, appsecret }
 * ✔ Approval: { grant_type: "client_credentials", appkey, secretkey }  // 필드명이 다릅니다!

 * Spring Bean 으로 등록해 두고 주입 받아 사용하면 됩니다.
 */
public class KisAuthClient {

    // KIS 도메인 (REST 인증은 HTTPS, 실전/모의 각각 다름)
    // 실전
    public static final String PROD_REST_BASE = "https://openapi.koreainvestment.com:9443";
    // 모의
    public static final String PAPER_REST_BASE = "https://openapivts.koreainvestment.com:29443";

    private static final String TOKEN_PATH = "/oauth2/tokenP";     // 접근 토큰 발급
    private static final String APPROVAL_PATH = "/oauth2/Approval"; // 웹소켓 접속키 발급

    private final RestTemplate rest;
    private final String appKey;
    private final String appSecret;

    private final DateTimeFormatter KIS_TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // 간단 캐시 (프로덕션/모의 각각 1개씩)
    private volatile CacheEntry prodToken;
    private volatile CacheEntry paperToken;

    public KisAuthClient(RestTemplate restTemplate) {
        this.rest = restTemplate;
        // TODO: 실제 값은 application.yml 에서 주입받도록 변경하세요 (@Value 또는 @ConfigurationProperties)
        this.appKey = System.getenv().getOrDefault("KIS_APP_KEY", "<APP_KEY>");
        this.appSecret = System.getenv().getOrDefault("KIS_APP_SECRET", "<APP_SECRET>");
    }

    public KisAuthClient(RestTemplate restTemplate, String appKey, String appSecret) {
        this.rest = restTemplate;
        this.appKey = appKey;
        this.appSecret = appSecret;
    }

    public enum Env { PROD, PAPER }

    /** 접근 토큰 가져오기 (캐시 유효하면 재사용) */
    public synchronized String getAccessToken(Env env) {
        CacheEntry cached = (env == Env.PROD) ? prodToken : paperToken;
        if (cached != null && cached.expiresAt != null && LocalDateTime.now().isBefore(cached.expiresAt.minusMinutes(1))) {
            return cached.value;
        }
        String base = (env == Env.PROD) ? PROD_REST_BASE : PAPER_REST_BASE;
        TokenResponse res = requestAccessToken(base);
        Objects.requireNonNull(res, "token response is null");

        CacheEntry entry = new CacheEntry();
        entry.value = res.accessToken;
        entry.expiresAt = parseKisExpiry(res.accessTokenTokenExpired);

        if (env == Env.PROD) prodToken = entry; else paperToken = entry;
        return entry.value;
    }

    /** 웹소켓 접속키(approval_key) 발급 - REST 토큰 불필요 */
    public String getApprovalKey(Env env) {
        String base = (env == Env.PROD) ? PROD_REST_BASE : PAPER_REST_BASE;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("grant_type", "client_credentials");
        body.put("appkey", appKey);
        body.put("secretkey", appSecret); // 주의: tokenP와 달리 key 이름이 secretkey

        ResponseEntity<ApprovalResponse> resp = rest.postForEntity(base + APPROVAL_PATH, new HttpEntity<>(body, headers), ApprovalResponse.class);
        ApprovalResponse dto = resp.getBody();
        if (dto == null || dto.approvalKey == null) {
            throw new IllegalStateException("Failed to get approval_key: " + resp);
        }
        return dto.approvalKey;
    }

    // ===== 내부 구현 =====

    private TokenResponse requestAccessToken(String base) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("grant_type", "client_credentials");
        body.put("appkey", appKey);
        body.put("appsecret", appSecret);

        ResponseEntity<TokenResponse> resp = rest.postForEntity(base + TOKEN_PATH, new HttpEntity<>(body, headers), TokenResponse.class);
        TokenResponse dto = resp.getBody();
        if (dto == null || dto.accessToken == null) {
            throw new IllegalStateException("Failed to get access_token: " + resp);
        }
        return dto;
    }

    private LocalDateTime parseKisExpiry(String ts) {
        try {
            return LocalDateTime.parse(ts, KIS_TS);
        } catch (Exception e) {
            // 만약 응답이 없거나 포맷이 다르면 24시간으로 가정
            return LocalDateTime.now().plusHours(24);
        }
    }

    private static class CacheEntry {
        String value;
        LocalDateTime expiresAt;
    }

    // ===== DTO =====
    public static class TokenResponse {
        @JsonProperty("access_token")
        public String accessToken;
        @JsonProperty("access_token_token_expired")
        public String accessTokenTokenExpired; // "yyyy-MM-dd HH:mm:ss"
    }

    public static class ApprovalResponse {
        @JsonProperty("approval_key")
        public String approvalKey;
    }

    // ===== 사용 예시 =====
    public static void main(String[] args) {
        RestTemplate rt = new RestTemplate();
        KisAuthClient client = new KisAuthClient(rt);

        // 1) REST 접근 토큰
        String accessToken = client.getAccessToken(Env.PROD);
        System.out.println("access_token: " + accessToken);

        // 2) 웹소켓 approval_key (REST 토큰 불필요)
        String approvalKey = client.getApprovalKey(Env.PROD);
        System.out.println("approval_key: " + approvalKey);

        // 3) (참고) 웹소켓 구독 메시지 스켈레톤
        // String subscribe = "{" +
        //     "\"header\":{" +
        //         "\"approval_key\":\"" + approvalKey + "\"," +
        //         "\"custtype\":\"P\"," +
        //         "\"tr_type\":\"1\"," + // 1=등록, 2=해제
        //         "\"content-type\":\"utf-8\"}," +
        //     "\"body\":{" +
        //         "\"input\":{" +
        //             "\"tr_id\":\"H0STCNT0\"," +
        //             "\"tr_key\":\"005930\"}}}";
    }
}

