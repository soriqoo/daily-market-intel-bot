package com.dbot.dmib.job

import com.dbot.dmib.config.AppProperties
import com.dbot.dmib.datasource.FredClient
import com.dbot.dmib.datasource.FxClient
import com.dbot.dmib.datasource.StooqClient
import com.dbot.dmib.domain.MarketSnapshot
import com.dbot.dmib.domain.Metric
import com.dbot.dmib.domain.MetricType
import com.dbot.dmib.llm.DigestPrompt
import com.dbot.dmib.llm.DailyDigest
import com.dbot.dmib.llm.OpenAiClient
import com.dbot.dmib.notify.GmailNotifier
import com.dbot.dmib.notify.SlackNotifier
import com.dbot.dmib.store.RunStore
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.security.MessageDigest
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Component
class DailyMarketJob(
    private val props: AppProperties,
    private val stooq: StooqClient,
    private val fx: FxClient,
    private val fred: FredClient,
    private val runStore: RunStore,
    // 조건부 빈: enabled=false면 Bean이 없으므로 nullable
    private val openai: OpenAiClient?,
    private val slack: SlackNotifier?,
    private val gmail: GmailNotifier?
) {

    /**
     * 매일 08:00 (Asia/Seoul). 실제 크론/zone은 application.yml에서 관리.
     */
    @Scheduled(cron = "\${app.schedule.cron}", zone = "\${app.timezone}")
    fun run() {
        val runDate = LocalDate.now()

        // 테스트/로컬에서 키가 없을 때 컨텍스트 로딩은 성공해야 하므로, 실행만 스킵
        if (openai == null || slack == null) {
            return
        }

        // 동일 날짜 중복 전송 방지
        if (runStore.alreadySent(runDate)) {
            return
        }

        // 지표 수집 (MVP)
        val metricsMono: Mono<List<Metric>> = Mono.zip(
            stooq.fetchLatestAndPrevClose("^spx"),
            stooq.fetchLatestAndPrevClose("^ndx"),
            fx.fetchUsdKrw(),
            fred.fetch10yLatestAndPrev()
        ).map { tuple ->
            val (spxLatest, spxPrev) = tuple.t1
            val (ndxLatest, ndxPrev) = tuple.t2
            val usdkrw = tuple.t3
            val (y10, y10Prev) = tuple.t4

            listOf(
                Metric("S&P 500 (^SPX)", MetricType.INDEX, runDate, spxLatest, spxPrev),
                Metric("Nasdaq-100 (^NDX)", MetricType.INDEX, runDate, ndxLatest, ndxPrev),
                Metric("USDKRW", MetricType.FX, runDate, usdkrw, null),
                Metric("US 10Y (DGS10)", MetricType.YIELD, runDate, y10, y10Prev)
            )
        }

        metricsMono
            .flatMap { metrics ->
                val snapshot = MarketSnapshot(runDate, metrics)
                val system = DigestPrompt.system()
                val user = DigestPrompt.user(snapshot)

                openai.summarize(system, user)
                    .map { digest ->
                        val message = buildSlackMessage(snapshot, digest)
                        val hash = sha256(message)
                        Triple(snapshot, digest, Pair(message, hash))
                    }
            }
            .flatMap { (snapshot, digest, msgAndHash) ->
                val (message, hash) = msgAndHash

                // Slack 전송은 필수
                slack.send(message)
                    .doOnSuccess {
                        // Email은 옵션
                        if (props.mail.enabled && gmail != null && props.mail.to.isNotBlank()) {
                            gmail.send(
                                to = props.mail.to,
                                subject = "[DMIB] ${digest.title}",
                                body = message
                            )
                        }
                        runStore.markSent(runDate, hash)
                    }
            }
            .doOnError { e ->
                val err = (e.message ?: e.toString()).take(2000)
                runStore.markFailed(runDate, payloadHash = "N/A", error = err)

                // 실패 알림도 Slack으로(가능하면)
                slack.send(":warning: DMIB FAILED (${runDate.format(DateTimeFormatter.ISO_DATE)})\n$err")
                    .subscribe()
            }
            .subscribe()
    }

    private fun buildSlackMessage(snapshot: MarketSnapshot, digest: DailyDigest): String {
        fun fmt(v: BigDecimal): String = v.stripTrailingZeros().toPlainString()

        val metricsLines = snapshot.metrics.joinToString("\n") { m ->
            val ch = m.change?.let { fmt(it) } ?: "-"
            val chPct = m.changePct?.let { "${it.setScale(2, java.math.RoundingMode.HALF_UP)}%" } ?: "-"
            "• ${m.name}: ${fmt(m.value)} (Δ $ch / $chPct)"
        }

        val bullets = digest.summaryBullets.joinToString("\n") { "• $it" }
        val risks = digest.risks.joinToString("\n") { "• $it" }
        val actions = digest.actionItems.joinToString("\n") { "• $it" }
        val keys = digest.keyNumbers.joinToString("\n") { "• $it" }

        return """
            *${digest.title}*  (${snapshot.runDate})
            
            *Metrics*
            $metricsLines
            
            *Summary*
            $bullets
            
            *Key Numbers*
            $keys
            
            *Risks*
            $risks
            
            *Action Items*
            $actions
        """.trimIndent()
    }

    private fun sha256(s: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(s.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}
