package com.dbot.dmib.monitoring

import com.dbot.dmib.notify.SlackNotifier
import com.dbot.dmib.store.RunStore
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class ExecutionMonitorScheduler(
    private val runStore: RunStore,
    private val slack: SlackNotifier?
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * "미실행 감지" 스케줄러(운영 핵심)
     *
     * 예) 매일 08:10에 체크:
     * - 오늘 run_date가 SENT가 아니면 Slack 경고
     * - Slack이 꺼져 있으면 로그만 남김
     *
     * 크론은 application.yml에서 조정 가능하게 두는 걸 추천하지만,
     * 우선 하드코딩으로 시작해도 실무에서 흔함(MVP).
     */
    @Scheduled(cron = "0 10 8 * * *", zone = "\${app.timezone}")
    fun checkMorningRun() {
        val today = LocalDate.now()
        val latest = runStore.findLatest()

        // slack 비활성화 시 운영 경고는 로그로만 남김
        if (slack == null) {
            log.warn("Monitoring skipped (Slack notifier not available). today={}, latest={}", today, latest)
            return
        }

        val ok = latest != null && latest.runDate == today && latest.status == "SENT"
        if (ok) {
            log.info("Monitoring OK. today={}, latestStatus={}", today, latest?.status)
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
            append("조치: dmib logs 확인 후 재기동/원인 분석")
        }

        slack.send(msg).subscribe()
        log.warn("Monitoring alert sent. today={}, latest={}", today, latest)
    }
}
