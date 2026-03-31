package com.dbot.dmib.monitoring

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
class ExecutionMonitorSchedulerScheduleTests(
    @Value("\${app.monitoring.check-cron}") private val checkCron: String
) {

    @Test
    fun monitoringCheckScheduleRunsEveryTenMinutes() {
        assertEquals("0 */10 8-11 * * *", checkCron)

        val method = ExecutionMonitorScheduler::class.java.getDeclaredMethod("checkMorningRun")
        val scheduled = method.getAnnotation(Scheduled::class.java)

        assertEquals("\${app.monitoring.check-cron}", scheduled.cron)
    }
}
