package pl.tupsonik.niewtop.data

import android.net.Uri
import java.util.Locale

object OfferAnalyzer {
    private val shorteners = setOf(
        "bit.ly", "tinyurl.com", "t.co", "ow.ly", "is.gd", "cutt.ly", "rb.gy"
    )

    fun analyze(rawUrl: String): OfferAnalysis {
        val input = rawUrl.trim()
        val uri = runCatching { Uri.parse(input) }.getOrNull()
        val host = uri?.host?.lowercase(Locale.US).orEmpty()

        if (uri == null || (uri.scheme != "http" && uri.scheme != "https") || host.isBlank()) {
            return OfferAnalysis(
                url = input,
                host = host,
                platform = "Nieznana",
                riskLevel = "UNKNOWN",
                confidence = "LOW",
                label = "NIEZNANY",
                icon = "❔",
                summary = "Nie udało się wiarygodnie odczytać adresu. Sprawdź link przed wysłaniem pieniędzy.",
                reasons = listOf(
                    OfferReason("Nieprawidłowy adres", "Link nie ma poprawnego schematu http/https albo hosta.", "Analiza lokalna")
                ),
                nextSteps = listOf("Otwórz link w przeglądarce i skopiuj pełny adres.", "Nie loguj się na stronie, której adresu nie rozpoznajesz.")
            )
        }

        val reasons = mutableListOf<OfferReason>()
        var hardSignals = 0
        var cautionSignals = 0

        if (uri.scheme != "https") {
            hardSignals++
            reasons += OfferReason("Brak HTTPS", "Adres używa zwykłego HTTP zamiast szyfrowanego HTTPS.", "Analiza adresu URL")
        }
        if (isIpHost(host)) {
            hardSignals++
            reasons += OfferReason("Adres jest bezpośrednim IP", "Link prowadzi do adresu IP zamiast zwykłej nazwy domeny.", "Analiza adresu URL")
        }
        if (host.contains("xn--")) {
            cautionSignals++
            reasons += OfferReason("Domena zawiera Punycode", "Warto dokładnie sprawdzić nazwę domeny zapisaną z prefiksem xn--.", "Analiza adresu URL")
        }
        if (uri.userInfo != null) {
            hardSignals++
            reasons += OfferReason("Adres zawiera fragment przed hostem", "Fragment przed znakiem @ może sprawić, że właściwy host zostanie przeoczony.", "Analiza adresu URL")
        }
        if (shorteners.contains(host)) {
            cautionSignals++
            reasons += OfferReason("Skrócony link", "Link używa serwisu skracającego i ukrywa docelowy adres.", "Analiza adresu URL")
        }

        val platform = when {
            host == "olx.pl" || host.endsWith(".olx.pl") -> "OLX"
            host == "vinted.pl" || host.endsWith(".vinted.pl") -> "Vinted"
            host == "allegro.pl" || host.endsWith(".allegro.pl") -> "Allegro"
            else -> "Sklep / inna strona"
        }

        val suspiciousPaymentWords = listOf("blik", "payment", "pay", "checkout", "płatność", "platnosc")
        if (suspiciousPaymentWords.any { input.lowercase(Locale.US).contains(it) } && platform != "Allegro") {
            cautionSignals++
            reasons += OfferReason(
                "Adres zawiera ścieżkę płatniczą",
                "W linku znaleziono słowa związane z płatnością. Samo w sobie nie oznacza oszustwa, ale wymaga kontroli domeny.",
                "Analiza adresu URL"
            )
        }

        if (reasons.isEmpty()) {
            reasons += OfferReason(
                "Brak oczywistych sygnałów w adresie",
                "Adres jest poprawny, korzysta z HTTPS i nie wykazuje podstawowych flag URL.",
                "Analiza lokalna"
            )
        }

        val risk = when {
            hardSignals >= 2 -> "HIGH"
            hardSignals == 1 || cautionSignals >= 2 -> "CAUTION"
            else -> "LOW"
        }
        val label = when (risk) {
            "HIGH" -> "WYSOKIE RYZYKO"
            "CAUTION" -> "UWAŻAJ"
            else -> "BRAK OCZYWISTEJ FLAGI"
        }
        val icon = when (risk) {
            "HIGH" -> "🛑"
            "CAUTION" -> "⚠️"
            else -> "✅"
        }
        val summary = when (risk) {
            "HIGH" -> "Adres ma kilka mocnych sygnałów ostrzegawczych. Nie traktuj tego jako dowodu oszustwa, ale zatrzymaj płatność i zweryfikuj ofertę."
            "CAUTION" -> "Adres ma sygnały, które warto ręcznie zweryfikować przed płatnością."
            else -> "Sam adres nie pokazuje oczywistych czerwonych flag. To nie potwierdza uczciwości oferty."
        }

        return OfferAnalysis(
            url = input,
            host = host,
            platform = platform,
            riskLevel = risk,
            confidence = "HIGH",
            label = label,
            icon = icon,
            summary = summary,
            reasons = reasons,
            nextSteps = listOf(
                "Porównaj domenę znak po znaku z oficjalną domeną platformy.",
                "Nie płać poza systemem ochrony kupującego platformy, jeśli taki system jest dostępny.",
                "Sprawdź dane sprzedawcy i historię konta przed wysłaniem pieniędzy."
            )
        )
    }

    private fun isIpHost(host: String): Boolean =
        host.matches(Regex("""\\d{1,3}(\\.\\d{1,3}){3}"""))
}
