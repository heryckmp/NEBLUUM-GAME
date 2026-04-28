import java.awt.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

class Room extends TileMap {
    java.util.List<Enemy> enemies = new ArrayList<>();
    java.util.List<Collectible> collectibles = new ArrayList<>();
    java.util.List<EnemyProjectile> projectiles = new ArrayList<>();

    public Room(int cols, int rows) {
        super(cols, rows);
    }

    void addEnemy(Enemy e) { enemies.add(e); }
    void addCollectible(float x, float y, int type) { collectibles.add(new Collectible(x, y, type)); }
    
    public void addProjectile(float x, float y, float vx) {
        projectiles.add(new EnemyProjectile(x, y, vx, 0));
    }

    void update(Player player, SoundPlayer sound, Random rand) {
        super.update(player);
        double dt = 1.0; // Valor padrão para o update interno do Room

        for (int i = 0; i < enemies.size(); i++) {
            Enemy e = enemies.get(i);
            e.update(this, player, dt);

            if (e.state != 2 && e.active && e.getBounds().intersects(player.getBounds())) {
                player.takeDamage(e.contactDmg);
                if (player.hp > 0) sound.playHurt();
                float dx = player.x - e.x;
                player.vx = dx > 0 ? 5 : -5;
                player.vy = -4;
            }

            if (player.attacking && e.state != 2) {
                Rectangle ab = player.getAttackBox();
                if (ab.intersects(e.getBounds())) {
                    e.takeDamage(player.attackDamage, player.attackKnockbackX * (player.facingRight ? 1 : -1), player.attackKnockbackY);
                    spawnParticles(e.x + e.w / 2, e.y + e.h / 2, new Color(255, 200, 100, 180), 5, rand);
                    sound.playHit();
                    player.attackCooldown = 12;
                }
            }
        }

        for (int i = enemies.size() - 1; i >= 0; i--) {
            Enemy e = enemies.get(i);
            if (e.isDead()) {
                player.score += e.points;
                if (rand.nextInt(4) == 0) {
                    collectibles.add(new Collectible(e.x + e.w / 2, e.y, 1));
                }
                spawnParticles(e.x + e.w / 2, e.y + e.h / 2, Color.decode("#ff8844"), 12, rand);
                enemies.remove(i);
            }
        }

        for (int i = collectibles.size() - 1; i >= 0; i--) {
            Collectible c = collectibles.get(i);
            c.update(this, dt);
            if (!c.collected && c.getBounds().intersects(player.getBounds())) {
                c.collected = true;
                sound.playCoin();
                if (c.type == 0) { player.heal(3); spawnParticles(c.x, c.y, Color.GREEN, 8, rand); }
                else if (c.type == 1) { player.coins++; player.score += 10; }
                else if (c.type == 2) { player.shield = Math.min(player.shield + 5, 10); }
                else if (c.type == 3) { player.healthPotions++; }
                else if (c.type == 4) { player.bombs++; }
                else if (c.type == 5) { player.attackDamage++; }
                spawnParticles(c.x, c.y, c.type == 0 ? Color.GREEN : Color.YELLOW, 6, rand);
                collectibles.remove(i);
            }
        }

        for (int i = projectiles.size() - 1; i >= 0; i--) {
            EnemyProjectile p = projectiles.get(i);
            p.update(this, dt);
            if (p.dead) {
                spawnParticles(p.x, p.y, Color.decode("#ff4444"), 4, rand);
                projectiles.remove(i);
            } else if (p.getBounds().intersects(player.getBounds())) {
                player.takeDamage(p.damage);
                projectiles.remove(i);
            }
        }
    }

    boolean isComplete() {
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                Tile t = tiles[y][x];
                if (t.isExit()) {
                    return enemies.isEmpty();
                }
            }
        }
        return false;
    }

    // Theme colors for this room
    private Color themeBase = Color.decode("#0a0e1a");
    private Color themePlatform = Color.decode("#0d1830");
    private Color themeAccent = Color.decode("#004488");

    void drawBackground(Graphics2D g2, Camera cam, int W, int H) {
        super.drawBackground(g2, cam, W, H);
        long t = System.currentTimeMillis();

        // Gas giant planet (back-right)
        int planetOffX = (int)(cam.x * 0.04) % (W + 600);
        int pX = W - 220 - planetOffX % W; int pY = 30; int pR = 115;
        int pCX = pX + pR, pCY = pY + pR;
        // Planet body
        g2.setPaint(new GradientPaint(pX, pY, new Color(35, 18, 85, 70), pX + pR, pY + pR * 2, new Color(12, 5, 42, 90)));
        g2.fillOval(pX, pY, pR * 2, pR * 2);
        g2.setPaint(null);
        // Atmospheric bands
        for (int band = 0; band < 6; band++) {
            int by = pY + 22 + band * 16;
            int distFromCenter = by + 5 - pCY;
            int halfChord = (int)Math.sqrt(Math.max(0, (long)pR * pR - (long)distFromCenter * distFromCenter));
            if (halfChord > 0) {
                Color bc = band % 2 == 0 ? new Color(70, 30, 140, 18) : new Color(90, 55, 170, 14);
                g2.setColor(bc);
                g2.fillRect(pCX - halfChord, by, halfChord * 2, 9);
            }
        }
        // Planet ring system
        g2.setStroke(new BasicStroke(4f));
        g2.setColor(new Color(110, 80, 200, 22));
        g2.drawOval(pX - 35, pCY - 10, (pR + 35) * 2, 20);
        g2.setStroke(new BasicStroke(2f));
        g2.setColor(new Color(140, 100, 220, 14));
        g2.drawOval(pX - 22, pCY - 6, (pR + 22) * 2, 12);
        g2.setStroke(new BasicStroke(1f));
        // Planet limb glow
        g2.setColor(new Color(130, 80, 255, 22));
        g2.drawOval(pX, pY, pR * 2, pR * 2);

        // Moon / cratered asteroid (far left)
        int moonOff = (int)(cam.x * 0.025) % (W + 200);
        int mX = 50 + (W - moonOff) % W % 300; int mY = 80; int mR = 38;
        g2.setColor(new Color(38, 32, 52, 60));
        g2.fillOval(mX, mY, mR * 2, mR * 2);
        // Moon craters
        g2.setColor(new Color(18, 14, 30, 55));
        g2.fillOval(mX + 8, mY + 10, 13, 11);
        g2.fillOval(mX + mR + 4, mY + mR - 4, 9, 8);
        g2.fillOval(mX + 13, mY + mR + 6, 10, 9);
        // Moon highlight
        g2.setColor(new Color(80, 70, 115, 28));
        g2.fillOval(mX + 4, mY + 4, mR - 4, mR / 2);

        // Horizon nebula density clouds
        for (int i = 0; i < 4; i++) {
            int nx = (int)((i * 380L - (long)(cam.x * 0.09)) % (W + 600) - 100);
            int ny = H - 130 - i * 20;
            Color nc = i % 2 == 0 ? new Color(55, 10, 140, 14) : new Color(0, 35, 100, 11);
            g2.setColor(nc);
            g2.fillOval(nx, ny, 320 + i * 80, 90 + i * 25);
        }
    }

    void drawCollectibles(Graphics2D g2) {
        for (Collectible c : collectibles) c.draw(g2);
    }

    void drawEnemies(Graphics2D g2) {
        for (Enemy e : enemies) e.draw(g2);
    }

    void drawProjectiles(Graphics2D g2) {
        long t = System.currentTimeMillis();
        for (EnemyProjectile p : projectiles) {
            int px = (int)p.x, py = (int)p.y;
            // Trail de energia
            for (int i = 1; i <= 4; i++) {
                int alpha = 60 - i * 14;
                float trailX = p.x - p.vx * i * 1.5f;
                g2.setColor(new Color(255, 80, 80, alpha));
                g2.fillOval((int)trailX - 3, py - 3, 6, 6);
            }
            // Orbe principal
            int pulse = (int)(Math.sin(t * 0.01 + px) * 20);
            g2.setColor(new Color(255, 60 + pulse, 60, 200));
            g2.fillOval(px - 5, py - 5, 10, 10);
            // Núcleo brilhante
            g2.setColor(new Color(255, 200, 200, 220));
            g2.fillOval(px - 2, py - 2, 4, 4);
            // Glow exterior
            g2.setColor(new Color(255, 50, 50, 40 + pulse / 2));
            g2.fillOval(px - 8, py - 8, 16, 16);
        }
    }

    void setTheme(Color base, Color plat, Color accent) {
        this.themeBase = base;
        this.themePlatform = plat;
        this.themeAccent = accent;
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                Tile t = tiles[y][x];
                if (t.isSolid()) t.color = base;
            }
        }
    }
}
