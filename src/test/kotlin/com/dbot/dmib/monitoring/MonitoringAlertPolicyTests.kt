package com.dbot.dmib.monitoring

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.OffsetDateTime

class MonitoringAlertPolicyTests {

    @Test
    fun sendsImmediatelyWhenNoAlertWasSentYet() {
        val now = OffsetDateTime.parse("2026-03-31T08:10:00+09:00")

        assertTrue(MonitoringAlertPolicy.shouldSend(now, null))
    }

    @Test
    fun skipsFollowUpChecksWithinTheSameHour() {
        val lastSentAt = OffsetDateTime.parse("2026-03-31T08:10:00+09:00")
        val now = OffsetDateTime.parse("2026-03-31T08:20:00+09:00")

        assertFalse(MonitoringAlertPolicy.shouldSend(now, lastSentAt))
    }

    @Test
    fun sendsReminderOnTheHourWhenIssueIsStillOpen() {
        val lastSentAt = OffsetDateTime.parse("2026-03-31T08:10:00+09:00")
        val now = OffsetDateTime.parse("2026-03-31T09:00:00+09:00")

        assertTrue(MonitoringAlertPolicy.shouldSend(now, lastSentAt))
    }

    @Test
    fun skipsAdditionalReminderWithinTheSameHourAfterTopOfHourAlert() {
        val lastSentAt = OffsetDateTime.parse("2026-03-31T09:00:00+09:00")
        val now = OffsetDateTime.parse("2026-03-31T09:10:00+09:00")

        assertFalse(MonitoringAlertPolicy.shouldSend(now, lastSentAt))
    }
}
