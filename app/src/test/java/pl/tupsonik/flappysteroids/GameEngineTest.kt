package pl.tupsonik.flappysteroids

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CollisionTest {
    @Test
    fun circleRect_inside_isCollision() {
        assertTrue(Collision.circleIntersectsRect(0.5f, 0.5f, 0.05f, 0.4f, 0.4f, 0.6f, 0.6f))
    }

    @Test
    fun circleRect_farAway_isNotCollision() {
        assertFalse(Collision.circleIntersectsRect(0.1f, 0.1f, 0.03f, 0.4f, 0.4f, 0.6f, 0.6f))
    }

    @Test
    fun circleRect_cornerDistance_isHandledExactly() {
        assertTrue(Collision.circleIntersectsRect(0.2f, 0.2f, 0.1f, 0.25f, 0.25f, 0.6f, 0.6f))
        assertFalse(Collision.circleIntersectsRect(0.2f, 0.2f, 0.06f, 0.25f, 0.25f, 0.6f, 0.6f))
    }

    @Test
    fun circleRect_tangentCountsAsCollision() {
        assertTrue(Collision.circleIntersectsRect(0.25f, 0.50f, 0.25f, 0.50f, 0.25f, 0.75f, 0.75f))
        assertFalse(Collision.circleIntersectsRect(0.25f, 0.50f, 0.24f, 0.50f, 0.25f, 0.75f, 0.75f))
    }

    @Test
    fun circleCircle_overlapAndSeparation() {
        assertTrue(Collision.circleIntersectsCircle(0f, 0f, 0.1f, 0.15f, 0f, 0.06f))
        assertFalse(Collision.circleIntersectsCircle(0f, 0f, 0.1f, 0.2f, 0f, 0.06f))
    }
}

class GameEngineTest {
    @Test
    fun startAndFlap_work() {
        val engine = GameEngine(seed = 7)
        engine.start()
        assertTrue(engine.state.status == GameStatus.PLAYING)
        engine.flap()
        assertTrue(engine.state.velocity < 0f)
    }

    @Test
    fun simulation_staysFinite() {
        val engine = GameEngine(seed = 123)
        engine.start()

        repeat(900) {
            engine.tick(1f / 60f)
            val s = engine.state
            assertTrue(s.birdY.isFinite())
            assertTrue(s.velocity.isFinite())
            assertTrue(s.worldTime.isFinite())
            if (s.status == GameStatus.GAME_OVER) engine.start()
        }
    }

    @Test
    fun spawnTimer_survivesTicks_andResetsOnStart() {
        val engine = GameEngine(seed = 99)
        engine.start()
        val firstTimer = engine.state.spawnTimer
        engine.tick()
        assertTrue(engine.state.spawnTimer < firstTimer)
        engine.start()
        assertTrue(kotlin.math.abs(engine.state.spawnTimer - 0.70f) < 0.001f)
    }
}
