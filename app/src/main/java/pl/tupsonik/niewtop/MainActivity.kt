package pl.tupsonik.flappysteroids

import android.content.Context
import android.os.Bundle
import android.view.Window
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

private enum class Screen { MENU, PLAYING, GAME_OVER }
private enum class PowerType { COIN, SHIELD, BOOST }

private data class Pipe(
    val x: Float,
    val gapCenter: Float,
    val gapSize: Float,
    val width: Float,
    val passed: Boolean = false
)

private data class PowerUp(
    val x: Float,
    val y: Float,
    val type: PowerType,
    val radius: Float = 22f
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemBars(window)
        setContent { FlappySteroids() }
    }

    private fun hideSystemBars(window: Window) {
        window.insetsController?.let {
            it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
            it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}

@Composable
private fun FlappySteroids() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("flappy_steroids", Context.MODE_PRIVATE) }

    var screen by remember { mutableStateOf(Screen.MENU) }
    var birdY by remember { mutableFloatStateOf(0.5f) }
    var velocity by remember { mutableFloatStateOf(0f) }
    var pipes by remember { mutableStateOf(emptyList<Pipe>()) }
    var powerUps by remember { mutableStateOf(emptyList<PowerUp>()) }
    var score by remember { mutableIntStateOf(0) }
    var coins by remember { mutableIntStateOf(0) }
    var combo by remember { mutableIntStateOf(0) }
    var shield by remember { mutableStateOf(false) }
    var boostTime by remember { mutableFloatStateOf(0f) }
    var spawnTimer by remember { mutableFloatStateOf(0f) }
    var worldTime by remember { mutableFloatStateOf(0f) }
    var gameSeed by remember { mutableIntStateOf(0) }
    var best by remember { mutableIntStateOf(prefs.getInt("best", 0)) }

    fun resetGame() {
        gameSeed++
        birdY = 0.5f
        velocity = 0f
        pipes = emptyList()
        powerUps = emptyList()
        score = 0
        coins = 0
        combo = 0
        shield = false
        boostTime = 0f
        spawnTimer = 0.7f
        worldTime = 0f
        screen = Screen.PLAYING
    }

    fun flap() {
        if (screen == Screen.MENU || screen == Screen.GAME_OVER) resetGame()
        else velocity = -0.0125f
    }

    LaunchedEffect(screen, gameSeed) {
        if (screen != Screen.PLAYING) return@LaunchedEffect

        while (true) {
            delay(16)
            val dt = 1f / 60f
            worldTime += dt
            velocity += 0.00062f
            birdY += velocity

            val difficulty = min(1.9f, 1f + score * 0.012f)
            val speed = 0.0075f * difficulty * if (boostTime > 0f) 1.18f else 1f
            val gap = max(0.22f, 0.36f - score * 0.0022f)

            spawnTimer -= dt
            if (spawnTimer <= 0f) {
                val center = Random.nextFloat() * 0.48f + 0.26f
                pipes = pipes + Pipe(1.08f, center, gap, 0.105f)

                if (Random.nextFloat() < 0.62f) {
                    val type = when {
                        Random.nextFloat() < 0.58f -> PowerType.COIN
                        Random.nextBoolean() -> PowerType.SHIELD
                        else -> PowerType.BOOST
                    }
                    powerUps = powerUps + PowerUp(
                        1.28f,
                        (center + Random.nextFloat() * 0.20f - 0.10f).coerceIn(0.16f, 0.84f),
                        type
                    )
                }
                spawnTimer = max(0.88f, 1.48f - score * 0.008f)
            }

            pipes = pipes.map { it.copy(x = it.x - speed) }
            powerUps = powerUps.map { it.copy(x = it.x - speed * 1.08f) }.filter { it.x > -0.15f }

            var newScore = score
            var newCombo = combo

            pipes = pipes.map { pipe ->
                if (!pipe.passed && pipe.x + pipe.width < 0.27f) {
                    newScore += 1 + newCombo / 5
                    newCombo += 1
                    pipe.copy(passed = true)
                } else pipe
            }

            val birdX = 0.27f
            val birdRadius = 0.038f
            val birdTop = birdY - birdRadius
            val birdBottom = birdY + birdRadius
            var hit = birdY < 0.045f || birdY > 0.955f

            for (pipe in pipes) {
                val overlapsX = birdX + birdRadius > pipe.x && birdX - birdRadius < pipe.x + pipe.width
                val gapTop = pipe.gapCenter - pipe.gapSize / 2f
                val gapBottom = pipe.gapCenter + pipe.gapSize / 2f
                if (overlapsX && (birdTop < gapTop || birdBottom > gapBottom)) {
                    hit = true
                    break
                }
            }

            var newShield = shield
            if (hit && shield) {
                newShield = false
                velocity = -0.010f
                birdY = birdY.coerceIn(0.12f, 0.88f)
                pipes = pipes.map { it.copy(x = it.x - 0.08f) }
                hit = false
            }

            val collected = powerUps.filter {
                abs(it.x - birdX) < 0.075f && abs(it.y - birdY) < 0.075f
            }

            if (collected.isNotEmpty()) {
                for (power in collected) {
                    when (power.type) {
                        PowerType.COIN -> {
                            coins += 1
                            newScore += 5
                        }
                        PowerType.SHIELD -> newShield = true
                        PowerType.BOOST -> boostTime = 4.5f
                    }
                }
                powerUps = powerUps.filterNot { it in collected }
            }

            score = newScore
            combo = newCombo
            shield = newShield
            boostTime = max(0f, boostTime - dt)

            if (hit) {
                if (score > best) {
                    best = score
                    prefs.edit().putInt("best", best).apply()
                }
                screen = Screen.GAME_OVER
                break
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF080B14))
            .pointerInput(screen) { detectTapGestures { flap() } }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF101B3A), Color(0xFF172B52), Color(0xFF0B1020))
                    )
                )
        ) {
            drawWorld(
                screen = screen,
                worldTime = worldTime,
                birdY = birdY,
                pipes = pipes,
                powerUps = powerUps,
                shield = shield,
                boost = boostTime > 0f,
                score = score
            )
        }

        if (screen == Screen.MENU) {
            OverlayMenu(best)
        }

        if (screen == Screen.GAME_OVER) {
            GameOverOverlay(score, best, coins)
        }

        if (screen == Screen.PLAYING) {
            Text(
                text = "$score",
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 28.dp),
                color = Color.White,
                fontSize = 42.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "BEST $best",
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 76.dp),
                color = Color(0xFFB7C2D9),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "🪙 $coins" + if (shield) "   🛡️" else "" + if (boostTime > 0f) "   ⚡" else "",
                modifier = Modifier.align(Alignment.TopStart).padding(start = 22.dp, top = 28.dp),
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            if (combo >= 3) {
                Text(
                    text = "COMBO x" + (1 + combo / 5),
                    modifier = Modifier.align(Alignment.TopEnd).padding(end = 22.dp, top = 32.dp),
                    color = Color(0xFFFFD166),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black
                )
            }
            if (boostTime > 0f) {
                Text(
                    text = "BOOST " + String.format("%.1f", boostTime) + "s",
                    modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 28.dp),
                    color = Color(0xFFFFD166),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

@Composable
private fun OverlayMenu(best: Int) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "FLAPPY\nSTERIOIDS",
            color = Color.White,
            fontSize = 46.sp,
            lineHeight = 43.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(bottom = 110.dp)
        )
        Text(
            text = "TAP TO START • LEĆ PO SWÓJ REKORD\n\nREKORD  $best",
            color = Color(0xFFB8C4DD),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 22.sp,
            modifier = Modifier.padding(top = 150.dp)
        )
    }
}

@Composable
private fun GameOverOverlay(score: Int, best: Int, coins: Int) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "KONIEC LOTU",
            color = Color.White,
            fontSize = 34.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(bottom = 150.dp)
        )
        Text(
            text = "WYNIK  $score\nREKORD  $best\n🪙  $coins\n\nTAP = JESZCZE RAZ",
            color = Color(0xFFE5ECFF),
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 30.sp,
            modifier = Modifier.padding(top = 70.dp)
        )
    }
}

private fun DrawScope.drawWorld(
    screen: Screen,
    worldTime: Float,
    birdY: Float,
    pipes: List<Pipe>,
    powerUps: List<PowerUp>,
    shield: Boolean,
    boost: Boolean,
    score: Int
) {
    val w = size.width
    val h = size.height

    for (i in 0 until 28) {
        val x = ((i * 83f + worldTime * (12f + i % 4 * 5f)) % (w + 40f)) - 20f
        val y = (i * 47f) % h
        val r = if (i % 5 == 0) 2.4f else 1.2f
        drawCircle(Color(0xFFB9C7EA).copy(alpha = 0.35f), r, Offset(x, y))
    }

    val buildingBase = h * 0.91f
    for (i in 0..11) {
        val bw = w / 13f
        val bh = h * (0.04f + (i % 4) * 0.018f)
        drawRect(
            color = Color(0xFF10182C),
            topLeft = Offset(i * bw, buildingBase - bh),
            size = androidx.compose.ui.geometry.Size(bw - 3f, bh)
        )
    }

    for (pipe in pipes) {
        val left = pipe.x * w
        val right = left + pipe.width * w
        val gapTop = (pipe.gapCenter - pipe.gapSize / 2f) * h
        val gapBottom = (pipe.gapCenter + pipe.gapSize / 2f) * h
        drawPipe(left, 0f, right, gapTop, true)
        drawPipe(left, gapBottom, right, h, false)
    }

    for (power in powerUps) {
        val color = when (power.type) {
            PowerType.COIN -> Color(0xFFFFD166)
            PowerType.SHIELD -> Color(0xFF62D9FF)
            PowerType.BOOST -> Color(0xFFFF6B9D)
        }
        val center = Offset(power.x * w, power.y * h)
        drawCircle(color.copy(alpha = 0.18f), power.radius * 1.8f, center)
        drawCircle(color, power.radius, center)
        drawCircle(Color.White.copy(alpha = 0.8f), power.radius - 5f, center, style = Stroke(2f))
    }

    val bx = 0.27f * w
    val by = birdY * h
    val birdR = min(w, h) * 0.038f

    if (shield) {
        drawCircle(Color(0xFF62D9FF).copy(alpha = 0.16f), birdR * 1.9f, Offset(bx, by))
        drawCircle(Color(0xFF62D9FF), birdR * 1.45f, Offset(bx, by), style = Stroke(4f))
    }

    if (boost) {
        drawLine(
            Color(0xFFFFD166).copy(alpha = 0.7f),
            Offset(bx - birdR * 3.4f, by),
            Offset(bx - birdR * 1.0f, by),
            strokeWidth = birdR * 0.8f
        )
    }

    drawCircle(Color(0xFFFFD166), birdR, Offset(bx, by))
    drawCircle(Color(0xFFFF8A3D), birdR * 0.82f, Offset(bx - birdR * 0.05f, by + birdR * 0.12f))
    drawCircle(Color.White, birdR * 0.27f, Offset(bx + birdR * 0.48f, by - birdR * 0.32f))
    drawCircle(Color.Black, birdR * 0.12f, Offset(bx + birdR * 0.53f, by - birdR * 0.32f))
    drawLine(
        Color(0xFFFFF0B0),
        Offset(bx + birdR * 0.75f, by + birdR * 0.05f),
        Offset(bx + birdR * 1.35f, by + birdR * 0.05f),
        strokeWidth = birdR * 0.38f
    )

    drawRect(
        color = Color(0xFF62D9FF).copy(alpha = 0.45f),
        topLeft = Offset(0f, h * 0.94f),
        size = androidx.compose.ui.geometry.Size(w, 3f)
    )

    if (screen == Screen.PLAYING && score >= 10) {
        val pulse = 0.10f + 0.06f * ((worldTime * 4f) % 1f)
        drawRect(
            color = Color(0xFFFF4D8D).copy(alpha = pulse),
            topLeft = Offset(0f, 0f),
            size = androidx.compose.ui.geometry.Size(w, h)
        )
    }
}

private fun DrawScope.drawPipe(left: Float, top: Float, right: Float, bottom: Float, capAtBottom: Boolean) {
    if (bottom <= top) return
    val body = Color(0xFF35D07F)
    val dark = Color(0xFF159B5A)
    val capH = 28f

    drawRect(
        color = body,
        topLeft = Offset(left, top),
        size = androidx.compose.ui.geometry.Size(right - left, bottom - top)
    )

    if (capAtBottom) {
        drawRect(
            color = dark,
            topLeft = Offset(left - 7f, max(top, bottom - capH)),
            size = androidx.compose.ui.geometry.Size(right - left + 14f, capH)
        )
    } else {
        drawRect(
            color = dark,
            topLeft = Offset(left - 7f, top),
            size = androidx.compose.ui.geometry.Size(right - left + 14f, capH)
        )
    }

    drawRect(
        color = Color.White.copy(alpha = 0.14f),
        topLeft = Offset(left + 8f, top),
        size = androidx.compose.ui.geometry.Size(8f, bottom - top)
    )
}
