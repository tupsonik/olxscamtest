package pl.tupsonik.niewtop.data

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

object CloudHistoryStore {
    @Serializable
    private data class AnalysisInsert(
        val id: String,
        @SerialName("user_id") val userId: String,
        @SerialName("input_type") val inputType: String,
        @SerialName("normalized_url") val normalizedUrl: String? = null,
        val platform: String? = null,
        @SerialName("risk_level") val riskLevel: String,
        val confidence: String,
        val summary: String
    )

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

        SupabaseAuth.client()
            .from("analyses")
            .insert(
                AnalysisInsert(
                    id = java.util.UUID.randomUUID().toString(),
                    userId = user.id,
                    inputType = "url",
                    normalizedUrl = analysis.url,
                    platform = analysis.platform,
                    riskLevel = analysis.riskLevel,
                    confidence = analysis.confidence,
                    summary = analysis.summary
                )
            )
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
            .map { row ->
                val host = row.normalizedUrl?.let { url ->
                    runCatching { java.net.URI(url).host.orEmpty() }.getOrDefault("")
                }.orEmpty()

                AnalysisHistoryItem(
                    id = row.id,
                    url = row.normalizedUrl.orEmpty(),
                    title = host.ifBlank { row.platform ?: "Sprawdzona oferta" },
                    host = host,
                    riskLevel = row.riskLevel,
                    riskLabel = when (row.riskLevel) {
                        "HIGH" -> "Wysokie ryzyko"
                        "CAUTION" -> "Uwaga"
                        "LOW" -> "Niskie ryzyko"
                        else -> "Nieznane"
                    },
                    icon = when (row.riskLevel) {
                        "HIGH" -> "⚠️"
                        "CAUTION" -> "🟡"
                        "LOW" -> "🟢"
                        else -> "❔"
                    },
                    createdAt = runCatching {
                        java.time.Instant.parse(row.createdAt).toEpochMilli()
                    }.getOrDefault(System.currentTimeMillis())
                )
            }
    }
}
