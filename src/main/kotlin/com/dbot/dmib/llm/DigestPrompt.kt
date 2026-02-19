package com.dbot.dmib.llm

import com.dbot.dmib.domain.MarketSnapshot

object DigestPrompt {
    fun system(): String = """
        You are a market briefing assistant.
        Output MUST be valid JSON object with fields:
        title (string),
        summaryBullets (array of strings),
        keyNumbers (array of strings),
        risks (array of strings),
        actionItems (array of strings).
        Keep it concise and decision-oriented.
    """.trimIndent()

    fun user(snapshot: MarketSnapshot): String {
        val lines = snapshot.metrics.joinToString("\n") { m ->
            val ch = m.change?.toPlainString() ?: "N/A"
            val chPct = m.changePct?.toPlainString()?.let { "$it%" } ?: "N/A"
            "${m.name}: ${m.value} (prev=${m.prevValue ?: "N/A"}, change=$ch, changePct=$chPct, asOf=${m.asOf})"
        }
        return """
            Date: ${snapshot.runDate}
            Metrics:
            $lines
            
            Write a brief daily market intelligence digest.
        """.trimIndent()
    }
}
