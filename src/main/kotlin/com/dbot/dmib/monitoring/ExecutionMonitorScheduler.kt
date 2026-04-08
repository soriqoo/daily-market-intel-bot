package com.dbot.dmib.monitoring

import com.dbot.dmib.notify.SlackNotifier
import com.dbot.dmib.store.MonitorAlertStore
import com.dbot.dmib.store.RunStore
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId

@Component
class ExecutionMonitorScheduler(
    private val runStore: RunStore,
    private val alertStore: MonitorAlertStore,
    private val slack: SlackNotifier?,
    @Value("\${app.timezone}") private val appTimezone: String
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Volatile
    private var lastFallbackAlertAt: OffsetDateTime? = null

    @Scheduled(cron = "\${app.monitoring.check-cron}", zone = "\${app.timezone}")
    fun checkMorningRun() {
        val zone = ZoneId.of(appTimezone)
        val now = MonitoringAlertPolicy.normalize(OffsetDateTime.now(zone))
        val today = LocalDate.now(zone)
        val alertKey = "DMIB_MISSED_OR_FAILED_$today"

        if (now.hour == 8 && now.minute == 0) {
            log.info("Monitoring warm-up tick skipped. now={}", now)
            return
        }

        val latest = runStore.findLatest()
        val latestRunDate = latest?.runDate
        val latestStatus = latest?.status
        val latestSentAt = latest?.sentAt
        val latestError = latest?.error

        if (slack == null) {
            log.warn(
                "Monitoring skipped (Slack notifier not available). today={}, latestRunDate={}, latestStatus={}",
                today, latestRunDate, latestStatus
            )
            return
        }

        val ok = latestRunDate == today && latestStatus == "SENT"
        if (ok) {
            clearAlertState(alertKey)
            log.info("Monitoring OK. today={}, latestStatus={}", today, latestStatus)
            return
        }

        val shouldSend = try {
            alertStore.tryAcquire(alertKey, now)
        } catch (e: Exception) {
            val allow = MonitoringAlertPolicy.shouldSend(now, lastFallbackAlertAt)
            if (allow) {
                lastFallbackAlertAt = now
            }

            log.warn(
                "AlertStore failed (fallback policy). allow={}, err={}",
                allow,
                e.message ?: e.javaClass.simpleName
            )
            allow
        }

        if (!shouldSend) {
            log.info(
                "Monitoring alert skipped. key={}, latestRunDate={}, latestStatus={}, now={}",
                alertKey, latestRunDate, latestStatus, now
            )
            return
        }

        val msg = buildString {
            append(":warning: *DMIB 미실행/실패 감지* ($today)\n")
            append("• latestRunDate: ${latestRunDate ?: "null"}\n")
            append("• latestStatus: ${latestStatus ?: "null"}\n")
            append("• sentAt: ${latestSentAt ?: "null"}\n")
            if (!latestError.isNullOrBlank()) {
                append("• error: ${latestError.take(300)}\n")
            }
            append("\n:hammer_and_wrench: *조치*\n")
            append("1) `dmib logs` 확인\n")
            append("2) 외부 API/FRED/환율 응답 확인\n")
            append("3) 필요 시 `dmib restart`\n")
        }

        slack.send(msg).subscribe()
        log.warn(
            "Monitoring alert sent. today={}, latestRunDate={}, latestStatus={}, now={}",
            today, latestRunDate, latestStatus, now
        )
    }

    private fun clearAlertState(alertKey: String) {
        lastFallbackAlertAt = null

        try {
            alertStore.clear(alertKey)
        } catch (e: Exception) {
            log.warn(
                "Failed to clear monitoring alert state. key={}, err={}",
                alertKey,
                e.message ?: e.javaClass.simpleName
            )
        }
    }
}
