package com.example.LAGO.repository;

import com.example.LAGO.domain.Ticks60m;
import com.example.LAGO.domain.Ticks60mId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface Ticks60mRepository extends JpaRepository<Ticks60m, Ticks60mId> {

    @Query("SELECT t FROM Ticks60m t JOIN FETCH t.stockInfo s WHERE s.code = :code AND t.id.bucket >= :startTime AND t.id.bucket < :endTime ORDER BY t.id.bucket ASC")
    List<Ticks60m> findByCodeAndBucketRange(
            @Param("code") String code,
            @Param("startTime") OffsetDateTime startTime,
            @Param("endTime") OffsetDateTime endTime
    );

    @Query("SELECT t FROM Ticks60m t JOIN FETCH t.stockInfo s WHERE s.code = :code ORDER BY t.id.bucket DESC")
    List<Ticks60m> findLatestByCode(@Param("code") String code, Pageable pageable);
}