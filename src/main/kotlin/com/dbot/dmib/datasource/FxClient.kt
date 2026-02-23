package com.dbot.dmib.datasource

import com.fasterxml.jackson.databind.JsonNode
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.time.LocalDate

class FxClient(private val webClient: WebClient) {

    fun fetchUsdKrw(): Mono<BigDecimal> {
        val url = "https://api.exchangerate.host/latest?base=USD&symbols=KRW"
        return webClient.get().uri(url).retrieve().bodyToMono(JsonNode::class.java)
            .map { node -> node["rates"]["KRW"].asText().toBigDecimal() }
    }

    fun today(): LocalDate = LocalDate.now()
}
