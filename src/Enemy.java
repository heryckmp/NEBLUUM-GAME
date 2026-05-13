import java.awt.*;

/**
 * Classe base abstrata para todos os inimigos.
 * Template Method: update() orquestra a lógica; subclasses implementam updateAI() e draw().
 *
 * Comportamento de knockback:
 *  - Durante invincibleTimer (frames logo apos tomar dano), a IA é pausada
 *    e o clampToBounds é desativado. Isso permite que o impulso do ataque
 *    empurre o inimigo para fora do mapa ou dentro de um buraco.
 */
public abstract class Enemy extends Entity {

    public int     hp, maxHp;
    public int     state = 0;       // 0 = ativo, 2 = morto
    public int     stateTimer;
    public boolean facingRight   = false;
    public int     contactDmg    = 1;
    public int     points        = 50;
    public int     animTimer     = 0;
    public boolean active        = false;
    public boolean isRidden      = false;
    public int     guaranteedDrop = -1;
    public int     invincibleTimer = 0;

    protected Enemy(float x, float y, int w, int h, int hp) {
        super(x, y, w, h);
        this.maxHp = hp;
        this.hp    = hp;
        this.stateTimer = 60 + (int)(Math.random() * 60);
    }

    // ── Contrato da Entity (nao utilizado diretamente) ──────
    @Override
    public final void update(Room room, double dt) {}

    // ── Template Method ─────────────────────────────────────
    public final void update(Room room, Player player, double dt) {
        if (state == 2) { stateTimer--; return; }
        if (invincibleTimer > 0) invincibleTimer--;
        animTimer++;

        if (!active) {
            float dx = x - player.x, dy = y - player.y;
            if (Math.sqrt(dx*dx + dy*dy) < 500) active = true;
            return;
        }

        boolean inKnockback = invincibleTimer > 0;

        if (!inKnockback) {
            // IA so roda fora do knockback — sem isso a IA anularia o impulso do acerto
            updateAI(room, player);
        } else {
            // Friccao suave: o inimigo desacelera naturalmente mas nao para de chofre
            vx *= 0.82f;
        }

        if (usesGravity()) {
            applyGravity(GameConstants.GRAVITY);
            if (isRidden) vx *= 0.6f;
        }

        moveAndCollide(room, vx, vy);
        afterMove(room);

        // Clamp so fora do knockback: inimigo empurrado pode sair do mapa e cair
        if (!inKnockback) {
            clampToBounds(room);
        }

        checkTrapDeath(room);
        checkFallDeath(room);
    }

    /** Cada subclasse implementa sua IA. */
    protected abstract void updateAI(Room room, Player player);

    /** Hook pos-movimento (ex: Turret reseta posicao x). */
    protected void afterMove(Room room) {}

    /** Ghost sobrescreve para false. */
    protected boolean usesGravity() { return true; }

    /** Boss e Turret sobrescrevem para true. */
    protected boolean isImmuneToTraps() { return false; }

    /** Boss sobrescreve para false (nao pode ser montado). */
    public boolean isMountable() { return true; }

    // ── Dano e morte ────────────────────────────────────────
    public void takeDamage(int dmg, float kx, float ky) {
        if (invincibleTimer > 0) return;
        hp -= dmg;
        vx = kx; vy = ky;
        invincibleTimer = 15;
        if (hp <= 0) { state = 2; stateTimer = 30; }
    }

    public boolean isDead() { return state == 2 && stateTimer <= 0; }
    public boolean dead()   { return isDead(); }

    // ── Helpers protegidos ──────────────────────────────────
    protected boolean checkHoleAhead(Room room) {
        float lookDist = Math.abs(vx) * 5f + 10f;
        float nextX = x + (facingRight ? w + lookDist : -lookDist);
        float footY = y + h + 4;
        int tx = (int)(nextX / GameConstants.TILE_SIZE);
        int ty = (int)(footY  / GameConstants.TILE_SIZE);
        if (facingRight && (x + w + lookDist) > room.getCols() * GameConstants.TILE_SIZE) return true;
        if (!facingRight && (x - lookDist) < 0) return true;
        Tile floor = room.getTile(tx, ty);
        return floor == null || (!floor.isSolid() && !floor.isPlatform());
    }

    protected void drawHPBar(Graphics2D g2, int px, int py) {
        if (hp >= maxHp) return;
        int bw = w + 4, bh = 4, bx = px - 2, by = py - 10;
        g2.setColor(new Color(60, 0, 0, 180));
        g2.fillRoundRect(bx, by, bw, bh, 2, 2);
        float ratio = (float)hp / maxHp;
        Color barColor = ratio > 0.5f ? new Color(80, 220, 80)
                       : ratio > 0.25f ? new Color(255, 200, 0)
                       : new Color(255, 60, 60);
        g2.setColor(barColor);
        g2.fillRoundRect(bx, by, (int)(bw * ratio), bh, 2, 2);
        g2.setColor(new Color(255, 255, 255, 40));
        g2.drawRoundRect(bx, by, bw, bh, 2, 2);
    }

    protected void drawRiddenIndicator(Graphics2D g2, int px, int py) {
        if (!isRidden) return;
        long t = System.currentTimeMillis();
        float pulse = (float)(Math.sin(t * 0.008) * 0.4 + 0.6);
        g2.setColor(new Color(255, 200, 50, (int)(180 * pulse)));
        g2.setStroke(new BasicStroke(3f));
        g2.drawArc(px, py - 8, w, 16, 0, 180);
        g2.setStroke(new BasicStroke(1.5f));
        g2.setColor(new Color(255, 255, 150, (int)(120 * pulse)));
        g2.drawLine(px + w/4, py - 2, px + 3*w/4, py - 2);
        g2.setStroke(new BasicStroke(1f));
    }

    // ── Privados ────────────────────────────────────────────
    private void checkTrapDeath(Room room) {
        if (isImmuneToTraps() || !usesGravity()) return;
        int tx1 = (int)(x / 32), tx2 = (int)((x + w - 0.1f) / 32);
        int ty1 = (int)(y / 32), ty2 = (int)((y + h + 2.0f) / 32);
        for (int ty = ty1; ty <= ty2; ty++)
            for (int tx = tx1; tx <= tx2; tx++) {
                Tile t = room.getTile(tx, ty);
                if (t != null && (t.isSpike() || t.isHazard())) {
                    state = 2; stateTimer = 0; hp = 0;
                }
            }
    }

    private void checkFallDeath(Room room) {
        // Morte imediata se saiu do mapa verticalmente
        if (y > room.getRows() * 32 + 8) {
            state = 2; stateTimer = 0; hp = 0;
            return;
        }
        // Morte rapida ao cair em buraco: inimigo esta caindo (vy > 0),
        // nao esta no chao, e nao ha nenhum tile solido nos proximos 4 tiles abaixo.
        // Isso faz o inimigo morrer assim que entra num buraco, sem ficar visivel no vazio.
        if (!usesGravity() || onGround || vy <= 0.5f) return;
        int tx1 = (int)(x / 32);
        int tx2 = (int)((x + w - 0.1f) / 32);
        int tyStart = (int)((y + h) / 32) + 1;
        int tyEnd   = tyStart + 4; // verifica 4 tiles abaixo
        boolean hasGroundBelow = false;
        for (int ty = tyStart; ty <= tyEnd && ty < room.getRows(); ty++) {
            for (int tx = tx1; tx <= tx2; tx++) {
                Tile t = room.getTile(tx, ty);
                if (t != null && (t.isSolid() || t.isPlatform())) {
                    hasGroundBelow = true;
                    break;
                }
            }
            if (hasGroundBelow) break;
        }
        if (!hasGroundBelow) {
            state = 2; stateTimer = 0; hp = 0;
        }
    }
}
