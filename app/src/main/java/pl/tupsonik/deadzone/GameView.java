package pl.tupsonik.deadzone;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class GameView extends View {

    private static final float WORLD_W = 5200f;
    private static final float WORLD_H = 4200f;
    private static final float PLAYER_SPEED = 235f;
    private static final float ZOMBIE_SPEED = 72f;
    private static final float ZOMBIE_CHASE_SPEED = 112f;
    private static final float BULLET_SPEED = 650f;
    private static final float DAY_LENGTH = 150f;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Random random = new Random(1337L);
    private final List<Tree> trees = new ArrayList<>();
    private final List<House> houses = new ArrayList<>();
    private final List<LootCrate> crates = new ArrayList<>();
    private final List<Zombie> zombies = new ArrayList<>();
    private final List<Bullet> bullets = new ArrayList<>();

    private float playerX = WORLD_W * 0.5f;
    private float playerY = WORLD_H * 0.5f;
    private float playerHp = 100f;
    private float hunger = 100f;
    private float thirst = 100f;
    private float stamina = 100f;

    private int ammo = 18;
    private int medkits = 2;
    private int water = 1;
    private int food = 2;

    private float cameraX;
    private float cameraY;
    private float worldTime = 72f;
    private float meleeCooldown = 0f;
    private float shotCooldown = 0f;
    private float damageFlash = 0f;
    private float lastFrame;
    private boolean running;
    private boolean dead;
    private boolean inventoryOpen;

    private float aimX = 1f;
    private float aimY = 0f;

    private float joystickBaseX;
    private float joystickBaseY;
    private float joystickX;
    private float joystickY;
    private boolean joystickActive;
    private boolean attackHeld;
    private boolean fireHeld;

    private final RectF attackButton = new RectF();
    private final RectF fireButton = new RectF();
    private final RectF bagButton = new RectF();
    private final RectF lootButton = new RectF();

    public GameView(Context context) {
        super(context);
        setFocusable(true);
        setKeepScreenOn(true);
        initWorld();
    }

    private void initWorld() {
        trees.clear();
        houses.clear();
        crates.clear();
        zombies.clear();
        bullets.clear();

        playerX = WORLD_W * 0.5f;
        playerY = WORLD_H * 0.5f;

        for (int i = 0; i < 190; i++) {
            trees.add(new Tree(
                    80 + random.nextFloat() * (WORLD_W - 160),
                    80 + random.nextFloat() * (WORLD_H - 160),
                    16 + random.nextFloat() * 18
            ));
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 4; col++) {
                float x = 780 + col * 920f;
                float y = 620 + row * 900f;
                houses.add(new House(x, y, 420, 290));
                crates.add(new LootCrate(x + 92, y + 155));
                crates.add(new LootCrate(x + 320, y + 82));
            }
        }

        houses.add(new House(3290, 670, 690, 430));
        houses.add(new House(3330, 1480, 470, 370));
        houses.add(new House(3820, 1480, 510, 370));

        for (int i = 0; i < 30; i++) {
            float angle = random.nextFloat() * 6.2831853f;
            float dist = 450 + random.nextFloat() * 1800;
            float x = clamp(playerX + (float) Math.cos(angle) * dist, 60, WORLD_W - 60);
            float y = clamp(playerY + (float) Math.sin(angle) * dist, 60, WORLD_H - 60);
            zombies.add(new Zombie(x, y));
        }

        cameraX = playerX;
        cameraY = playerY;
        worldTime = 72f;
        playerHp = 100f;
        hunger = 100f;
        thirst = 100f;
        stamina = 100f;
        ammo = 18;
        medkits = 2;
        water = 1;
        food = 2;
        dead = false;
        inventoryOpen = false;
        attackHeld = false;
        fireHeld = false;
    }

    public void startLoop() {
        if (running) return;
        running = true;
        lastFrame = System.nanoTime() / 1_000_000_000f;
        postInvalidateOnAnimation();
    }

    public void stopLoop() {
        running = false;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float now = System.nanoTime() / 1_000_000_000f;
        float dt = Math.min(0.05f, Math.max(0f, now - lastFrame));
        lastFrame = now;

        if (running && !dead && !inventoryOpen) {
            update(dt);
        }

        drawGame(canvas);

        if (running) {
            postInvalidateOnAnimation();
        }
    }

    private void update(float dt) {
        worldTime = (worldTime + dt) % DAY_LENGTH;
        meleeCooldown = Math.max(0f, meleeCooldown - dt);
        shotCooldown = Math.max(0f, shotCooldown - dt);
        damageFlash = Math.max(0f, damageFlash - dt);

        float moveX = joystickActive ? joystickX : 0f;
        float moveY = joystickActive ? joystickY : 0f;
        float inputLength = (float) Math.sqrt(moveX * moveX + moveY * moveY);

        if (inputLength > 0.06f) {
            moveX /= inputLength;
            moveY /= inputLength;

            aimX = moveX;
            aimY = moveY;

            float speed = PLAYER_SPEED;
            if (inputLength > 0.70f && stamina > 1f) {
                speed *= 1.18f;
                stamina -= dt * 7f;
            } else {
                stamina += dt * 12f;
            }

            float nx = clamp(playerX + moveX * speed * dt, 32f, WORLD_W - 32f);
            float ny = clamp(playerY + moveY * speed * dt, 32f, WORLD_H - 32f);

            if (!hitsSolid(nx, playerY, 27f)) playerX = nx;
            if (!hitsSolid(playerX, ny, 27f)) playerY = ny;
        } else {
            stamina += dt * 16f;
        }

        stamina = clamp(stamina, 0f, 100f);
        hunger = clamp(hunger - dt * 0.10f, 0f, 100f);
        thirst = clamp(thirst - dt * 0.18f, 0f, 100f);

        if (hunger <= 0f) playerHp -= dt * 0.9f;
        if (thirst <= 0f) playerHp -= dt * 1.5f;

        if (attackHeld) meleeAttack();
        if (fireHeld) fireWeapon();

        for (Zombie zombie : zombies) {
            zombie.update(dt);
        }

        for (Bullet bullet : bullets) {
            bullet.update(dt);
        }

        resolveBullets();

        if (playerHp <= 0f) {
            playerHp = 0f;
            dead = true;
            attackHeld = false;
            fireHeld = false;
        }

        cameraX += (playerX - cameraX) * Math.min(1f, dt * 8f);
        cameraY += (playerY - cameraY) * Math.min(1f, dt * 8f);
    }

    private boolean hitsSolid(float x, float y, float radius) {
        for (House house : houses) {
            RectF r = house.rect();
            if (x + radius > r.left && x - radius < r.right
                    && y + radius > r.top && y - radius < r.bottom) {
                return true;
            }
        }
        return false;
    }

    private void meleeAttack() {
        if (meleeCooldown > 0f) return;
        meleeCooldown = 0.42f;

        float sx = playerX + aimX * 34f;
        float sy = playerY + aimY * 34f;

        Iterator<Zombie> iterator = zombies.iterator();
        while (iterator.hasNext()) {
            Zombie zombie = iterator.next();
            float dx = zombie.x - sx;
            float dy = zombie.y - sy;
            float distanceSquared = dx * dx + dy * dy;
            float dot = dx * aimX + dy * aimY;

            if (dot > -8f && distanceSquared < 92f * 92f) {
                iterator.remove();
                break;
            }
        }
    }

    private void fireWeapon() {
        if (shotCooldown > 0f || ammo <= 0) return;

        shotCooldown = 0.18f;
        ammo--;

        float startX = playerX + aimX * 32f;
        float startY = playerY + aimY * 32f;
        bullets.add(new Bullet(startX, startY, aimX, aimY));
    }

    private void resolveBullets() {
        Iterator<Bullet> bulletIterator = bullets.iterator();

        while (bulletIterator.hasNext()) {
            Bullet bullet = bulletIterator.next();

            if (bullet.life <= 0f
                    || bullet.x < 0f || bullet.x > WORLD_W
                    || bullet.y < 0f || bullet.y > WORLD_H) {
                bulletIterator.remove();
                continue;
            }

            Iterator<Zombie> zombieIterator = zombies.iterator();
            boolean hit = false;

            while (zombieIterator.hasNext()) {
                Zombie zombie = zombieIterator.next();
                float dx = zombie.x - bullet.x;
                float dy = zombie.y - bullet.y;

                if (dx * dx + dy * dy < 28f * 28f) {
                    zombieIterator.remove();
                    hit = true;
                    break;
                }
            }

            if (hit) {
                bulletIterator.remove();
            }
        }
    }

    private void drawGame(Canvas canvas) {
        int width = getWidth();
        int height = getHeight();

        canvas.drawColor(0xFF2E4434);

        float left = cameraX - width * 0.5f;
        float top = cameraY - height * 0.5f;

        canvas.save();
        canvas.translate(-left, -top);

        drawGround(canvas, left, top, width, height);
        drawRoads(canvas);
        drawHouses(canvas);
        drawTrees(canvas);
        drawCrates(canvas);

        for (Bullet bullet : bullets) drawBullet(canvas, bullet);
        for (Zombie zombie : zombies) drawZombie(canvas, zombie);

        drawPlayer(canvas);
        canvas.restore();

        drawNightOverlay(canvas, width, height);
        drawHud(canvas, width, height);

        if (inventoryOpen) drawInventory(canvas, width, height);
        if (dead) drawDeath(canvas, width, height);

        if (damageFlash > 0f) {
            paint.setColor(0x44D94138);
            canvas.drawRect(0, 0, width, height, paint);
        }
    }

    private void drawGround(Canvas canvas, float left, float top, int width, int height) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFF314A37);
        canvas.drawRect(left, top, left + width, top + height, paint);

        paint.setColor(0xFF3A553F);
        float cell = 150f;
        int startX = (int) Math.floor(left / cell) - 1;
        int startY = (int) Math.floor(top / cell) - 1;

        for (int gx = startX; gx < startX + width / 150 + 4; gx++) {
            for (int gy = startY; gy < startY + height / 150 + 4; gy++) {
                if (((gx * 31) ^ (gy * 17)) % 6 == 0) {
                    canvas.drawCircle(gx * cell + 42f, gy * cell + 70f, 2.2f, paint);
                }
            }
        }
    }

    private void drawRoads(Canvas canvas) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFF57564F);
        canvas.drawRect(0f, 1940f, WORLD_W, 2140f, paint);
        canvas.drawRect(2440f, 0f, 2640f, WORLD_H, paint);

        paint.setColor(0xFF807A6D);
        paint.setStrokeWidth(5f);
        paint.setStyle(Paint.Style.STROKE);

        for (int x = 20; x < WORLD_W; x += 80) {
            canvas.drawLine(x, 2040f, x + 40, 2040f, paint);
        }

        for (int y = 20; y < WORLD_H; y += 80) {
            canvas.drawLine(2540f, y, 2540f, y + 40, paint);
        }

        paint.setStyle(Paint.Style.FILL);
    }

    private void drawHouses(Canvas canvas) {
        for (House house : houses) {
            RectF r = house.rect();

            paint.setColor(0xFF7C7669);
            canvas.drawRect(r, paint);

            paint.setColor(0xFF4C483F);
            canvas.drawRect(r.left + 12f, r.top + 12f, r.right - 12f, r.top + 42f, paint);

            paint.setColor(0xFF2C302D);
            canvas.drawRect(r.centerX() - 30f, r.bottom - 70f, r.centerX() + 30f, r.bottom, paint);

            paint.setColor(0xFF97A58B);
            canvas.drawRect(r.left + 40f, r.top + 72f, r.left + 112f, r.top + 135f, paint);
            canvas.drawRect(r.right - 112f, r.top + 72f, r.right - 40f, r.top + 135f, paint);
        }
    }

    private void drawTrees(Canvas canvas) {
        for (Tree tree : trees) {
            if (!isVisible(tree.x, tree.y, 70f)) continue;

            paint.setColor(0xFF4E3826);
            canvas.drawRect(tree.x - 5f, tree.y + 8f, tree.x + 5f, tree.y + 32f, paint);

            paint.setColor(0xFF173B24);
            canvas.drawCircle(tree.x, tree.y, tree.radius, paint);

            paint.setColor(0xFF285833);
            canvas.drawCircle(tree.x - 9f, tree.y - 8f, tree.radius * 0.70f, paint);
        }
    }

    private void drawCrates(Canvas canvas) {
        for (LootCrate crate : crates) {
            if (crate.opened || !isVisible(crate.x, crate.y, 55f)) continue;

            paint.setColor(0xFF8D6A3D);
            canvas.drawRoundRect(crate.x - 18f, crate.y - 18f, crate.x + 18f, crate.y + 18f, 6f, 6f, paint);

            paint.setColor(0xFFD2B06D);
            paint.setStrokeWidth(5f);
            canvas.drawLine(crate.x - 13f, crate.y, crate.x + 13f, crate.y, paint);
        }
    }

    private void drawZombie(Canvas canvas, Zombie zombie) {
        if (!isVisible(zombie.x, zombie.y, 70f)) return;

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFF7F8C74);
        canvas.drawCircle(zombie.x, zombie.y, 20f, paint);

        paint.setColor(0xFF334830);
        canvas.drawRect(zombie.x - 18f, zombie.y + 4f, zombie.x + 18f, zombie.y + 18f, paint);

        paint.setColor(0xFFE1D9A7);
        canvas.drawCircle(zombie.x - 7f, zombie.y - 5f, 3f, paint);
        canvas.drawCircle(zombie.x + 7f, zombie.y - 5f, 3f, paint);
    }

    private void drawBullet(Canvas canvas, Bullet bullet) {
        paint.setColor(0xFFF3D28A);
        canvas.drawCircle(bullet.x, bullet.y, 5f, paint);
    }

    private void drawPlayer(Canvas canvas) {
        paint.setColor(0xFF3F708D);
        canvas.drawCircle(playerX, playerY, 25f, paint);

        paint.setColor(0xFFD0BFA0);
        canvas.drawCircle(playerX, playerY - 7f, 12f, paint);

        paint.setColor(0xFF202729);
        canvas.drawRect(playerX - 11f, playerY + 6f, playerX + 11f, playerY + 26f, paint);

        paint.setColor(0xFFD8C36A);
        paint.setStrokeWidth(7f);
        canvas.drawLine(playerX, playerY, playerX + aimX * 35f, playerY + aimY * 35f, paint);
    }

    private void drawNightOverlay(Canvas canvas, int width, int height) {
        float phase = worldTime / DAY_LENGTH;
        float daylight = (float) Math.sin(phase * Math.PI * 2d - Math.PI / 2d) * 0.5f + 0.5f;
        daylight = clamp(daylight, 0.08f, 1f);

        int alpha = (int) (150f * (1f - daylight));
        if (alpha > 0) {
            paint.setColor((alpha << 24) | 0x07111A);
            canvas.drawRect(0f, 0f, width, height, paint);
        }
    }

    private void drawHud(Canvas canvas, int width, int height) {
        attackButton.set(width - 150f, height - 145f, width - 55f, height - 50f);
        fireButton.set(width - 275f, height - 160f, width - 175f, height - 60f);
        bagButton.set(width - 125f, 18f, width - 24f, 70f);
        lootButton.set(width - 285f, 18f, width - 140f, 70f);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0x70202720);
        canvas.drawRoundRect(18f, 18f, 355f, 132f, 18f, 18f, paint);

        drawBar(canvas, 32f, 34f, 175f, 18f, playerHp / 100f, 0xFFB94743, "HP");
        drawBar(canvas, 32f, 60f, 175f, 18f, hunger / 100f, 0xFFC39C55, "HUN");
        drawBar(canvas, 32f, 86f, 175f, 18f, thirst / 100f, 0xFF5E9FC6, "THI");

        paint.setColor(0xFFF2EBDD);
        paint.setTextSize(18f);
        canvas.drawText("AMMO " + ammo + "   MED " + medkits + "   Z " + zombies.size(), 220f, 47f, paint);

        paint.setTextSize(16f);
        canvas.drawText(formatTime(), 220f, 73f, paint);
        canvas.drawText("STAMINA " + (int) stamina + "%", 220f, 99f, paint);

        drawButton(canvas, bagButton, "BAG", 16f);
        drawButton(canvas, lootButton, "LOOT", 15f);
        drawButton(canvas, fireButton, "FIRE", 14f);
        drawButton(canvas, attackButton, "MELEE", 12f);

        if (joystickActive) {
            paint.setColor(0x3D000000);
            canvas.drawCircle(joystickBaseX, joystickBaseY, 70f, paint);

            paint.setColor(0xA9DEE3D4);
            canvas.drawCircle(
                    joystickBaseX + joystickX * 52f,
                    joystickBaseY + joystickY * 52f,
                    26f,
                    paint
            );
        } else {
            paint.setColor(0x3D000000);
            canvas.drawCircle(95f, height - 92f, 70f, paint);

            paint.setColor(0x774E5C4E);
            canvas.drawCircle(95f, height - 92f, 28f, paint);
        }

        paint.setColor(0xDDE6DECE);
        paint.setTextSize(16f);
        canvas.drawText("Ruch", 72f, height - 25f, paint);
    }

    private void drawBar(Canvas canvas, float x, float y, float width, float height,
                         float value, int color, String label) {
        paint.setColor(0xAA111512);
        canvas.drawRoundRect(x, y, x + width, y + height, 8f, 8f, paint);

        paint.setColor(color);
        canvas.drawRoundRect(
                x, y,
                x + width * clamp(value, 0f, 1f),
                y + height,
                8f, 8f,
                paint
        );

        paint.setColor(0xFFEDE7DA);
        paint.setTextSize(12f);
        canvas.drawText(label, x + 6f, y + 14f, paint);
    }

    private void drawButton(Canvas canvas, RectF rect, String text, float size) {
        paint.setColor(0xBB1A211B);
        canvas.drawRoundRect(rect, 16f, 16f, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2f);
        paint.setColor(0xFFD8C36A);
        canvas.drawRoundRect(rect, 16f, 16f, paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(0xFFF4EDDC);
        paint.setTextSize(size);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(
                text,
                rect.centerX(),
                rect.centerY() - (paint.ascent() + paint.descent()) / 2f,
                paint
        );
        paint.setTextAlign(Paint.Align.LEFT);
    }

    private void drawInventory(Canvas canvas, int width, int height) {
        paint.setColor(0xE6141815);
        canvas.drawRect(0f, 0f, width, height, paint);

        paint.setColor(0xFFF0E6D2);
        paint.setTextSize(30f);
        canvas.drawText("PLECAK", 60f, 60f, paint);

        String[] items = {
                "Woda x" + water,
                "Jedzenie x" + food,
                "Apteczka x" + medkits,
                "Amunicja x" + ammo
        };

        String[] hints = {
                "DOTKNIJ: +42 THI",
                "DOTKNIJ: +38 HUN",
                "DOTKNIJ: +55 HP",
                "Amunicja do pistoletu"
        };

        for (int i = 0; i < 4; i++) {
            float x = 60f + (i % 2) * 260f;
            float y = 100f + (i / 2) * 155f;

            paint.setColor(0xAA252D26);
            canvas.drawRoundRect(x, y, x + 220f, y + 120f, 16f, 16f, paint);

            paint.setColor(0xFFE7DECA);
            paint.setTextSize(20f);
            canvas.drawText(items[i], x + 18f, y + 42f, paint);

            paint.setTextSize(14f);
            canvas.drawText(hints[i], x + 18f, y + 75f, paint);
        }

        paint.setColor(0xFFC9C0AD);
        paint.setTextSize(16f);
        canvas.drawText("BAG: wróć do gry", 60f, height - 45f, paint);
    }

    private void drawDeath(Canvas canvas, int width, int height) {
        paint.setColor(0xCC070807);
        canvas.drawRect(0f, 0f, width, height, paint);

        paint.setColor(0xFFE8DCC8);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(48f);
        canvas.drawText("NIE ŻYJESZ", width / 2f, height / 2f - 30f, paint);

        paint.setTextSize(20f);
        canvas.drawText(
                "Dotknij ekran, aby rozpocząć nową wyprawę",
                width / 2f,
                height / 2f + 18f,
                paint
        );

        paint.setTextAlign(Paint.Align.LEFT);
    }

    private String formatTime() {
        int seconds = (int) (worldTime / DAY_LENGTH * 24f * 60f * 60f);
        int hours = (seconds / 3600) % 24;
        int minutes = (seconds / 60) % 60;
        return String.format(Locale.US, "DZIEŃ 01  %02d:%02d", hours, minutes);
    }

    private boolean isVisible(float x, float y, float margin) {
        float dx = x - cameraX;
        float dy = y - cameraY;

        return Math.abs(dx) < getWidth() * 0.5f + margin
                && Math.abs(dy) < getHeight() * 0.5f + margin;
    }

    private void lootNearestCrate() {
        LootCrate best = null;
        float bestDistance = 105f * 105f;

        for (LootCrate crate : crates) {
            if (crate.opened) continue;

            float dx = crate.x - playerX;
            float dy = crate.y - playerY;
            float distanceSquared = dx * dx + dy * dy;

            if (distanceSquared < bestDistance) {
                bestDistance = distanceSquared;
                best = crate;
            }
        }

        if (best == null) return;

        best.opened = true;

        switch (random.nextInt(4)) {
            case 0:
                water++;
                break;
            case 1:
                food++;
                break;
            case 2:
                ammo += 6 + random.nextInt(9);
                break;
            default:
                medkits++;
                break;
        }
    }

    private void useInventoryItem(int index) {
        if (index == 0 && water > 0) {
            water--;
            thirst = clamp(thirst + 42f, 0f, 100f);
        } else if (index == 1 && food > 0) {
            food--;
            hunger = clamp(hunger + 38f, 0f, 100f);
        } else if (index == 2 && medkits > 0 && playerHp < 100f) {
            medkits--;
            playerHp = clamp(playerHp + 55f, 0f, 100f);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        if (event.getActionMasked() == MotionEvent.ACTION_DOWN && dead) {
            initWorld();
            return true;
        }

        if (inventoryOpen) {
            if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                if (bagButton.contains(x, y)) {
                    inventoryOpen = false;
                    return true;
                }

                int index = inventoryIndexAt(x, y);
                if (index >= 0) {
                    useInventoryItem(index);
                }
            }
            return true;
        }

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (bagButton.contains(x, y)) {
                    inventoryOpen = true;
                    attackHeld = false;
                    fireHeld = false;
                    return true;
                }

                if (lootButton.contains(x, y)) {
                    lootNearestCrate();
                    return true;
                }

                if (fireButton.contains(x, y)) {
                    fireHeld = true;
                    return true;
                }

                if (attackButton.contains(x, y)) {
                    attackHeld = true;
                    return true;
                }

                if (x < getWidth() * 0.45f && y > getHeight() * 0.52f) {
                    joystickActive = true;
                    joystickBaseX = x;
                    joystickBaseY = y;
                    updateJoystick(x, y);
                    return true;
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (joystickActive) {
                    updateJoystick(x, y);
                    return true;
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                joystickActive = false;
                joystickX = 0f;
                joystickY = 0f;
                attackHeld = false;
                fireHeld = false;
                return true;
        }

        return true;
    }

    private void updateJoystick(float x, float y) {
        float dx = x - joystickBaseX;
        float dy = y - joystickBaseY;
        float length = (float) Math.sqrt(dx * dx + dy * dy);

        float max = 70f;
        if (length > max) {
            dx *= max / length;
            dy *= max / length;
        }

        joystickX = dx / max;
        joystickY = dy / max;
    }

    private int inventoryIndexAt(float x, float y) {
        if (x < 60f || x > 540f || y < 100f || y > 410f) return -1;

        int column = x < 320f ? 0 : 1;
        int row = y < 255f ? 0 : 1;
        return row * 2 + column;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private class Zombie {
        float x;
        float y;
        float wanderX;
        float wanderY;
        float wanderTimer;
        float attackTimer;

        Zombie(float x, float y) {
            this.x = x;
            this.y = y;
            chooseWanderDirection();
        }

        void chooseWanderDirection() {
            float dx = random.nextFloat() * 2f - 1f;
            float dy = random.nextFloat() * 2f - 1f;
            float length = (float) Math.sqrt(dx * dx + dy * dy);

            if (length < 0.01f) {
                wanderX = 1f;
                wanderY = 0f;
            } else {
                wanderX = dx / length;
                wanderY = dy / length;
            }

            wanderTimer = 1.5f + random.nextFloat() * 2.8f;
        }

        void update(float dt) {
            float dx = playerX - x;
            float dy = playerY - y;
            float distance = (float) Math.sqrt(dx * dx + dy * dy);

            float moveX;
            float moveY;
            float speed;

            if (distance < 700f) {
                moveX = dx;
                moveY = dy;

                float length = Math.max(1f, distance);
                moveX /= length;
                moveY /= length;
                speed = ZOMBIE_CHASE_SPEED;
            } else {
                wanderTimer -= dt;
                if (wanderTimer <= 0f) {
                    chooseWanderDirection();
                }
                moveX = wanderX;
                moveY = wanderY;
                speed = ZOMBIE_SPEED * 0.55f;
            }

            float nx = clamp(x + moveX * speed * dt, 25f, WORLD_W - 25f);
            float ny = clamp(y + moveY * speed * dt, 25f, WORLD_H - 25f);

            if (!hitsSolid(nx, y, 18f)) x = nx;
            if (!hitsSolid(x, ny, 18f)) y = ny;

            if (distance < 48f) {
                attackTimer -= dt;
                if (attackTimer <= 0f) {
                    attackTimer = 1.15f;
                    playerHp -= 7f;
                    damageFlash = 0.12f;
                }
            }
        }
    }

    private static class Tree {
        final float x;
        final float y;
        final float radius;

        Tree(float x, float y, float radius) {
            this.x = x;
            this.y = y;
            this.radius = radius;
        }
    }

    private static class House {
        final float x;
        final float y;
        final float width;
        final float height;

        House(float x, float y, float width, float height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        RectF rect() {
            return new RectF(x, y, x + width, y + height);
        }
    }

    private static class LootCrate {
        final float x;
        final float y;
        boolean opened;

        LootCrate(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }

    private class Bullet {
        float x;
        float y;
        float vx;
        float vy;
        float life = 1.5f;

        Bullet(float x, float y, float directionX, float directionY) {
            this.x = x;
            this.y = y;
            this.vx = directionX * BULLET_SPEED;
            this.vy = directionY * BULLET_SPEED;
        }

        void update(float dt) {
            x += vx * dt;
            y += vy * dt;
            life -= dt;
        }
    }
}
