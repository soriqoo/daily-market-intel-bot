package com.dbot.dmib.ai

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.http.HttpHeaders
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientRequestException
import reactor.core.publisher.Mono
import reactor.util.retry.Retry
import java.time.Duration

@Component
class GeminiClient(
    private val webClient: WebClient
) {
    private val om = jacksonObjectMapper()
    private val log = LoggerFactory.getLogger(javaClass)

    fun generateJson(apiKey: String, model: String, prompt: String): Mono<JsonNode> {
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
            .exchangeToMono { resp ->
                resp.bodyToMono(String::class.java)
                    .defaultIfEmpty("")
                    .flatMap { responseBody ->
                        val status = resp.statusCode().value()
                        if (status !in 200..299) {
                            return@flatMap Mono.error<JsonNode>(
                                GeminiApiException(
                                    statusCode = status,
                                    responseBody = responseBody.toGeminiErrorSnippet()
                                )
                            )
                        }

                        try {
                            Mono.just(om.readTree(responseBody))
                        } catch (e: Exception) {
                            Mono.error(IllegalStateException("Gemini response parse failed", e))
                        }
                    }
            }
            .map { root ->
                val rawText = root["candidates"]
                    ?.get(0)
                    ?.get("content")
                    ?.get("parts")
                    ?.get(0)
                    ?.get("text")
                    ?.asText()
                    ?: error("Gemini response missing text")

                // ✅ 1. "json" prefix 제거
                val cleaned = rawText
                    .removePrefix("json")
                    .removePrefix("```json")
                    .removeSuffix("```")
                    .trim()

                try {
                    om.readTree(cleaned)
                } catch (e: Exception) {
                    om.createObjectNode().put("rawText", cleaned)
                }
            }
            .retryWhen(
                Retry.backoff(3, Duration.ofMillis(500))
                    .maxBackoff(Duration.ofSeconds(5))
                    .filter { it.isRetryableGeminiError() }
                    .doBeforeRetry { signal ->
                        val failure = signal.failure()
                        when (failure) {
                            is GeminiApiException -> {
                                log.warn(
                                    "Retrying Gemini call. attempt={}, model={}, status={}, body={}",
                                    signal.totalRetries() + 1,
                                    model,
                                    failure.statusCode,
                                    failure.responseBody
                                )
                            }

                            else -> {
                                log.warn(
                                    "Retrying Gemini call. attempt={}, model={}, err={}",
                                    signal.totalRetries() + 1,
                                    model,
                                    failure.message ?: failure.javaClass.simpleName
                                )
                            }
                        }
                    }
            )
            .doOnError { e ->
                when (e) {
                    is GeminiApiException -> {
                        log.warn(
                            "Gemini call failed. model={}, status={}, body={}",
                            model,
                            e.statusCode,
                            e.responseBody
                        )
                    }

                    else -> {
                        log.warn(
                            "Gemini call failed. model={}, err={}",
                            model,
                            e.message ?: e.javaClass.simpleName
                        )
                    }
                }
            }
    }
}

data class GeminiApiException(
    val statusCode: Int,
    val responseBody: String
) : RuntimeException("Gemini status=$statusCode body=$responseBody")

internal fun Throwable.isRetryableGeminiError(): Boolean =
    this is WebClientRequestException || (this is GeminiApiException && (statusCode == 429 || statusCode in 500..599))

internal fun String.toGeminiErrorSnippet(maxLength: Int = 300): String =
    replace("\r", "")
        .replace("\n", "\\n")
        .take(maxLength)
