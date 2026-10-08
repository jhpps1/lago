package com.example.LAGO.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Objects;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Ticks30mId implements Serializable {

    @Column(name = "stock_info_id")
    private Integer stockInfoId;

    @Column(name = "bucket")
    private OffsetDateTime bucket;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Ticks30mId ticks30mId = (Ticks30mId) o;
        return Objects.equals(stockInfoId, ticks30mId.stockInfoId) &&
               Objects.equals(bucket, ticks30mId.bucket);
    }

    @Override
    public int hashCode() {
        return Objects.hash(stockInfoId, bucket);
    }
}