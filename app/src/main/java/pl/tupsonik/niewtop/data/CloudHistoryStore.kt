package pl.tupsonik.niewtop.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order

object CloudHistoryStore {
    @Serializable
    private data class AnalysisRow(
        val id: String,
        @SerialName("user_id") val userId: String,
        @SerialName("input_type") val inputType: String,
        @SerialName("normalized_url") val normalizedUrl: String? = null,
        val platform: String? = null,
        @SerialName("risk_level") val riskLevel: String,
        val confidence: String,
        val summary: String,
        @SerialName("created_at") val createdAt: String
    )

    suspend fun save(analysis: OfferAnalysis): Result<Unit> = runCatching {
        val user = SupabaseAuth.currentUser() ?: error("Brak zalogowanego użytkownika.")

        val row = AnalysisRow(
            id = java.util.UUID.randomUUID().toString(),
            userId = user.id,
            inputType = "url",
            normalizedUrl = analysis.url,
            platform = analysis.platform,
            riskLevel = analysis.riskLevel,
            confidence = analysis.confidence,
            summary = analysis.summary,
            createdAt = ""
        )

        SupabaseAuth.client().from("analyses").insert(row)
    }

    suspend fun load(limit: Int = 50): Result<List<AnalysisHistoryItem>> = runCatching {
        SupabaseAuth.currentUser() ?: return@runCatching emptyList()

        SupabaseAuth.client()
            .from("analyses")
            .select {
                order("created_at", Order.DESCENDING)
                limit(limit)
            }
            .decodeList<AnalysisRow>()
            .map {
                val host = it.normalizedUrl?.let { url ->
                    runCatching { java.net.URI(url).host.orEmpty() }.getOrDefault("")
                }.orEmpty()

                AnalysisHistoryItem(
                    id = it.id,
                    url = it.normalizedUrl.orEmpty(),
                    title = host.ifBlank { it.platform ?: "Sprawdzona oferta" },
                    host = host,
                    riskLevel = it.riskLevel,
                    riskLabel = when (it.riskLevel) {
                        "HIGH" -> "Wysokie ryzyko"
                        "CAUTION" -> "Uwaga"
                        "LOW" -> "Niskie ryzyko"
                        else -> "Nieznane"
                    },
                    icon = when (it.riskLevel) {
                        "HIGH" -> "⚠️"
                        "CAUTION" -> "🟡"
                        "LOW" -> "🟢"
                        else -> "❔"
                    },
                    createdAt = runCatching {
                        java.time.Instant.parse(it.createdAt).toEpochMilli()
                    }.getOrDefault(System.currentTimeMillis())
                )
            }
    }
}
