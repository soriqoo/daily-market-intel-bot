package com.dbot.dmib.store

import org.springframework.jdbc.core.JdbcTemplate
import java.time.LocalDate
import java.time.OffsetDateTime

data class RunRecord(
    val runDate: LocalDate,
    val status: String,
    val payloadHash: String,
    val sentAt: OffsetDateTime?,
    val error: String?
)

class RunStore(private val jdbcTemplate: JdbcTemplate) {

    fun alreadySent(runDate: LocalDate): Boolean {
        val cnt = jdbcTemplate.queryForObject(
            "SELECT COUNT(1) FROM job_run WHERE run_date = ? AND status = 'SENT'",
            Long::class.java,
            runDate.toString()
        ) ?: 0L
        return cnt > 0
    }

    fun findSentHash(runDate: LocalDate): String? {
        return jdbcTemplate.query(
            "SELECT payload_hash FROM job_run WHERE run_date = ? AND status = 'SENT' LIMIT 1",
            arrayOf(runDate.toString())
        ) { rs, _ -> rs.getString("payload_hash") }
            .firstOrNull()
    }

    /** ✅ 운영/감시용: 마지막 실행 기록(가장 최근 run_date)을 조회 */
    fun findLatest(): RunRecord? {
        return jdbcTemplate.query(
            """
            SELECT run_date, status, payload_hash, sent_at, error
            FROM job_run
            ORDER BY run_date DESC
            LIMIT 1
            """.trimIndent()
        ) { rs, _ ->
            RunRecord(
                runDate = LocalDate.parse(rs.getString("run_date")),
                status = rs.getString("status"),
                payloadHash = rs.getString("payload_hash"),
                sentAt = rs.getString("sent_at")?.let { OffsetDateTime.parse(it) },
                error = rs.getString("error")
            )
        }.firstOrNull()
    }

    fun markSent(runDate: LocalDate, payloadHash: String) {
        jdbcTemplate.update(
            """
            INSERT INTO job_run(run_date, status, payload_hash, sent_at, error)
            VALUES(?, 'SENT', ?, ?, NULL)
            ON CONFLICT(run_date) DO UPDATE SET
              status='SENT', payload_hash=excluded.payload_hash, sent_at=excluded.sent_at, error=NULL
            """.trimIndent(),
            runDate.toString(), payloadHash, OffsetDateTime.now().toString()
        )
    }

    fun markFailed(runDate: LocalDate, payloadHash: String, error: String) {
        jdbcTemplate.update(
            """
            INSERT INTO job_run(run_date, status, payload_hash, sent_at, error)
            VALUES(?, 'FAILED', ?, NULL, ?)
            ON CONFLICT(run_date) DO UPDATE SET
              status='FAILED', payload_hash=excluded.payload_hash, error=excluded.error
            """.trimIndent(),
            runDate.toString(), payloadHash, error.take(2000)
        )
    }
}
