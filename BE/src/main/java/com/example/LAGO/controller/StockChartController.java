package com.example.LAGO.controller;

import com.example.LAGO.constants.Interval;
import com.example.LAGO.dto.StockChartDto;
import com.example.LAGO.service.StockChartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/stocks")
@Tag(name = "주식데이터 조회", description = "주식 차트 데이터 조회 API (1m, 3m, 5m, 10m, 15m, 30m, 60m, 1D, 1W, 1M, 1Y)")
public class StockChartController {

    private final StockChartService stockChartService;

    @GetMapping("/{code}")
    @Operation(
            summary = "종목별 시간간격별 차트 데이터 조회",
            description = "특정 종목의 지정 시간간격 차트 데이터를 기간별로 조회합니다 (KST 기준)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 시간 간격 또는 파라미터"),
            @ApiResponse(responseCode = "404", description = "데이터를 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 에러")
    })
    public ResponseEntity<List<StockChartDto>> getChartData(
            @Parameter(description = "종목 코드", required = true, example = "005930")
            @PathVariable("code") String code,
            @Parameter(description = "시간 간격", required = true, example = "1m")
            @RequestParam("interval") Interval interval,
            @Parameter(description = "시작 시간 (KST)", required = true, example = "2024-08-13T09:00:00")
            @RequestParam("fromDateTime") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDateTime,
            @Parameter(description = "종료 시간 (KST)", required = true, example = "2024-08-13T15:30:00")
            @RequestParam("toDateTime") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDateTime
    ) {
        // interval은 enum이므로 별도 유효성 검증 불필요

        // 디버깅 로그
        System.out.println("🔍 차트 데이터 요청:");
        System.out.println("code: " + code);
        System.out.println("interval: " + interval.getCode());
        System.out.println("fromDateTime: " + fromDateTime);
        System.out.println("toDateTime: " + toDateTime);

        List<StockChartDto> result = stockChartService.getChartDataByCodeAndInterval(
                code, interval.getCode(), fromDateTime, toDateTime);

        System.out.println("📊 차트 조회 결과: " + result.size() + "건");
        if (!result.isEmpty()) {
            System.out.println("첫 번째 데이터: " + result.get(0).getBucket());
            System.out.println("마지막 데이터: " + result.get(result.size() - 1).getBucket());
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/{code}/latest")
    @Operation(
            summary = "종목별 시간간격별 최신 차트 데이터 조회",
            description = "특정 종목의 지정 시간간격 최신 차트 데이터(100개)를 조회합니다"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 시간 간격"),
            @ApiResponse(responseCode = "404", description = "데이터를 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 에러")
    })
    public ResponseEntity<List<StockChartDto>> getLatestChartData(
            @Parameter(description = "종목 코드", required = true, example = "005930")
            @PathVariable("code") String code,
            @Parameter(description = "시간 간격", required = true, example = "3m")
            @RequestParam("interval") Interval interval
    ) {
        // interval은 enum이므로 별도 유효성 검증 불필요

        List<StockChartDto> result = stockChartService.getLatestChartDataByCode(code, interval.getCode());

        if (result.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(result);
    }

    @GetMapping("/intervals")
    @Operation(
            summary = "지원하는 시간 간격 목록 조회",
            description = "시스템에서 지원하는 모든 시간 간격을 조회합니다"
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    public ResponseEntity<List<String>> getSupportedIntervals() {
        return ResponseEntity.ok(stockChartService.getSupportedIntervals());
    }
}