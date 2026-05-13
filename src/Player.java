import java.awt.*;
import java.awt.geom.*;
import java.util.Random;

public class Player extends Entity {
    // Status
    public int hp = 5, maxHp = 5, energy = 100, maxEnergy = 100;
    public int score = 0, coins = 0, bombs = 3, healthPotions = 0;
    public int livesCollected = 0;
    public int starPower = 0;
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
    private float moveSpeed = GameConstants.MOVE_SPEED;
    private float jumpPower = GameConstants.JUMP_POWER;
    private float acceleration = GameConstants.ACCELERATION;

    // Game Feel
    private int coyoteTimer = 0;
    private int jumpBufferTimer = 0;
    private final int MAX_COYOTE = GameConstants.COYOTE_FRAMES;
    private final int MAX_BUFFER = GameConstants.JUMP_BUFFER_FRAMES;

    public boolean facingRight = true;
    public boolean attacking = false;
    public boolean isDashing = false;
    public int invincible = 0;
    public int attackDuration = 0;
    private int dashCooldown = 0;

    // Rodeo
    public Enemy ridingEnemy = null;
    public int rodeoTimer = 0;
    private int rodeoDamageTick = 0;

    // Visual (publico para PlayerRenderer)
    public int animTick = 0;
    public float[] dashTrailX = new float[6];
    public float[] dashTrailY = new float[6];
    public int dashTrailHead = 0;
    public int healFlash = 0;
    public int emeraldBurstTimer = 0;
    public int starBurstTimer = 0;
    private int hitsSinceLastStarLoss = 0;

    public boolean inputLeft, inputRight, inputJump, inputDown,
                   inputAttack, inputDash, inputLightning, inputUse, inputDrop;

    private static final PlayerRenderer RENDERER = new PlayerRenderer();

    public Player(float x, float y) { super(x, y, 20, 32); }

    public void respawn(float x, float y) {
        this.x = x; this.y = y;
        vx = 0; vy = 0; hp = maxHp;
        invincible = 120;
        lightningAmmo = 100;
        livesCollected = 0;
        score = 0; coins = 0;
        isStar = false; starPower = 0;
        starBurstTimer = 0; emeraldBurstTimer = 0;
        healFlash = 0; hitsSinceLastStarLoss = 0;
        ridingEnemy = null; rodeoTimer = 0; rodeoDamageTick = 0;
    }

    @Override
    public void update(Room room, double dt) {
        animTick++;
        dashTrailX[dashTrailHead % dashTrailX.length] = x;
        dashTrailY[dashTrailHead % dashTrailY.length] = y;
        dashTrailHead++;

        if (ridingEnemy != null) {
            updateRodeo(room);
            if (healFlash > 0) healFlash--;
            if (emeraldBurstTimer > 0) emeraldBurstTimer--;
            if (starBurstTimer > 0) starBurstTimer--;
            if (invincible > 0) invincible--;
            return;
        }

        if (isDashing) { updateDash(room); return; }

        handleMovementLogic();
        handleJumpLogic();
        applyGravity(GameConstants.GRAVITY);
        moveAndCollide(room, vx, vy);
        handleCombatLogic(room);
        checkEnvironmentalDamage(room);

        if (invincible > 0) invincible--;
        if (dashCooldown > 0) dashCooldown--;
        if (energy < maxEnergy) energy++;
        if (healFlash > 0) healFlash--;
        if (emeraldBurstTimer > 0) emeraldBurstTimer--;
        if (starBurstTimer > 0) starBurstTimer--;

        if (y > GameConstants.FALL_DEATH_Y) hp = 0;

        if (starPower > 0 && animTick % Math.max(1, 10 - starPower) == 0) {
            room.spawnParticles(x + w/2, y + h/2, Color.YELLOW, 1, new Random());
        }
    }

    private void updateRodeo(Room room) {
        Enemy e = ridingEnemy;
        if (e.state == 2 || !room.enemies.contains(e)) { dismount(); return; }

        rodeoTimer++;
        x = e.x + e.w / 2f - w / 2f;
        y = e.y - h;
        vx = 0; vy = 0;
        facingRight = e.facingRight;

        rodeoDamageTick++;
        if (rodeoDamageTick >= GameConstants.RODEO_PASSIVE_INTERVAL) {
            rodeoDamageTick = 0;
            e.takeDamage(GameConstants.RODEO_PASSIVE_DAMAGE, 0, 0);
            room.spawnParticles(e.x + e.w/2, e.y, new Color(255,200,50), 5, new Random());
        }

        if (inputJump) {
            vy = jumpPower * 1.1f;
            e.takeDamage(2, e.facingRight ? 3 : -3, -3);
            dismount();
        }

        if (inputAttack && attackCooldown <= 0) {
            e.vx = (facingRight ? 1 : -1) * 5f;
            attackCooldown = 20;
            room.spawnParticles(e.x + e.w/2, e.y + e.h/2, new Color(255,120,0), 8, new Random());
        }
        if (attackCooldown > 0) attackCooldown--;
    }

    public boolean tryMount(Enemy e) {
        if (ridingEnemy != null || e.state == 2 || !e.isMountable()) return false;
        if (vy <= 0) return false;
        ridingEnemy = e;
        e.isRidden = true;
        rodeoTimer = 0; rodeoDamageTick = 0;
        invincible = 20; vy = 0;
        return true;
    }

    public void dismount() {
        if (ridingEnemy != null) { ridingEnemy.isRidden = false; ridingEnemy = null; }
        rodeoTimer = 0; rodeoDamageTick = 0; invincible = 20;
    }

    private void checkEnvironmentalDamage(Room room) {
        int ts = GameConstants.TILE_SIZE;
        int tx1 = (int)(x / ts), tx2 = (int)((x + w - 0.1f) / ts);
        int ty1 = (int)(y / ts), ty2 = (int)((y + h + 2.0f) / ts);
        for (int ty = ty1; ty <= ty2; ty++)
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

    private void handleMovementLogic() {
        if (inputLeft)       { vx -= acceleration; if (vx < -moveSpeed) vx = -moveSpeed; facingRight = false; }
        else if (inputRight) { vx += acceleration; if (vx >  moveSpeed) vx =  moveSpeed; facingRight = true; }
        else                   vx *= friction;
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
        if (inputAttack && attackCooldown <= 0) {
            attacking = true;
            attackDuration = GameConstants.ATTACK_DURATION;
            attackCooldown = GameConstants.ATTACK_COOLDOWN;
        }
        if (attacking) { attackDuration--; if (attackDuration <= 0) attacking = false; }
        // Raio: ammo infinita enquanto starPower estiver ativo
        if (inputLightning && (lightningAmmo > 0 || starPower > 0)) {
            firingLightning = true;
            if (starPower <= 0) lightningAmmo--; // so consome fora do star mode
        } else firingLightning = false;
        if (inputDash && dashCooldown <= 0 && energy >= GameConstants.DASH_COST) startDash();
    }

    private void startDash() {
        isDashing = true;
        dashCooldown = GameConstants.DASH_COOLDOWN;
        energy -= GameConstants.DASH_COST;
        vx = facingRight ? GameConstants.DASH_SPEED : -GameConstants.DASH_SPEED;
        vy = 0;
        invincible = GameConstants.INVINCIBLE_ON_DASH;
    }

    private void updateDash(Room room) {
        x += vx; invincible = 2;
        if (Math.abs(vx) > 0) vx *= 0.9f;
        if (Math.abs(vx) < 2) isDashing = false;
        handleTileCollision(room, true);
    }

    public void takeDamage(int dmg) {
        if (invincible > 0 || isDashing) return;

        // Defesa do star power: cada stack absorve ~14% do dano (7 stacks = imunidade total)
        if (starPower > 0) {
            float reduction = starPower / (float) GameConstants.STAR_POWER_MAX; // 0..1
            dmg = Math.max(0, dmg - (int)(dmg * reduction));
            // Mesmo com dano reduzido a 0, conta o hit para perda de stack
        }

        if (dmg > 0) hp -= dmg;
        invincible = GameConstants.INVINCIBLE_ON_HIT;
        if (starPower > 0) {
            hitsSinceLastStarLoss++;
            if (hitsSinceLastStarLoss >= GameConstants.HITS_TO_LOSE_STAR) {
                starPower = 0;
                hitsSinceLastStarLoss = 0;
                healFlash = 30;
            }
        }
    }

    public void heal(int amount)         { hp = Math.min(maxHp, hp + amount); healFlash = 55; }
    public void triggerStarBurst()       { starBurstTimer = 75; }
    public void increaseJumpPower()      { jumpPower -= 0.8f; healFlash = 30; }

    public Rectangle getAttackBox() {
        int aw = 32, ah = 32;
        return new Rectangle((int)(facingRight ? x + w : x - aw), (int)y, aw, ah);
    }

    @Override
    public void draw(Graphics2D g2) {
        RENDERER.draw(g2, this);
    }
}
