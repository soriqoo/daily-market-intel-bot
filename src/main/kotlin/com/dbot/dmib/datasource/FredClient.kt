package com.dbot.dmib.datasource

import org.springframework.http.HttpHeaders
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.math.BigDecimal

class FredClient(private val webClient: WebClient) {

    fun fetchLatestAndPrev(seriesId: String): Mono<Pair<BigDecimal, BigDecimal?>> {
        val url = "https://fred.stlouisfed.org/graph/fredgraph.csv?id=$seriesId"
        return webClient.get()
            .uri(url)
            .header(HttpHeaders.ACCEPT, "text/csv,*/*")
            .header(HttpHeaders.USER_AGENT, "dmib-bot/1.0")
            .exchangeToMono { resp ->
                resp.bodyToMono(String::class.java).map { body ->
                    val status = resp.statusCode().value()
                    if (status !in 200..299) error("FRED status=$status for $seriesId")

                    val csv = body.trim()
                    // FRED는 DATE, 또는 observation_date 로 시작하는 경우가 있음
                    if (!csv.startsWith("DATE,") && !csv.startsWith("observation_date,") && !csv.startsWith("Date,")) {
                        error("FRED non-CSV for $seriesId, snippet=${csv.take(200).replace("\n","\\n")}")
                    }

                    val rows = csv.lines().drop(1)
                        .mapNotNull {
                            val p = it.split(",")
                            if (p.size < 2) null else p[0] to p[1]
                        }
                        .filter { (_, v) -> v != "." && v.isNotBlank() }

                    if (rows.size < 2) error("FRED CSV too short for $seriesId, snippet=${csv.take(200)}")

                    rows.last().second.toBigDecimal() to rows[rows.size - 2].second.toBigDecimal()
                }
            }
    }
}
