package pl.tupsonik.niewtop.data

import android.content.Intent
import androidx.activity.ComponentActivity
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.FlowType
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.engine.android.Android

object SupabaseAuth {
    private val configured =
        BuildConfig.SUPABASE_URL.isNotBlank() &&
        BuildConfig.SUPABASE_PUBLISHABLE_KEY.isNotBlank()

    private val client: SupabaseClient by lazy {
        createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY
        ) {
            install(Auth) {
                scheme = "niewtop"
                host = "auth"
                flowType = FlowType.PKCE
            }
            install(Postgrest)
            httpEngine = Android.create()
        }
    }

    fun isConfigured(): Boolean = configured

    fun currentUser() =
        if (configured) client.auth.currentUserOrNull() else null

    suspend fun signInWithGoogle() {
        check(configured) { "Supabase nie jest jeszcze skonfigurowane dla Nie Wtop." }
        client.auth.signInWith(Google)
    }

    suspend fun signOut() {
        if (configured) client.auth.signOut()
    }

    fun handleDeepLink(activity: ComponentActivity, intent: Intent?) {
        if (!configured || intent == null) return
        client.handleDeeplinks(intent)
    }
}
