package com.dbot.dmib.datasource

import org.springframework.http.HttpHeaders
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.math.BigDecimal

class FredClient(private val webClient: WebClient) {

    fun fetch10yLatestAndPrev(): Mono<Pair<BigDecimal, BigDecimal?>> {
        return webClient.get()
            .uri("https://fred.stlouisfed.org/graph/fredgraph.csv?id=DGS10")
            .header(HttpHeaders.ACCEPT, "text/csv,*/*")
            .header(HttpHeaders.USER_AGENT, "dmib-bot/1.0")
            .retrieve()
            .bodyToMono(String::class.java)
            .map { body ->
                val csv = body.trim()

                if (!csv.startsWith("DATE,") && !csv.startsWith("Date,")) {
                    val snippet = csv.take(200).replace("\n", "\\n")
                    error("FRED non-CSV response for DGS10, snippet=$snippet")
                }

                val rows = csv.lines().drop(1)
                    .mapNotNull { line ->
                        val parts = line.split(",")
                        if (parts.size < 2) null else parts[0] to parts[1]
                    }
                    .filter { (_, v) -> v != "." && v.isNotBlank() }

                if (rows.size < 2) {
                    val snippet = csv.take(200).replace("\n", "\\n")
                    error("FRED CSV too short for DGS10, snippet=$snippet")
                }

                val last = rows.last()
                val prev = rows.dropLast(1).last()

                last.second.toBigDecimal() to prev.second.toBigDecimal()
            }
    }
}
