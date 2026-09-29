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
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.cos

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemBars(window)
        setContent { FlappySteroidsApp() }
    }

    private fun hideSystemBars(window: Window) {
        window.insetsController?.let {
            it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
            it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}

@Composable
private fun FlappySteroidsApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("flappy_steroids", Context.MODE_PRIVATE) }
    val engine = remember { GameEngine(seed = 9137) }

    var game by remember { mutableStateOf(engine.state) }
    var best by remember { mutableIntStateOf(prefs.getInt("best", 0)) }

    fun flap() {
        engine.flap()
        game = engine.state
    }

    LaunchedEffect(game.status) {
        if (game.status != GameStatus.PLAYING) return@LaunchedEffect

        while (true) {
            delay(16L)
            engine.tick()
            game = engine.state

            if (game.score > best) {
                best = game.score
                prefs.edit().putInt("best", best).apply()
            }

            if (game.status != GameStatus.PLAYING) break
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070A12))
            .pointerInput(game.status) { detectTapGestures { flap() } }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF111936), Color(0xFF1A2A4D), Color(0xFF090D19))
                    )
                )
        ) {
            drawWorld(game)
        }

        when (game.status) {
            GameStatus.MENU -> MenuOverlay(best)
            GameStatus.GAME_OVER -> GameOverOverlay(game, best)
            GameStatus.PLAYING -> {
                Text(
                    text = game.score.toString(),
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 20.dp),
                    color = Color.White,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Black
                )
                RowHud(game, best)
            }
        }
    }
}

@Composable
private fun RowHud(game: GameState, best: Int) {
    Column(
        modifier = Modifier
            .align(Alignment.TopStart)
            .padding(start = 18.dp, top = 22.dp)
    ) {
        Text(
            text = "BEST " + best,
            color = Color(0xFFB9C5DF),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "COINS " + game.coins,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 4.dp)
        )
    }

    Column(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(end = 18.dp, top = 22.dp),
        horizontalAlignment = Alignment.End
    ) {
        if (game.multiplier > 1) {
            Text(
                text = "COMBO x" + game.multiplier,
                color = Color(0xFFFFD166),
                fontSize = 15.sp,
                fontWeight = FontWeight.Black
            )
        }
        if (game.shield) {
            Text(
                text = "SHIELD",
                color = Color(0xFF62D9FF),
                fontSize = 11.sp,
                fontWeight = FontWeight.Black
            )
        }
        if (game.boostTimer > 0f) {
            Text(
                text = "BOOST " + game.boostTimer.format1() + "s",
                color = Color(0xFFFFD166),
                fontSize = 11.sp,
                fontWeight = FontWeight.Black
            )
        }
        if (game.magnetTimer > 0f) {
            Text(
                text = "MAGNET",
                color = Color(0xFF9D8CFF),
                fontSize = 11.sp,
                fontWeight = FontWeight.Black
            )
        }
        if (game.slowTimer > 0f) {
            Text(
                text = "SLOWMO",
                color = Color(0xFF62FFE3),
                fontSize = 11.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

private fun Float.format1(): String = "%.1f".format(this)

@Composable
private fun MenuOverlay(best: Int) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "FLAPPY\nSTEROIDS",
            color = Color.White,
            fontSize = 52.sp,
            lineHeight = 48.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(bottom = 128.dp)
        )
        Text(
            text = "TAP = LOT\n\nCOINS   SHIELD   BOOST\nMAGNET   SLOWMO\n\nREKORD  " + best,
            color = Color(0xFFD5DEF2),
            fontSize = 14.sp,
            lineHeight = 23.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 170.dp)
        )
    }
}

@Composable
private fun GameOverOverlay(game: GameState, best: Int) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = "KONIEC LOTU",
            color = Color.White,
            fontSize = 36.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(bottom = 160.dp)
        )
        Text(
            text = "WYNIK  " + game.score +
                "\nREKORD  " + best +
                "\nCOINS  " + game.coins +
                "\nCOMBO  x" + game.multiplier +
                "\n\nTAP = JESZCZE RAZ",
            color = Color(0xFFE3EBFF),
            fontSize = 18.sp,
            lineHeight = 31.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 85.dp)
        )
    }
}

private fun DrawScope.drawWorld(game: GameState) {
    val w = size.width
    val h = size.height

    val shakeAmount = if (game.shakeTimer > 0f) 10f * (game.shakeTimer / 0.28f) else 0f
    val shakeX = sin(game.worldTime * 120f) * shakeAmount
    val shakeY = cos(game.worldTime * 105f) * shakeAmount * 0.7f

    withTransform({
        translate(shakeX, shakeY)
    }) {
        for (i in 0 until 34) {
            val x = ((i * 89f + game.worldTime * (14f + i % 5 * 6f)) % (w + 50f)) - 25f
            val y = (i * 53f) % h
            val r = if (i % 6 == 0) 2.5f else 1.3f
            drawCircle(Color(0xFFBFD0FF).copy(alpha = 0.30f), r, Offset(x, y))
        }

        val ground = h * 0.925f
        for (i in 0..13) {
            val bw = w / 14f
            val bh = h * (0.035f + (i % 5) * 0.014f)
            drawRect(
                color = Color(0xFF0D1427),
                topLeft = Offset(i * bw, ground - bh),
                size = androidx.compose.ui.geometry.Size(bw - 4f, bh)
            )
        }

        game.pipes.forEach { pipe ->
            val left = pipe.x * w
            val right = left + pipe.width * w
            val gapTop = (pipe.gapCenter - pipe.gapSize / 2f) * h
            val gapBottom = (pipe.gapCenter + pipe.gapSize / 2f) * h
            drawPipe(left, 0f, right, gapTop, true)
            drawPipe(left, gapBottom, right, h, false)
        }

        game.pickups.forEach { pickup ->
            val cx = pickup.x * w
            val cy = pickup.y * h
            val rr = min(w, h) * pickup.radius
            val color = when (pickup.type) {
                PickupType.COIN -> Color(0xFFFFD166)
                PickupType.SHIELD -> Color(0xFF62D9FF)
                PickupType.BOOST -> Color(0xFFFF6B9D)
                PickupType.MAGNET -> Color(0xFF9D8CFF)
                PickupType.SLOWMO -> Color(0xFF62FFE3)
            }
            drawCircle(color.copy(alpha = 0.15f), rr * 2.0f, Offset(cx, cy))
            drawCircle(color, rr, Offset(cx, cy), style = Stroke(max(3f, rr * 0.25f)))
            drawCircle(Color.White.copy(alpha = 0.9f), rr * 0.22f, Offset(cx, cy))
        }

        game.sparks.forEach { spark ->
            val alpha = (spark.life / 0.8f).coerceIn(0f, 1f)
            drawCircle(
                Color(0xFFFFD166).copy(alpha = alpha),
                min(w, h) * 0.008f,
                Offset(spark.x * w, spark.y * h)
            )
        }

        val bx = 0.27f * w
        val by = game.birdY * h
        val birdR = min(w, h) * 0.038f

        if (game.shield) {
            drawCircle(Color(0xFF62D9FF).copy(alpha = 0.10f), birdR * 2.25f, Offset(bx, by))
            drawCircle(Color(0xFF62D9FF), birdR * 1.50f, Offset(bx, by), style = Stroke(4f))
        }

        if (game.magnetTimer > 0f) {
            drawCircle(Color(0xFF9D8CFF).copy(alpha = 0.10f), birdR * 2.9f, Offset(bx, by))
        }

        if (game.boostTimer > 0f) {
            for (i in 1..3) {
                drawLine(
                    Color(0xFFFFD166).copy(alpha = 0.75f - i * 0.15f),
                    Offset(bx - birdR * (1.0f + i * 0.9f), by + i * 3f),
                    Offset(bx - birdR * 0.7f, by),
                    strokeWidth = birdR * 0.26f
                )
            }
        }

        val tilt = (game.velocity * 900f).coerceIn(-28f, 28f)
        rotate(tilt, pivot = Offset(bx, by)) {
            drawCircle(Color(0xFFFFD166), birdR, Offset(bx, by))
            drawCircle(
                Color(0xFFFF8A3D),
                birdR * 0.84f,
                Offset(bx - birdR * 0.03f, by + birdR * 0.11f)
            )
            drawCircle(Color.White, birdR * 0.27f, Offset(bx + birdR * 0.48f, by - birdR * 0.30f))
            drawCircle(Color.Black, birdR * 0.12f, Offset(bx + birdR * 0.53f, by - birdR * 0.30f))
            drawLine(
                Color(0xFFFFF0B0),
                Offset(bx + birdR * 0.76f, by + birdR * 0.05f),
                Offset(bx + birdR * 1.40f, by + birdR * 0.05f),
                strokeWidth = birdR * 0.38f
            )
        }

        drawRect(
            color = Color(0xFF62D9FF).copy(alpha = 0.35f),
            topLeft = Offset(0f, h * 0.94f),
            size = androidx.compose.ui.geometry.Size(w, 3f)
        )
    }
}

private fun DrawScope.drawPipe(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    topCap: Boolean
) {
    if (bottom <= top) return
    val body = Color(0xFF36D985)
    val dark = Color(0xFF138E52)
    val capH = 30f

    drawRect(
        color = body,
        topLeft = Offset(left, top),
        size = androidx.compose.ui.geometry.Size(right - left, bottom - top)
    )

    if (topCap) {
        drawRect(
            color = dark,
            topLeft = Offset(left - 8f, max(top, bottom - capH)),
            size = androidx.compose.ui.geometry.Size(right - left + 16f, capH)
        )
    } else {
        drawRect(
            color = dark,
            topLeft = Offset(left - 8f, top),
            size = androidx.compose.ui.geometry.Size(right - left + 16f, capH)
        )
    }

    drawRect(
        color = Color.White.copy(alpha = 0.12f),
        topLeft = Offset(left + 9f, top),
        size = androidx.compose.ui.geometry.Size(8f, bottom - top)
    )
}
