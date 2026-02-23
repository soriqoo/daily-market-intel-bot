package com.dbot.dmib.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app")
data class AppProperties(
    val timezone: String = "Asia/Seoul",
    val schedule: Schedule = Schedule(),
    val openai: OpenAi = OpenAi(),
    val slack: Slack = Slack(),
    val mail: Mail = Mail(),
) {
    data class Schedule(
        val cron: String = "0 0 8 * * *"
    )
    data class OpenAi(
        val enabled: Boolean = false,
        val apiKey: String = "",
        val model: String = "gpt-4.1-mini"
    )
    data class Slack(
        val enabled: Boolean = false,
        val webhookUrl: String = ""
    )
    data class Mail(
        val enabled: Boolean = false,
        val to: String = ""
    )
}
