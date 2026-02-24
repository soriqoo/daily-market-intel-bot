package com.dbot.dmib.notify

import com.dbot.dmib.config.AppProperties
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
class TestSlackController(
    private val props: AppProperties,
    private val slack: SlackNotifier?
) {
    @PostMapping("/internal/test/slack")
    fun testSlack(): Mono<String> {
        if (!props.slack.enabled || slack == null) return Mono.just("SLACK disabled")
        return slack.send("DMIB app slack test ✅")
            .thenReturn("SENT")
    }
}
