import java.awt.*;
import java.util.Random;

class Collectible {
    float x, y;
    int type;
    boolean collected = false;
    int bobOffset;

    Collectible(float x, float y, int type) {
        this.x = x; this.y = y; this.type = type;
        bobOffset = (int)(Math.random() * 100);
    }

    void update() { bobOffset++; }

    Rectangle getBounds() {
        return new Rectangle((int)x - 8, (int)y - 8, 20, 20);
    }

    void draw(Graphics2D g2) {
        if (collected) return;
        int by = (int)(y + Math.sin(bobOffset * 0.08) * 3);

        if (type == 0) {
            g2.setColor(Color.decode("#00ff88"));
            g2.fillOval((int)x - 6, by - 6, 14, 14);
            g2.setColor(new Color(0, 255, 136, 50));
            g2.fillOval((int)x - 9, by - 9, 20, 20);
        } else if (type == 1) {
            g2.setColor(Color.decode("#ffcc00"));
            g2.fillOval((int)x - 5, by - 5, 12, 12);
            g2.setColor(new Color(255, 200, 0, 40));
            g2.fillOval((int)x - 8, by - 8, 18, 18);
        } else if (type == 2) {
            g2.setColor(Color.decode("#6688ff"));
            g2.fillRect((int)x - 2, by - 6, 4, 12);
            g2.fillRect((int)x - 6, by - 2, 12, 4);
        } else if (type == 3) {
            g2.setColor(Color.decode("#ff4488"));
            g2.fillRect((int)x - 4, by - 6, 9, 12);
            g2.setColor(Color.WHITE);
            g2.fillRect((int)x, by - 3, 3, 4);
        } else if (type == 4) {
            g2.setColor(Color.decode("#555555"));
            g2.fillOval((int)x - 4, by - 5, 10, 10);
            g2.setColor(Color.decode("#ff8800"));
            g2.fillOval((int)x + 2, by - 7, 3, 3);
        } else if (type == 5) {
            g2.setColor(Color.decode("#00ffcc"));
            g2.fillRect((int)x + 2, by - 10, 4, 16);
            g2.fillRect((int)x - 4, by - 2, 14, 4);
        }
    }
}

class Enemy {
    float x, y, w, h;
    float vx, vy;
    int hp, maxHp;
    int type;
    int state;
    int stateTimer;
    boolean facingRight = false;
    int contactDmg = 1;
    int points = 50;
    int animTimer = 0;
    boolean onGround = false;
    boolean active = false;

    Enemy(float x, float y, int type, int hp) {
        this.x = x; this.y = y; this.type = type;
        this.maxHp = hp; this.hp = hp;
        w = 24; h = 28;
        stateTimer = 60 + (int)(Math.random() * 60);
    }

    void update(Room room, Player player) {
        if (state == 2) { stateTimer--; return; }
        animTimer++;

        if (!active) {
            float dx = x - player.x;
            float dy = y - player.y;
            if (Math.sqrt(dx * dx + dy * dy) < 400) active = true;
            return;
        }

        if (type == 0) updateWalker(player);
        else if (type == 1) updateJumper(player);
        else if (type == 2) updateShooter(room, player);
        else if (type == 3) updateChaser(room, player);
        else if (type == 4) updateBoss(player);

        vy += 0.4;
        if (vy > 10) vy = 10;

        x += vx;
        collideX(room);
        y += vy;
        onGround = false;
        collideY(room);

        if (y > 800) { state = 2; stateTimer = 0; }
    }

    void updateWalker(Player player) {
        if (stateTimer-- <= 0) {
            facingRight = !facingRight;
            stateTimer = 60 + (int)(Math.random() * 90);
        }
        vx = facingRight ? 1.2f : -1.2f;
    }

    void updateJumper(Player player) {
        float dx = player.x - x;
        facingRight = dx > 0;
        if (onGround) {
            vx = facingRight ? 2 : -2;
            if (stateTimer-- <= 0) {
                vy = -8;
                stateTimer = 30 + (int)(Math.random() * 40);
            }
        } else {
            vx = facingRight ? 1.5f : -1.5f;
        }
    }

    void updateShooter(Room room, Player player) {
        float dx = player.x - x;
        facingRight = dx > 0;
        vx = 0;

        if (stateTimer-- <= 0) {
            float dir = facingRight ? 4 : -4;
            EnemyProjectile proj = new EnemyProjectile();
            proj.x = x + (facingRight ? w : 0);
            proj.y = y + 12;
            proj.vx = dir;
            proj.vy = 0;
            room.projectiles.add(proj);
            stateTimer = 80 + (int)(Math.random() * 50);
        }
    }

    void updateChaser(Room room, Player player) {
        float dx = player.x - x;
        float speed = 2.5f;
        if (dx > 0) { facingRight = true; vx = (float) speed; }
        else { facingRight = false; vx = (float) -speed; }

        int aheadX = facingRight ? 1 : -1;
        int tx = (int) ((x + w / 2 + aheadX * Game.GS) / Game.GS);
        int ty = (int) ((y + h) / Game.GS);
        Tile front = room.getTile(tx, ty);
        if (front.isSolid() && onGround) {
            vy = -7;
        }
    }

    void updateBoss(Player player) {
        float dx = player.x - x;
        facingRight = dx > 0;
        stateTimer--;
        if (stateTimer <= 0) {
            int phase = (50 - stateTimer / 40) % 3;
            if (phase == 0) {
                vx = facingRight ? 5 : -5;
                stateTimer = 30;
            } else if (phase == 1) {
                vy = -10;
                vx = facingRight ? 3 : -3;
                stateTimer = 40;
            } else {
                vx *= 0.8f;
                stateTimer = 50;
            }
        }
    }

    void collideX(TileMap map) {
        int tx1 = (int) (x / Game.GS);
        int tx2 = (int) ((x + w - 0.01f) / Game.GS);
        int ty1 = (int) (y / Game.GS);
        int ty2 = (int) ((y + h - 0.01f) / Game.GS);
        for (int ty = ty1; ty <= ty2; ty++) {
            for (int tx = tx1; tx <= tx2; tx++) {
                Tile t = map.getTile(tx, ty);
                if (t.isSolid()) {
                    float ol = (x + w) - tx * Game.GS;
                    float or2 = (tx * Game.GS + Game.GS) - x;
                    if (ol < or2) { x = tx * Game.GS - w; }
                    else { x = tx * Game.GS + Game.GS; }
                    vx = 0;
                }
            }
        }
    }

    void collideY(TileMap map) {
        int tx1 = (int) (x / Game.GS);
        int tx2 = (int) ((x + w - 0.01f) / Game.GS);
        int ty1 = (int) (y / Game.GS);
        int ty2 = (int) ((y + h - 0.01f) / Game.GS);
        for (int ty = ty1; ty <= ty2; ty++) {
            for (int tx = tx1; tx <= tx2; tx++) {
                Tile t = map.getTile(tx, ty);
                if (t.isSolid()) {
                    float ot = (y + h) - ty * Game.GS;
                    float ob = (ty * Game.GS + Game.GS) - y;
                    if (ot < ob) {
                        y = ty * Game.GS - h;
                        if (vy > 0) vy = 0;
                        onGround = true;
                    } else {
                        y = ty * Game.GS + Game.GS;
                        if (vy < 0) vy = 0;
                    }
                }
            }
        }
    }

    void takeDamage(int dmg, float knockbackX, float knockbackY) {
        hp -= dmg;
        vx = knockbackX;
        vy = knockbackY;
        if (hp <= 0) { state = 2; stateTimer = 30; }
    }

    Rectangle getBounds() { return new Rectangle((int) x, (int) y, (int) w, (int) h); }
    boolean dead() { return state == 2 && stateTimer <= 0; }

    void draw(Graphics2D g2) {
        if (state == 2) {
            int alpha = stateTimer * 8;
            int sh = (int) (h * stateTimer / 30.0);
            g2.setColor(new Color(255, 100, 100, Math.max(0, alpha)));
            g2.fillRect((int) x, (int) (y + h - sh), (int) w, sh);
            return;
        }

        int px = (int) x, py = (int) y;

        if (type == 0) {
            g2.setColor(Color.decode("#66aa44"));
            g2.fillRect(px, py + 4, (int) w, (int) h - 4);
            g2.setColor(Color.decode("#448822"));
            g2.fillRect(px + 2, py + 6, (int) w - 4, (int) h - 8);
            int ex = facingRight ? px + 14 : px + 4;
            g2.setColor(Color.decode("#ccffcc"));
            g2.fillRect(ex, py + 8, 5, 5);
            g2.setColor(Color.RED);
            g2.fillRect(ex + 1, py + 9, 3, 3);
        } else if (type == 1) {
            g2.setColor(Color.decode("#cc44aa"));
            g2.fillRect(px + 2, py, (int) w - 4, (int) h);
            g2.setColor(Color.decode("#882266"));
            int jx = facingRight ? px + 4 : px + (int) w - 8;
            g2.fillRect(jx, py + 6, 5, 6);
            g2.setColor(Color.WHITE);
            g2.fillRect(jx + 1, py + 7, 3, 3);
        } else if (type == 2) {
            g2.setColor(Color.decode("#aa8800"));
            g2.fillRect(px, py, (int) w, (int) h);
            int gx = facingRight ? px + (int) w : px - 10;
            g2.setColor(Color.decode("#ccaa00"));
            g2.fillRect(gx, py + 10, 10, 4);
            g2.setColor(Color.WHITE);
            int ey = facingRight ? px + (int) w - 4 : px + 1;
            g2.fillRect(ey, py + 6, 3, 3);
        } else if (type == 3) {
            g2.setColor(Color.decode("#ff6622"));
            int pulse = (int) (Math.sin(animTimer * 0.15) * 3);
            g2.fillRect(px - pulse, py, (int) w + pulse * 2, (int) h);
            g2.setColor(new Color(255, 100, 34, 40));
            g2.fillOval(px - 4 - pulse, py - 4, (int) w + 8 + pulse * 2, (int) h + 8);
            g2.setColor(Color.WHITE);
            g2.fillRect(px + 8, py + 6, 4, 4);
            g2.fillRect(px + 14, py + 6, 4, 4);
        } else if (type == 4) {
            g2.setColor(Color.decode("#aa0000"));
            g2.fillRect(px - 8, py - 8, (int) w + 16, (int) h + 16);
            g2.setColor(Color.decode("#660000"));
            g2.fillRect(px - 4, py - 4, (int) w + 8, (int) h + 8);
            g2.setColor(Color.YELLOW);
            g2.fillRect(px + 4, py + 2, 10, 8);
            g2.fillRect(px + (int) w - 10, py + 2, 10, 8);
            float ratio = (float) hp / maxHp;
            g2.setColor(Color.BLACK);
            g2.fillRect(px - 10, py - 14, (int) w + 20, 4);
            g2.setColor(ratio > 0.5 ? Color.decode("#00cc44") : Color.RED);
            g2.fillRect(px - 10, py - 14, (int) ((w + 20) * ratio), 4);
        }
    }
}

class EnemyProjectile {
    float x, y, vx, vy;
    int damage = 1;
    boolean dead = false;
    int life = 200;

    void update(TileMap map) {
        x += vx;
        y += vy;
        life--;
        if (life <= 0) dead = true;

        int tx = (int) (x / Game.GS);
        int ty = (int) (y / Game.GS);
        Tile t = map.getTile(tx, ty);
        if (t != null && t.isSolid()) dead = true;
    }

    Rectangle getBounds() { return new Rectangle((int) x - 3, (int) y - 3, 7, 7); }
}
