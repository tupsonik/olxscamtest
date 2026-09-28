package pl.tupsonik.niewtop.data

data class OfferReason(
    val title: String,
    val observation: String,
    val source: String
)

data class OfferAnalysis(
    val url: String,
    val host: String,
    val platform: String,
    val riskLevel: String,
    val confidence: String,
    val label: String,
    val icon: String,
    val summary: String,
    val reasons: List<OfferReason>,
    val nextSteps: List<String>
)

data class AnalysisHistoryItem(
    val id: String,
    val url: String,
    val title: String,
    val host: String,
    val riskLevel: String,
    val riskLabel: String,
    val icon: String,
    val createdAt: Long
) {
    companion object {
        fun from(analysis: OfferAnalysis): AnalysisHistoryItem =
            AnalysisHistoryItem(
                id = java.util.UUID.randomUUID().toString(),
                url = analysis.url,
                title = analysis.host.ifBlank { "Sprawdzona oferta" },
                host = analysis.host,
                riskLevel = analysis.riskLevel,
                riskLabel = analysis.label,
                icon = analysis.icon,
                createdAt = System.currentTimeMillis()
            )
    }
}
