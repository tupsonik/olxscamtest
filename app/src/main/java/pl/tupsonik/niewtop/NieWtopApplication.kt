package pl.tupsonik.niewtop

import android.app.Application
import pl.tupsonik.niewtop.data.HistoryStore

class NieWtopApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        HistoryStore.init(this)
    }
}
