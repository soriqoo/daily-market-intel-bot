package com.dbot.dmib.datasource

import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.math.BigDecimal

class StooqClient(private val webClient: WebClient) {

    fun fetchLatestAndPrevClose(symbol: String): Mono<Pair<BigDecimal, BigDecimal?>> {
        return webClient.get()
            .uri { b ->
                b.scheme("https")
                    .host("stooq.com")
                    .path("/q/d/l/")
                    .queryParam("s", symbol)     // ✅ 인코딩 안전
                    .queryParam("i", "d")
                    .build()
            }
            .header(HttpHeaders.ACCEPT, "text/csv,*/*")
            .header(HttpHeaders.USER_AGENT, "dmib-bot/1.0")
            .retrieve()
            .bodyToMono(String::class.java)
            .map { body ->
                val csv = body.trim()

                // ✅ 기대한 CSV가 아니면(HTML/오류/빈값) 원인을 로그로 남길 수 있게 예외 메시지에 스니펫 포함
                if (!csv.startsWith("Date,") && !csv.startsWith("DATE,")) {
                    val snippet = csv.take(200).replace("\n", "\\n")
                    error("Stooq non-CSV response for symbol=$symbol, snippet=$snippet")
                }

                val lines = csv.lines().filter { it.isNotBlank() }
                if (lines.size < 3) {
                    val snippet = csv.take(200).replace("\n", "\\n")
                    error("Stooq CSV too short for symbol=$symbol, snippet=$snippet")
                }

                val rows = lines.drop(1)
                val last = rows.last().split(",")
                val prev = rows.dropLast(1).lastOrNull()?.split(",")

                val lastClose = last[4].toBigDecimal()
                val prevClose = prev?.get(4)?.toBigDecimal()

                lastClose to prevClose
            }
    }
}
