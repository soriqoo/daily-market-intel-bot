package com.dbot.dmib.monitoring

import com.dbot.dmib.notify.SlackNotifier
import com.dbot.dmib.store.MonitorAlertStore
import com.dbot.dmib.store.RunStore
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.LocalDate
import java.time.OffsetDateTime

@Component
class ExecutionMonitorScheduler(
    private val runStore: RunStore,
    private val alertStore: MonitorAlertStore,
    private val slack: SlackNotifier?
) {
    private val log = LoggerFactory.getLogger(javaClass)

    // 러프 운영 초기값: 60분
    private val throttleWindow: Duration = Duration.ofMinutes(60)

    // DB까지 죽었을 때도 채널 도배를 막기 위한 메모리 fallback(프로세스 단위)
    @Volatile private var lastFallbackAlertAt: OffsetDateTime? = null

    /**
     * 미실행/실패 감지:
     * - 08:00~11:59 사이 10분마다 체크 (서버 재기동/지연에도 대응)
     * - 오늘 SENT가 아니면 경고
     * - throttle 60분: 같은 날짜 경고는 60분에 1번만 전송
     */
    @Scheduled(cron = "0 */10 8-11 * * *", zone = "\${app.timezone}")
    fun checkMorningRun() {
        val today = LocalDate.now()
        val latest = runStore.findLatest()

        if (slack == null) {
            log.warn("Monitoring skipped (Slack notifier not available). today={}, latest={}", today, latest)
            return
        }

        val ok = latest != null && latest.runDate == today && latest.status == "SENT"
        if (ok) {
            log.info("Monitoring OK. today={}, latestStatus={}", today, latest?.status)
            return
        }

        // 같은 날짜에 대한 경고는 throttle
        val alertKey = "DMIB_MISSED_OR_FAILED_${today}"

        val shouldSend = try {
            alertStore.tryAcquire(alertKey, throttleWindow)
        } catch (e: Exception) {
            // DB가 죽어 throttle 테이블을 못 쓰는 경우에도 무한 도배 방지
            val now = OffsetDateTime.now()
            val last = lastFallbackAlertAt
            val allow = last == null || Duration.between(last, now) >= throttleWindow
            if (allow) lastFallbackAlertAt = now
            log.warn("AlertStore failed (fallback throttle). allow={}, err={}", allow, e.message ?: e.javaClass.simpleName)
            allow
        }

        if (!shouldSend) {
            log.info("Monitoring alert throttled. key={}, windowMinutes={}", alertKey, throttleWindow.toMinutes())
            return
        }

        val msg = buildString {
            append("⚠️ *DMIB 미실행/실패 감지* ($today)\n")
            append("• latestRunDate: ${latest?.runDate ?: "null"}\n")
            append("• latestStatus: ${latest?.status ?: "null"}\n")
            append("• sentAt: ${latest?.sentAt ?: "null"}\n")
            if (!latest?.error.isNullOrBlank()) {
                append("• error: ${latest?.error!!.take(300)}\n")
            }
            append("\n조치:\n")
            append("1) dmib logs 확인\n")
            append("2) 외부 API/FRED/환율 응답 확인\n")
            append("3) 필요 시 dmib restart")
        }

        slack.send(msg).subscribe()
        log.warn("Monitoring alert sent. today={}, latest={}", today, latest)
    }
}
