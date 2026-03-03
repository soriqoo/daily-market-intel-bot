package com.dbot.dmib.store

import org.springframework.jdbc.core.JdbcTemplate
import java.time.Duration
import java.time.OffsetDateTime

class MonitorAlertStore(
    private val jdbcTemplate: JdbcTemplate
) {
    /**
     * alertKey에 대해 throttleWindow 동안 "한 번만" 보내도록 토큰을 획득한다.
     * - true: 지금 보내도 됨(기록/갱신 성공)
     * - false: 아직 window 내라서 보내면 안 됨
     *
     * Postgres 기준으로 원자적으로 동작하도록 upsert + WHERE 조건을 사용.
     */
    fun tryAcquire(alertKey: String, throttleWindow: Duration, now: OffsetDateTime = OffsetDateTime.now()): Boolean {
        val nowStr = now.toString()
        val thresholdStr = now.minus(throttleWindow).toString()

        val updated = jdbcTemplate.update(
            """
            INSERT INTO monitor_alert(alert_key, last_sent_at)
            VALUES(?, ?)
            ON CONFLICT(alert_key) DO UPDATE SET
              last_sent_at = EXCLUDED.last_sent_at
            WHERE monitor_alert.last_sent_at < ?
            """.trimIndent(),
            alertKey, nowStr, thresholdStr
        )
        // insert 또는 update 성공이면 1
        return updated == 1
    }
}
