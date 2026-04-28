
import java.awt.*;
import java.awt.geom.*;

public class Player extends Entity {
    // Status
    public int hp = 12, maxHp = 12, energy = 100, maxEnergy = 100;
    public int score = 0, coins = 0, bombs = 3, healthPotions = 2;
    public int shield = 0;

    // Combate
    public int attackDamage = 3;
    public int attackKnockbackX = 6;
    public int attackKnockbackY = -4;
    public int attackCooldown = 0;

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

    public boolean inputLeft, inputRight, inputJump, inputDown, inputAttack, inputDash, inputUse, inputDrop;

    public Player(float x, float y) { super(x, y, 20, 32); }

    public void respawn(float x, float y) {
        this.x = x; this.y = y;
        vx = 0; vy = 0; hp = maxHp;
        invincible = 120;
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

        if (invincible > 0) invincible--;
        if (dashCooldown > 0) dashCooldown--;
        if (energy < maxEnergy) energy++;
        if (y > 800) hp = 0;
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
    public void heal(int amount) { hp = Math.min(maxHp, hp + amount); }

    public Rectangle getAttackBox() {
        int aw = 32, ah = 32;
        return new Rectangle((int)(facingRight ? x + w : x - aw), (int)y, aw, ah);
    }

    @Override
    public void draw(Graphics2D g2) {
        if (invincible > 0 && !isDashing && (invincible / 3) % 2 == 0) return;
        int px = (int)x, py = (int)y;
        long t = System.currentTimeMillis();

        // Nebula dash trail (violet → cyan)
        if (isDashing) {
            for (int i = 1; i < dashTrailX.length; i++) {
                int idx = (dashTrailHead - i + dashTrailX.length * 10) % dashTrailX.length;
                if (dashTrailX[idx] == 0) continue;
                int alpha = Math.max(0, 115 - i * 22);
                float ratio = (float)i / dashTrailX.length;
                int r = (int)(130 * ratio); int gb = 210 - (int)(70 * ratio);
                g2.setColor(new Color(r, gb, 255, alpha));
                g2.fillRoundRect((int)dashTrailX[idx] + 3, (int)dashTrailY[idx] + 8, w - 6, h - 16, 8, 8);
            }
        }

        // Shield aura
        if (shield > 0) {
            int sp = (int)(Math.sin(animTick * 0.12) * 5);
            g2.setColor(new Color(100, 60, 255, 28 + sp * 3));
            g2.fillOval(px - 11, py - 11, w + 22, h + 22);
            g2.setColor(new Color(160, 100, 255, 75));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawOval(px - 11, py - 11, w + 22, h + 22);
            g2.setStroke(new BasicStroke(1f));
        }

        // Outer suit glow
        Color suitGlow = isDashing ? new Color(120, 40, 255, 90) : new Color(60, 120, 255, 35);
        g2.setColor(suitGlow);
        g2.fillRoundRect(px - 4, py - 4, w + 8, h + 8, 14, 14);

        // Thruster pack (back side)
        int thrX = facingRight ? px - 4 : px + w - 1;
        g2.setColor(new Color(25, 22, 45));
        g2.fillRoundRect(thrX, py + 13, 5, h - 22, 3, 3);
        // Thruster flame
        if (Math.abs(vx) > 0.5f || isDashing) {
            int fLen = isDashing ? 16 : 7 + (int)(Math.sin(t * 0.025) * 3);
            int fDir = facingRight ? -fLen : 5;
            int fa = isDashing ? 190 : 80 + (int)(Math.sin(t * 0.02) * 35);
            g2.setColor(new Color(60, 180, 255, fa));
            g2.fillOval(thrX + fDir, py + h - 21, fLen + 2, 8);
            g2.setColor(new Color(180, 80, 255, fa / 2));
            g2.fillOval(thrX + fDir + 2, py + h - 20, fLen - 3, 5);
        }

        // Legs (spacesuit boots)
        g2.setColor(new Color(22, 18, 50));
        g2.fillRoundRect(px + 2, py + h - 10, 7, 10, 3, 3);
        g2.fillRoundRect(px + w - 9, py + h - 10, 7, 10, 3, 3);
        // Boot glow strip
        g2.setColor(new Color(80, 160, 255, 55));
        g2.fillRect(px + 2, py + h - 2, 7, 2);
        g2.fillRect(px + w - 9, py + h - 2, 7, 2);

        // Suit body
        Color suitTop = isDashing ? new Color(100, 60, 255) : new Color(38, 30, 85);
        Color suitBot = isDashing ? new Color(60, 20, 200) : new Color(18, 14, 50);
        g2.setPaint(new GradientPaint(px, py + 10, suitTop, px, py + h - 10, suitBot));
        g2.fillRoundRect(px + 1, py + 10, w - 2, h - 18, 6, 6);
        g2.setPaint(null);

        // Chest panel LEDs
        int led1A = 80 + (int)(Math.sin(t * 0.004) * 40);
        int led2A = 80 + (int)(Math.sin(t * 0.004 + 1.5) * 40);
        g2.setColor(new Color(0, 220, 255, led1A));
        g2.fillRect(px + 5, py + 17, 4, 2);
        g2.setColor(new Color(180, 80, 255, led2A));
        g2.fillRect(px + 11, py + 17, 3, 2);
        // Suit seam
        g2.setColor(new Color(120, 100, 255, 30));
        g2.drawLine(px + w / 2, py + 12, px + w / 2, py + h - 12);

        // Helmet (rounded)
        g2.setPaint(new GradientPaint(px, py, new Color(55, 45, 100), px, py + 16, new Color(28, 22, 65)));
        g2.fillRoundRect(px, py, w, 18, 10, 10);
        g2.setPaint(null);

        // Visor (oval, glowing)
        int vx2 = px + 2; int vy = py + 3;
        g2.setColor(new Color(30, 200, 255, 200));
        g2.fillRoundRect(vx2, vy, w - 4, 10, 6, 6);
        // Visor inner reflection
        g2.setColor(new Color(255, 255, 255, 60));
        g2.fillRoundRect(vx2 + 2, vy + 1, (w - 8) / 2, 3, 2, 2);
        // Visor star reflection
        int starA = 80 + (int)(Math.sin(t * 0.003) * 40);
        g2.setColor(new Color(255, 255, 255, starA));
        g2.fillOval(facingRight ? px + w - 7 : px + 3, vy + 2, 2, 2);

        // Helmet neon rim
        g2.setColor(new Color(80, 160, 255, 70));
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(px, py, w, 18, 10, 10);
        // Suit outline
        g2.setColor(new Color(100, 80, 255, 50));
        g2.drawRoundRect(px + 1, py + 10, w - 2, h - 18, 6, 6);
        g2.setStroke(new BasicStroke(1f));

        // Plasma attack arc
        if (attacking) {
            int arcX = facingRight ? px + w - 2 : px - 36;
            int arcY = py + h / 2 - 18;
            int al = (int)(230 * (attackDuration / 8f));
            g2.setColor(new Color(255, 160, 0, al));
            g2.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawArc(arcX, arcY, 38, 38, facingRight ? -60 : 120, facingRight ? 120 : -120);
            g2.setColor(new Color(255, 255, 120, al));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawArc(arcX + 4, arcY + 4, 30, 30, facingRight ? -50 : 130, facingRight ? 100 : -100);
            g2.setStroke(new BasicStroke(1f));
        }
    }
}
