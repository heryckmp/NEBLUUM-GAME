import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

enum TileType {
    EMPTY, SOLID, PLATFORM, SPIKE, HAZARD, DECO, EXIT
}

class Tile {
    TileType type;
    Color color;

    Tile(TileType type) { this.type = type; this.color = null; }
    Tile(TileType type, Color color) { this.type = type; this.color = color; }

    boolean isSolid() { return type == TileType.SOLID; }
    boolean isPlatform() { return type == TileType.PLATFORM; }
    boolean isSpike() { return type == TileType.SPIKE; }
    boolean isHazard() { return type == TileType.HAZARD; }
    boolean isExit() { return type == TileType.EXIT; }
}

class TileMap {
    protected int cols, rows;
    protected Tile[][] tiles;
    protected CopyOnWriteArrayList<Particle> particles = new CopyOnWriteArrayList<>();
    protected CopyOnWriteArrayList<Bomb> bombs = new CopyOnWriteArrayList<>();

    public TileMap(int cols, int rows) {
        this.cols = cols;
        this.rows = rows;
        tiles = new Tile[rows][cols];
        for (int y = 0; y < rows; y++)
            for (int x = 0; x < cols; x++)
                tiles[y][x] = new Tile(TileType.EMPTY);
    }

    public void setTile(int x, int y, TileType type) {
        if (x >= 0 && x < cols && y >= 0 && y < rows)
            tiles[y][x] = new Tile(type);
    }

    public void setTile(int x, int y, Tile t) {
        if (x >= 0 && x < cols && y >= 0 && y < rows)
            tiles[y][x] = t;
    }

    public Tile getTile(int x, int y) {
        if (x < 0 || x >= cols || y < 0 || y >= rows) return new Tile(TileType.SOLID);
        return tiles[y][x];
    }

    public void fillRect(int x1, int y1, int x2, int y2, TileType type) {
        for (int y = y1; y <= y2; y++)
            for (int x = x1; x <= x2; x++)
                setTile(x, y, type);
    }

    public int getCols() { return cols; }
    public int getRows() { return rows; }

    public void update(Player player) {
        particles.removeIf(p -> !p.update());

        bombs.removeIf(b -> {
            b.update(this);
            if (b.timer <= 0) {
                b.explode(this, player);
                return true;
            }
            return false;
        });
    }

    public void spawnParticles(float x, float y, Color color, int count, Random rand) {
        for (int i = 0; i < count; i++) {
            particles.add(new Particle(x, y, color, rand));
        }
    }

    public void spawnBomb(float x, float y, float vx) {
        bombs.add(new Bomb(x, y, vx));
    }

    void drawBackground(Graphics2D g2, Camera cam, int W, int H) {
        long t = System.currentTimeMillis();

        // Void gradient base
        g2.setPaint(new GradientPaint(0, 0, new Color(2, 1, 14), 0, H, new Color(6, 3, 28)));
        g2.fillRect(0, 0, W, H);
        g2.setPaint(null);

        // Layer 1 — tiny background stars
        long seed = 42L;
        for (int i = 0; i < 130; i++) {
            seed = (seed * 6364136223846793005L + 1442695040888963407L) & 0xFFFFFFFFFFFFL;
            int sx = (int)(((seed % (W + 200)) + (long)(cam.x * 0.03)) % (W + 200));
            seed = (seed * 6364136223846793005L + 1442695040888963407L) & 0xFFFFFFFFFFFFL;
            int sy = (int)(seed % H);
            int b = 60 + (int)(Math.sin(t * 0.0008 + i * 0.3) * 30) + (int)(seed % 60);
            b = Math.max(30, Math.min(180, b));
            g2.setColor(new Color(b, b, Math.min(255, b + 50), 170));
            g2.fillRect(sx, sy, 1, 1);
        }
        // Layer 2 — medium blue-tinted stars
        seed = 137L;
        for (int i = 0; i < 55; i++) {
            seed = (seed * 6364136223846793005L + 1442695040888963407L) & 0xFFFFFFFFFFFFL;
            int sx = (int)(((seed % (W + 200)) + (long)(cam.x * 0.08)) % (W + 200));
            seed = (seed * 6364136223846793005L + 1442695040888963407L) & 0xFFFFFFFFFFFFL;
            int sy = (int)(seed % (H - 80));
            int alpha = Math.max(60, Math.min(220, 130 + (int)(Math.sin(t * 0.0015 + i * 0.7) * 45)));
            g2.setColor(new Color(190, 215, 255, alpha));
            if ((seed % 5) == 0) g2.fillOval(sx - 1, sy - 1, 3, 3);
            else g2.fillRect(sx, sy, 2, 2);
        }
        // Layer 3 — bright foreground stars with cross flare
        seed = 521L;
        for (int i = 0; i < 22; i++) {
            seed = (seed * 6364136223846793005L + 1442695040888963407L) & 0xFFFFFFFFFFFFL;
            int sx = (int)(((seed % (W + 200)) + (long)(cam.x * 0.18)) % (W + 200));
            seed = (seed * 6364136223846793005L + 1442695040888963407L) & 0xFFFFFFFFFFFFL;
            int sy = (int)(seed % (H - 100));
            int alpha = Math.max(80, Math.min(240, 160 + (int)(Math.sin(t * 0.002 + i * 1.1) * 55)));
            g2.setColor(new Color(240, 245, 255, alpha));
            g2.fillOval(sx - 1, sy - 1, 3, 3);
            g2.setColor(new Color(200, 220, 255, alpha / 3));
            g2.drawLine(sx - 4, sy, sx + 4, sy);
            g2.drawLine(sx, sy - 4, sx, sy + 4);
        }
        // Nebula gas clouds — volumetric layered
        int[][] nebDefs = { {80,20,180}, {180,20,80}, {0,60,160}, {100,0,150}, {20,100,120}, {0,80,60} };
        seed = 777L;
        for (int i = 0; i < 6; i++) {
            seed = (seed * 6364136223846793005L + 1442695040888963407L) & 0xFFFFFFFFFFFFL;
            int nx = (int)(((seed % (long)(W * 3)) - (long)(cam.x * 0.06)) % (W + 400) - 200);
            seed = (seed * 6364136223846793005L + 1442695040888963407L) & 0xFFFFFFFFFFFFL;
            int ny = (int)(seed % (H - 80)) - 40;
            seed = (seed * 6364136223846793005L + 1442695040888963407L) & 0xFFFFFFFFFFFFL;
            int nw = 120 + (int)(seed % 200); int nh = nw / 2 + (int)(seed % 50);
            int[] nc = nebDefs[i % nebDefs.length];
            for (int layer = 0; layer < 3; layer++) {
                int lw = nw - layer * 25, lh = nh - layer * 12;
                g2.setColor(new Color(nc[0], nc[1], nc[2], 14 + layer * 5));
                g2.fillOval(nx + layer * 12, ny + layer * 6, Math.max(10, lw), Math.max(5, lh));
            }
        }
        // Galactic aurora bands on horizon
        for (int band = 0; band < 14; band++) {
            float wave = (float)(Math.sin(t * 0.001 + band * 0.5) * 12);
            int ay = H - 90 + band * 5 + (int)wave;
            int alpha = Math.max(0, 20 - band * 2);
            if (alpha > 0) {
                Color ac = band % 3 == 0 ? new Color(100, 30, 220, alpha)
                         : band % 3 == 1 ? new Color(0, 180, 120, alpha)
                         :                 new Color(180, 60, 255, alpha);
                g2.setColor(ac);
                g2.fillRect(0, ay, W, 6);
            }
        }
        // Shooting star
        long shootCycle = (t / 5000) % 9;
        if (shootCycle < 2) {
            float prog = (t % 5000) / 1000f;
            if (prog < 1f) {
                int stx = (int)(prog * (W + 150));
                int sty = (int)(40 + shootCycle * 55);
                for (int seg = 0; seg < 10; seg++) {
                    int alpha = Math.max(0, 170 - seg * 18);
                    g2.setColor(new Color(220, 230, 255, alpha));
                    g2.fillRect(stx - seg * 13, sty + seg * 4, 2, 1);
                }
                g2.setColor(new Color(255, 255, 255, 220));
                g2.fillOval(stx - 2, sty - 2, 4, 4);
            }
        }
    }

    void drawTiles(Graphics2D g2, Camera cam) {
        int tx1 = (int)(cam.x / Game.GS) - 1;
        int tx2 = (int)((cam.x + cam.W) / Game.GS) + 1;
        int ty1 = (int)(cam.y / Game.GS) - 1;
        int ty2 = (int)((cam.y + cam.H) / Game.GS) + 1;

        tx1 = Math.max(0, tx1);
        ty1 = Math.max(0, ty1);
        tx2 = Math.min(cols - 1, tx2);
        ty2 = Math.min(rows - 1, ty2);

        for (int ty = ty1; ty <= ty2; ty++) {
            for (int tx = tx1; tx <= tx2; tx++) {
                Tile t = tiles[ty][tx];
                int px = tx * Game.GS;
                int py = ty * Game.GS;

                switch (t.type) {
                    case SOLID -> drawSolidBlock(g2, px, py, t);
                    case PLATFORM -> drawPlatformBlock(g2, px, py);
                    case SPIKE -> drawSpike(g2, px, py);
                    case EXIT -> drawExit(g2, px, py);
                }
            }
        }
    }

    void drawSolidBlock(Graphics2D g2, int x, int y, Tile t) {
        Color base = t.color != null ? t.color : new Color(16, 12, 30);
        Color top = new Color(Math.min(255, base.getRed()+18), Math.min(255, base.getGreen()+10), Math.min(255, base.getBlue()+28));
        g2.setPaint(new GradientPaint(x, y, top, x, y + Game.GS, base));
        g2.fillRect(x, y, Game.GS, Game.GS);
        g2.setPaint(null);
        // Mineral crystal vein
        if ((x / Game.GS + y / Game.GS) % 2 == 0) {
            g2.setColor(new Color(90, 40, 180, 28));
            g2.drawLine(x + 4, y + 9, x + Game.GS - 7, y + 15);
        } else {
            g2.setColor(new Color(40, 60, 180, 22));
            g2.drawLine(x + 9, y + 4, x + 15, y + Game.GS - 8);
        }
        // Crystal glint top edge
        g2.setColor(new Color(150, 100, 255, 60));
        g2.fillRect(x, y, Game.GS, 2);
        g2.setColor(new Color(210, 190, 255, 18));
        g2.fillRect(x, y, Game.GS, 1);
        // Depth shadow left
        g2.setColor(new Color(0, 0, 0, 40));
        g2.fillRect(x, y, 2, Game.GS);
        // Subtle grid
        g2.setColor(new Color(255, 255, 255, 5));
        g2.drawRect(x, y, Game.GS, Game.GS);
        // Crater detail
        if ((x / Game.GS * 3 + y / Game.GS * 7) % 5 == 0) {
            g2.setColor(new Color(0, 0, 0, 38));
            g2.fillOval(x + 8, y + 10, 13, 8);
            g2.setColor(new Color(160, 130, 255, 14));
            g2.drawOval(x + 8, y + 10, 13, 8);
        }
    }

    void drawPlatformBlock(Graphics2D g2, int x, int y) {
        long t2 = System.currentTimeMillis();
        // Floating asteroid fragment
        g2.setPaint(new GradientPaint(x, y, new Color(28, 20, 52), x, y + 10, new Color(12, 8, 28)));
        g2.fillRoundRect(x, y, Game.GS, 10, 4, 4);
        g2.setPaint(null);
        // Crystal mineral top glow (nebula cyan)
        int glowA = 100 + (int)(Math.sin(t2 * 0.002 + x * 0.08) * 45);
        g2.setColor(new Color(0, 220, 255, glowA));
        g2.fillRect(x, y, Game.GS, 2);
        // Sub-surface glow
        g2.setColor(new Color(80, 140, 255, 16));
        g2.fillRect(x + 2, y + 2, Game.GS - 4, 4);
        // Micro crystal glints
        for (int i = 0; i < 3; i++) {
            int gx = x + 5 + i * (Game.GS / 3);
            int ga = 55 + (int)(Math.sin(t2 * 0.003 + i * 2.1) * 38);
            g2.setColor(new Color(180, 220, 255, ga));
            g2.fillRect(gx, y, 2, 2);
        }
        // Edge dust fade
        g2.setColor(new Color(100, 80, 200, 10));
        g2.fillRect(x, y, 3, 10);
        g2.fillRect(x + Game.GS - 3, y, 3, 10);
    }

    void drawSpike(Graphics2D g2, int x, int y) {
        int s = Game.GS / 3;
        long t2 = System.currentTimeMillis();
        int pulse = (int)(Math.sin(t2 * 0.006 + x * 0.08) * 32);
        // Alien crystal base rock
        g2.setColor(new Color(18, 8, 34));
        g2.fillRect(x, y + s - 2, Game.GS, 5);
        // Danger aura (violet)
        g2.setColor(new Color(160, 0, 255, 10 + pulse / 4));
        g2.fillRect(x, y - 6, Game.GS, s + 8);
        for (int i = 0; i < 3; i++) {
            int sx = x + i * s;
            // Crystal shadow
            g2.setColor(new Color(40, 0, 80, 100));
            g2.fillPolygon(new int[]{sx+2, sx+s/2+2, sx+s+2}, new int[]{y+s+2, y+2, y+s+2}, 3);
            // Crystal body — violet to rose
            g2.setPaint(new GradientPaint(sx+s/2, y, new Color(210, 60+pulse, 255), sx+s/2, y+s, new Color(80, 0, 150)));
            g2.fillPolygon(new int[]{sx, sx+s/2, sx+s}, new int[]{y+s, y, y+s}, 3);
            g2.setPaint(null);
            // Crystal facet
            g2.setColor(new Color(230, 190, 255, 75));
            g2.drawLine(sx+s/2, y, sx+s/4, y+s/2);
            // Glowing tip
            g2.setColor(new Color(255, 210, 255, 175 + pulse));
            g2.fillOval(sx+s/2-2, y-1, 4, 4);
        }
    }

    void drawExit(Graphics2D g2, int x, int y) {
        long t2 = System.currentTimeMillis();
        double angle = t2 * 0.002;
        int cx = x + Game.GS / 2, cy = y + Game.GS / 2;
        int pulse = (int)(Math.sin(t2 * 0.004) * 25);
        // Gravitational lens glow
        g2.setColor(new Color(120, 0, 255, 10 + pulse / 4));
        g2.fillOval(x - 10, y - 10, Game.GS + 20, Game.GS + 20);
        // Einstein ring
        g2.setStroke(new BasicStroke(2.5f));
        g2.setColor(new Color(180, 100, 255, 80 + pulse));
        g2.drawOval(x - 3, y - 3, Game.GS + 6, Game.GS + 6);
        g2.setStroke(new BasicStroke(1f));
        // Hawking radiation arcs
        g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int ray = 0; ray < 8; ray++) {
            double a = angle + ray * Math.PI / 4;
            int rx1 = cx + (int)(Math.cos(a) * 10);
            int ry1 = cy + (int)(Math.sin(a) * 10);
            int rx2 = cx + (int)(Math.cos(a) * 16);
            int ry2 = cy + (int)(Math.sin(a) * 16);
            int rayA = Math.max(20, 60 + (int)(Math.sin(a * 2 + t2 * 0.003) * 40));
            g2.setColor(new Color(200, 150, 255, rayA));
            g2.drawLine(rx1, ry1, rx2, ry2);
        }
        g2.setStroke(new BasicStroke(1f));
        // Wormhole distortion rings
        int[] ringR = {14, 11, 8, 5, 2};
        Color[] ringC = {new Color(200,150,255,40+pulse), new Color(140,60,255,80), new Color(80,0,180,140), new Color(20,0,60,200), Color.BLACK};
        for (int ring = 0; ring < 5; ring++) {
            int r = ringR[ring];
            g2.setColor(ringC[ring]);
            g2.fillOval(cx - r, cy - r, r * 2, r * 2);
        }
        // Central pinpoint
        g2.setColor(new Color(255, 255, 255, 160 + pulse));
        g2.fillOval(cx - 1, cy - 1, 3, 3);
        g2.setColor(new Color(180, 120, 255, 130 + pulse));
        g2.setFont(new Font("Monospaced", Font.BOLD, 7));
        g2.drawString("EXIT", x + 2, y + Game.GS - 3);
    }

    void drawParticles(Graphics2D g2) {
        for (Particle p : particles) {
            g2.setColor(p.color);
            g2.fillOval((int)p.x - p.size / 2, (int)p.y - p.size / 2, p.size, p.size);
        }
    }

    void destroyTile(int tx, int ty) {
        if (tx >= 0 && tx < cols && ty >= 0 && ty < rows) {
            tiles[ty][tx] = new Tile(TileType.EMPTY);
        }
    }
}

// --- Particle ---
class Particle {
    float x, y, vx, vy;
    Color color;
    int life, maxLife;
    int size;

    Particle(float x, float y, Color color, Random rand) {
        this.x = x; this.y = y; this.color = color;
        this.vx = (float)(rand.nextDouble() * 5 - 2.5);
        this.vy = (float)(rand.nextDouble() * -5 - 1);
        this.life = 20 + rand.nextInt(30);
        this.maxLife = life;
        this.size = 2 + rand.nextInt(4);
    }

    boolean update() {
        life--;
        vy += 0.15;
        x += vx;
        y += vy;
        return life > 0;
    }
}

// --- Bomb ---
class Bomb {
    float x, y, vx;
    int timer = 90;
    static final int RADIUS = 3; // tiles

    Bomb(float x, float y, float vx) {
        this.x = x; this.y = y; this.vx = vx;
    }

    void update(TileMap map) {
        timer--;
        x += vx;

        // Simple gravity
        float belowY = y + 8;
        int ty = (int)(belowY / Game.GS);
        int tx = (int)((x + 5) / Game.GS);
        Tile t = map.getTile(tx, ty);
        if (t != null && t.isSolid()) {
            vx = 0;
            y = ty * Game.GS - 8;
        }
    }

    void explode(TileMap map, Player player) {
        map.spawnParticles(x, y, new Color(255, 150, 50, 200), 30, new Random());
        map.spawnParticles(x, y, Color.decode("#ffaa00"), 15, new Random());
        map.spawnParticles(x, y, Color.RED, 10, new Random());

        // Destroy tiles in range
        int ct = (int)(x / Game.GS);
        int rt = (int)(y / Game.GS);
        for (int dy = -RADIUS; dy <= RADIUS; dy++) {
            for (int dx = -RADIUS; dx <= RADIUS; dx++) {
                if (dx*dx + dy*dy <= RADIUS*RADIUS + 2) {
                    Tile t = map.getTile(ct + dx, rt + dy);
                    if (t.isSolid()) map.destroyTile(ct + dx, rt + dy);
                }
            }
        }

        // Damage enemies
    }
}
