package com.example.LAGO.service;

import com.example.LAGO.domain.InvestmentTerm;
import com.example.LAGO.domain.KnowTerm;
import com.example.LAGO.dto.InvestmentTermDto;
import com.example.LAGO.repository.InvestmentTermRepository;
import com.example.LAGO.repository.KnowTermRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 투자 용어 서비스
 * INVESTMENT_TERM 테이블 비즈니스 로직 처리
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class InvestmentTermService {

    private final InvestmentTermRepository investmentTermRepository;
    private final KnowTermRepository knowTermRepository;

    /**
     * 모든 투자 용어 조회
     *
     * @param userId 사용자 ID (선택사항)
     * @return 투자 용어 리스트
     */
    public List<InvestmentTermDto> getAllInvestmentTerms(Integer userId) {
        log.debug("모든 투자 용어 조회 요청 - userId: {}", userId);
        
        List<InvestmentTerm> investmentTerms = investmentTermRepository.findAllOrderByTermId();
        
        log.debug("투자 용어 {}개 조회됨", investmentTerms.size());
        
        return investmentTerms.stream()
                .map(term -> {
                    InvestmentTermDto dto = new InvestmentTermDto(term);
                    if (userId != null) {
                        dto.setKnowStatus(getKnowStatus(userId, term.getTermId()));
                    }
                    return dto;
                })
                .collect(Collectors.toList());
    }


    /**
     * 키워드로 투자 용어 검색 (용어명만 검색)
     *
     * @param keyword 검색 키워드
     * @param userId 사용자 ID (선택사항)
     * @return 검색된 투자 용어 리스트
     */
    public List<InvestmentTermDto> searchInvestmentTerms(String keyword, Integer userId) {
        log.debug("투자 용어 검색 요청 - keyword: {}, userId: {}", keyword, userId);
        
        List<InvestmentTerm> investmentTerms = investmentTermRepository.findByTermContaining(keyword);
        
        log.debug("검색된 투자 용어 {}개", investmentTerms.size());
        
        return investmentTerms.stream()
                .map(term -> {
                    InvestmentTermDto dto = new InvestmentTermDto(term);
                    if (userId != null) {
                        dto.setKnowStatus(getKnowStatus(userId, term.getTermId()));
                    }
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * 사용자의 특정 용어에 대한 이해도 조회
     *
     * @param userId 사용자 ID
     * @param termId 용어 ID
     * @return 이해도 (true: 안다, false: 모른다, null: 기록 없음)
     */
    private Boolean getKnowStatus(Integer userId, Integer termId) {
        Optional<KnowTerm> knowTerm = knowTermRepository.findByUserIdAndTermId(userId, termId);
        
        if (knowTerm.isPresent()) {
            return knowTerm.get().getCorrect();
        }
        
        return null; // 기록 없음
    }
}