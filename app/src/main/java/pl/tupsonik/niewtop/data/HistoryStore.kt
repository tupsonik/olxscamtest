package pl.tupsonik.niewtop.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object HistoryStore {
    private const val PREFS = "niewtop_history"
    private const val KEY = "items"
    private var context: Context? = null

    fun init(appContext: Context) {
        context = appContext.applicationContext
    }

    fun add(item: AnalysisHistoryItem) {
        val prefs = requireContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val array = JSONArray(prefs.getString(KEY, "[]"))
        val output = JSONArray()
        output.put(toJson(item))
        for (i in 0 until minOf(array.length(), 49)) {
            output.put(array.getJSONObject(i))
        }
        prefs.edit().putString(KEY, output.toString()).apply()
    }

    fun list(): List<AnalysisHistoryItem> {
        val prefs = requireContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val array = JSONArray(prefs.getString(KEY, "[]"))
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
        context ?: error("HistoryStore.init() must be called from Application.onCreate().")
}
