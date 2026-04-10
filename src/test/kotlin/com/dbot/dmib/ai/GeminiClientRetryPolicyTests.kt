package com.dbot.dmib.ai

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.web.reactive.function.client.WebClientRequestException
import java.net.ConnectException
import java.net.URI

class GeminiClientRetryPolicyTests {

    @Test
    fun serviceUnavailableIsRetryable() {
        assertTrue(GeminiApiException(503, "service unavailable").isRetryableGeminiError())
    }

    @Test
    fun resourceExhaustedStyleStatusIsRetryable() {
        assertTrue(GeminiApiException(429, "quota exceeded").isRetryableGeminiError())
    }

    @Test
    fun badRequestIsNotRetryable() {
        assertFalse(GeminiApiException(400, "bad request").isRetryableGeminiError())
    }

    @Test
    fun requestExceptionIsRetryable() {
        val error = WebClientRequestException(
            ConnectException("connection failed"),
            HttpMethod.POST,
            URI.create("https://generativelanguage.googleapis.com"),
            HttpHeaders.EMPTY
        )

        assertTrue(error.isRetryableGeminiError())
    }

    @Test
    fun errorSnippetNormalizesNewlinesAndTruncates() {
        val snippet = "line1\nline2\r\nline3".toGeminiErrorSnippet(maxLength = 12)
        assertEquals("line1\\nline2", snippet)
    }
}
