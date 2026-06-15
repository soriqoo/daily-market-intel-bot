package com.dbot.dmib.datasource

import org.springframework.http.HttpHeaders
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientRequestException
import reactor.core.publisher.Mono
import reactor.util.retry.Retry
import java.math.BigDecimal
import java.time.Duration
import java.time.LocalDate

class FredClient(private val webClient: WebClient) {

    /**
     * FRED graph CSV에서 최신값과 직전 유효값을 가져온다.
     * 일부 장기 시계열은 전체 CSV 응답이 느려 최근 2년 범위만 조회한다.
     */
    fun fetchLatestAndPrev(seriesId: String): Mono<Pair<BigDecimal, BigDecimal?>> {
        val url = fredGraphUrl(seriesId)

        return webClient.get()
            .uri(url)
            .header(HttpHeaders.ACCEPT, "text/csv,*/*")
            .header(HttpHeaders.USER_AGENT, "dmib-bot/1.0")
            .exchangeToMono { resp ->
                resp.bodyToMono(String::class.java).map { body ->
                    val status = resp.statusCode().value()
                    if (status !in 200..299) {
                        error("FRED status=$status for $seriesId")
                    }

                    val csv = body.trim()
                    val okHeader = csv.startsWith("observation_date,") ||
                        csv.startsWith("DATE,") ||
                        csv.startsWith("Date,")

                    if (!okHeader) {
                        val snippet = csv.take(200).replace("\n", "\\n")
                        error("FRED non-CSV for $seriesId, snippet=$snippet")
                    }

                    val rows = csv.lines()
                        .drop(1)
                        .mapNotNull { line ->
                            val p = line.split(",")
                            if (p.size < 2) null else p[0] to p[1]
                        }
                        .filter { (_, v) -> v.isNotBlank() && v != "." }

                    if (rows.size < 2) {
                        val snippet = csv.take(200).replace("\n", "\\n")
                        error("FRED CSV too short for $seriesId, snippet=$snippet")
                    }

                    val last = rows.last().second.toBigDecimal()
                    val prev: BigDecimal? = rows[rows.size - 2].second.toBigDecimal()

                    Pair(last, prev)
                }
            }
            .retryWhen(
                Retry.backoff(3, Duration.ofMillis(400))
                    .maxBackoff(Duration.ofSeconds(3))
                    .filter { it is WebClientRequestException }
            )
    }

    internal fun fredGraphUrl(seriesId: String, today: LocalDate = LocalDate.now()): String {
        val startDate = today.minusYears(2)
        return "https://fred.stlouisfed.org/graph/fredgraph.csv?id=$seriesId&cosd=$startDate"
    }
}
