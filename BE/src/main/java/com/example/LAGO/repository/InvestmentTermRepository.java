package com.example.LAGO.repository;

import com.example.LAGO.domain.InvestmentTerm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

/**
 * 투자 용어 Repository
 * INVESTMENT_TERM 테이블 데이터 접근 계층
 */
@Repository
public interface InvestmentTermRepository extends JpaRepository<InvestmentTerm, Integer> {
    
    /**
     * 모든 투자 용어 조회 (term_id 오름차순 정렬)
     *
     * @return 모든 투자 용어 리스트
     */
    @Query("SELECT i FROM InvestmentTerm i ORDER BY i.termId ASC")
    List<InvestmentTerm> findAllOrderByTermId();


    /**
     * 용어명(제목)에 키워드가 포함된 용어 검색
     *
     * @param keyword 검색 키워드
     * @return 검색된 투자 용어 리스트
     */
    @Query("SELECT i FROM InvestmentTerm i WHERE i.term LIKE %:keyword% ORDER BY i.termId ASC")
    List<InvestmentTerm> findByTermContaining(@Param("keyword") String keyword);
}