package com.example.LAGO.controller;

import com.example.LAGO.dto.StockDayDto;
import com.example.LAGO.dto.StockMinuteDto;
import com.example.LAGO.service.StockDayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/stocks/day")
@Tag(name = "주식 일봉", description = "일별 주식 정보 API")
public class StockDayController {

    private final StockDayService stockDayService;

//    @GetMapping("/{stockId}")
//    @Operation(summary = "일별 주식 정보 조회", description = "지정한 주식의 특정 기간 일별 정보를 조회합니다.")
//    @ApiResponses({
//            @ApiResponse(responseCode = "200", description = "조회 성공"),
//            @ApiResponse(responseCode = "404", description = "정보를 찾을 수 없음"),
//            @ApiResponse(responseCode = "500", description = "서버 에러")
//    })
//    public List<StockDayDto> getStockDays(
//            @Parameter(description = "주식 ID", required = true, example = "1")
//            @PathVariable("stockId") Integer stockInfoId,
//            @Parameter(description = "시작일", required = true, example = "2023-01-01")
//            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
//            @Parameter(description = "종료일", required = true, example = "2023-12-31")
//            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
//    ) {
//        return stockDayService.getStockDays(stockInfoId, start, end);
//    }

    // 주식 종목 코드로 조회
    @GetMapping("/{code}")
    @Operation(
            summary = "일봉 주식 정보 조회",
            description = "일 단위(interval)의 주식(code) 일봉 데이터를 기간(start~end)으로 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "정보를 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 에러")
    })
    public ResponseEntity<List<StockDayDto>> getStockDaysByCode(
            @Parameter(description = "종목 코드", required = true, example = "005930")
            @PathVariable("code") String code,
            @Parameter(description = "시작일시", required = true, example = "2023-01-01")
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @Parameter(description = "종료일시", required = true, example = "2023-12-31")
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        // 디버깅 로그
        System.out.println("🔍 Request Params:");
        System.out.println("code: " + code);
        System.out.println("start: " + start);
        System.out.println("end: " + end);

        // 서비스 시그니처에 맞게 구현하세요.
        List<StockDayDto> result = stockDayService.getDaysByCode(code, start, end);

        System.out.println("📊 Query Result Count: " + result.size());
        if (!result.isEmpty()) {
            System.out.println("첫 번째 데이터: " + result.get(0).getDate());
            System.out.println("마지막 데이터: " + result.get(result.size() - 1).getDate());
        }

        return ResponseEntity.ok(result);
    }
}
