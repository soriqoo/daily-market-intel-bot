package com.dbot.dmib.datasource

import org.springframework.http.HttpHeaders
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.math.BigDecimal

class FredClient(private val webClient: WebClient) {

    /**
     * FRED CSV (graph endpoint)에서 특정 seriesId의 최신값과 전일값(바로 이전값)을 가져온다.
     * 예: SP500, NASDAQCOM, DGS10
     */
    fun fetchLatestAndPrev(seriesId: String): Mono<Pair<BigDecimal, BigDecimal?>> {
        val url = "https://fred.stlouisfed.org/graph/fredgraph.csv?id=$seriesId"

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

                    // FRED는 보통 observation_date 또는 DATE/Date 형태의 헤더
                    val okHeader = csv.startsWith("observation_date,") ||
                            csv.startsWith("DATE,") ||
                            csv.startsWith("Date,")

                    if (!okHeader) {
                        val snippet = csv.take(200).replace("\n", "\\n")
                        error("FRED non-CSV for $seriesId, snippet=$snippet")
                    }

                    // 결측치(.) 제거하고 최소 2개 이상 확보
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
                    val prev = rows[rows.size - 2].second.toBigDecimal()

                    last to prev
                }
            }
    }
}
