package com.dbot.dmib.notify

import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

class SlackNotifier(
    private val webClient: WebClient,
    private val webhookUrl: String
) {
    fun send(text: String): Mono<Void> {
        val payload = mapOf("text" to text)
        return webClient.post().uri(webhookUrl)
            .bodyValue(payload)
            .retrieve()
            .bodyToMono(String::class.java)
            .then()
    }
}
