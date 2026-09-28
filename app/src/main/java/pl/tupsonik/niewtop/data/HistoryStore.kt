package pl.tupsonik.niewtop.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object HistoryStore {
    private const val PREFS = "niewtop_history"
    private val contextRef = arrayOfNulls<Context>(1)

    fun init(appContext: Context) {
        contextRef[0] = appContext.applicationContext
    }

    fun add(item: AnalysisHistoryItem, userId: String?) {
        val prefs = requireContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val key = storageKey(userId)
        val array = JSONArray(prefs.getString(key, "[]"))
        val output = JSONArray()
        output.put(toJson(item))
        for (i in 0 until minOf(array.length(), 49)) {
            output.put(array.getJSONObject(i))
        }
        prefs.edit().putString(key, output.toString()).apply()
    }

    fun list(userId: String?): List<AnalysisHistoryItem> {
        val prefs = requireContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val array = JSONArray(prefs.getString(storageKey(userId), "[]"))
        return buildList {
            for (i in 0 until array.length()) {
                val json = array.optJSONObject(i) ?: continue
                add(
                    AnalysisHistoryItem(
                        id = json.optString("id"),
                        url = json.optString("url"),
                        title = json.optString("title"),
                        host = json.optString("host"),
                        riskLevel = json.optString("riskLevel"),
                        riskLabel = json.optString("riskLabel"),
                        icon = json.optString("icon"),
                        createdAt = json.optLong("createdAt")
                    )
                )
            }
        }
    }

    private fun storageKey(userId: String?): String =
        "items_" + (userId?.takeIf { it.isNotBlank() } ?: "guest")

    private fun toJson(item: AnalysisHistoryItem) = JSONObject().apply {
        put("id", item.id)
        put("url", item.url)
        put("title", item.title)
        put("host", item.host)
        put("riskLevel", item.riskLevel)
        put("riskLabel", item.riskLabel)
        put("icon", item.icon)
        put("createdAt", item.createdAt)
    }

    private fun requireContext(): Context =
        contextRef[0] ?: error("HistoryStore.init() must be called from Application.onCreate().")
}
