package pl.tupsonik.niewtop

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import pl.tupsonik.niewtop.data.AnalysisHistoryItem
import pl.tupsonik.niewtop.data.CloudHistoryStore
import pl.tupsonik.niewtop.data.HistoryStore
import pl.tupsonik.niewtop.data.OfferAnalysis
import pl.tupsonik.niewtop.data.OfferAnalyzer
import pl.tupsonik.niewtop.data.SupabaseAuth

class MainActivity : ComponentActivity() {
    private val sharedOffer = mutableStateOf("")
    private val authTick = mutableStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SupabaseAuth.handleDeepLink(this, intent)
        sharedOffer.value = intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()

        setContent {
            NieWtopTheme {
                NieWtopApp(
                    sharedOffer = sharedOffer.value,
                    authTick = authTick.value,
                    onSharedOfferConsumed = { sharedOffer.value = "" }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        SupabaseAuth.handleDeepLink(this, intent)
        intent.getStringExtra(Intent.EXTRA_TEXT)?.let { sharedOffer.value = it }
        authTick.value++
    }

    override fun onResume() {
        super.onResume()
        authTick.value++
    }
}

private enum class AppScreen { CHECK, HISTORY, ACCOUNT, RESULT }

@Composable
private fun NieWtopTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(
            primary = Color(0xFF72E6A1),
            onPrimary = Color(0xFF07110B),
            secondary = Color(0xFF202B38),
            background = Color(0xFF0B0F14),
            surface = Color(0xFF121820),
            onBackground = Color(0xFFF4F7FA),
            onSurface = Color(0xFFF4F7FA),
            onSurfaceVariant = Color(0xFF9AA7B5),
            outline = Color(0xFF2A3542)
        ),
        content = content
    )
}

@Composable
private fun NieWtopApp(
    sharedOffer: String,
    authTick: Int,
    onSharedOfferConsumed: () -> Unit
) {
    var screen by remember { mutableStateOf(AppScreen.CHECK) }
    var url by remember { mutableStateOf("") }
    var analysis by remember { mutableStateOf<OfferAnalysis?>(null) }
    var signedIn by remember(authTick) { mutableStateOf(SupabaseAuth.currentUser() != null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(sharedOffer) {
        if (sharedOffer.isNotBlank()) {
            extractUrl(sharedOffer).takeIf { it.isNotBlank() }?.let {
                url = it
                screen = AppScreen.CHECK
            }
            onSharedOfferConsumed()
        }
    }

    LaunchedEffect(authTick) {
        signedIn = SupabaseAuth.currentUser() != null
    }

    when (screen) {
        AppScreen.CHECK, AppScreen.HISTORY, AppScreen.ACCOUNT -> {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                bottomBar = {
                    NavigationBar(
                        containerColor = Color(0xFF11171F),
                        modifier = Modifier.navigationBarsPadding()
                    ) {
                        NavigationBarItem(
                            selected = screen == AppScreen.CHECK,
                            onClick = { screen = AppScreen.CHECK },
                            icon = { Text("🛡️") },
                            label = { Text("Sprawdź") }
                        )
                        NavigationBarItem(
                            selected = screen == AppScreen.HISTORY,
                            onClick = { screen = AppScreen.HISTORY },
                            icon = { Text("🕘") },
                            label = { Text("Historia") }
                        )
                        NavigationBarItem(
                            selected = screen == AppScreen.ACCOUNT,
                            onClick = { screen = AppScreen.ACCOUNT },
                            icon = { Text("👤") },
                            label = { Text("Konto") }
                        )
                    }
                }
            ) { padding ->
                Box(Modifier.fillMaxSize().padding(padding)) {
                    when (screen) {
                        AppScreen.CHECK -> CheckScreen(
                            url = url,
                            onUrlChange = { url = it },
                            onAnalyze = {
                                val result = OfferAnalyzer.analyze(url)
                                analysis = result
                                if (result.riskLevel != "UNKNOWN") {
                                    HistoryStore.add(AnalysisHistoryItem.from(result), SupabaseAuth.currentUser()?.id)
                                    if (SupabaseAuth.currentUser() != null) scope.launch { CloudHistoryStore.save(result) }
                                    screen = AppScreen.RESULT
                                }
                            },
                            onScreenshot = { },
                            onAccount = { screen = AppScreen.ACCOUNT },
                            signedIn = signedIn
                        )
                        AppScreen.HISTORY -> HistoryScreen(
                            userSignedIn = signedIn,
                            onOpen = {
                                url = it.url
                                analysis = OfferAnalyzer.analyze(it.url)
                                screen = AppScreen.RESULT
                            }
                        )
                        AppScreen.ACCOUNT -> AccountScreen(
                            signedIn = signedIn,
                            onSignedIn = {
                                signedIn = true
                                screen = AppScreen.ACCOUNT
                            },
                            onSignedOut = {
                                signedIn = false
                                screen = AppScreen.CHECK
                            }
                        )
                        else -> Unit
                    }
                }
            }
        }
        AppScreen.RESULT -> {
            ResultScreen(
                analysis = analysis,
                onBack = { screen = AppScreen.CHECK },
                onHistory = { screen = AppScreen.HISTORY }
            )
        }
    }
}

@Composable
private fun CheckScreen(
    url: String,
    onUrlChange: (String) -> Unit,
    onAnalyze: () -> Unit,
    onScreenshot: () -> Unit,
    onAccount: () -> Unit,
    signedIn: Boolean
) {
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { selected ->
        if (selected != null) onScreenshot()
    }

    Column(
        Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Nie Wtop", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
                Text("Sprawdź, zanim zapłacisz.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box(
                Modifier
                    .size(48.dp)
                    .background(Color(0xFF16202B), RoundedCornerShape(15.dp))
                    .clickable(onClick = onAccount),
                contentAlignment = Alignment.Center
            ) {
                Text(if (signedIn) "G" else "👤", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }

        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF121820))
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Masz ofertę?", fontSize = 23.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Wklej link do OLX, Vinted, Allegro albo sklepu. Zaczynamy od lokalnych sygnałów, bez udawania pewności.",
                    color = Color(0xFFB4BEC9),
                    lineHeight = 21.sp
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = onUrlChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    placeholder = { Text("https://...") },
                    label = { Text("Link do oferty") }
                )
                Button(
                    onClick = onAnalyze,
                    enabled = url.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Sprawdź ofertę", fontWeight = FontWeight.Bold)
                }
            }
        }

        Text("Możesz też", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(
                onClick = { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                label = { Text("📷 Screenshot") }
            )
            AssistChip(
                onClick = { onUrlChange("https://") },
                label = { Text("↗ Udostępnij") }
            )
        }

        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF10161D))
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("Jak to działa?", fontWeight = FontWeight.Bold)
                Text("1  Link → rozpoznanie platformy", color = Color(0xFFB4BEC9))
                Text("2  Link → lokalne sygnały ryzyka", color = Color(0xFFB4BEC9))
                Text("3  Wynik → konkretne rzeczy do sprawdzenia", color = Color(0xFFB4BEC9))
            }
        }

        Spacer(Modifier.weight(1f))
        Text(
            "Nie Wtop nie wydaje wyroku na sprzedawcę. Pokazuje sygnały i źródła, które pomagają podjąć decyzję.",
            color = Color(0xFF778493),
            fontSize = 12.sp,
            lineHeight = 17.sp
        )
    }
}

@Composable
private fun ResultScreen(
    analysis: OfferAnalysis?,
    onBack: () -> Unit,
    onHistory: () -> Unit
) {
    if (analysis == null) {
        onBack()
        return
    }

    val tone = when (analysis.riskLevel) {
        "HIGH" -> Color(0xFFFF8A8A)
        "CAUTION" -> Color(0xFFFFC857)
        "LOW" -> Color(0xFF72E6A1)
        else -> Color(0xFF9AA7B5)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                Modifier.fillMaxWidth().padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onBack) { Text("← Wróć") }
                Spacer(Modifier.weight(1f))
                Text("Analiza", fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onHistory) { Text("Historia") }
            }
        }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF121820))
                ) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(analysis.icon, fontSize = 30.sp)
                        Text(analysis.label, color = tone, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                        Text(analysis.summary, color = Color(0xFFB4BEC9))
                        Text("Pewność: " + analysis.confidence, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        Text(analysis.platform + "  •  " + analysis.host, color = Color(0xFF8D99A7), fontSize = 12.sp)
                    }
                }
            }
            item { Text("Sygnały", fontSize = 19.sp, fontWeight = FontWeight.Bold) }
            items(analysis.reasons) { reason ->
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF121820))
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(reason.title, fontWeight = FontWeight.Bold)
                        Text(reason.observation, color = Color(0xFFB4BEC9))
                        Text(reason.source, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                    }
                }
            }
            item {
                Text("Co zrobić", fontSize = 19.sp, fontWeight = FontWeight.Bold)
                analysis.nextSteps.forEach { step ->
                    Text("• " + step, color = Color(0xFFB4BEC9), modifier = Modifier.padding(vertical = 3.dp))
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun HistoryScreen(
    userSignedIn: Boolean,
    onOpen: (AnalysisHistoryItem) -> Unit
) {
    var history by remember(userSignedIn) {
        mutableStateOf(HistoryStore.list(SupabaseAuth.currentUser()?.id))
    }
    var loading by remember(userSignedIn) { mutableStateOf(false) }

    LaunchedEffect(userSignedIn) {
        if (userSignedIn && SupabaseAuth.isConfigured()) {
            loading = true
            CloudHistoryStore.load().onSuccess { cloudItems ->
                history = cloudItems
                cloudItems.forEach { HistoryStore.add(it, SupabaseAuth.currentUser()?.id) }
            }
            loading = false
        }
    }

    Column(
        Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Historia", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
        Text(
            if (userSignedIn) "Twoje sprawdzenia z chmury Supabase." else "Historia lokalna. Zaloguj Google, żeby synchronizować ją z kontem.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (loading) {
            CircularProgressIndicator(Modifier.size(24.dp))
        } else if (history.isEmpty()) {
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF121820))
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("Jeszcze nic nie sprawdzono.", fontWeight = FontWeight.Bold)
                    Text("Po pierwszej analizie wynik pojawi się tutaj automatycznie.", color = Color(0xFFB4BEC9))
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(history) { item ->
                    HistoryCard(item, onClick = { onOpen(item) })
                }
            }
        }
    }
}
@Composable
private fun HistoryCard(item: AnalysisHistoryItem, onClick: () -> Unit) {
    val tone = when (item.riskLevel) {
        "HIGH" -> Color(0xFFFF8A8A)
        "CAUTION" -> Color(0xFFFFC857)
        else -> Color(0xFF72E6A1)
    }

    Card(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(17.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121820))
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(42.dp).background(tone.copy(alpha = .12f), RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(item.icon, fontSize = 20.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold)
                Text(item.host, color = Color(0xFF8D99A7), fontSize = 12.sp)
            }
            Text(item.riskLabel, color = tone, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AccountScreen(
    signedIn: Boolean,
    onSignedIn: () -> Unit,
    onSignedOut: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    val user = SupabaseAuth.currentUser()

    Column(
        Modifier.fillMaxSize().padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Konto", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)

        if (!signedIn || user == null) {
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF121820))
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Zaloguj się przez Google", fontSize = 21.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Konto przygotuje aplikację do przypisywania historii do użytkownika i późniejszej synchronizacji.",
                        color = Color(0xFFB4BEC9)
                    )
                    if (SupabaseAuth.isConfigured()) {
                        Button(
                            onClick = {
                                scope.launch {
                                    loading = true
                                    error = ""
                                    runCatching { SupabaseAuth.signInWithGoogle() }
                                        .onFailure { error = it.message ?: "Nie udało się rozpocząć logowania." }
                                    loading = false
                                    if (error.isBlank()) onSignedIn()
                                }
                            },
                            enabled = !loading,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            if (loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            else Text("G  Kontynuuj z Google", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Text(
                            "Google jest podpięte w kodzie. Brakuje tylko osobnego projektu Supabase i konfiguracji OAuth dla Nie Wtop.",
                            color = Color(0xFFFFC857),
                            fontSize = 13.sp
                        )
                    }
                    if (error.isNotBlank()) Text(error, color = Color(0xFFFF8A8A), fontSize = 13.sp)
                }
            }
        } else {
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF121820))
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        Modifier.size(54.dp).background(Color(0xFF72E6A1), RoundedCornerShape(17.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("G", color = Color(0xFF07110B), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Text(user.email ?: "Konto Google", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Zalogowano przez Google", color = Color(0xFF8D99A7))
                }
            }

            OutlinedButton(
                onClick = {
                    scope.launch {
                        runCatching { SupabaseAuth.signOut() }
                        onSignedOut()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Wyloguj")
            }
        }

        Text("Bezpieczeństwo", fontSize = 19.sp, fontWeight = FontWeight.Bold)
        Text(
            "Nie Wtop nie prosi o hasła do banku, CVV, kody SMS ani dane karty.",
            color = Color(0xFFB4BEC9),
            lineHeight = 20.sp
        )
    }
}

private fun extractUrl(text: String): String =
    Regex("https?://[^\\s]+")
        .find(text)
        ?.value
        ?.trimEnd('.', ',', ')', ']', '}')
        .orEmpty()
