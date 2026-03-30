package com.dbot.dmib.store

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate

@SpringBootTest
@ActiveProfiles("test")
class RunStoreIntegrationTests {

    @Autowired
    lateinit var runStore: RunStore

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    @BeforeEach
    fun setUp() {
        jdbcTemplate.update("DELETE FROM monitor_alert")
        jdbcTemplate.update("DELETE FROM job_run")
    }

    @Test
    fun markSentPersistsHashAndLatestRecord() {
        val runDate = LocalDate.of(2026, 3, 30)

        runStore.markSent(runDate, "hash-123")

        assertEquals("hash-123", runStore.findSentHash(runDate))

        val latest = runStore.findLatest()
        assertNotNull(latest)
        assertEquals(runDate, latest?.runDate)
        assertEquals("SENT", latest?.status)
        assertEquals("hash-123", latest?.payloadHash)
        assertNotNull(latest?.sentAt)
        assertNull(latest?.error)
    }

    @Test
    fun markFailedTruncatesErrorTo2000Characters() {
        val runDate = LocalDate.of(2026, 3, 30)
        val longError = "x".repeat(2500)

        runStore.markFailed(runDate, "failed-hash", longError)

        val latest = runStore.findLatest()
        assertNotNull(latest)
        assertEquals("FAILED", latest?.status)
        assertEquals("failed-hash", latest?.payloadHash)
        assertEquals(2000, latest?.error?.length)
    }

    @Test
    fun findLatestReturnsMostRecentRunDate() {
        runStore.markFailed(LocalDate.of(2026, 3, 29), "older", "older failure")
        runStore.markSent(LocalDate.of(2026, 3, 30), "newer")

        val latest = runStore.findLatest()

        assertNotNull(latest)
        assertEquals(LocalDate.of(2026, 3, 30), latest?.runDate)
        assertEquals("SENT", latest?.status)
    }
}
