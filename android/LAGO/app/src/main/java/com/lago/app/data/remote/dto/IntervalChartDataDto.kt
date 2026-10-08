package com.lago.app.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 인터벌 차트 데이터 DTO
 * API 응답: /api/stocks/{code}?interval=MINUTE30&fromDateTime=2024-08-13T09:00:00&toDateTime=2024-08-15T15:30:00
 */
@Serializable
data class IntervalChartDataDto(
    @SerialName("stockInfoId")
    val stockInfoId: Int,
    
    @SerialName("bucket")
    val bucket: String, // "2024-08-13T09:00:00" (버킷 시작 시각)
    
    @SerialName("code")
    val code: String, // "005930"
    
    @SerialName("interval")
    val interval: String, // "30m"
    
    @SerialName("openPrice")
    val openPrice: Long,
    
    @SerialName("highPrice")
    val highPrice: Long,
    
    @SerialName("lowPrice")
    val lowPrice: Long,
    
    @SerialName("closePrice")
    val closePrice: Long,
    
    @SerialName("volume")
    val volume: Long
)