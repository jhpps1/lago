package com.example.LAGO.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/test")
@Tag(name = "연결 테스트", description = "연결 테스트용 API")
@Validated
public class TestController {

    /**
     * 실행 테스트용 API
     */
    @GetMapping("/health")
    @Operation(
            summary = "테스트용 API",
            description = "실행 테스트를 위한 API입니다."
    )
    public ResponseEntity<String> checkConnection() {
        return ResponseEntity.ok("good");
    }
}
