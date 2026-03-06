package com.dbot.dmib.monitoring

import com.dbot.dmib.notify.SlackNotifier
import com.dbot.dmib.store.MonitorAlertStore
import com.dbot.dmib.store.RunStore
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Duration
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
    private val throttleWindow: Duration = Duration.ofMinutes(60)

    @Volatile
    private var lastFallbackAlertAt: OffsetDateTime? = null

    /**
     * 08:00~11:59 사이 10분마다 실행
     * - 오늘 runDate가 SENT가 아니면 경고
     * - 같은 날짜 경고는 60분에 1번만
     */
    @Scheduled(cron = "0 */10 8-11 * * *", zone = "\${app.timezone}")
    fun checkMorningRun() {
        val zone = ZoneId.of(appTimezone)
        val today = LocalDate.now(zone)

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
            log.info("Monitoring OK. today={}, latestStatus={}", today, latestStatus)
            return
        }

        val alertKey = "DMIB_MISSED_OR_FAILED_$today"

        val shouldSend = try {
            alertStore.tryAcquire(alertKey, throttleWindow)
        } catch (e: Exception) {
            // DB 자체가 죽어 throttle 테이블을 못 쓰는 경우 fallback
            val now = OffsetDateTime.now().withSecond(0).withNano(0)
            val last = lastFallbackAlertAt
            val allow = last == null || Duration.between(last, now) >= throttleWindow
            if (allow) {
                lastFallbackAlertAt = now
            }

            log.warn(
                "AlertStore failed (fallback throttle). allow={}, err={}",
                allow,
                e.message ?: e.javaClass.simpleName
            )
            allow
        }

        if (!shouldSend) {
            log.info(
                "Monitoring alert throttled. key={}, windowMinutes={}, latestRunDate={}, latestStatus={}",
                alertKey, throttleWindow.toMinutes(), latestRunDate, latestStatus
            )
            return
        }

        val msg = buildString {
            append("⚠️ *DMIB 미실행/실패 감지* ($today)\n")
            append("• latestRunDate: ${latestRunDate ?: "null"}\n")
            append("• latestStatus: ${latestStatus ?: "null"}\n")
            append("• sentAt: ${latestSentAt ?: "null"}\n")
            if (!latestError.isNullOrBlank()) {
                append("• error: ${latestError.take(300)}\n")
            }
            append("\n조치:\n")
            append("1) dmib logs 확인\n")
            append("2) 외부 API/FRED/환율 응답 확인\n")
            append("3) 필요 시 dmib restart")
        }

        slack.send(msg).subscribe()
        log.warn(
            "Monitoring alert sent. today={}, latestRunDate={}, latestStatus={}",
            today, latestRunDate, latestStatus
        )
    }
}
