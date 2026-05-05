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
        double dt = 1.0; 

        // Colisão com inimigos e ataques
        for (int i = 0; i < enemies.size(); i++) {
            Enemy e = enemies.get(i);
            e.update(this, player, dt);

            // 1. Prioridade: Ataque de Espada do Player
            if (player.attacking && e.state != 2) {
                Rectangle ab = player.getAttackBox();
                if (ab.intersects(e.getBounds())) {
                    e.takeDamage(player.attackDamage, player.attackKnockbackX * (player.facingRight ? 1 : -1), player.attackKnockbackY);
                    spawnParticles(e.x + e.w / 2, e.y + e.h / 2, new Color(255, 200, 100, 180), 5, rand);
                    sound.playHit();
                    player.attackCooldown = 12;
                    continue; 
                }
            }

            // 2. Dano de Contato do Inimigo (ou RODEO se cair em cima)
            if (e.state != 2 && e.active && player.ridingEnemy == null && e.getBounds().intersects(player.getBounds())) {
                // Verifica se o player está caindo sobre a cabeça do inimigo (rodeo)
                boolean landingOnTop = player.vy > 1.5f
                    && (player.y + player.h) < (e.y + e.h * 0.55f);

                if (landingOnTop && player.tryMount(e)) {
                    // Rodeo iniciado! Partículas de comemoração
                    spawnParticles(e.x + e.w / 2, e.y, new Color(255, 220, 50), 12, rand);
                    spawnParticles(e.x + e.w / 2, e.y, Color.WHITE, 6, rand);
                } else if (player.ridingEnemy == null) {
                    // Colisão normal: toma dano
                    player.takeDamage(e.contactDmg);
                    if (player.hp > 0) sound.playHurt();
                    float dx = player.x - e.x;
                    player.vx = dx > 0 ? 5 : -5;
                    player.vy = -4;
                }
            }

            // Ataque de Raios (Ajustado: Dano imediato e bônus de dano)
            if (player.firingLightning && e.state != 2) {
                int lx = (int)(player.facingRight ? player.x + player.w : player.x - 150);
                Rectangle lightningBox = new Rectangle(lx, (int)player.y, 150, (int)player.h);
                if (lightningBox.intersects(e.getBounds())) {
                    // Dano imediato com bônus de 5% por item de raio
                    int finalDmg = (int)(1 * player.lightningDamageMult);
                    if (finalDmg < 1) finalDmg = 1;
                    
                    e.takeDamage(finalDmg, 0, 0); 
                    spawnParticles(e.x + e.w/2, e.y + e.h/2, Color.CYAN, 2, rand);
                }
            }
        }

        // Morte do Player
        if (player.hp <= 0) {
            spawnParticles(player.x + player.w / 2, player.y + player.h / 2, new Color(255, 100, 0), 30, rand);
            spawnParticles(player.x + player.w / 2, player.y + player.h / 2, Color.YELLOW, 20, rand);
            sound.playDeath();
        }

        for (int i = enemies.size() - 1; i >= 0; i--) {
            Enemy e = enemies.get(i);
            if (e.isDead()) {
                player.score += e.points;
                if (e.guaranteedDrop != -1) {
                    collectibles.add(new Collectible(e.x + e.w / 2, e.y, e.guaranteedDrop));
                } else {
                    collectibles.add(new Collectible(e.x + e.w / 2, e.y, 6)); // Munição de Raio garantida!
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
                else if (c.type == 3) { 
                    player.livesCollected++; 
                    player.heal(0); // Trigger heal burst visual
                    player.hp = player.maxHp;
                }
                else if (c.type == 4) { player.bombs++; }
                else if (c.type == 5) { player.attackDamage++; }
                else if (c.type == 6) { 
                    player.lightningAmmo = Math.min(player.maxLightningAmmo, player.lightningAmmo + 50);
                    player.lightningItems++;
                    player.lightningDamageMult += 0.05f;
                    player.emeraldBurstTimer = 60; // Inicia efeito visual esmeralda
                }
                else if (c.type == 7) { 
                    player.starPower++;
                    player.triggerStarBurst();           // Burst dourado espetacular
                    spawnParticles(c.x, c.y, Color.WHITE,  20, rand);
                    spawnParticles(c.x, c.y, Color.YELLOW, 30, rand);
                    spawnParticles(c.x, c.y, new Color(255, 180, 255), 15, rand);
                }
                else if (c.type == 8) {
                    player.increaseJumpPower();
                    spawnParticles(c.x, c.y, new Color(100, 200, 255), 15, rand);
                }
                spawnParticles(c.x, c.y, (c.type == 6 ? new Color(0, 255, 120) : (c.type == 8 ? Color.CYAN : (c.type == 0 ? Color.GREEN : Color.YELLOW))), 6, rand);
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

    boolean checkExit(Player player) {
        int ts = 32;
        int tx1 = (int)(player.x / ts);
        int tx2 = (int)((player.x + player.w) / ts);
        int ty1 = (int)(player.y / ts);
        int ty2 = (int)((player.y + player.h) / ts);

        for (int ty = ty1; ty <= ty2; ty++) {
            for (int tx = tx1; tx <= tx2; tx++) {
                Tile t = getTile(tx, ty);
                if (t != null && t.isExit()) {
                    return enemies.isEmpty();
                }
            }
        }
        return false;
    }

    boolean isComplete() {
        return false; 
    }

    private Color themeBase = Color.decode("#0a0e1a");
    private Color themePlatform = Color.decode("#0d1830");
    private Color themeAccent = Color.decode("#004488");

    void drawBackground(Graphics2D g2, Camera cam, int W, int H) {
        super.drawBackground(g2, cam, W, H);
        long t = System.currentTimeMillis();

        // Gas giant planet
        int planetOffX = (int)(cam.x * 0.04) % (W + 600);
        int pX = W - 220 - planetOffX % W; int pY = 30; int pR = 115;
        int pCX = pX + pR, pCY = pY + pR;
        g2.setPaint(new GradientPaint(pX, pY, new Color(35, 18, 85, 70), pX + pR, pY + pR * 2, new Color(12, 5, 42, 90)));
        g2.fillOval(pX, pY, pR * 2, pR * 2);
        g2.setPaint(null);
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
        g2.setStroke(new BasicStroke(4f));
        g2.setColor(new Color(110, 80, 200, 22));
        g2.drawOval(pX - 35, pCY - 10, (pR + 35) * 2, 20);
        g2.setStroke(new BasicStroke(1f));

        // Moon
        int mX = 50 + (int)(cam.x * 0.025) % 300; int mY = 80; int mR = 38;
        g2.setColor(new Color(38, 32, 52, 60));
        g2.fillOval(mX, mY, mR * 2, mR * 2);

        // Clouds
        for (int i = 0; i < 4; i++) {
            int nx = (int)((i * 380L - (long)(cam.x * 0.09)) % (W + 600) - 100);
            int ny = H - 130 - i * 20;
            g2.setColor(i % 2 == 0 ? new Color(55, 10, 140, 14) : new Color(0, 35, 100, 11));
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
        for (EnemyProjectile p : projectiles) {
            int px = (int)p.x, py = (int)p.y;
            g2.setColor(new Color(255, 80, 80, 150));
            g2.fillOval(px - 5, py - 5, 10, 10);
            g2.setColor(Color.WHITE);
            g2.fillOval(px - 2, py - 2, 4, 4);
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
