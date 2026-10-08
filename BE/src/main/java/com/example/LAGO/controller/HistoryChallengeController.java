package com.example.LAGO.controller;

import com.example.LAGO.constants.Interval;
import com.example.LAGO.dto.response.HistoryChallengeDataResponse;
import com.example.LAGO.dto.response.HistoryChallengeNewsResponse;
import com.example.LAGO.dto.response.HistoryChallengeResponse;
import com.example.LAGO.exception.NoContentException;
import com.example.LAGO.service.HistoryChallengeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/history-challenge")
@Tag(name = "역사챌린지", description = "역사챌린지 정보 및 차트를 조회합니다.")
@Validated
public class HistoryChallengeController {

    private final HistoryChallengeService historyChallengeService;

    @GetMapping()
    @Operation(
            summary = "역사챌린지 조회",
            description = "현재 진행 중인 역사챌린지 정보를 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "역사챌린지 조회 성공"),
            @ApiResponse(responseCode = "204", description = "진행 중인 역사챌린지 없음"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<HistoryChallengeResponse> getHistoryChallenge() {
        HistoryChallengeResponse response = historyChallengeService.getHistoryChallenge();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{challengeId}")
    @Operation(
            summary = "역사챌린지 차트 조회",
            description = "현재 진행 중인 역사챌린지 차트를 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "역사챌린지 차트 조회 성공"),
            @ApiResponse(responseCode = "204", description = "진행 중인 역사챌린지 없음"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<List<HistoryChallengeDataResponse>> getHistoryChallengeChart(
            @Parameter(description = "챌린지 ID", required = true, example = "1")
            @Validated @PathVariable Integer challengeId,
            @Parameter(description = "간격", required = true, example = "1D")
            @Validated @RequestParam Interval interval,
            @Parameter(description = "시작일시", required = true, example = "2025-08-19T15:00:00")
            @Validated @RequestParam LocalDateTime fromDateTime,
            @Parameter(description = "끝일시", required = true, example = "2025-08-19T15:00:00")
            @Validated @RequestParam LocalDateTime toDateTime
    ) {
        List<HistoryChallengeDataResponse> response = historyChallengeService.getHistoryChallengeData(challengeId, interval, fromDateTime, toDateTime);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{challengeId}/news")
    @Operation(
            summary = "역사챌린지 뉴스 조회",
            description = "현재 진행 중인 역사챌린지의 뉴스를 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "역사챌린지 뉴스 목록 조회 성공"),
            @ApiResponse(responseCode = "204", description = "진행 중인 역사챌린지 없음"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<List<HistoryChallengeNewsResponse>> getHistoryChallengeNews(
            @Parameter(description = "챌린지 ID", required = true, example = "1")
            @Validated @PathVariable Integer challengeId,
            @Parameter(description = "과거일시", required = true, example = "2025-08-19T15:00:00")
            @Validated @RequestParam LocalDateTime pastDateTime
    ) {
        List<HistoryChallengeNewsResponse> response = historyChallengeService.getChallengeNewsList(challengeId, pastDateTime);

        if (response == null || response.isEmpty()) {
            throw new NoContentException("역사챌린지 뉴스가 없습니다.");
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{challengeId}/news/{challengeNewsId}")
    @Operation(
            summary = "역사챌린지 뉴스 상세 조회",
            description = "역사챌린지의 뉴스를 단건 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "역사챌린지 뉴스 상세 조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 값"),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류")
    })
    public ResponseEntity<HistoryChallengeNewsResponse> getHistoryChallengeNewsDetail(
            @Parameter(description = "챌린지 ID", required = true, example = "1")
            @Validated @PathVariable Integer challengeId,
            @Parameter(description = "챌린지 뉴스 ID", required = true, example = "1")
            @Validated @PathVariable Integer challengeNewsId
    ) {
        HistoryChallengeNewsResponse response = historyChallengeService.getChallengeNews(challengeId, challengeNewsId);

        if (response == null) {
            throw new NoContentException("역사챌린지 뉴스가 없습니다.");
        }

        return ResponseEntity.ok(response);
    }
}
