package com.dbot.dmib.job

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@ConditionalOnProperty(prefix = "app.internal-test", name = ["enabled"], havingValue = "true")
class TestRunController(
    private val job: DailyMarketJob
) {
    @PostMapping("/internal/test/run")
    fun runOnce(): String {
        job.run()
        return "OK"
    }
}
