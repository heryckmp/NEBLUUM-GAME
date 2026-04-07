import java.awt.*;
import java.util.Random;
import java.util.List;

public class Player {
    float x, y;
    int w, h;
    float vx, vy;
    int hp = 12, maxHp = 12;
    int shield = 0;
    int energy = 100, maxEnergy = 100;
    int score = 0;

    double speed = 3.2;
    double jumpForce = -9.5;
    int maxJumps = 2;
    int jumpsUsed = 0;
    boolean onGround = false;
    boolean facingRight = true;

    int attackCooldown = 0;
    int attackDuration = 0;
    boolean attacking = false;
    float attackX, attackY, attackW, attackH;
    int attackDamage = 3;
    int attackKnockbackX = 6;
    int attackKnockbackY = -4;

    int dashCooldown = 0;
    boolean isDashing = false;
    int dashDuration = 0;
    float dashVx, dashVy;

    int invincible = 0;

    int coins = 0, bombs = 3, healthPotions = 2;
    int maxCoins = 999;

    boolean inputLeft, inputRight, inputJump, inputDown, inputAttack, inputDash, inputUse, inputDrop;

    private boolean jumpReleased = true;

    public Player(float x, float y) {
        this.x = x; this.y = y;
        w = 20; h = 32;
        vx = 0; vy = 0;
    }

    public void respawn(float x, float y) {
        this.x = x; this.y = y;
        vx = 0; vy = 0;
        hp = maxHp;
        shield = 0;
        energy = maxEnergy;
        invincible = 120;
        isDashing = false;
        attacking = false;
    }

    public void update(TileMap map) {
        if (isDashing) {
            dashDuration--;
            if (dashDuration <= 0) isDashing = false;
            map.spawnParticles(x + w/2, y + h/2, new Color(0, 200, 255, 80), 3, new Random());
            x += dashVx;
            y += dashVy;
            invincible = Math.max(invincible, 1);
            return;
        }

        vy += 0.42;
        if (vy > 12) vy = 12;

        vx = 0;
        if (inputLeft) vx = (float)-speed;
        if (inputRight) vx = (float)speed;

        applyMovement(map);

        if (inputJump && jumpReleased && jumpsUsed < maxJumps) {
            vy = (float)jumpForce;
            jumpsUsed++;
            onGround = false;
            jumpReleased = false;
        }
        if (!inputJump) jumpReleased = true;

        if (inputDown && onGround) {
            vy = 3;
            onGround = false;
        }

        if (attackCooldown > 0) attackCooldown--;
        if (inputAttack && attackCooldown <= 0 && !attacking) {
            startAttack();
        }

        if (attacking) {
            attackDuration--;
            if (attackDuration <= 0) attacking = false;
        }

        if (dashCooldown > 0) dashCooldown--;
        if (inputDash && dashCooldown <= 0 && energy >= 25) {
            startDash();
        }

        if (inputUse && healthPotions > 0) {
            if (hp < maxHp) {
                healthPotions--;
                heal(4);
                map.spawnParticles(x + w/2, y + h/2, new Color(0, 255, 150, 180), 12, new Random());
            }
        }

        if (inputDrop && bombs > 0) {
            bombs--;
            map.spawnBomb(facingRight ? x + w : x - 20, y + h, facingRight ? 6 : -6);
        }

        if (invincible > 0) invincible--;
        if (energy < maxEnergy) energy = Math.min(maxEnergy, energy + 1);
        if (y > 800) die();
    }

    private void applyMovement(TileMap map) {
        x += vx;
        handleTileCollision(map, true);

        y += vy;
        onGround = false;
        handleTileCollision(map, false);
    }

    private void handleTileCollision(TileMap map, boolean isX) {
        int tx1 = (int)(x / 32);
        int tx2 = (int)((x + w - 0.01f) / 32);
        int ty1 = (int)(y / 32);
        int ty2 = (int)((y + h - 0.01f) / 32);

        for (int ty = ty1; ty <= ty2; ty++) {
            for (int tx = tx1; tx <= tx2; tx++) {
                Tile tile = map.getTile(tx, ty);
                if (tile != null && tile.isSolid() && !tile.isPlatform()) {
                    resolveCollision(tx * 32, ty * 32, 32, 32, isX, false);
                } else if (tile != null && tile.isPlatform() && vy >= 0) {
                    resolveCollision(tx * 32, ty * 32, 32, 32, isX, true);
                }
            }
        }
    }

    private void resolveCollision(int tileX, int tileY, int tileW, int tileH, boolean isX, boolean isPlatform) {
        float overlapL = (x + w) - tileX;
        float overlapR = (tileX + tileW) - x;
        float overlapT = (y + h) - tileY;
        float overlapB = (tileY + tileH) - y;

        float minOverX = Math.min(overlapL, overlapR);
        float minOverY = Math.min(overlapT, overlapB);

        if (isPlatform) {
            if (vy >= 0 && minOverY < minOverX && minOverY < 32/3 && minOverY > 0 && y + h - vy <= tileY + 3) {
                y = tileY - h;
                vy = 0;
                onGround = true;
                jumpsUsed = 0;
            }
            return;
        }

        if (minOverX < minOverY) {
            if (overlapL < overlapR) x = tileX - w;
            else x = tileX + tileW;
            vx = 0;
        } else {
            if (overlapT < overlapB) {
                y = tileY - h;
                if (vy > 0) vy = 0;
                onGround = true;
                jumpsUsed = 0;
            } else {
                y = tileY + tileH;
                if (vy < 0) vy = 0;
            }
        }
    }

    void startAttack() {
        attacking = true;
        attackDuration = 8;
        attackCooldown = 18;
        if (facingRight) {
            attackX = x + w; attackY = y + 2; attackW = 30; attackH = 28;
        } else {
            attackX = x - 30; attackY = y + 2; attackW = 30; attackH = 28;
        }
    }

    void startDash() {
        isDashing = true;
        dashDuration = 8;
        dashCooldown = 40;
        energy -= 25;
        if (inputRight) { dashVx = 12; dashVy = 0; facingRight = true; }
        else if (inputLeft) { dashVx = -12; dashVy = 0; facingRight = false; }
        else if (inputDown) { dashVx = 0; dashVy = 8; }
        else { dashVx = facingRight ? 8 : -8; dashVy = 0; }
        invincible = 10;
    }

    void heal(int amount) { hp = Math.min(maxHp, hp + amount); }
    void takeDamage(int dmg) {
        if (invincible > 0 || isDashing) return;
        if (shield > 0) { shield = Math.max(0, shield - dmg); invincible = 30; return; }
        hp -= dmg;
        invincible = 40;
        if (hp <= 0) die();
    }

    void die() { hp = 0; }

    public Rectangle getBounds() {
        return new Rectangle((int)x, (int)y, w, h);
    }

    public Rectangle getAttackBox() {
        if (attacking) return new Rectangle((int)attackX, (int)attackY, (int)attackW, (int)attackH);
        return new Rectangle(0, 0, 0, 0);
    }

    public void draw(Graphics2D g2) {
        int px = (int)x, py = (int)y;

        if (invincible > 0 && (invincible / 3) % 2 == 0) return;

        g2.setColor(new Color(0, 0, 0, 40));
        g2.fillRect(px + 3, py + h, w, 4);

        if (isDashing) {
            g2.setColor(new Color(0, 150, 255, 60));
            g2.fillOval(px - 10, py + h - 5, w + 20, 8);
        }

        g2.setColor(new Color(0, 180, 220));
        g2.fillRect(px + 2, py + 8, w - 4, h - 12);

        int gc = 60 + (int)(Math.sin(System.currentTimeMillis() * 0.005) * 20);
        g2.setColor(new Color(0, 220, 255, gc));
        g2.fillOval(px + 1, py + 12, w - 2, 12);

        g2.setColor(new Color(0, 160, 200));
        g2.fillRect(px + 1, py + 2, w - 2, 10);

        g2.setColor(new Color(255, 255, 255));
        int visorX = facingRight ? px + 13 : px + 3;
        g2.fillRect(visorX, py + 4, 5, 5);
        g2.setColor(new Color(150, 255, 255, 180));
        g2.fillRect(visorX + 1, py + 5, 3, 3);

        g2.setColor(new Color(0, 120, 160));
        if (onGround && Math.abs(vx) > 0.1) {
            int legA = (int)(Math.sin(System.currentTimeMillis() * 0.012) * 4);
            g2.fillRect(px + 3, py + h - 6, 6, 6 + legA);
            g2.fillRect(px + w - 9, py + h - 6, 6, 6 - legA);
        } else if (!onGround) {
            g2.fillRect(px + 1, py + h - 6, 6, 6);
            g2.fillRect(px + w - 7, py + h - 6, 6, 6);
        } else {
            g2.fillRect(px + 3, py + h - 6, 6, 6);
            g2.fillRect(px + w - 9, py + h - 6, 6, 6);
        }

        if (shield > 0) {
            g2.setColor(new Color(100, 150, 255, 30));
            g2.drawOval(px - 6, py - 6, w + 12, h + 12);
        }

        if (attacking) {
            int atx = facingRight ? px + w + 4 : px - 24;
            g2.setColor(new Color(0, 255, 200, 180));
            g2.fillRect(atx, py + 6, 20, 4);
            g2.setColor(new Color(255, 255, 255, 120));
            g2.fillOval(atx + 6, py + 4, 10, 10);
            g2.setColor(new Color(0, 255, 200, 30));
            g2.fillRect(atx - 5, py, 30, 20);
        }
    }
}
