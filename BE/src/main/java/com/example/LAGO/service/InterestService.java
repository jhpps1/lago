package com.example.LAGO.service;

import com.example.LAGO.domain.Interest;
import com.example.LAGO.domain.StockInfo;
import com.example.LAGO.dto.response.InterestResponse;
import com.example.LAGO.dto.response.InterestToggleResponse;
import com.example.LAGO.repository.InterestRepository;
import com.example.LAGO.repository.StockInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InterestService {
    private final InterestRepository interestRepository;
    private final StockInfoRepository stockInfoRepository;

    // 1. 특정 유저 관심 종목 리스트 조회
    public List<InterestResponse> list(long userId) {
        return interestRepository.findWithStockInfoByUserId(userId)
                .stream()
                .map(InterestResponse::from)
                .collect(Collectors.toList());
    }

    // 2. 토글 기능 (없으면 추가, 있으면 삭제)
    @Transactional
    public InterestToggleResponse toggle(long userId, String code) {
        if (interestRepository.existsByUserIdAndStockInfo_Code(userId, code)) {
            // 존재하면 삭제
            long deletedCount = interestRepository.deleteByUserIdAndStockInfo_Code(userId, code);
            return new InterestToggleResponse(false, deletedCount > 0);
        } else {
            // 존재하지 않으면 추가
            StockInfo stockInfo = stockInfoRepository.findByCode(code)
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 종목 코드: " + code));

            Interest interest = Interest.builder()
                    .userId(userId)
                    .stockInfoId(stockInfo.getStockInfoId())
                    .build();

            interestRepository.save(interest);
            return new InterestToggleResponse(true, true);
        }
    }



}
