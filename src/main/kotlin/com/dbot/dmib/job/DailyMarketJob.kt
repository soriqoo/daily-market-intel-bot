package com.dbot.dmib.job

import com.dbot.dmib.notify.SlackNotifier
import com.dbot.dmib.store.RunStore
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import java.security.MessageDigest
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId

@Component
class DailyMarketJob(
    private val reportService: MarketReportService,
    private val runStore: RunStore,
    private val slack: SlackNotifier?,
    @Value("\${app.timezone}") private val appTimezone: String
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(cron = "\${app.schedule.cron}", zone = "\${app.timezone}")
    fun run() {
        val zone = ZoneId.of(appTimezone)
        val runDate = LocalDate.now(zone)
        val startedAt = OffsetDateTime.now()

        if (slack == null) {
            log.info("Slack notifier not available. Skip run. date={}", runDate)
            return
        }

        reportService.buildReport(runDate)
            .flatMap { rr ->
                val hash = sha256(rr.slackText)

                val prevHash = runStore.findSentHash(runDate)
                if (prevHash != null && prevHash == hash) {
                    log.info("Skip sending (same payload already sent). date={}, hash={}", runDate, hash)
                    return@flatMap Mono.empty<Void>()
                }

                if (rr.metrics.isEmpty()) {
                    val errText = ":warning: DMIB FAILED ($runDate) - no metrics available.\n" +
                            rr.errors.joinToString("\n") { "• $it" }.ifBlank { "• unknown error" }

                    runStore.markFailed(runDate, payloadHash = hash, error = errText)
                    return@flatMap slack.send(errText)
                }

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
                slack.send(":warning: DMIB ERROR ($runDate)\n$err").subscribe()
            }
            .subscribe()
    }

    private fun sha256(s: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(s.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}
