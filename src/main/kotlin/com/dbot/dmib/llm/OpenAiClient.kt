package com.dbot.dmib.llm

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

class OpenAiClient(
    private val webClient: WebClient,
    private val apiKey: String,
    private val model: String
) {
    private val om = jacksonObjectMapper()

    fun summarize(system: String, user: String): Mono<DailyDigest> {
        val body = mapOf(
            "model" to model,
            "response_format" to mapOf("type" to "json_object"),
            "messages" to listOf(
                mapOf("role" to "system", "content" to system),
                mapOf("role" to "user", "content" to user)
            ),
            "temperature" to 0.2
        )

        return webClient.post()
            .uri("https://api.openai.com/v1/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .retrieve()
            .bodyToMono(JsonNode::class.java)
            .map { root ->
                val content = root["choices"][0]["message"]["content"].asText()
                om.readValue(content, DailyDigest::class.java)
            }
    }
}
