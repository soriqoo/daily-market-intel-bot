package com.dbot.dmib.internal

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = ["app.internal-test.enabled=true"]
)
@ActiveProfiles("test")
class InternalTestEndpointsEnabledTests {

    @LocalServerPort
    var port: Int = 0

    private lateinit var webTestClient: WebTestClient

    @BeforeEach
    fun setUp() {
        webTestClient = WebTestClient.bindToServer()
            .baseUrl("http://localhost:$port")
            .build()
    }

    @Test
    fun runEndpointExecutesWhenInternalTestIsEnabled() {
        webTestClient.post()
            .uri("/internal/test/run")
            .exchange()
            .expectStatus().isOk
            .expectBody(String::class.java)
            .isEqualTo("OK")
    }

    @Test
    fun slackEndpointRespondsWhenInternalTestIsEnabled() {
        webTestClient.post()
            .uri("/internal/test/slack")
            .exchange()
            .expectStatus().isOk
            .expectBody(String::class.java)
            .isEqualTo("SLACK disabled")
    }
}
