package com.dbot.dmib.datasource

import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.time.LocalDate

class FredClient(private val webClient: WebClient) {

    fun fetch10yLatestAndPrev(): Mono<Pair<BigDecimal, BigDecimal?>> {
        // 그래프 CSV 다운로드 형태(가볍게 파싱)
        val url = "https://fred.stlouisfed.org/graph/fredgraph.csv?id=DGS10"
        return webClient.get().uri(url).retrieve().bodyToMono(String::class.java)
            .map { csv ->
                // header: DATE,DGS10
                val rows = csv.trim().lines().drop(1)
                    .mapNotNull { line ->
                        val parts = line.split(",")
                        if (parts.size < 2) null else parts[0] to parts[1]
                    }
                    .filter { (_, v) -> v != "." } // 결측치
                val last = rows.last()
                val prev = rows.dropLast(1).lastOrNull()
                val lastVal = last.second.toBigDecimal()
                val prevVal = prev?.second?.toBigDecimal()
                lastVal to prevVal
            }
    }

    fun today(): LocalDate = LocalDate.now()
}
