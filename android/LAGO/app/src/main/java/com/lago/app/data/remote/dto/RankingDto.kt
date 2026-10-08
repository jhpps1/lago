package com.lago.app.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * 랭킹 API 응답 DTO (profitRate, totalProfit는 사용하지 않음)
 */
data class RankingDto(
    @SerializedName("rank")
    val rank: Int,
    @SerializedName("userId")
    val userId: Int,
    @SerializedName("username")
    val username: String,
    @SerializedName("totalAsset")
    val totalAsset: Long,
    @SerializedName("profitRate")
    val profitRate: Double,
    @SerializedName("totalProfit")
    val totalProfit: Long,
    @SerializedName("personality")
    val personality: String,
    @SerializedName("isAi")
    val isAi: Boolean = false
)

/**
 * 계산된 랭킹 데이터
 */
data class CalculatedRankingUser(
    val rank: Int,
    val userId: Int,
    val username: String,
    val totalAsset: Long,
    val calculatedProfitRate: Double,    // (totalAsset - 1,000,000) / 1,000,000 * 100
    val calculatedProfit: Long,          // totalAsset - 1,000,000
    val isCurrentUser: Boolean = false,
    val isAi: Boolean = false,
    val personality: String = "위험중립형"
)