package com.dbot.dmib.monitoring

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import java.time.LocalDate

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class MonitoringControllerIntegrationTests {

    @LocalServerPort
    var port: Int = 0

    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    private lateinit var webTestClient: WebTestClient

    @BeforeEach
    fun setUp() {
        jdbcTemplate.update("DELETE FROM monitor_alert")
        jdbcTemplate.update("DELETE FROM job_run")
        webTestClient = WebTestClient.bindToServer()
            .baseUrl("http://localhost:$port")
            .build()
    }

    @Test
    fun lastRunReturnsLatestStoredRun() {
        jdbcTemplate.update(
            """
            INSERT INTO job_run(run_date, status, payload_hash, sent_at, error)
            VALUES (?, 'SENT', ?, ?, NULL)
            """.trimIndent(),
            LocalDate.of(2026, 3, 30).toString(),
            "hash-123",
            "2026-03-30T08:00:00+09:00"
        )

        webTestClient.get()
            .uri("/internal/monitoring/last-run")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.service").isEqualTo("dmib")
            .jsonPath("$.timezone").isEqualTo("Asia/Seoul")
            .jsonPath("$.lastRunDate").isEqualTo("2026-03-30")
            .jsonPath("$.status").isEqualTo("SENT")
            .jsonPath("$.sentAt").isEqualTo("2026-03-30T08:00+09:00")
            .jsonPath("$.error").doesNotExist()
    }

    @Test
    fun lastRunReturnsNullFieldsWhenNoHistoryExists() {
        val body = webTestClient.get()
            .uri("/internal/monitoring/last-run")
            .exchange()
            .expectStatus().isOk
            .expectBody(Map::class.java)
            .returnResult()
            .responseBody ?: emptyMap<String, Any?>()

        assertEquals("dmib", body["service"])
        assertEquals("Asia/Seoul", body["timezone"])
        assertEquals(null, body["lastRunDate"])
        assertEquals(null, body["status"])
        assertEquals(null, body["sentAt"])
        assertEquals(null, body["error"])
    }
}
