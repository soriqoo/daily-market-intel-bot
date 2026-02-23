package com.dbot.dmib.datasource

import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.time.LocalDate

class StooqClient(private val webClient: WebClient) {

    // 예: https://stooq.com/q/d/l/?s=^spx&i=d
    fun fetchLatestAndPrevClose(symbol: String): Mono<Pair<BigDecimal, BigDecimal?>> {
        val url = "https://stooq.com/q/d/l/?s=$symbol&i=d"
        return webClient.get().uri(url).retrieve().bodyToMono(String::class.java)
            .map { csv ->
                // CSV header: Date,Open,High,Low,Close,Volume
                val lines = csv.trim().lines()
                if (lines.size < 2) error("Stooq CSV too short for $symbol")
                val rows = lines.drop(1).filter { it.isNotBlank() }
                val last = rows.last().split(",")
                val prev = rows.dropLast(1).lastOrNull()?.split(",")
                val lastClose = last[4].toBigDecimal()
                val prevClose = prev?.get(4)?.toBigDecimal()
                lastClose to prevClose
            }
    }

    fun today(): LocalDate = LocalDate.now()
}
