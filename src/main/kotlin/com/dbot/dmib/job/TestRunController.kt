package com.dbot.dmib.job

import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class TestRunController(
    private val job: DailyMarketJob
) {
    @PostMapping("/internal/test/run")
    fun runOnce(): String {
        job.run()
        return "OK"
    }
}
