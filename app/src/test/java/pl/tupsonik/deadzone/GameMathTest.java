package pl.tupsonik.deadzone;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GameMathTest {

    @Test
    public void clampKeepsValueInRange() {
        assertEquals(0f, clamp(-5f, 0f, 100f), 0.001f);
        assertEquals(100f, clamp(120f, 0f, 100f), 0.001f);
        assertEquals(42f, clamp(42f, 0f, 100f), 0.001f);
    }

    @Test
    public void distanceIsCalculatedCorrectly() {
        float dx = 30f;
        float dy = 40f;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);

        assertEquals(50f, distance, 0.001f);
        assertTrue(Float.isFinite(distance));
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
