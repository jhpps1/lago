package com.example.LAGO.controller;

import com.example.LAGO.service.PriceSimulatorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 주가 시뮬레이터 컨트롤러
 * AI 자동매매봇 테스트용 더미 주가 데이터 관리
 */
@RestController
@RequestMapping("/api/test/price-simulator")
@RequiredArgsConstructor
@Tag(name = "주가 시뮬레이터", description = "AI 자동매매봇 테스트용 더미 주가 생성 API")
@ConditionalOnProperty(value = "app.price.simulator.enabled", havingValue = "true", matchIfMissing = true)
public class PriceSimulatorController {

    private final PriceSimulatorService priceSimulatorService;

    /**
     * 시뮬레이터 상태 조회
     */
    @GetMapping("/status")
    @Operation(
        summary = "시뮬레이터 상태 조회",
        description = "현재 주가 시뮬레이터의 상태와 설정을 조회합니다."
    )
    public ResponseEntity<PriceSimulatorService.SimulatorStatus> getStatus() {
        PriceSimulatorService.SimulatorStatus status = priceSimulatorService.getStatus();
        return ResponseEntity.ok(status);
    }

    /**
     * 변동성 조정
     */
    @PostMapping("/volatility/{volatility}")
    @Operation(
        summary = "변동성 조정",
        description = "주가 변동성을 조정합니다. 0.1(낮음) ~ 3.0(높음)"
    )
    public ResponseEntity<Map<String, Object>> setVolatility(
            @Parameter(description = "변동성 (0.1 ~ 3.0)", required = true, example = "1.5")
            @PathVariable double volatility
    ) {
        priceSimulatorService.setVolatility(volatility);
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "변동성 설정 완료",
            "volatility", volatility
        ));
    }

    /**
     * 주가 강제 설정
     */
    @PostMapping("/force-price/{price}")
    @Operation(
        summary = "주가 강제 설정",
        description = "삼성전자 주가를 특정 가격으로 강제 설정합니다."
    )
    public ResponseEntity<Map<String, Object>> forcePrice(
            @Parameter(description = "설정할 가격 (70000 ~ 80000)", required = true, example = "77000")
            @PathVariable int price
    ) {
        priceSimulatorService.forcePrice(price);
        
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "주가 강제 설정 완료",
            "price", price
        ));
    }

    /**
     * 수동 시뮬레이션 실행
     */
    @PostMapping("/trigger")
    @Operation(
        summary = "수동 시뮬레이션 실행",
        description = "스케줄러를 기다리지 않고 즉시 주가 데이터를 생성합니다."
    )
    public ResponseEntity<Map<String, Object>> triggerSimulation() {
        try {
            priceSimulatorService.generateSamsungPriceData();
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "수동 시뮬레이션 실행 완료"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "success", false,
                "message", "시뮬레이션 실행 실패: " + e.getMessage()
            ));
        }
    }
}