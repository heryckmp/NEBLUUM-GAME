import java.awt.*;
import java.awt.geom.*;
import java.util.Random;

public class Player extends Entity {
    // Status
    public int hp = 5, maxHp = 5, energy = 100, maxEnergy = 100;
    public int score = 0, coins = 0, bombs = 3, healthPotions = 0;
    public int livesCollected = 0;
    public int starPower = 0; // De 0 a 7 (um por fase)
    public int lightningItems = 0; 
    public float lightningDamageMult = 1.0f;
    public boolean isStar = false;
    public int lightningAmmo = 100;
    public int maxLightningAmmo = 200;
    public int shield = 0;

    // Combate
    public int attackDamage = 3;
    public int attackKnockbackX = 6;
    public int attackKnockbackY = -4;
    public int attackCooldown = 0;
    public boolean firingLightning = false;

    // Movimento
    private float moveSpeed = 4.0f;
    private float jumpPower = -9.2f;
    private float acceleration = 0.5f;

    // Game Feel
    private int coyoteTimer = 0;
    private int jumpBufferTimer = 0;
    private final int MAX_COYOTE = 6;
    private final int MAX_BUFFER = 5;

    public boolean facingRight = true;
    public boolean attacking = false;
    public boolean isDashing = false;
    public int invincible = 0;
    private int attackDuration = 0;
    private int dashCooldown = 0;

    // Visual
    private int animTick = 0;
    private float[] dashTrailX = new float[6];
    private float[] dashTrailY = new float[6];
    private int dashTrailHead = 0;
    private int healFlash = 0;  
    public int emeraldBurstTimer = 0; 

    public boolean inputLeft, inputRight, inputJump, inputDown, inputAttack, inputDash, inputLightning, inputUse, inputDrop;

    public Player(float x, float y) { super(x, y, 20, 32); }

    public void respawn(float x, float y) {
        this.x = x; this.y = y;
        vx = 0; vy = 0; hp = maxHp;
        invincible = 120;
        lightningAmmo = 100;
        livesCollected = 0;
        isStar = false;
    }

    @Override
    public void update(Room room, double dt) {
        animTick++;
        dashTrailX[dashTrailHead % dashTrailX.length] = x;
        dashTrailY[dashTrailHead % dashTrailY.length] = y;
        dashTrailHead++;

        if (isDashing) { updateDash(room); return; }

        handleMovementLogic();
        handleJumpLogic();
        applyGravity(0.42f);
        moveAndCollide(room, vx, vy);
        handleCombatLogic(room);

        checkEnvironmentalDamage(room);

        if (invincible > 0) invincible--;
        if (dashCooldown > 0) dashCooldown--;
        if (energy < maxEnergy) energy++;
        
        // --- Temporizadores Visuais (Decremento por frame) ---
        if (healFlash > 0) healFlash--;
        if (emeraldBurstTimer > 0) emeraldBurstTimer--;
        
        if (y > 600) hp = 0; 

        if (starPower > 0 && animTick % Math.max(1, 10 - starPower) == 0) {
            room.spawnParticles(x + w/2, y + h/2, Color.YELLOW, 1, new Random());
        }
    }

    private void checkEnvironmentalDamage(Room room) {
        int ts = 32;
        int tx1 = (int) (x / ts);
        int tx2 = (int) ((x + w - 0.1f) / ts);
        int ty1 = (int) (y / ts);
        int ty2 = (int) ((y + h - 0.1f) / ts);
        for (int ty = ty1; ty <= ty2; ty++) {
            for (int tx = tx1; tx <= tx2; tx++) {
                Tile t = room.getTile(tx, ty);
                if (t != null && (t.isSpike() || t.isHazard())) {
                    takeDamage(1);
                    vy = -5;
                    vx = (x + w/2 < tx * ts + ts/2) ? -4 : 4;
                    return; 
                }
            }
        }
    }

    private void handleMovementLogic() {
        if (inputLeft) { vx -= acceleration; if (vx < -moveSpeed) vx = -moveSpeed; facingRight = false; }
        else if (inputRight) { vx += acceleration; if (vx > moveSpeed) vx = moveSpeed; facingRight = true; }
        else vx *= friction;
    }

    private void handleJumpLogic() {
        if (onGround) coyoteTimer = MAX_COYOTE; else if (coyoteTimer > 0) coyoteTimer--;
        if (inputJump) jumpBufferTimer = MAX_BUFFER; else if (jumpBufferTimer > 0) jumpBufferTimer--;
        if (jumpBufferTimer > 0 && coyoteTimer > 0) {
            vy = jumpPower; onGround = false; coyoteTimer = 0; jumpBufferTimer = 0;
        }
        if (!inputJump && vy < -2) vy *= 0.5f;
    }

    private void handleCombatLogic(Room room) {
        if (attackCooldown > 0) attackCooldown--;
        if (inputAttack && attackCooldown <= 0) { attacking = true; attackDuration = 8; attackCooldown = 20; }
        if (attacking) { attackDuration--; if (attackDuration <= 0) attacking = false; }
        if (inputLightning && lightningAmmo > 0) { firingLightning = true; lightningAmmo--; }
        else { firingLightning = false; }
        if (inputDash && dashCooldown <= 0 && energy >= 30) startDash();
    }

    private void startDash() {
        isDashing = true; dashCooldown = 45; energy -= 30;
        vx = facingRight ? 12 : -12; vy = 0; invincible = 15;
    }

    private void updateDash(Room room) {
        x += vx; invincible = 2;
        if (Math.abs(vx) > 0) vx *= 0.9f;
        if (Math.abs(vx) < 2) isDashing = false;
        handleTileCollision(room, true);
    }

    public void takeDamage(int dmg) { if (invincible > 0 || isDashing) return; hp -= dmg; invincible = 45; }
    public void heal(int amount) { hp = Math.min(maxHp, hp + amount); healFlash = 55; }

    public Rectangle getAttackBox() {
        int aw = 32, ah = 32;
        return new Rectangle((int)(facingRight ? x + w : x - aw), (int)y, aw, ah);
    }

    @Override
    public void draw(Graphics2D g2) {
        if (invincible > 0 && !isDashing && (invincible / 3) % 2 == 0) return;
        int px = (int)x, py = (int)y;
        int cx = px + w/2, cy = py + h/2;
        long t = System.currentTimeMillis();

        // --- ElementoVisual: Esmeralda Star Burst (prog = 1.0 -> 0.0) ---
        if (emeraldBurstTimer > 0) {
            float prog = emeraldBurstTimer / 60f;
            // 1. 3 anéis concêntricos expandindo
            for (int i = 0; i < 3; i++) {
                int ringSize = (int)((1 - prog) * 160) + (i * 25);
                g2.setColor(new Color(0, 255, 120, (int)(prog * 120 / (i + 1))));
                g2.setStroke(new BasicStroke(3f * prog));
                g2.drawOval(cx - ringSize/2, cy - ringSize/2, ringSize, ringSize);
            }
            // 2. 8 raios de estrela (dupla camada)
            for (int i = 0; i < 8; i++) {
                double angle = i * Math.PI / 4;
                int len = (int)(prog * 90);
                int rx = cx + (int)(Math.cos(angle) * len);
                int ry = cy + (int)(Math.sin(angle) * len);
                g2.setStroke(new BasicStroke(10f * prog));
                g2.setColor(new Color(0, 255, 120, (int)(prog * 60)));
                g2.drawLine(cx, cy, rx, ry);
                g2.setStroke(new BasicStroke(2f * prog));
                g2.setColor(new Color(220, 255, 240, (int)(prog * 255)));
                g2.drawLine(cx, cy, rx, ry);
            }
            // 3. Núcleo central
            g2.setColor(Color.WHITE);
            int coreSize = (int)(12 * prog);
            g2.fillOval(cx - coreSize/2, cy - coreSize/2, coreSize, coreSize);
            g2.setStroke(new BasicStroke(1f));
        }

        // --- Estrela Crescente (1 raio por item) ---
        if (lightningItems > 0) {
            g2.setColor(new Color(255, 255, 100, 200));
            g2.setStroke(new BasicStroke(2f));
            for (int i = 0; i < lightningItems; i++) {
                double angle = t * 0.005 + (i * (Math.PI * 2 / lightningItems));
                int rLen = 18 + (int)(Math.sin(t * 0.01 + i) * 4);
                int x2 = cx + (int)(Math.cos(angle) * rLen);
                int y2 = cy + (int)(Math.sin(angle) * rLen);
                g2.setColor(new Color(255, 255, 0, 150));
                g2.drawLine(cx, cy, x2, y2);
                g2.setColor(Color.WHITE);
                g2.fillOval(x2 - 1, y2 - 1, 3, 3);
            }
            g2.setStroke(new BasicStroke(1f));
        }

        // --- Heal Star Burst (prog = 1.0 -> 0.0) ---
        if (healFlash > 0) {
            float prog = healFlash / 55f;
            int size = (int)((1 - prog) * 120); 
            g2.setColor(new Color(255, 255, 180, (int)(prog * 220)));
            g2.setStroke(new BasicStroke(2.5f * prog));
            for (int i = 0; i < 12; i++) {
                double angle = i * Math.PI / 6 + (1-prog) * 2;
                int x2 = cx + (int)(Math.cos(angle) * size);
                int y2 = cy + (int)(Math.sin(angle) * size);
                g2.drawLine(cx, cy, x2, y2);
            }
            g2.setStroke(new BasicStroke(1f));
        }

        // Traje e outras camadas
        float lRatio = Math.min(1.0f, lightningItems / 10f);
        float sRatio = starPower / 7f;
        float totalRatio = Math.max(lRatio, sRatio);
        Color suitTop = Color.decode("#261e55");
        Color suitBot = Color.decode("#120e32");
        if (totalRatio > 0) {
            int r = (int)(suitTop.getRed() * (1 - totalRatio) + 255 * totalRatio);
            int g = (int)(suitTop.getGreen() * (1 - totalRatio) + 255 * totalRatio);
            int b = (int)(suitTop.getBlue() * (1 - totalRatio) + 100 * totalRatio);
            suitTop = new Color(r, g, b);
        }
        if (starPower >= 7) { drawStarForm(g2, px, py, t); return; }

        if (isDashing) {
            for (int i = 1; i < dashTrailX.length; i++) {
                int idx = (dashTrailHead - i + dashTrailX.length * 10) % dashTrailX.length;
                if (dashTrailX[idx] == 0) continue;
                int alpha = Math.max(0, 115 - i * 22);
                g2.setColor(new Color(130, 210, 255, alpha));
                g2.fillRoundRect((int)dashTrailX[idx] + 3, (int)dashTrailY[idx] + 8, w - 6, h - 16, 8, 8);
            }
        }
        g2.setPaint(new GradientPaint(px, py + 10, suitTop, px, py + h - 10, suitBot));
        g2.fillRoundRect(px + 1, py + 10, w - 2, h - 18, 6, 6);
        g2.setPaint(null);
        g2.setColor(suitTop.brighter());
        g2.fillRoundRect(px, py, w, 18, 10, 10);
        g2.setColor(new Color(30, 200, 255, (int)(200 * (1-totalRatio))));
        g2.fillRoundRect(px + 2, py + 3, w - 4, 10, 6, 6);

        // Lightning visual
        if (firingLightning) {
            g2.setColor(new Color(255, 255, 0, 200));
            int lx = px + (facingRight ? w : 0);
            int ly = cy;
            g2.setStroke(new BasicStroke(2f));
            Random rand = new Random();
            int curX = lx, curY = ly;
            for (int i = 0; i < 5; i++) {
                int nextX = lx + (facingRight ? 30 : -30) * (i + 1);
                int nextY = ly + rand.nextInt(20) - 10;
                g2.drawLine(curX, curY, nextX, nextY);
                curX = nextX; curY = nextY;
            }
            g2.setStroke(new BasicStroke(1f));
        }
    }

    private void drawStarForm(Graphics2D g2, int px, int py, long t) {
        int cx = px + w/2, cy = py + h/2;
        int pulse = (int)(Math.sin(t * 0.01) * 8);
        g2.setColor(new Color(255, 255, 200, 100));
        g2.fillOval(cx - 30 - pulse, cy - 30 - pulse, 60 + pulse*2, 60 + pulse*2);
        g2.setColor(Color.WHITE);
        for(int i=0; i<4; i++) {
            double a = t * 0.005 + i * Math.PI/2;
            int len = 25 + pulse;
            int x2 = cx + (int)(Math.cos(a) * len);
            int y2 = cy + (int)(Math.sin(a) * len);
            g2.setStroke(new BasicStroke(3f));
            g2.drawLine(cx, cy, x2, y2);
        }
        g2.setColor(new Color(255, 255, 220));
        g2.fillOval(cx - 12, cy - 12, 24, 24);
        g2.setColor(Color.WHITE);
        g2.fillOval(cx - 6, cy - 6, 12, 12);
        g2.setStroke(new BasicStroke(1f));
    }
}
