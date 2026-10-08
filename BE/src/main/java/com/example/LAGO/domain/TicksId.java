package com.example.LAGO.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.OffsetDateTime;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class TicksId implements Serializable {

    @Column(name = "stock_info_id")
    private Integer stockInfoId;

    @Column(name = "ts")
    private OffsetDateTime ts;
}
