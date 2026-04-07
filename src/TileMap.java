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
        // Far background parallax stars
        g2.setColor(new Color(100, 150, 255, 3));
        long seed = 42;
        for (int i = 0; i < 60; i++) {
            seed = (seed * 6364136223846793005L + 1) & 0xFFFFFFFFFFFFL;
            int sx = (int)((seed % 1600) + (cam.x * 0.1) % 1600) % (W + 50) - 25;
            seed = (seed * 6364136223846793005L + 1) & 0xFFFFFFFFFFFFL;
            int sy = (int)(seed % H);
            seed = (seed * 6364136223846793005L + 1) & 0xFFFFFFFFFFFFL;
            int sz = 1 + (int)(seed % 2);
            g2.fillOval(sx, sy, sz, sz);
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
        Color base = t.color != null ? t.color : Color.decode("#1a2040");
        g2.setColor(base);
        g2.fillRect(x, y, Game.GS, Game.GS);

        // Top highlight
        g2.setColor(new Color(0, 120, 255, 25));
        g2.fillRect(x, y, Game.GS, 2);

        // Grid line
        g2.setColor(new Color(255, 255, 255, 5));
        g2.drawRect(x, y, Game.GS, Game.GS);
    }

    void drawPlatformBlock(Graphics2D g2, int x, int y) {
        g2.setColor(Color.decode("#0d3060"));
        g2.fillRect(x, y, Game.GS, 8);
        g2.setColor(new Color(0, 150, 255, 40));
        g2.fillRect(x, y, Game.GS, 2);
        g2.setColor(new Color(255, 255, 255, 8));
        g2.fillRect(x, y + 2, Game.GS, 1);
    }

    void drawSpike(Graphics2D g2, int x, int y) {
        g2.setColor(Color.decode("#ff4455"));
        int s = Game.GS / 3;
        for (int i = 0; i < 3; i++) {
            int sx = x + i * s;
            g2.fillPolygon(new int[]{sx, sx + s/2, sx + s}, new int[]{y + s, y, y + s}, 3);
        }
    }

    void drawExit(Graphics2D g2, int x, int y) {
        int pulse = 60 + (int)(Math.sin(System.currentTimeMillis() * 0.004) * 30);
        g2.setColor(new Color(0, 255, 200, pulse));
        g2.fillRect(x + 4, y + 2, Game.GS - 8, Game.GS - 2);
        g2.setColor(new Color(0, 255, 200, pulse / 2));
        g2.fillOval(x + 6, y + 6, Game.GS - 12, Game.GS - 12);
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
