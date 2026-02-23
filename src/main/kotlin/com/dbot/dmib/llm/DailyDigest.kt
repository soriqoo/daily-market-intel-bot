package com.dbot.dmib.llm

data class DailyDigest(
    val title: String,
    val summaryBullets: List<String>,
    val keyNumbers: List<String>,
    val risks: List<String>,
    val actionItems: List<String>
)
