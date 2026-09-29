package pl.tupsonik.pacjack;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.*;
import android.view.MotionEvent;
import android.view.View;
import java.util.*;

public class GameView extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int N = 13;
    private final int[][] maze = {
        {1,1,1,1,1,1,1,1,1,1,1,1,1},
        {1,0,0,0,1,0,0,0,1,0,0,0,1},
        {1,0,1,0,1,0,1,0,1,0,1,0,1},
        {1,0,1,0,0,0,1,0,0,0,1,0,1},
        {1,0,1,1,1,0,1,1,1,0,1,0,1},
        {1,0,0,0,0,0,0,0,0,0,0,0,1},
        {1,1,1,0,1,1,1,0,1,1,1,0,1},
        {1,0,0,0,1,0,0,0,1,0,0,0,1},
        {1,0,1,0,1,0,1,0,1,0,1,0,1},
        {1,0,1,0,0,0,1,0,0,0,1,0,1},
        {1,0,1,1,1,0,1,1,1,0,1,0,1},
        {1,0,0,0,0,0,0,0,0,0,0,0,1},
        {1,1,1,1,1,1,1,1,1,1,1,1,1}
    };

    private final boolean[][] dots = new boolean[N][N];
    private final ArrayList<int[]> ghosts = new ArrayList<>();
    private final Random random = new Random();
    private final SharedPreferences prefs;

    private long coins;
    private int px = 1, py = 1, score, level = 1, bet = 10;
    private boolean gameOver = false, won = false, paused = false, menu = false;
    private float cell, ox, oy;
    private long lastGhostMove = 0;
    private String playerDir = "right";

    public GameView(Context c) {
        super(c);
        prefs = c.getSharedPreferences("pacjack", 0);
        coins = prefs.getLong("coins", 556);
        reset();
        setFocusable(true);
    }

    private void save() {
        prefs.edit().putLong("coins", coins).apply();
    }

    private void reset() {
        px = 1; py = 1; score = 0; level = 1;
        gameOver = false; won = false; paused = false; menu = false;
        playerDir = "right";
        ghosts.clear();
        ghosts.add(new int[]{11,11});
        ghosts.add(new int[]{11,1});
        ghosts.add(new int[]{1,11});
        for (int y=0;y<N;y++) for (int x=0;x<N;x++) dots[y][x] = maze[y][x] == 0;
        invalidate();
    }

    private void text(Canvas c, String s, float x, float y, float size, int color) {
        p.setTypeface(Typeface.create("sans", Typeface.BOLD));
        p.setTextSize(size); p.setColor(color); p.setTextAlign(Paint.Align.CENTER);
        c.drawText(s, x, y, p);
    }

    private boolean open(int x, int y) {
        return x >= 0 && x < N && y >= 0 && y < N && maze[y][x] == 0;
    }

    private void moveGhosts() {
        if (paused || menu || gameOver) return;
        long now = System.currentTimeMillis();
        int interval = Math.max(220, 460 - (level - 1) * 30);
        if (now - lastGhostMove < interval) return;
        lastGhostMove = now;

        for (int i=0;i<ghosts.size();i++) {
            int[] g = ghosts.get(i);
            ArrayList<int[]> options = new ArrayList<>();
            int[][] d = {{1,0},{-1,0},{0,1},{0,-1}};
            for (int[] v : d) if (open(g[0]+v[0], g[1]+v[1])) options.add(v);
            if (options.isEmpty()) continue;

            int[] chosen = null;
            int best = Integer.MAX_VALUE;
            for (int[] v : options) {
                int nx = g[0]+v[0], ny = g[1]+v[1];
                int dist = Math.abs(nx-px) + Math.abs(ny-py);
                if (dist < best) { best = dist; chosen = v; }
            }

            // Different ghosts occasionally choose a different legal route,
            // so they don't move as one synchronized block.
            if (options.size() > 1 && (i + px + py + level) % 5 == 0) {
                chosen = options.get(random.nextInt(options.size()));
            }
            g[0] += chosen[0];
            g[1] += chosen[1];
        }

        for (int[] g : ghosts) {
            if (g[0] == px && g[1] == py) {
                gameOver = true;
                won = false;
                coins = Math.max(0, coins - bet);
                save();
            }
        }
        invalidate();
    }

    private void move(int dx, int dy) {
        if (gameOver) { reset(); return; }
        if (paused || menu) return;

        if (dx > 0) playerDir = "right";
        if (dx < 0) playerDir = "left";
        if (dy > 0) playerDir = "down";
        if (dy < 0) playerDir = "up";

        int nx = px + dx, ny = py + dy;
        if (!open(nx, ny)) return;

        px = nx; py = ny;
        if (dots[py][px]) {
            dots[py][px] = false;
            score += 10;
            coins += 2;
            save();
        }

        for (int[] g : ghosts) {
            if (g[0] == px && g[1] == py) {
                gameOver = true; won = false;
                coins = Math.max(0, coins - bet);
                save();
            }
        }

        if (px == 11 && py == 11) {
            coins += bet * (level + 1);
            level++;
            won = true;
            gameOver = true;
            save();
        }
        invalidate();
    }

    private void drawArrow(Canvas c, float x, float y, String arrow) {
        text(c, arrow, x, y, 30, Color.WHITE);
    }

    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        int W = getWidth(), H = getHeight();
        c.drawColor(Color.rgb(7,8,18));

        cell = Math.min((W-24f)/N, (H-245f)/N);
        ox = (W-cell*N)/2f;
        oy = 128;

        text(c, "PAC JACK", W/2f, 36, 25, Color.WHITE);
        text(c, "🪙 " + coins, W/2f, 67, 17, Color.rgb(255,210,45));
        text(c, "LEVEL " + level + "   •   SCORE " + score, W/2f, 94, 11, Color.LTGRAY);
        text(c, "STAKE  " + bet + "   •   WIN x" + (level+1), W/2f, 112, 11, Color.rgb(100,220,255));

        for (int y=0;y<N;y++) for (int x=0;x<N;x++) {
            float l=ox+x*cell, t=oy+y*cell;
            if (maze[y][x]==1) {
                p.setColor(Color.rgb(36,43,91));
                c.drawRoundRect(l+1,t+1,l+cell-1,t+cell-1,7,7,p);
            } else if (dots[y][x]) {
                p.setColor(Color.rgb(255,214,70));
                c.drawCircle(l+cell/2,t+cell/2,3,p);
            }
        }

        // Pac-Man with a simple animated mouth.
        p.setColor(Color.rgb(255,211,35));
        c.drawCircle(ox+(px+.5f)*cell, oy+(py+.5f)*cell, cell*.34f, p);
        p.setColor(Color.rgb(7,8,18));
        float mouth = (System.currentTimeMillis()/140)%2==0 ? cell*.30f : cell*.08f;
        Path path = new Path();
        path.moveTo(ox+(px+.5f)*cell, oy+(py+.5f)*cell);
        path.lineTo(ox+(px+.5f)*cell + (playerDir.equals("left")?-mouth:playerDir.equals("right")?mouth:0),
                oy+(py+.5f)*cell + (playerDir.equals("up")?-mouth:playerDir.equals("down")?mouth:0));
        path.close();
        c.drawPath(path,p);

        int[] ghostColors = {Color.rgb(255,67,90), Color.rgb(255,92,138), Color.rgb(255,112,67)};
        for (int i=0;i<ghosts.size();i++) {
            int[] g=ghosts.get(i);
            p.setColor(ghostColors[i%ghostColors.length]);
            c.drawCircle(ox+(g[0]+.5f)*cell, oy+(g[1]+.5f)*cell, cell*.30f,p);
            p.setColor(Color.WHITE);
            c.drawCircle(ox+(g[0]+.40f)*cell,oy+(g[1]+.43f)*cell,cell*.055f,p);
            c.drawCircle(ox+(g[0]+.60f)*cell,oy+(g[1]+.43f)*cell,cell*.055f,p);
            p.setColor(Color.rgb(35,45,95));
            c.drawCircle(ox+(g[0]+.40f)*cell,oy+(g[1]+.43f)*cell,cell*.025f,p);
            c.drawCircle(ox+(g[0]+.60f)*cell,oy+(g[1]+.43f)*cell,cell*.025f,p);
        }

        // Bottom controls: real clickable zones, not just labels.
        p.setColor(Color.rgb(16,19,34));
        c.drawRoundRect(10,H-112,W-10,H-10,22,22,p);
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(1);
        p.setColor(Color.rgb(42,47,73));
        c.drawRoundRect(10,H-112,W-10,H-10,22,22,p);
        p.setStyle(Paint.Style.FILL);

        drawArrow(c,W*.17f,H-58,"←");
        drawArrow(c,W*.34f,H-80,"↑");
        drawArrow(c,W*.34f,H-32,"↓");
        drawArrow(c,W*.51f,H-58,"→");
        text(c, "BET -", W*.68f,H-58,10,Color.rgb(255,110,110));
        text(c, "BET +", W*.84f,H-58,10,Color.rgb(100,230,150));
        text(c, "☰", W*.95f,H-58,21,Color.WHITE);

        if (paused && !menu && !gameOver) {
            p.setColor(Color.argb(210,0,0,0)); c.drawRect(0,0,W,H,p);
            text(c,"PAUZA",W/2f,H/2f-30,28,Color.WHITE);
            text(c,"DOTKNIJ ABY WZNOWIĆ",W/2f,H/2f+10,12,Color.rgb(170,180,200));
        }

        if (gameOver) {
            p.setColor(Color.argb(235,0,0,0)); c.drawRect(0,0,W,H,p);
            text(c,won?"JACKPOT!":"BUST!",W/2f,H/2f-35,34,won?Color.rgb(255,215,45):Color.rgb(255,80,90));
            text(c,won?("+"+(bet*(level+1))+" COINS"):"DOTKNIJ ABY ZAGRAĆ PONOWNIE",W/2f,H/2f+5,14,Color.WHITE);
        }

        if (menu) {
            p.setColor(Color.argb(220,4,5,12)); c.drawRect(0,0,W,H,p);
            p.setColor(Color.rgb(17,20,36)); c.drawRoundRect(28,145,W-28,H-145,24,24,p);
            text(c,"GAME MENU",W/2f,190,11,Color.rgb(255,210,70));
            text(c,"PAC JACK",W/2f,225,28,Color.WHITE);
            text(c,"NOWA GRA",W/2f,300,16,Color.WHITE);
            text(c,paused?"WZNÓW":"PAUZA",W/2f,355,16,Color.WHITE);
            text(c,"ZAMKNIJ MENU",W/2f,410,16,Color.WHITE);
        }

        if (!paused && !menu && !gameOver) {
            moveGhosts();
            postInvalidateDelayed(80);
        }
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        if (e.getAction() != MotionEvent.ACTION_UP) return true;
        float x=e.getX(), y=e.getY();
        int W=getWidth(), H=getHeight();

        if (menu) {
            if (y>=265 && y<=330) { reset(); return true; }
            if (y>330 && y<=385) { paused=!paused; menu=false; invalidate(); return true; }
            if (y>385 && y<=445) { menu=false; invalidate(); return true; }
            return true;
        }

        if (gameOver) { reset(); return true; }

        if (paused) { paused=false; invalidate(); return true; }

        if (y > H-125) {
            if (x < W*.27f) move(-1,0);
            else if (x < W*.40f && y < H-56) move(0,-1);
            else if (x < W*.40f) move(0,1);
            else if (x < W*.58f) move(1,0);
            else if (x < W*.75f) { bet=Math.max(10,bet-10); invalidate(); }
            else if (x < W*.91f) { bet=Math.min(100,bet+10); invalidate(); }
            else { menu=true; invalidate(); }
            return true;
        }

        if (y>oy && y<oy+cell*N) {
            float dx=x-(ox+(px+.5f)*cell), dy=y-(oy+(py+.5f)*cell);
            if (Math.abs(dx)>Math.abs(dy)) move(dx>0?1:-1,0);
            else move(0,dy>0?1:-1);
        }
        return true;
    }
}
