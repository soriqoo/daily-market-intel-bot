package com.dbot.dmib.job

import com.dbot.dmib.datasource.FredClient
import com.dbot.dmib.datasource.FxClient
import com.dbot.dmib.datasource.StooqClient
import com.dbot.dmib.domain.Metric
import com.dbot.dmib.domain.MetricType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.time.LocalDate

@Service
class MarketReportService(
    private val stooq: StooqClient,
    private val fx: FxClient,
    private val fred: FredClient
) {
    private val log = LoggerFactory.getLogger(javaClass)

    data class MetricResult(
        val metric: Metric?,
        val error: String?
    )

    data class ReportResult(
        val runDate: LocalDate,
        val metrics: List<Metric>,
        val errors: List<String>,
        val slackText: String
    )

    fun buildReport(runDate: LocalDate): Mono<ReportResult> {
        val spx = stooq.fetchLatestAndPrevClose("^spx")
            .map { (latest, prev) ->
                Metric("S&P 500 (^SPX)", MetricType.INDEX, runDate, latest, prev)
            }
            .map { MetricResult(it, null) }
            .onErrorResume { e ->
                Mono.just(MetricResult(null, "S&P500 fetch failed: ${e.message ?: e.javaClass.simpleName}"))
            }

        val ndx = stooq.fetchLatestAndPrevClose("^ndx")
            .map { (latest, prev) ->
                Metric("Nasdaq-100 (^NDX)", MetricType.INDEX, runDate, latest, prev)
            }
            .map { MetricResult(it, null) }
            .onErrorResume { e ->
                Mono.just(MetricResult(null, "Nasdaq-100 fetch failed: ${e.message ?: e.javaClass.simpleName}"))
            }

        val usdkrw = fx.fetchUsdKrw()
            .map { latest ->
                Metric("USDKRW", MetricType.FX, runDate, latest, null)
            }
            .map { MetricResult(it, null) }
            .onErrorResume { e ->
                Mono.just(MetricResult(null, "USDKRW fetch failed: ${e.message ?: e.javaClass.simpleName}"))
            }

        val y10 = fred.fetch10yLatestAndPrev()
            .map { (latest, prev) ->
                Metric("US 10Y (DGS10)", MetricType.YIELD, runDate, latest, prev)
            }
            .map { MetricResult(it, null) }
            .onErrorResume { e ->
                Mono.just(MetricResult(null, "US10Y fetch failed: ${e.message ?: e.javaClass.simpleName}"))
            }

        return Mono.zip(spx, ndx, usdkrw, y10)
            .map { tuple ->
                val results = listOf(tuple.t1, tuple.t2, tuple.t3, tuple.t4)
                val metrics = results.mapNotNull { it.metric }
                val errors = results.mapNotNull { it.error }

                val slackText = formatSlack(runDate, metrics, errors)
                ReportResult(runDate, metrics, errors, slackText)
            }
            .doOnNext { rr ->
                log.info("Market report built. date={}, metrics={}, errors={}", rr.runDate, rr.metrics.size, rr.errors.size)
            }
    }

    private fun formatSlack(runDate: LocalDate, metrics: List<Metric>, errors: List<String>): String {
        fun fmt(v: BigDecimal): String = v.stripTrailingZeros().toPlainString()

        val lines = metrics.joinToString("\n") { m ->
            val ch = m.change?.let { fmt(it) } ?: "-"
            val chPct = m.changePct?.let { "${it.setScale(2, java.math.RoundingMode.HALF_UP)}%" } ?: "-"
            "• ${m.name}: ${fmt(m.value)} (Δ $ch / $chPct)"
        }.ifBlank { "• (no metrics available)" }

        // 룰 기반 “한 줄 코멘트”(LLM 없이도 유용하게)
        val quick = buildList {
            metrics.firstOrNull { it.name.contains("US 10Y") }?.change?.let { c ->
                if (c > BigDecimal("0")) add("금리 상승: 성장주/리스크 자산 변동성 주의")
            }
            metrics.firstOrNull { it.name == "USDKRW" }?.value?.let { v ->
                if (v > BigDecimal("1400")) add("환율 높음: 해외투자 환차 영향 체크")
            }
        }.distinct().take(2).joinToString(" / ").ifBlank { "변동 요인 체크" }

        val errLines = if (errors.isNotEmpty()) {
            errors.joinToString("\n") { "• $it" }
        } else {
            "• none"
        }

        return """
            *DMIB Market Snapshot*  ($runDate)
            
            *Metrics*
            $lines
            
            *Quick Note*
            • $quick
            
            *Fetch Errors*
            $errLines
        """.trimIndent()
    }
}
