package com.example.LAGO.realtime;

import com.example.LAGO.dto.StockInfoDto;
import com.example.LAGO.service.StockInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 종목코드와 STOCK_INFO 테이블의 stock_info_id 간 매핑을 관리하는 클래스
 * 압축 저장 시 메모리 효율성을 위해 문자열 종목코드를 정수 ID로 변환
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockIdMapper {

    private final StockInfoService stockInfoService;

    // 메모리 캐시: 양방향 매핑
    private final Map<String, Integer> codeToIdMap = new ConcurrentHashMap<>();
    private final Map<Integer, String> idToCodeMap = new ConcurrentHashMap<>();

    /**
     * 애플리케이션 시작 시 STOCK_INFO 테이블에서 모든 매핑 정보 로드
     */
    @PostConstruct
    public void loadStockMappings() {
        try {
            log.info("📊 Loading stock mappings (Test Mode - DB disabled)...");
            
            // 임시: DB 연결 없이 기본 매핑만 사용
//            createDefaultMappings();
            
            // DB 로드 부분

            List<StockInfoDto> allStocks = stockInfoService.getAllStockInfo();
            for (StockInfoDto stock : allStocks) {
                if (stock.getCode() != null && stock.getStockInfoId() != null) {
                    codeToIdMap.put(stock.getCode(), stock.getStockInfoId());
                    idToCodeMap.put(stock.getStockInfoId(), stock.getCode());
                }
            }

            
            log.info("✅ Loaded {} stock mappings successfully (Test Mode)", codeToIdMap.size());
            
        } catch (Exception e) {
            log.error("❌ Failed to load stock mappings: {}", e.getMessage(), e);
            log.warn("⚠️ Using default mappings only");
        }
    }
    
    /**
     * 테스트용 기본 매핑 생성
     */
    /*
    private void createDefaultMappings() {
        // 기본 테스트 종목들
        codeToIdMap.put("005930", 1);  // 삼성전자
        codeToIdMap.put("000660", 2);  // SK하이닉스
        codeToIdMap.put("035420", 3);  // NAVER
        
        idToCodeMap.put(1, "005930");
        idToCodeMap.put(2, "000660");
        idToCodeMap.put(3, "035420");
        
        log.info("🔧 Created default stock mappings for testing");
    }*/

    /**
     * 종목코드로 stock_info_id 조회
     * @param stockCode 종목코드 (예: "005930")
     * @return stock_info_id 또는 null (매핑 정보가 없는 경우)
     */
    public Integer getStockId(String stockCode) {
        if (stockCode == null || stockCode.trim().isEmpty()) {
            return null;
        }
        
        Integer stockId = codeToIdMap.get(stockCode.trim());
        
        if (stockId == null) {
            log.warn("⚠️ Unknown stock code: {}", stockCode);
        }
        
        return stockId;
    }

    /**
     * stock_info_id로 종목코드 조회
     * @param stockId stock_info_id
     * @return 종목코드 또는 null (매핑 정보가 없는 경우)
     */
    public String getStockCode(Integer stockId) {
        if (stockId == null) {
            return null;
        }
        
        String stockCode = idToCodeMap.get(stockId);
        
        if (stockCode == null) {
            log.warn("⚠️ Unknown stock ID: {}", stockId);
        }
        
        return stockCode;
    }

    /**
     * 특정 종목코드가 매핑에 존재하는지 확인
     * @param stockCode 종목코드
     * @return 존재 여부
     */
    public boolean containsStockCode(String stockCode) {
        return stockCode != null && codeToIdMap.containsKey(stockCode.trim());
    }

    /**
     * 특정 stock_info_id가 매핑에 존재하는지 확인
     * @param stockId stock_info_id
     * @return 존재 여부
     */
    public boolean containsStockId(Integer stockId) {
        return stockId != null && idToCodeMap.containsKey(stockId);
    }

    /**
     * 캐시된 매핑 정보 갱신 (새로운 종목이 추가된 경우)
     * DB에서 다시 로드하여 캐시 갱신
     */
    public void refreshCache() {
        log.info("🔄 Refreshing stock mappings cache...");
        
        codeToIdMap.clear();
        idToCodeMap.clear();
        
        loadStockMappings();
        
        log.info("✅ Stock mappings cache refreshed");
    }

    /**
     * 현재 로드된 매핑 정보 통계
     * @return 통계 정보 문자열
     */
    public String getStatistics() {
        return String.format("StockIdMapper[mappings=%d, codes=%d, ids=%d]",
            codeToIdMap.size(),
            codeToIdMap.keySet().size(),
            idToCodeMap.keySet().size());
    }

    /**
     * 캐시 크기 반환
     * @return 캐시된 매핑 개수
     */
    public int size() {
        return codeToIdMap.size();
    }

    /**
     * 캐시가 비어있는지 확인
     * @return 비어있음 여부
     */
    public boolean isEmpty() {
        return codeToIdMap.isEmpty();
    }
}