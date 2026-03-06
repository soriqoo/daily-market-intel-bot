package com.dbot.dmib.store

import org.springframework.jdbc.core.JdbcTemplate
import java.time.Duration
import java.time.OffsetDateTime

class MonitorAlertStore(
    private val jdbcTemplate: JdbcTemplate
) {
    /**
     * 같은 alertKey에 대해 throttleWindow 동안 한 번만 알림 전송 허용
     * - true  -> 이번에는 전송해도 됨
     * - false -> 아직 윈도우 안이라 skip
     */
    fun tryAcquire(
        alertKey: String,
        throttleWindow: Duration,
        now: OffsetDateTime = OffsetDateTime.now()
    ): Boolean {
        // 초/나노를 버려서 "10:00 vs 10:10" 오차를 조금 줄임
        val normalizedNow = now.withSecond(0).withNano(0)
        val nowStr = normalizedNow.toString()
        val thresholdStr = normalizedNow.minus(throttleWindow).toString()

        val updated = jdbcTemplate.update(
            """
            INSERT INTO monitor_alert(alert_key, last_sent_at)
            VALUES(?, ?)
            ON CONFLICT(alert_key) DO UPDATE SET
              last_sent_at = EXCLUDED.last_sent_at
            WHERE monitor_alert.last_sent_at <= ?
            """.trimIndent(),
            alertKey, nowStr, thresholdStr
        )

        return updated == 1
    }
}
