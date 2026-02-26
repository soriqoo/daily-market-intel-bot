package com.dbot.dmib.ai

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Component
class GeminiClient(
    private val webClient: WebClient
) {
    private val om = jacksonObjectMapper()

    fun generateText(apiKey: String, model: String, prompt: String): Mono<String> {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

        val body = mapOf(
            "contents" to listOf(
                mapOf(
                    "role" to "user",
                    "parts" to listOf(mapOf("text" to prompt))
                )
            ),
            "generationConfig" to mapOf(
                "temperature" to 0.2
            )
        )

        return webClient.post()
            .uri(url)
            .header(HttpHeaders.CONTENT_TYPE, "application/json")
            .header(HttpHeaders.USER_AGENT, "dmib-bot/1.0")
            .bodyValue(body)
            .retrieve()
            .bodyToMono(JsonNode::class.java)
            .map { root ->
                root["candidates"]?.get(0)?.get("content")?.get("parts")?.get(0)?.get("text")?.asText()
                    ?: error("Gemini response missing text: ${root.toString().take(300)}")
            }
    }

    fun generateJson(apiKey: String, model: String, prompt: String): Mono<JsonNode> {
        return generateText(apiKey, model, prompt).map { text ->
            try {
                om.readTree(text)
            } catch (_: Exception) {
                om.createObjectNode().put("rawText", text)
            }
        }
    }
}
