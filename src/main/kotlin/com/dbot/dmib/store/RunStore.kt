package com.dbot.dmib.store

import org.springframework.jdbc.core.JdbcTemplate
import java.time.LocalDate
import java.time.OffsetDateTime

class RunStore(private val jdbcTemplate: JdbcTemplate) {

    fun alreadySent(runDate: LocalDate): Boolean {
        val cnt = jdbcTemplate.queryForObject(
            "SELECT COUNT(1) FROM job_run WHERE run_date = ? AND status = 'SENT'",
            Long::class.java,
            runDate.toString()
        ) ?: 0L
        return cnt > 0
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
