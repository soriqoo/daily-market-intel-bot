package com.dbot.dmib.job

import com.dbot.dmib.ai.GeminiClient
import com.dbot.dmib.datasource.FredClient
import com.dbot.dmib.datasource.FxClient
import com.dbot.dmib.domain.Metric
import com.dbot.dmib.domain.MetricType
import com.fasterxml.jackson.databind.JsonNode
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

@Service
class MarketReportService(
    private val fx: FxClient,
    private val fred: FredClient,
    private val gemini: GeminiClient,
    @Value("\${app.ai.enabled:false}") private val aiEnabled: Boolean,
    @Value("\${app.ai.geminiApiKey:}") private val geminiApiKey: String,
    @Value("\${app.ai.geminiModel:gemini-1.5-flash}") private val geminiModel: String
) {
    private val log = LoggerFactory.getLogger(javaClass)

    data class MetricResult(
        val metric: Metric?,
        val error: String?
    )

    data class ReportResult(
        val runDate: LocalDate,
        val metrics: List<Metric>,
        val errors: List<String>,
        val slackText: String
    )

    fun buildReport(runDate: LocalDate): Mono<ReportResult> {
        val sp500 = fred.fetchLatestAndPrev("SP500")
            .map { (latest, prev) ->
                Metric("S&P 500", MetricType.INDEX, runDate, latest, prev)
            }
            .map { MetricResult(it, null) }
            .onErrorResume { e ->
                Mono.just(MetricResult(null, "S&P500 fetch failed: ${e.message ?: e.javaClass.simpleName}"))
            }

        val nasdaq = fred.fetchLatestAndPrev("NASDAQCOM")
            .map { (latest, prev) ->
                Metric("Nasdaq", MetricType.INDEX, runDate, latest, prev)
            }
            .map { MetricResult(it, null) }
            .onErrorResume { e ->
                Mono.just(MetricResult(null, "Nasdaq fetch failed: ${e.message ?: e.javaClass.simpleName}"))
            }

        val usdkrw = fx.fetchUsdKrw()
            .map { latest ->
                Metric("USDKRW", MetricType.FX, runDate, latest, null)
            }
            .map { MetricResult(it, null) }
            .onErrorResume { e ->
                Mono.just(MetricResult(null, "USDKRW fetch failed: ${e.message ?: e.javaClass.simpleName}"))
            }

        val y10 = fred.fetchLatestAndPrev("DGS10")
            .map { (latest, prev) ->
                Metric("US 10Y", MetricType.YIELD, runDate, latest, prev)
            }
            .map { MetricResult(it, null) }
            .onErrorResume { e ->
                Mono.just(MetricResult(null, "US10Y fetch failed: ${e.message ?: e.javaClass.simpleName}"))
            }

        return Mono.zip(sp500, nasdaq, usdkrw, y10)
            .flatMap { tuple ->
                val results = listOf(tuple.t1, tuple.t2, tuple.t3, tuple.t4)
                val metrics = results.mapNotNull { it.metric }
                val errors = results.mapNotNull { it.error }

                val baseText = formatSlack(runDate, metrics, errors)

                if (!aiEnabled || geminiApiKey.isBlank() || !shouldCallAi(metrics)) {
                    return@flatMap Mono.just(ReportResult(runDate, metrics, errors, baseText))
                }

                val prompt = buildGeminiPrompt(runDate, metrics)

                gemini.generateJson(geminiApiKey, geminiModel, prompt)
                    .map { aiJson ->
                        val aiSection = formatAiSection(aiJson)
                        ReportResult(runDate, metrics, errors, baseText + "\n\n" + aiSection)
                    }
                    .onErrorResume { e ->
                        Mono.just(ReportResult(runDate, metrics, errors, baseText + "\n\n*AI Analysis*\n• (AI 호출 실패) ${e.message ?: e.javaClass.simpleName}"))
                    }
            }
    }

    /**
     * Slack 메시지를 "한국 개인투자자 관점"으로 읽기 쉽게 만든다.
     * - 단위 명시
     * - 변동률은 %로 강조
     * - 룰 기반 해석/액션 제시 (LLM 없이)
     */
    private fun formatSlack(runDate: LocalDate, metrics: List<Metric>, errors: List<String>): String {
        val sp = metrics.find { it.name == "S&P 500" }
        val nq = metrics.find { it.name == "Nasdaq" }
        val fx = metrics.find { it.name == "USDKRW" }
        val y10 = metrics.find { it.name.contains("US 10Y") }

        fun pct(m: Metric?): String =
            m?.changePct?.setScale(2, RoundingMode.HALF_UP)?.toPlainString()?.let { "$it%" } ?: "-"

        fun delta(m: Metric?): String =
            m?.change?.setScale(2, RoundingMode.HALF_UP)?.toPlainString() ?: "-"

        fun num0(v: BigDecimal?): String =
            v?.setScale(0, RoundingMode.HALF_UP)?.toPlainString() ?: "-"

        fun num2(v: BigDecimal?): String =
            v?.setScale(2, RoundingMode.HALF_UP)?.toPlainString() ?: "-"

        fun arrow(p: BigDecimal): String =
            when {
                p > BigDecimal.ZERO -> "▲"
                p < BigDecimal.ZERO -> "▼"
                else -> "—"
            }

        val headline = buildList {
            sp?.changePct?.let { add("S&P ${arrow(it)} ${pct(sp)}") }
            nq?.changePct?.let { add("Nasdaq ${arrow(it)} ${pct(nq)}") }
            fx?.value?.let { add("환율 ${num0(it)}원") }
            y10?.value?.let { add("10Y ${num2(it)}%") }
        }.takeIf { it.isNotEmpty() }?.joinToString(" | ") ?: "데이터 수집 중"

        val metricLines = buildList {
            sp?.let { add("• S&P 500: ${num2(it.value)} pt (Δ ${delta(it)} / ${pct(it)})") } ?: add("• S&P 500: (N/A)")
            nq?.let { add("• Nasdaq: ${num2(it.value)} pt (Δ ${delta(it)} / ${pct(it)})") } ?: add("• Nasdaq: (N/A)")
            fx?.let { add("• USDKRW: ${num0(it.value)} 원/달러") } ?: add("• USDKRW: (N/A)")
            y10?.let { add("• US 10Y: ${num2(it.value)} %") } ?: add("• US 10Y: (N/A)")
        }

        val interpretationLines = buildInterpretations(sp, nq, fx, y10)
            .ifEmpty { listOf("특별 신호 없음") }
            .map { "• $it" }

        val actionLines = buildActions(sp, nq, fx, y10)
            .ifEmpty { listOf("체크할 항목 없음") }
            .map { "• $it" }

        val errorLines = if (errors.isEmpty()) {
            listOf("• none")
        } else {
            errors.map { "• $it" }
        }

        val lines = mutableListOf<String>()
        lines += "📊 *DMIB Morning Brief* ($runDate)"
        lines += ""
        lines += "*Headline*  $headline"
        lines += ""
        lines += "*Metrics*"
        lines += metricLines
        lines += ""
        lines += "*Interpretation*"
        lines += interpretationLines
        lines += ""
        lines += "*Action Items*"
        lines += actionLines
        lines += ""
        lines += "*Fetch Errors*"
        lines += errorLines

        return lines.joinToString("\n")
    }

    private fun arrow(pct: BigDecimal): String =
        when {
            pct > BigDecimal.ZERO -> "▲"
            pct < BigDecimal.ZERO -> "▼"
            else -> "—"
        }

    /**
     * 룰 기반 해석:
     * - 변동폭(지수) / 금리 구간 / 환율 구간
     */
    private fun buildInterpretations(
        sp: Metric?,
        nq: Metric?,
        fx: Metric?,
        y10: Metric?
    ): List<String> {
        val out = mutableListOf<String>()

        // 변동폭 기반(지수)
        sp?.changePct?.let { p ->
            if (p <= BigDecimal("-1.00")) out.add("미국 시장 조정(하락) 강도 ↑: 단기 변동성 확대 가능")
            if (p >= BigDecimal("1.00")) out.add("미국 시장 강세(상승) 신호: 리스크 온 분위기")
        }
        nq?.changePct?.let { p ->
            if (p <= BigDecimal("-1.00")) out.add("기술주 중심 약세: 성장주/고밸류 변동성 주의")
            if (p >= BigDecimal("1.00")) out.add("기술주 강세: 성장주 심리 개선 가능")
        }

        // 금리 구간(대략적인 직관 룰)
        y10?.value?.let { v ->
            if (v >= BigDecimal("4.00")) out.add("10Y 금리 4%대: 성장주에 부담, 채권 매력 상대적 ↑")
            if (v <= BigDecimal("3.50")) out.add("10Y 금리 낮은 구간: 성장주에 우호적일 수 있음")
        }

        // 환율 구간(한국 개인투자자 관점)
        fx?.value?.let { v ->
            if (v >= BigDecimal("1400")) out.add("환율 1400원 이상: 달러 강세/원화 약세 구간(환차 영향 큼)")
            if (v <= BigDecimal("1300")) out.add("환율 1300원대 이하: 환차 부담 완화 구간")
        }

        return out.distinct().take(4)
    }

    private fun buildActions(
        sp: Metric?,
        nq: Metric?,
        fx: Metric?,
        y10: Metric?
    ): List<String> {
        val out = mutableListOf<String>()

        // 변동폭이 크면 "리밸런싱/포지션 점검" 유도
        val bigMove = listOfNotNull(sp?.changePct?.abs(), nq?.changePct?.abs()).any { it >= BigDecimal("1.00") }
        if (bigMove) out.add("보유 종목/ETF 변동폭 큰 날: 포지션/리스크(손절·추매 기준) 점검")

        // 환율 높으면 환전/분할매수 체크
        fx?.value?.let { v ->
            if (v >= BigDecimal("1400")) out.add("환전/추가매수는 분할 접근 고려(환율 고점 리스크)")
        }

        // 금리 높으면 성장주 비중/듀레이션 점검
        y10?.value?.let { v ->
            if (v >= BigDecimal("4.00")) out.add("금리 부담 구간: 고밸류 성장주 비중/현금비중 점검")
        }

        // 기본 행동(데이터 기반 루틴)
        out.add("오늘 일정(경제지표/실적발표) 확인 후 과도한 매매 자제")

        return out.distinct().take(4)
    }

    // Gemini AI 분석 호출을 진행할지 여부
    private fun shouldCallAi(metrics: List<Metric>): Boolean {
        val sp = metrics.find { it.name == "S&P 500" }
        val nq = metrics.find { it.name == "Nasdaq" }
        val fx = metrics.find { it.name == "USDKRW" }
        val y10 = metrics.find { it.name.contains("US 10Y") }

        val bigMove = listOfNotNull(sp?.changePct?.abs(), nq?.changePct?.abs())
            .any { it >= BigDecimal("1.20") }
        val highFx = fx?.value?.let { it >= BigDecimal("1450") } ?: false
        val highY10 = y10?.value?.let { it >= BigDecimal("4.30") } ?: false

        return bigMove || highFx || highY10
    }

    private fun buildGeminiPrompt(runDate: LocalDate, metrics: List<Metric>): String {
        val lines = metrics.joinToString("\n") { m ->
            val pct = m.changePct?.setScale(2, RoundingMode.HALF_UP)?.toPlainString()?.let { "$it%" } ?: "N/A"
            val ch = m.change?.setScale(2, RoundingMode.HALF_UP)?.toPlainString() ?: "N/A"
            "${m.name}: value=${m.value} change=$ch changePct=$pct"
        }

        return """
        너는 한국 개인투자자를 위한 시장 브리핑 어시스턴트다.
        반드시 한국어로만 답하라.
        
        아래 형식의 JSON만 출력하라. 다른 텍스트는 절대 출력하지 말 것.
        
        {
          "signal": "긍정 | 중립 | 부정 중 하나",
          "summary": ["3줄 이내의 핵심 요약"],
          "risks": ["3줄 이내의 리스크 요약"],
          "actionItems": ["3줄 이내의 실천 가능한 행동 제안"]
        }

        시장 데이터:
        Date: $runDate
        $lines

        규칙:
        - 숫자 해석을 쉽게 설명할 것
        - 과장 금지
        - 투자 확정적 표현 금지
        - 초보자도 이해 가능하게 작성
        """.trimIndent()
    }

    private fun formatAiSection(aiJson: JsonNode): String {

        // Gemini가 JSON 파싱 실패하면 rawText로 들어오는 fallback
        val raw = aiJson["rawText"]?.asText()
        if (!raw.isNullOrBlank()) {
            val lines = mutableListOf<String>()
            lines += "*AI Analysis*"
            lines += "• $raw"
            return lines.joinToString("\n")
        }

        val signal = aiJson["signal"]?.asText() ?: "중립"
        val summary = aiJson["summary"]?.mapNotNull { it.asText() }?.take(3).orEmpty()
        val risks = aiJson["risks"]?.mapNotNull { it.asText() }?.take(3).orEmpty()
        val actions = aiJson["actionItems"]?.mapNotNull { it.asText() }?.take(3).orEmpty()

        // 비어 있으면 최소 문구
        fun bullets(items: List<String>, emptyText: String): List<String> =
            if (items.isEmpty()) listOf("• $emptyText") else items.map { "• $it" }

        val lines = mutableListOf<String>()
        lines += "*AI Analysis*"
        lines += ""
        lines += "🧭 오늘 시장 신호: *$signal*"
        lines += ""
        lines += "📌 한 줄 요약"
        lines += bullets(summary, "요약 없음")
        lines += ""
        lines += "⚠️ 리스크 포인트"
        lines += bullets(risks, "리스크 없음")
        lines += ""
        lines += "🎯 오늘의 행동 제안"
        lines += bullets(actions, "행동 제안 없음")

        return lines.joinToString("\n")
    }

    private fun BigDecimal.abs(): BigDecimal = this.abs()
}
