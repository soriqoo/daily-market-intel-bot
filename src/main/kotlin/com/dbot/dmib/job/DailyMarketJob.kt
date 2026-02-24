package com.dbot.dmib.job

import com.dbot.dmib.notify.SlackNotifier
import com.dbot.dmib.store.RunStore
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import java.security.MessageDigest
import java.time.LocalDate
import java.time.OffsetDateTime

@Component
class DailyMarketJob(
    private val reportService: MarketReportService,
    private val runStore: RunStore,
    private val slack: SlackNotifier?
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "\${app.schedule.cron}", zone = "\${app.timezone}")
    fun run() {
        val runDate = LocalDate.now()
        val startedAt = OffsetDateTime.now()

        // Slack 비활성/미주입이면 실행 자체를 스킵(운영 안전)
        if (slack == null) {
            log.info("Slack notifier not available. Skip run. date={}", runDate)
            return
        }

        reportService.buildReport(runDate)
            .flatMap { rr ->
                val hash = sha256(rr.slackText)

                // Idempotency: 같은 날짜에 같은 payload면 전송 스킵
                val prevHash = runStore.findSentHash(runDate)
                if (prevHash != null && prevHash == hash) {
                    log.info("Skip sending (same payload already sent). date={}, hash={}", runDate, hash)
                    return@flatMap Mono.empty<Void>()
                }

                // metrics가 0개면 실패 처리 + Slack 실패 메시지 전송(운영 관측)
                if (rr.metrics.isEmpty()) {
                    val errText = ":warning: DMIB FAILED ($runDate) - no metrics available.\n" +
                            rr.errors.joinToString("\n") { "• $it" }.ifBlank { "• unknown error" }

                    runStore.markFailed(runDate, payloadHash = hash, error = errText)
                    return@flatMap slack.send(errText)
                }

                // 정상 전송
                slack.send(rr.slackText)
                    .doOnSuccess {
                        runStore.markSent(runDate, hash)
                        val elapsedMs = java.time.Duration.between(startedAt, OffsetDateTime.now()).toMillis()
                        log.info("DMIB SENT. date={}, hash={}, elapsedMs={}", runDate, hash, elapsedMs)
                    }
            }
            .doOnError { e ->
                val err = (e.message ?: e.toString()).take(2000)
                runStore.markFailed(runDate, payloadHash = "N/A", error = err)
                log.error("DMIB ERROR. date={}, err={}", runDate, err)
                // Slack이 있으니 실패도 알림(에이전트 운영성)
                slack.send(":warning: DMIB ERROR ($runDate)\n$err").subscribe()
            }
            .subscribe()
    }

    private fun sha256(s: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(s.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}
