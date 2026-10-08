package com.example.LAGO.repository;

import com.example.LAGO.domain.KnowTerm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface KnowTermRepository extends JpaRepository<KnowTerm, Integer> {
    
    Optional<KnowTerm> findByUserIdAndTermId(Integer userId, Integer termId);
    
    boolean existsByUserIdAndTermId(Integer userId, Integer termId);
}