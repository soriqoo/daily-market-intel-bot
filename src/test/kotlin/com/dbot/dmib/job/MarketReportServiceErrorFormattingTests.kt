package com.dbot.dmib.job

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MarketReportServiceErrorFormattingTests {

    @Test
    fun `maps retry exhaustion to friendly timeout message`() {
        val message = formatFetchError("Nasdaq", IllegalStateException("Retries exhausted: 3/3"))

        assertEquals("Nasdaq fetch failed: 응답 지연으로 재시도 3회 후 실패", message)
    }

    @Test
    fun `maps read timeout to friendly timeout message`() {
        val message = formatFetchError("US10Y", IllegalStateException("ReadTimeoutException"))

        assertEquals("US10Y fetch failed: 응답 지연(timeout)으로 실패", message)
    }
}
