package com.example.LAGO.controller;

import com.example.LAGO.service.SocialLoginService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "소셜 로그인 API", description = "구글, 카카오 소셜 로그인 관련 API (안드로이드 SDK 방식)")
public class AuthController {

    private final SocialLoginService socialLoginService;

    @Operation(
            summary = "구글 소셜 로그인", 
            description = "안드로이드에서 구글 SDK로 받은 Access Token으로 로그인 처리"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "로그인 성공 또는 회원가입 필요"),
        @ApiResponse(responseCode = "400", description = "로그인 실패")
    })
    @PostMapping("/login/google")
    public ResponseEntity<Map<String, Object>> googleLogin(
            @Parameter(description = "구글 Access Token (안드로이드 SDK에서 획득)", required = true)
            @RequestBody Map<String, String> request) {
        
        try {
            String accessToken = request.get("accessToken");
            if (accessToken == null || accessToken.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "success", false,
                                "message", "Access Token이 필요합니다."
                        ));
            }

            Map<String, Object> result = socialLoginService.processSocialLogin("GOOGLE", accessToken);
            
            Map<String, Object> response = Map.of(
                    "success", true,
                    "message", "구글 로그인 처리 성공",
                    "data", result
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("구글 로그인 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "success", false,
                            "message", "구글 로그인에 실패했습니다.",
                            "error", e.getMessage()
                    ));
        }
    }

    @Operation(
            summary = "카카오 소셜 로그인", 
            description = "안드로이드에서 카카오 SDK로 받은 Access Token으로 로그인 처리"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "로그인 성공 또는 회원가입 필요"),
        @ApiResponse(responseCode = "400", description = "로그인 실패")
    })
    @PostMapping("/login/kakao")
    public ResponseEntity<Map<String, Object>> kakaoLogin(
            @Parameter(description = "카카오 Access Token (안드로이드 SDK에서 획득)", required = true)
            @RequestBody Map<String, String> request) {
        
        try {
            String accessToken = request.get("accessToken");
            if (accessToken == null || accessToken.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "success", false,
                                "message", "Access Token이 필요합니다."
                        ));
            }

            Map<String, Object> result = socialLoginService.processSocialLogin("KAKAO", accessToken);
            
            Map<String, Object> response = Map.of(
                    "success", true,
                    "message", "카카오 로그인 처리 성공",
                    "data", result
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("카카오 로그인 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "success", false,
                            "message", "카카오 로그인에 실패했습니다.",
                            "error", e.getMessage()
                    ));
        }
    }

    @Operation(
            summary = "회원가입 완료", 
            description = "임시 토큰으로 회원가입을 완료하고 정식 JWT 토큰을 발급합니다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "회원가입 완료 성공"),
        @ApiResponse(responseCode = "400", description = "회원가입 완료 실패")
    })
    @PostMapping("/api/auth/complete-signup")
    public ResponseEntity<Map<String, Object>> completeSignup(
            @Parameter(description = "회원가입 완료 정보", required = true)
            @RequestBody Map<String, String> request) {
        
        try {
            String tempToken = request.get("tempToken");
            String nickname = request.get("nickname");
            String personality = request.get("personality");

            if (tempToken == null || nickname == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "success", false,
                                "message", "임시 토큰과 닉네임은 필수입니다."
                        ));
            }

            Map<String, Object> result = socialLoginService.completeSignup(tempToken, nickname, personality);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("회원가입 완료 실패: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "success", false,
                            "message", "회원가입 완료에 실패했습니다.",
                            "error", e.getMessage()
                    ));
        }
    }

    @Operation(summary = "토큰 갱신", description = "리프레시 토큰으로 새로운 액세스 토큰을 발급합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "토큰 갱신 성공"),
        @ApiResponse(responseCode = "400", description = "토큰 갱신 실패")
    })
    @PostMapping("/api/auth/refresh")
    public ResponseEntity<Map<String, Object>> refreshToken(
            @Parameter(description = "리프레시 토큰", required = true)
            @RequestBody Map<String, String> request) {
        
        try {
            String refreshToken = request.get("refreshToken");
            if (refreshToken == null || refreshToken.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "success", false,
                                "message", "리프레시 토큰이 필요합니다."
                        ));
            }

            Map<String, Object> response = socialLoginService.refreshToken(refreshToken);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("토큰 갱신 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "success", false,
                            "message", "토큰 갱신에 실패했습니다.",
                            "error", e.getMessage()
                    ));
        }
    }

    @Operation(summary = "로그아웃", description = "사용자를 로그아웃하고 토큰을 무효화합니다.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "로그아웃 성공"),
        @ApiResponse(responseCode = "400", description = "로그아웃 실패")
    })
    @PostMapping("/api/auth/logout")
    public ResponseEntity<Map<String, Object>> logout(
            @Parameter(description = "로그아웃 정보", required = true)
            @RequestBody Map<String, Object> request) {
        
        try {
            Long userId = request.get("userId") instanceof Integer ? 
                ((Integer) request.get("userId")).longValue() : 
                (Long) request.get("userId");
            String refreshToken = (String) request.get("refreshToken");

            if (userId == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "success", false,
                                "message", "사용자 ID가 필요합니다."
                        ));
            }

            socialLoginService.logout(userId, refreshToken);
            
            Map<String, Object> response = Map.of(
                    "success", true,
                    "message", "로그아웃 성공"
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("로그아웃 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "success", false,
                            "message", "로그아웃에 실패했습니다.",
                            "error", e.getMessage()
                    ));
        }
    }
}