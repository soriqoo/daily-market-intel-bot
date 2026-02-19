package com.dbot.dmib

import com.dbot.dmib.config.AppProperties
import com.dbot.dmib.datasource.FredClient
import com.dbot.dmib.datasource.FxClient
import com.dbot.dmib.datasource.StooqClient
import com.dbot.dmib.llm.OpenAiClient
import com.dbot.dmib.notify.GmailNotifier
import com.dbot.dmib.notify.SlackNotifier
import com.dbot.dmib.store.RunStore
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.web.reactive.function.client.WebClient

@EnableScheduling
@EnableConfigurationProperties(AppProperties::class)
@SpringBootApplication
class DailyMarketIntelBotApplication {

	// === Data sources ===
	@Bean
	fun stooqClient(wc: WebClient) = StooqClient(wc)

	@Bean
	fun fxClient(wc: WebClient) = FxClient(wc)

	@Bean
	fun fredClient(wc: WebClient) = FredClient(wc)

	// === OpenAI (conditional) ===
	@Bean
	@ConditionalOnProperty(prefix = "app.openai", name = ["enabled"], havingValue = "true")
	fun openAiClient(
		wc: WebClient,
		@Value("\${app.openai.apiKey}") apiKey: String,
		@Value("\${app.openai.model}") model: String
	) = OpenAiClient(wc, apiKey, model)

	// === Slack (conditional) ===
	@Bean
	@ConditionalOnProperty(prefix = "app.slack", name = ["enabled"], havingValue = "true")
	fun slackNotifier(
		wc: WebClient,
		@Value("\${app.slack.webhookUrl}") url: String
	) = SlackNotifier(wc, url)

	// === Mail (conditional) ===
	@Bean
	@ConditionalOnProperty(prefix = "app.mail", name = ["enabled"], havingValue = "true")
	fun gmailNotifier(mailSender: JavaMailSender): GmailNotifier =
		GmailNotifier(mailSender)

	// === Store ===
	@Bean
	fun runStore(jdbc: JdbcTemplate) = RunStore(jdbc)
}

fun main(args: Array<String>) {
	runApplication<DailyMarketIntelBotApplication>(*args)
}
