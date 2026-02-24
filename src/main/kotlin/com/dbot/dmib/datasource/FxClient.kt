package com.dbot.dmib.datasource

import com.fasterxml.jackson.databind.JsonNode
import org.springframework.http.HttpHeaders
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.math.BigDecimal

class FxClient(private val webClient: WebClient) {

    fun fetchUsdKrw(): Mono<BigDecimal> {
        return webClient.get()
            .uri("https://open.er-api.com/v6/latest/USD")
            .header(HttpHeaders.ACCEPT, "application/json")
            .header(HttpHeaders.USER_AGENT, "dmib-bot/1.0")
            .retrieve()
            .bodyToMono(JsonNode::class.java)
            .map { node ->
                // 이 API는 rates가 node["rates"] 아래에 있음
                val rates = node.get("rates")
                val krw = rates?.get("KRW")

                if (krw == null) {
                    val snippet = node.toString().take(300)
                    error("FX response missing rates.KRW, body=$snippet")
                }

                krw.asText().toBigDecimal()
            }
    }
}
