package com.example.LAGO.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_year")
@Getter
@Setter
@NoArgsConstructor
public class StockYear {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "stock_year_id")
    private Integer stockYearId;

    @ManyToOne
    @JoinColumn(name = "stock_info_id", referencedColumnName = "stock_info_id")
    private StockInfo stockInfo;

    @Column(name = "date", nullable = false)
    private Integer date;

    @Column(name = "open_price", nullable = false)
    private Integer openPrice;

    @Column(name = "high_price", nullable = false)
    private Integer highPrice;

    @Column(name = "low_price", nullable = false)
    private Integer lowPrice;

    @Column(name = "close_price", nullable = false)
    private Integer closePrice;

    @Column(name = "fluctuation_rate")
    private Float fluctuationRate;

    @Column(name = "volume", nullable = false)
    private Long volume;

    @Column(name = "new_date")
    private LocalDateTime newDate;
}