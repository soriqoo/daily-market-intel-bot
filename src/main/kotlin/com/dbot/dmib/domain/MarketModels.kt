package com.dbot.dmib.domain

import java.math.BigDecimal
import java.time.LocalDate

enum class MetricType { INDEX, FX, YIELD }

data class Metric(
    val name: String,
    val type: MetricType,
    val asOf: LocalDate,
    val value: BigDecimal,
    val prevValue: BigDecimal? = null
) {
    val change: BigDecimal? = prevValue?.let { value.subtract(it) }
    val changePct: BigDecimal? = prevValue?.takeIf { it != BigDecimal.ZERO }?.let {
        value.subtract(it).divide(it, 6, java.math.RoundingMode.HALF_UP).multiply(BigDecimal("100"))
    }
}

data class MarketSnapshot(
    val runDate: LocalDate,
    val metrics: List<Metric>
)

