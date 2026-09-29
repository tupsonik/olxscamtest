package pl.tupsonik.flappysteroids

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

enum class GameStatus { MENU, PLAYING, GAME_OVER }
enum class PickupType { COIN, SHIELD, BOOST, MAGNET, SLOWMO }

data class Pipe(
    val x: Float,
    val gapCenter: Float,
    val gapSize: Float,
    val width: Float = 0.105f,
    val passed: Boolean = false
)

data class Pickup(
    val x: Float,
    val y: Float,
    val type: PickupType,
    val radius: Float = 0.026f
)

data class Spark(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val life: Float
)

data class GameState(
    val status: GameStatus = GameStatus.MENU,
    val birdY: Float = 0.5f,
    val velocity: Float = 0f,
    val pipes: List<Pipe> = emptyList(),
    val pickups: List<Pickup> = emptyList(),
    val sparks: List<Spark> = emptyList(),
    val score: Int = 0,
    val coins: Int = 0,
    val combo: Int = 0,
    val multiplier: Int = 1,
    val shield: Boolean = false,
    val boostTimer: Float = 0f,
    val magnetTimer: Float = 0f,
    val slowTimer: Float = 0f,
    val shakeTimer: Float = 0f,
    val worldTime: Float = 0f,
    val spawnTimer: Float = 0.7f
)

class GameEngine(seed: Int = 1) {
    private val random = Random(seed)
    var state: GameState = GameState()
        private set

    fun start() {
        state = GameState(status = GameStatus.PLAYING, birdY = 0.5f, score = 0, coins = 0, spawnTimer = 0.70f)
    }

    fun flap() {
        when (state.status) {
            GameStatus.MENU, GameStatus.GAME_OVER -> start()
            GameStatus.PLAYING -> {
                state = state.copy(velocity = -0.0128f)
            }
        }
    }

    fun tick(dtRaw: Float = 1f / 60f) {
        if (state.status != GameStatus.PLAYING) return
        val dt = dtRaw.coerceIn(0.008f, 0.033f)

        var birdY = state.birdY
        var velocity = state.velocity
        var pipes = state.pipes
        var pickups = state.pickups
        var sparks = state.sparks
        var score = state.score
        var coins = state.coins
        var combo = state.combo
        var shield = state.shield
        var boostTimer = max(0f, state.boostTimer - dt)
        var magnetTimer = max(0f, state.magnetTimer - dt)
        var slowTimer = max(0f, state.slowTimer - dt)
        var shakeTimer = max(0f, state.shakeTimer - dt)
        var worldTime = state.worldTime + dt
        var spawnTimer = state.spawnTimer

        val slowFactor = if (slowTimer > 0f) 0.62f else 1f
        val difficulty = min(2.25f, 1f + score * 0.014f)
        val speed = 0.0075f * difficulty *
            (if (boostTimer > 0f) 1.18f else 1f) *
            slowFactor
        val gravity = 0.00062f * slowFactor
        val gap = max(0.215f, 0.36f - score * 0.0025f)

        velocity += gravity
        birdY += velocity

        spawnTimer -= dt

        if (spawnTimer <= 0f) {
            val center = random.nextFloat() * 0.50f + 0.25f
            pipes = pipes + Pipe(x = 1.10f, gapCenter = center, gapSize = gap)

            val pickupChance = min(0.82f, 0.55f + score * 0.004f)
            if (random.nextFloat() < pickupChance) {
                val type = when {
                    random.nextFloat() < 0.47f -> PickupType.COIN
                    random.nextFloat() < 0.61f -> PickupType.SHIELD
                    random.nextFloat() < 0.78f -> PickupType.BOOST
                    random.nextFloat() < 0.90f -> PickupType.MAGNET
                    else -> PickupType.SLOWMO
                }
                pickups = pickups + Pickup(
                    x = 1.30f,
                    y = (center + random.nextFloat() * 0.24f - 0.12f).coerceIn(0.13f, 0.87f),
                    type = type
                )
            }
            spawnTimer = max(0.78f, 1.42f - score * 0.007f)
        }

        pipes = pipes
            .map { it.copy(x = it.x - speed) }
            .filter { it.x > -0.18f }

        pickups = pickups.map {
            val targetX = 0.27f
            val magnetPullX = if (magnetTimer > 0f) (targetX - it.x) * min(1f, dt * 5.5f) else 0f
            val magnetPullY = if (magnetTimer > 0f) (birdY - it.y) * min(1f, dt * 4.0f) else 0f
            it.copy(
                x = it.x - speed + magnetPullX,
                y = it.y + magnetPullY
            )
        }.filter { it.x > -0.15f }

        var justPassed = 0
        pipes = pipes.map { pipe ->
            if (!pipe.passed && pipe.x + pipe.width < 0.27f) {
                justPassed++
                pipe.copy(passed = true)
            } else pipe
        }

        if (justPassed > 0) {
            repeat(justPassed) {
                combo++
                score += 1 + min(5, combo / 5)
            }
        }

        val birdX = 0.27f
        val birdRadius = 0.038f
        var hit = birdY - birdRadius <= 0.045f || birdY + birdRadius >= 0.935f

        if (!hit) {
            for (pipe in pipes) {
                // Collision matches the visible pipe cap, which extends beyond the body.
                val left = pipe.x - 0.008f
                val right = pipe.x + pipe.width + 0.008f
                val gapTop = pipe.gapCenter - pipe.gapSize / 2f
                val gapBottom = pipe.gapCenter + pipe.gapSize / 2f

                val overlapsTop = Collision.circleIntersectsRect(
                    birdX, birdY, birdRadius,
                    left, 0f, right, gapTop
                )
                val overlapsBottom = Collision.circleIntersectsRect(
                    birdX, birdY, birdRadius,
                    left, gapBottom, right, 1f
                )

                if (overlapsTop || overlapsBottom) {
                    hit = true
                    break
                }
            }
        }

        var shieldConsumed = false
        if (hit && shield) {
            shield = false
            shieldConsumed = true
            hit = false
            velocity = -0.0105f
            birdY = birdY.coerceIn(0.12f, 0.88f)
            shakeTimer = 0.28f
            sparks = sparks + burst(birdX, birdY, 18)
        }

        val collected = pickups.filter {
            val pickupRadius = if (it.type == PickupType.COIN) it.radius * 1.15f else it.radius
            Collision.circleIntersectsCircle(
                birdX, birdY, birdRadius,
                it.x, it.y, pickupRadius
            )
        }

        if (collected.isNotEmpty()) {
            for (pickup in collected) {
                when (pickup.type) {
                    PickupType.COIN -> {
                        coins++
                        score += 5 + min(10, combo)
                    }
                    PickupType.SHIELD -> shield = true
                    PickupType.BOOST -> boostTimer = max(boostTimer, 5f)
                    PickupType.MAGNET -> magnetTimer = max(magnetTimer, 6f)
                    PickupType.SLOWMO -> slowTimer = max(slowTimer, 4f)
                }
            }
            pickups = pickups.filterNot { it in collected }
            sparks = sparks + burst(birdX, birdY, 12)
        }

        sparks = sparks.map {
            it.copy(
                x = it.x + it.vx * dt,
                y = it.y + it.vy * dt,
                vy = it.vy + 0.0012f,
                life = it.life - dt
            )
        }.filter { it.life > 0f }

        if (shieldConsumed) {
            combo = max(0, combo - 1)
        }

        if (hit) {
            state = state.copy(
                status = GameStatus.GAME_OVER,
                birdY = birdY,
                velocity = velocity,
                pipes = pipes,
                pickups = pickups,
                sparks = sparks,
                score = score,
                coins = coins,
                combo = combo,
                multiplier = 1 + combo / 5,
                shield = shield,
                boostTimer = boostTimer,
                magnetTimer = magnetTimer,
                slowTimer = slowTimer,
                shakeTimer = max(shakeTimer, 0.20f),
                worldTime = worldTime,
                spawnTimer = spawnTimer
            )
            return
        }

        state = state.copy(
            birdY = birdY,
            velocity = velocity,
            pipes = pipes,
            pickups = pickups,
            sparks = sparks,
            score = score,
            coins = coins,
            combo = combo,
            multiplier = 1 + combo / 5,
            shield = shield,
            boostTimer = boostTimer,
            magnetTimer = magnetTimer,
            slowTimer = slowTimer,
            shakeTimer = shakeTimer,
            worldTime = worldTime,
            spawnTimer = spawnTimer
        )
    }

    private fun burst(x: Float, y: Float, count: Int): List<Spark> =
        List(count) {
            Spark(
                x = x,
                y = y,
                vx = random.nextFloat() * 0.018f - 0.009f,
                vy = random.nextFloat() * 0.018f - 0.013f,
                life = 0.35f + random.nextFloat() * 0.45f
            )
        }
}
