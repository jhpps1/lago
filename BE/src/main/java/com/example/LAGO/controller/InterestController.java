package com.example.LAGO.controller;

import com.example.LAGO.dto.response.InterestResponse;
import com.example.LAGO.dto.response.InterestToggleResponse;
import com.example.LAGO.service.InterestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/{userId}/interest")
@Tag(name = "관심 종목 API", description = "특정 유저의 관심 종목을 조회/추가/삭제합니다.")
@Validated
public class InterestController {
    private final InterestService interestService;

    /**
     * 특정 유저의 관심 종목 리스트 조회
     */
    @GetMapping
    @Operation(
            summary = "관심 종목 목록 조회",
            description = "특정 유저의 관심 종목 목록을 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "관심 종목 목록 조회 성공"),
            @ApiResponse(responseCode = "204", description = "관심 종목이 없음"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<List<InterestResponse>> list(
            @Parameter(description = "유저 ID", required = true, example = "5")
            @PathVariable int userId
    ) {
        List<InterestResponse> responses = interestService.list(userId);
        if (responses.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(responses);
    }

    /**
     * 관심 종목 토글 (없으면 추가, 있으면 삭제)
     * 예) POST /api/users/1/interest/toggle?=code=005930
     */
    @PostMapping("/toggle")
    @Operation(
            summary = "관심 종목 토글",
            description = "종목 코드가 관심 목록에 없으면 추가, 있으면 삭제합니다. 예) POST /api/users/1/interest/toggle?code=005930"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "관심 종목 추가됨"),
            @ApiResponse(responseCode = "200", description = "관심 종목 삭제됨"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청(존재하지 않는 종목 코드 등)"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<InterestToggleResponse> toggle(
            @Parameter(description = "유저 ID", required = true, example = "5")
            @PathVariable int userId,
            @Parameter(description = "종목 코드", required = true, example = "005930")
            @RequestParam String code
    ){
        InterestToggleResponse result = interestService.toggle(userId, code);
        HttpStatus status = result.isAdded() ? HttpStatus.CREATED : HttpStatus.OK;
        return new ResponseEntity<>(result, status);
    }
    /**
     * 잘못된 종목 코드 등 비즈니스 예외 처리
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity
                .badRequest()
                .body(Map.of(
                        "success", false,
                        "message", e.getMessage()
                ));
    }
}
