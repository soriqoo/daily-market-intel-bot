package com.dbot.dmib.store

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import java.time.OffsetDateTime

@SpringBootTest
@ActiveProfiles("test")
class MonitorAlertStoreIntegrationTests {

    @Autowired
    lateinit var monitorAlertStore: MonitorAlertStore

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    @BeforeEach
    fun setUp() {
        jdbcTemplate.update("DELETE FROM monitor_alert")
        jdbcTemplate.update("DELETE FROM job_run")
    }

    @Test
    fun allowsImmediateAlertThenOnlyHourlyReminders() {
        val alertKey = "DMIB_MISSED_OR_FAILED_2026-03-31"

        assertTrue(monitorAlertStore.tryAcquire(alertKey, OffsetDateTime.parse("2026-03-31T08:10:00+09:00")))
        assertFalse(monitorAlertStore.tryAcquire(alertKey, OffsetDateTime.parse("2026-03-31T08:20:00+09:00")))
        assertTrue(monitorAlertStore.tryAcquire(alertKey, OffsetDateTime.parse("2026-03-31T09:00:00+09:00")))
        assertFalse(monitorAlertStore.tryAcquire(alertKey, OffsetDateTime.parse("2026-03-31T09:10:00+09:00")))
    }

    @Test
    fun clearResetsAlertStateForRecoveredJob() {
        val alertKey = "DMIB_MISSED_OR_FAILED_2026-03-31"

        assertTrue(monitorAlertStore.tryAcquire(alertKey, OffsetDateTime.parse("2026-03-31T08:10:00+09:00")))

        monitorAlertStore.clear(alertKey)

        assertTrue(monitorAlertStore.tryAcquire(alertKey, OffsetDateTime.parse("2026-03-31T08:20:00+09:00")))
    }
}
