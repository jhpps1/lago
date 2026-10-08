package com.example.LAGO.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class JwtTokenService {

    @Value("${jwt.secret:mySecretKeyThatIsAtLeast256BitsLongForHS256Algorithm}")
    private String jwtSecret;

    @Value("${jwt.access-token-expiry:1800000}") // 30분
    public long accessTokenExpiry;

    @Value("${jwt.refresh-token-expiry:604800000}") // 7일
    private long refreshTokenExpiry;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    public String generateAccessToken(Long userId, String email, String loginType) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("email", email);
        claims.put("loginType", loginType);
        claims.put("tokenType", "ACCESS");

        return createToken(claims, accessTokenExpiry);
    }

    public String generateRefreshToken(Long userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("tokenType", "REFRESH");

        return createToken(claims, refreshTokenExpiry);
    }

    private String createToken(Map<String, Object> claims, long expiry) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiry);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims extractClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            log.warn("JWT token expired: {}", e.getMessage());
            throw e;
        } catch (JwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            throw e;
        }
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = extractClaims(token);
        // JWT에서 숫자는 Integer로 저장되므로 Long으로 변환
        Integer userId = claims.get("userId", Integer.class);
        return userId != null ? userId.longValue() : null;
    }

    public String getEmailFromToken(String token) {
        Claims claims = extractClaims(token);
        return claims.get("email", String.class);
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractClaims(token);
            return !isTokenExpired(claims);
        } catch (JwtException e) {
            return false;
        }
    }

    public boolean isTokenExpired(Claims claims) {
        return claims.getExpiration().before(new Date());
    }

    public LocalDateTime getExpirationFromToken(String token) {
        Claims claims = extractClaims(token);
        return claims.getExpiration()
                .toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }

    public String getTokenType(String token) {
        Claims claims = extractClaims(token);
        return claims.get("tokenType", String.class);
    }

    public boolean isAccessToken(String token) {
        return "ACCESS".equals(getTokenType(token));
    }

    public boolean isRefreshToken(String token) {
        return "REFRESH".equals(getTokenType(token));
    }

    public String generateTempToken(String socialLoginId, String provider) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("socialLoginId", socialLoginId);
        claims.put("provider", provider);
        claims.put("tokenType", "TEMP");

        return createToken(claims, 600000); // 10분
    }

    public boolean isTempToken(String token) {
        return "TEMP".equals(getTokenType(token));
    }

    public String getSocialLoginIdFromToken(String token) {
        Claims claims = extractClaims(token);
        return claims.get("socialLoginId", String.class);
    }

    public String getProviderFromToken(String token) {
        Claims claims = extractClaims(token);
        return claims.get("provider", String.class);
    }
}