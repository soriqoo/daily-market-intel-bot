package com.dbot.dmib.monitoring

import com.dbot.dmib.store.RunStore
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class MonitoringController(
    private val runStore: RunStore
) {
    /**
     * 운영/디버깅용: 마지막 실행 기록 조회
     * - 운영에서는 외부 노출 금지(127.0.0.1 바인딩 + SSH 터널로만 접근 권장)
     * - 또는 prod 프로파일에서 비활성화(원하면 다음 단계에서 적용)
     */
    @GetMapping("/internal/monitoring/last-run")
    fun lastRun(): Map<String, Any?> {
        val last = runStore.findLatest()
        return mapOf(
            "lastRunDate" to last?.runDate?.toString(),
            "status" to last?.status,
            "sentAt" to last?.sentAt?.toString(),
            "error" to last?.error
        )
    }
}
