package com.example.LAGO.controller;

import com.example.LAGO.dto.StockYearDto;
import com.example.LAGO.service.StockYearService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/charts/stock-year")
@Tag(name = "주식 년도별", description = "년도별 주식 정보 API")
public class StockYearController {

    private final StockYearService stockYearService;

    @GetMapping("/{stockId}")
    @Operation(summary = "년도별 주식 정보 조회", description = "지정한 주식의 특정 기간 년도별 정보를 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "정보를 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 에러")
    })
    public List<StockYearDto> getStockYears(
            @Parameter(description = "주식 ID", required = true, example = "1")
            @PathVariable("stockId") Integer stockInfoId,
            @Parameter(description = "시작 년도", required = true, example = "2020")
            @RequestParam("start") Integer start,
            @Parameter(description = "종료 년도", required = true, example = "2023")
            @RequestParam("end") Integer end
    ) {
        return stockYearService.getStockYears(stockInfoId, start, end);
    }
}
