package com.dbot.dmib.store

import com.dbot.dmib.monitoring.MonitoringAlertPolicy
import org.springframework.jdbc.core.JdbcTemplate
import java.time.OffsetDateTime

class MonitorAlertStore(
    private val jdbcTemplate: JdbcTemplate
) {
    fun tryAcquire(
        alertKey: String,
        now: OffsetDateTime = OffsetDateTime.now()
    ): Boolean {
        val normalizedNow = MonitoringAlertPolicy.normalize(now)
        val lastSentAt = findLastSentAt(alertKey)

        if (!MonitoringAlertPolicy.shouldSend(normalizedNow, lastSentAt)) {
            return false
        }

        val nowStr = normalizedNow.toString()
        val updated = jdbcTemplate.update(
            """
            UPDATE monitor_alert
            SET last_sent_at = ?
            WHERE alert_key = ?
            """.trimIndent(),
            nowStr, alertKey
        )

        if (updated > 0) {
            return true
        }

        jdbcTemplate.update(
            """
            INSERT INTO monitor_alert(alert_key, last_sent_at)
            VALUES(?, ?)
            """.trimIndent(),
            alertKey, nowStr
        )

        return true
    }

    fun clear(alertKey: String) {
        jdbcTemplate.update(
            "DELETE FROM monitor_alert WHERE alert_key = ?",
            alertKey
        )
    }

    private fun findLastSentAt(alertKey: String): OffsetDateTime? {
        return jdbcTemplate.query(
            "SELECT last_sent_at FROM monitor_alert WHERE alert_key = ?",
            arrayOf(alertKey)
        ) { rs, _ -> OffsetDateTime.parse(rs.getString("last_sent_at")) }
            .firstOrNull()
    }
}
