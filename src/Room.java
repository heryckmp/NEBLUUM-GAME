import java.awt.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

class Room extends TileMap {
    java.util.List<Enemy> enemies = new ArrayList<Enemy>();
    java.util.List<Collectible> collectibles = new ArrayList<Collectible>();
    java.util.List<EnemyProjectile> projectiles = new ArrayList<EnemyProjectile>();

    public Room(int cols, int rows) {
        super(cols, rows);
    }

    void addEnemy(Enemy e) { enemies.add(e); }
    void addCollectible(float x, float y, int type) { collectibles.add(new Collectible(x, y, type)); }

    void update(Player player, SoundPlayer sound, Random rand) {
        super.update(player);

        for (int i = 0; i < enemies.size(); i++) {
            Enemy e = enemies.get(i);
            e.update(this, player);

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
            if (e.dead()) {
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
            c.update();
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
            p.update(this);
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

    void drawBackground(Graphics2D g2, Camera cam, int W, int H) {
        super.drawBackground(g2, cam, W, H);

        g2.setColor(new Color(12, 15, 30));
        for (int i = 0; i < 15; i++) {
            int bx = i * 200 - (int)(cam.x * 0.2) % 200 - 50;
            int bh = 40 + (i * 37 % 80);
            int by2 = H - bh;
            g2.fillRect(bx, by2, 60, bh);
        }
        g2.setColor(new Color(8, 12, 25));
        for (int i = 0; i < 12; i++) {
            int bx = i * 250 - (int)(cam.x * 0.4) % 250 - 80;
            int bh = 30 + (i * 53 % 60);
            int by2 = H - bh + 20;
            g2.fillRect(bx, by2, 50, bh);
        }

        g2.setColor(new Color(0, 150, 200, 15));
        for (int i = 0; i < 30; i++) {
            int lx = (int) ((i * 137 + cam.x * 0.08) % W);
            int ly = (int) ((Math.abs((i * 7L + (long)(cam.x * 0.05)) % H)) % (H - 100));
            g2.fillOval(lx, ly, 2, 2);
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
            g2.setColor(Color.decode("#ff4444"));
            g2.fillOval((int) p.x - 3, (int) p.y - 3, 7, 7);
            g2.setColor(new Color(255, 68, 68, 50));
            g2.fillOval((int) p.x - 5, (int) p.y - 5, 11, 11);
        }
    }

    void setTheme(Color base, Color plat, Color dec) {
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                Tile t = tiles[y][x];
                if (t.isSolid()) t.color = base;
            }
        }
    }
}
