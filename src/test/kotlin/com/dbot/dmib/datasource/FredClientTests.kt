package com.dbot.dmib.datasource

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient
import java.time.LocalDate

class FredClientTests {

    @Test
    fun `builds recent-range FRED graph CSV URL`() {
        val client = FredClient(WebClient.builder().build())

        val url = client.fredGraphUrl("NASDAQCOM", LocalDate.of(2026, 6, 15))

        assertEquals(
            "https://fred.stlouisfed.org/graph/fredgraph.csv?id=NASDAQCOM&cosd=2024-06-15",
            url
        )
    }
}
