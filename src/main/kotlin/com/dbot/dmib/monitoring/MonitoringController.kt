package com.dbot.dmib.monitoring

import com.dbot.dmib.store.RunStore
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class MonitoringController(
    private val runStore: RunStore,
    @Value("\${spring.application.name:dmib}") private val serviceName: String,
    @Value("\${app.timezone}") private val appTimezone: String,
    @Value("\${spring.profiles.active:default}") private val activeProfile: String
) {
    @GetMapping("/internal/monitoring/last-run")
    fun lastRun(): Map<String, Any?> {
        val last = runStore.findLatest()
        return mapOf(
            "service" to serviceName,
            "environment" to activeProfile,
            "timezone" to appTimezone,
            "lastRunDate" to last?.runDate?.toString(),
            "status" to last?.status,
            "sentAt" to last?.sentAt?.toString(),
            "error" to last?.error
        )
    }
}
