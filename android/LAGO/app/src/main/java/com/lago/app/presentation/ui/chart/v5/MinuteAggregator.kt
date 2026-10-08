package com.lago.app.presentation.ui.chart.v5

import java.time.*

data class Tick(
    val code: String,
    val date: String,        // "HHmmss"
    val openPrice: Int,
    val highPrice: Int,
    val lowPrice: Int,
    val closePrice: Int,
    val volume: Long
)

class MinuteAggregator(
    private val zone: ZoneId = ZoneId.of("Asia/Seoul"),
) {
    private var currentKey: String? = null
    private var cur: Candle? = null
    private var volSum: Long = 0

    /**
     * Aggregate ticks to a 1-minute candle. Push is invoked for every tick so UI stays live.
     * - time: epoch seconds (UTC) at minute start
     * - open: first trade of the minute (we use the first tick's closePrice)
     * - high/low: extremes within minute
     * - close: last trade
     * - volume: sum for the minute
     */
    fun onTick(t: Tick, push: (Candle, VolumeBar) -> Unit) {
        val today = LocalDate.now(zone)
        val hh = t.date.substring(0,2).toInt()
        val mm = t.date.substring(2,4).toInt()
        val minuteKey = "$today%02d:%02d".format(hh, mm)

        val minuteStart = LocalDateTime.of(today, LocalTime.of(hh, mm))
        val epochSecKST = minuteStart.toEpochSecond(ZoneOffset.of("+09:00"))

        if (currentKey != minuteKey) {
            // finalize previous minute (already updated live)
            cur?.let { push(it, VolumeBar(it.time, volSum)) }

            cur = Candle(epochSecKST, t.closePrice, t.closePrice, t.closePrice, t.closePrice)
            volSum = t.volume
            currentKey = minuteKey
        } else {
            cur?.apply {
                high = maxOf(high, t.highPrice)
                low  = minOf(low,  t.lowPrice)
                close = t.closePrice
            }
            volSum += t.volume
        }
        cur?.let { push(it, VolumeBar(it.time, volSum)) }
    }
    
    /**
     * Reset aggregator state when timeframe changes
     */
    fun reset() {
        currentKey = null
        cur = null
        volSum = 0
    }
}