package com.dbot.dmib.monitoring

import java.time.OffsetDateTime

object MonitoringAlertPolicy {

    fun normalize(now: OffsetDateTime): OffsetDateTime =
        now.withSecond(0).withNano(0)

    fun shouldSend(now: OffsetDateTime, lastSentAt: OffsetDateTime?): Boolean {
        val normalizedNow = normalize(now)
        val normalizedLastSentAt = lastSentAt?.let(::normalize)

        if (normalizedLastSentAt == null) {
            return true
        }

        if (normalizedNow.minute != 0) {
            return false
        }

        val currentHour = normalizedNow.withMinute(0)
        return normalizedLastSentAt.isBefore(currentHour)
    }
}
