import java.awt.*;
import java.awt.geom.*;

public class Enemy extends Entity {
    public int hp, maxHp;
    public int type;
    public int state; // 0=active, 2=dead
    public int stateTimer;
    public boolean facingRight = false;
    public int contactDmg = 1;
    public int points = 50;
    public int animTimer = 0;
    public boolean active = false;
    public boolean isRidden = false;
    public int guaranteedDrop = -1;

    // Turret (type 6) — posição fixa
    private float spawnX, spawnY;

    public Enemy(float x, float y, int type, int hp) {
        super(x, y, 24, 28);
        this.type = type;
        this.maxHp = hp;
        this.hp = hp;
        this.spawnX = x;
        this.spawnY = y;
        stateTimer = 60 + (int)(Math.random() * 60);
        if (type == 4) { w = 48; h = 48; } // Boss maior
    }

    @Override
    public void update(Room room, double dt) {}

    public void update(Room room, Player player, double dt) {
        if (state == 2) { stateTimer--; return; }
        animTimer++;

        if (!active) {
            float dx = x - player.x, dy = y - player.y;
            if (Math.sqrt(dx*dx + dy*dy) < 500) active = true;
            return;
        }

        handleAI(room, player);

        // Gravidade apenas para tipos terrestres
        if (type != 2) applyGravity(0.42f);

        // Rodeo: velocidade reduzida
        if (isRidden && type != 2) { vx *= 0.6f; }

        moveAndCollide(room, vx, vy);

        // Ghost: clamp sem bounds de chão
        if (type == 2) {
            clampToBounds(room);
        } else {
            clampToBounds(room);
        }

        // Turret: sempre volta à posição x de spawn
        if (type == 6) { x = spawnX; vx = 0; }

        // Verifica morte por armadilhas (exceto Ghost e Boss)
        if (type != 2 && type != 4 && type != 6) {
            int tx1 = (int) (x / 32);
            int tx2 = (int) ((x + w - 0.1f) / 32);
            int ty1 = (int) (y / 32);
            int ty2 = (int) ((y + h + 2.0f) / 32);
            for (int ty = ty1; ty <= ty2; ty++) {
                for (int tx = tx1; tx <= tx2; tx++) {
                    Tile t = room.getTile(tx, ty);
                    if (t != null && (t.isSpike() || t.isHazard())) {
                        state = 2; // Morte imediata ao cair na armadilha
                        stateTimer = 0;
                        hp = 0;
                    }
                }
            }
        }

        // Caiu fora do mapa → morte imediata
        if (y > room.getRows() * 32 + 64) { state = 2; stateTimer = 0; }
    }

    // ---------------------------------------------------------------
    // lookahead proporcional à velocidade para checagem de buracos
    // ---------------------------------------------------------------
    private boolean checkHoleAhead(Room room) {
        float speed = Math.abs(vx);
        float lookDist = speed * 5f + 10f; // lookahead dinâmico
        float nextX = x + (facingRight ? w + lookDist : -lookDist);
        float footY = y + h + 4;
        int tx = (int)(nextX / 32);
        int ty = (int)(footY / 32);
        Tile floor = room.getTile(tx, ty);
        // Também checa borda do mapa como "buraco"
        if (facingRight && (x + w + lookDist) > room.getCols() * 32) return true;
        if (!facingRight && (x - lookDist) < 0) return true;
        // Considera buraco se for vazio, ou se não for chão sólido/plataforma (incluindo lava e espinhos)
        return floor == null || (!floor.isSolid() && !floor.isPlatform());
    }

    private void handleAI(Room room, Player player) {
        boolean holeAhead = onGround && checkHoleAhead(room);

        switch (type) {
            case 0 -> aiWalker(room, holeAhead);
            case 1 -> aiShooter(room, player, holeAhead);
            case 2 -> aiGhost(player);
            case 3 -> aiChaser(room, player, holeAhead);
            case 4 -> aiBoss(room, player, holeAhead);
            case 5 -> aiBomber(room, player, holeAhead);
            case 6 -> aiTurret(room, player);
        }
    }

    // TYPE 0 — Walker
    private void aiWalker(Room room, boolean holeAhead) {
        if (stateTimer-- <= 0 || holeAhead) {
            facingRight = !facingRight;
            stateTimer = 50 + (int)(Math.random() * 60);
        }
        vx = facingRight ? 1.6f : -1.6f;
    }

    // TYPE 1 — Shooter: strafe mantendo distância, CHECA BURACOS
    private void aiShooter(Room room, Player player, boolean holeAhead) {
        float dx = player.x - x;
        float dist = Math.abs(dx);

        if (holeAhead) {
            vx = 0; // Para na beira do buraco e atira!
            facingRight = dx > 0; // Olha pro player
        } else {
            facingRight = dx > 0;
            if (dist > 260) {
                vx = dx > 0 ? 1.4f : -1.4f;
            } else if (dist < 100) {
                vx = dx > 0 ? -1.2f : 1.2f;
            } else {
                vx *= 0.85f;
            }
        }

        if (stateTimer-- <= 0) {
            room.addProjectile(x + w/2f, y + h/2f, facingRight ? 5 : -5);
            stateTimer = 70 + (int)(Math.random() * 30);
        }
    }

    // TYPE 2 — Ghost: persegue livremente, sem colisão com tiles
    private void aiGhost(Player player) {
        float dx = player.x - x;
        float dy = player.y - y;
        float dist = (float)Math.sqrt(dx*dx + dy*dy);
        float spd = dist < 150 ? 2.4f : 1.6f;
        vx += (dx > 0 ? spd : -spd) * 0.12f;
        vy += (dy > 0 ? spd : -spd) * 0.12f;
        float maxV = spd;
        if (Math.abs(vx) > maxV) vx = vx > 0 ? maxV : -maxV;
        if (Math.abs(vy) > maxV) vy = vy > 0 ? maxV : -maxV;
        // Ghost montado: amortece vy para não prender em teto
        if (isRidden) vy *= 0.5f;
        facingRight = dx > 0;
    }

    // TYPE 3 — Chaser: pula sobre buracos
    private void aiChaser(Room room, Player player, boolean holeAhead) {
        float dx = player.x - x;
        if (onGround) facingRight = dx > 0;

        if (holeAhead && onGround) {
            vy = -9.0f;
            vx = facingRight ? 4.5f : -4.5f;
        } else if (onGround) {
            vx = dx > 0 ? 2.8f : -2.8f;
        }
        // No ar mantém impulso horizontal
    }

    // TYPE 4 — Boss: persegue e atira em leque, CHECA BURACOS
    private void aiBoss(Room room, Player player, boolean holeAhead) {
        float dx = player.x - x;

        if (holeAhead) {
            vx = 0; // Para na borda
            facingRight = dx > 0;
        } else {
            facingRight = dx > 0;
            vx = dx > 0 ? 2.2f : -2.2f;
        }

        if (stateTimer-- <= 0) {
            float bx = x + w/2f, by = y + h/2f;
            float dir = facingRight ? 1 : -1;
            room.addProjectile(bx, by,       dir * 6.0f);
            room.addProjectile(bx, by - 12,  dir * 5.5f);
            room.addProjectile(bx, by + 12,  dir * 5.5f);
            stateTimer = 50;
        }
    }

    // TYPE 5 — Bomber: anda no chão, lança bombas com arco
    private void aiBomber(Room room, Player player, boolean holeAhead) {
        float dx = player.x - x;
        if (onGround && !holeAhead) facingRight = dx > 0;

        if (holeAhead && onGround) {
            vx = 0; // Para na borda
            facingRight = dx > 0; // Continua olhando pro player para atirar
        } else if (onGround) {
            vx = facingRight ? 1.8f : -1.8f;
        }

        if (stateTimer-- <= 0 && onGround) {
            // Lança projétil em arco (vy negativo)
            float bvx = facingRight ? 3.5f : -3.5f;
            room.addArcProjectile(x + w/2f, y, bvx, -6f, 2); // dmg=2
            stateTimer = 90 + (int)(Math.random() * 60);
        }
    }

    // TYPE 6 — Turret: fixa no chão, atira em múltiplos ângulos
    private void aiTurret(Room room, Player player) {
        float dx = player.x - x;
        facingRight = dx > 0;
        vx = 0; vy = 0;

        if (stateTimer-- <= 0) {
            float bx = x + w/2f, by = y + h/3f;
            float dir = facingRight ? 1 : -1;
            // Rajada tripla: horizontal + diagonal
            room.addProjectile(bx, by, dir * 5.0f);
            room.addArcProjectile(bx, by, dir * 4.0f, -3.0f, 1);
            room.addArcProjectile(bx, by, dir * 4.0f, 3.0f, 1);
            stateTimer = 80 + (int)(Math.random() * 40);
        }
    }

    public void takeDamage(int dmg, float kx, float ky) {
        hp -= dmg;
        vx = kx; vy = ky;
        if (hp <= 0) { state = 2; stateTimer = 30; }
    }

    public boolean isDead() { return state == 2 && stateTimer <= 0; }
    public boolean dead() { return isDead(); }

    @Override
    public void draw(Graphics2D g2) {
        if (state == 2) return;
        int px = (int)x, py = (int)y;
        long t = System.currentTimeMillis();

        switch (type) {
            case 0 -> drawWalker(g2, px, py, t);
            case 1 -> drawShooter(g2, px, py, t);
            case 2 -> drawGhost(g2, px, py, t);
            case 3 -> drawChaser(g2, px, py, t);
            case 4 -> drawBoss(g2, px, py, t);
            case 5 -> drawBomber(g2, px, py, t);
            case 6 -> drawTurret(g2, px, py, t);
            default -> drawWalker(g2, px, py, t);
        }
        drawHPBar(g2, px, py);

        if (isRidden) {
            long t2 = System.currentTimeMillis();
            float pulse = (float)(Math.sin(t2 * 0.008) * 0.4 + 0.6);
            g2.setColor(new Color(255, 200, 50, (int)(180 * pulse)));
            g2.setStroke(new BasicStroke(3f));
            g2.drawArc(px, py - 8, w, 16, 0, 180);
            g2.setStroke(new BasicStroke(1.5f));
            g2.setColor(new Color(255, 255, 150, (int)(120 * pulse)));
            g2.drawLine(px + w/4, py - 2, px + 3*w/4, py - 2);
            g2.setStroke(new BasicStroke(1f));
        }
    }

    private void drawHPBar(Graphics2D g2, int px, int py) {
        if (hp >= maxHp) return;
        int bw = w + 4, bh = 4;
        int bx = px - 2, by = py - 10;
        g2.setColor(new Color(60, 0, 0, 180));
        g2.fillRoundRect(bx, by, bw, bh, 2, 2);
        float ratio = (float)hp / maxHp;
        Color barColor = ratio > 0.5f ? new Color(80, 220, 80) : ratio > 0.25f ? new Color(255, 200, 0) : new Color(255, 60, 60);
        g2.setColor(barColor);
        g2.fillRoundRect(bx, by, (int)(bw * ratio), bh, 2, 2);
        g2.setColor(new Color(255, 255, 255, 40));
        g2.drawRoundRect(bx, by, bw, bh, 2, 2);
    }

    // ---- Draw helpers ----
    private void drawWalker(Graphics2D g2, int px, int py, long t) {
        int cx = px + w/2, cy = py + h/2;
        int pulse = (int)(Math.sin(t * 0.005 + px * 0.04) * 25);
        g2.setColor(new Color(80, 40, 200, 18 + pulse/3));
        g2.fillOval(px-7, py-7, w+14, h+14);
        int[] hx = new int[6], hy = new int[6];
        for (int i = 0; i < 6; i++) {
            double a = Math.PI/6 + i * Math.PI/3;
            hx[i] = cx + (int)(Math.cos(a)*(w/2-1));
            hy[i] = cy + (int)(Math.sin(a)*(h/2-2));
        }
        g2.setPaint(new GradientPaint(px, py, new Color(60,30,160), px, py+h, new Color(20,8,80)));
        g2.fillPolygon(hx, hy, 6);
        g2.setPaint(null);
        g2.setColor(new Color(120,80,255,70));
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawPolygon(hx, hy, 6);
        g2.setStroke(new BasicStroke(1f));
        int eyeR = 3 + (int)(Math.sin(t*0.008)*2);
        g2.setColor(new Color(200,160,255,80+pulse));
        g2.fillOval(cx-eyeR-2, cy-eyeR-2, (eyeR+2)*2, (eyeR+2)*2);
        g2.setColor(new Color(255,255,255,220));
        g2.fillOval(cx-2, cy-2, 4, 4);
        for (int i = 0; i < 3; i++) {
            double a = t*0.003 + i*Math.PI*2/3;
            int lx = cx + (int)(Math.cos(a)*(w/2+3));
            int ly = cy + (int)(Math.sin(a)*(h/2+1));
            g2.setColor(new Color(100,60,220,140));
            g2.fillOval(lx-2, ly-2, 4, 4);
        }
    }

    private void drawShooter(Graphics2D g2, int px, int py, long t) {
        int cx = px + w/2, cy = py + h/2;
        int pulse = (int)(Math.sin(t * 0.004) * 20);
        g2.setColor(new Color(255,120,0,22+pulse/2));
        g2.fillOval(px-6, py-6, w+12, h+12);
        g2.setPaint(new GradientPaint(px, py, new Color(220,110,20), px, py+h, new Color(130,55,5)));
        g2.fillOval(px+2, py+4, w-4, h-6);
        g2.setPaint(null);
        g2.setStroke(new BasicStroke(2f));
        g2.setColor(new Color(255,160,40,70));
        g2.drawOval(px-5, cy-4, w+10, 8);
        g2.setStroke(new BasicStroke(1f));
        int eyeP = (int)(Math.sin(t*0.007)*35);
        g2.setColor(new Color(255,50+eyeP,0));
        g2.fillOval(px+4, py+6, 5, 5);
        g2.fillOval(px+w-9, py+6, 5, 5);
        int canX = facingRight ? px+w : px-6;
        g2.setColor(new Color(140,70,0));
        g2.fillRect(canX, cy-2, 6, 4);
        g2.setColor(new Color(255,180,0,120+pulse));
        g2.fillOval(canX+(facingRight?4:-3), cy-4, 6, 8);
    }

    private void drawGhost(Graphics2D g2, int px, int py, long t) {
        float wave = (float)(Math.sin(t*0.003 + px*0.04)*5);
        int gy = py + (int)wave;
        g2.setColor(new Color(140,20,200,25));
        g2.fillOval(px-6, gy-6, w+12, h+8);
        g2.setColor(new Color(120,0,200,70));
        g2.fillOval(px, gy, w, h-4);
        g2.setColor(new Color(180,30,255,55));
        g2.fillOval(px+2, gy+2, w-4, h-8);
        int eyeA = 150 + (int)(Math.sin(t*0.006)*70);
        g2.setColor(new Color(240,140,255,eyeA));
        g2.fillOval(px+4, gy+7, 6, 6);
        g2.fillOval(px+w-10, gy+7, 6, 6);
        g2.setColor(new Color(255,220,255,210));
        g2.fillOval(px+5, gy+8, 3, 3);
        g2.fillOval(px+w-9, gy+8, 3, 3);
        g2.setColor(new Color(200,60,255,100));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawArc(px+5, gy+14, w-10, 7, 200, 140);
        g2.setStroke(new BasicStroke(1f));
    }

    private void drawChaser(Graphics2D g2, int px, int py, long t) {
        float speed = Math.abs(vx);
        if (speed > 0.8f) {
            int trailDir = vx > 0 ? -1 : 1;
            for (int i = 1; i <= 4; i++) {
                int alpha = 55 - i*12;
                int r = 200 - i*20; int gb = 60 - i*10;
                g2.setColor(new Color(Math.max(0,r), Math.max(0,gb), 80, Math.max(0,alpha)));
                g2.fillOval(px + trailDir*i*6, py+5, w-2, h-8);
            }
        }
        int[] cx2 = facingRight
            ? new int[]{px, px+w, px+w-3, px+w-5, px+2}
            : new int[]{px+w, px, px+3, px+5, px+w-2};
        int[] cy2 = {py+h/2, py+5, py+4, py+h-3, py+h-4};
        g2.setPaint(new GradientPaint(px, py, new Color(160,40,80), px, py+h, new Color(80,10,40)));
        g2.fillPolygon(cx2, cy2, 5);
        g2.setPaint(null);
        int eyeX = facingRight ? px+w-9 : px+4;
        int ep = (int)(Math.sin(t*0.009)*25);
        g2.setColor(new Color(255,180+ep,40));
        g2.fillOval(eyeX, py+h/2-4, 7, 7);
        g2.setColor(Color.WHITE);
        g2.fillOval(eyeX+2, py+h/2-2, 3, 3);
    }

    private void drawBoss(Graphics2D g2, int px, int py, long t) {
        int bw = w, bh = h;
        int bcx = px + bw/2, bcy = py + bh/2;
        double diskAngle = t * 0.0015;
        int plasma = (int)(Math.sin(t*0.003)*28);
        for (int ring = 3; ring >= 0; ring--) {
            int rExtra = ring*12, rAlpha = 8+ring*5;
            g2.setColor(new Color(80,0,160,rAlpha));
            g2.fillOval(px-rExtra, py-rExtra, bw+rExtra*2, bh+rExtra*2);
        }
        g2.setStroke(new BasicStroke(5f));
        for (int seg = 0; seg < 12; seg++) {
            float ratio = (float)seg/12;
            int r2=(int)(100+ratio*155), g3=(int)(40+ratio*120), a2=(int)(180-ratio*100);
            g2.setColor(new Color(r2,g3,10,Math.max(0,a2+plasma)));
            double startA = diskAngle*180/Math.PI + seg*30;
            g2.drawArc(px-4, bcy-10, bw+8, 20, (int)startA, 28);
        }
        g2.setStroke(new BasicStroke(1f));
        g2.setPaint(new GradientPaint(px, py, new Color(30,0,60), px, py+bh, new Color(5,0,20)));
        g2.fillOval(px, py+bh/4, bw, bh*3/4);
        g2.setPaint(null);
        g2.setPaint(new GradientPaint(px, py, new Color(50,0,90), px, py+bh/3, new Color(20,0,50)));
        g2.fillRoundRect(px+4, py, bw-8, bh/2, 12, 12);
        g2.setPaint(null);
        g2.setColor(Color.BLACK);
        g2.fillOval(bcx-bw/5, bcy-bh/5, bw*2/5, bh*2/5);
        int eyeP = (int)(Math.sin(t*0.006)*45);
        g2.setColor(new Color(255,80+eyeP,255));
        g2.fillOval(px+8, py+8, 10, 9);
        g2.fillOval(px+bw-18, py+8, 10, 9);
        g2.setColor(new Color(255,220,255,200));
        g2.fillOval(px+10, py+10, 5, 4);
        g2.fillOval(px+bw-16, py+10, 5, 4);
        g2.setColor(new Color(160,0,255,90));
        g2.setStroke(new BasicStroke(2f));
        g2.drawRoundRect(px, py+bh/4, bw, bh*3/4, 8, 8);
        g2.drawRoundRect(px+4, py, bw-8, bh/2, 12, 12);
        g2.setStroke(new BasicStroke(1f));
    }

    // TYPE 5 — Bomber: corpo laranja com fumaça
    private void drawBomber(Graphics2D g2, int px, int py, long t) {
        int cx = px + w/2, cy = py + h/2;
        int pulse = (int)(Math.sin(t*0.006)*20);
        // Aura de fogo
        g2.setColor(new Color(255,80,0,18+pulse/3));
        g2.fillOval(px-6, py-6, w+12, h+12);
        // Corpo hexagonal achatado
        g2.setPaint(new GradientPaint(px, py, new Color(200,80,0), px, py+h, new Color(100,30,0)));
        g2.fillRoundRect(px+2, py+4, w-4, h-6, 8, 8);
        g2.setPaint(null);
        // Olhos de chama
        g2.setColor(new Color(255,220,0, 180+pulse));
        g2.fillOval(px+3, py+8, 6, 5);
        g2.fillOval(px+w-9, py+8, 6, 5);
        g2.setColor(Color.WHITE);
        g2.fillOval(px+4, py+9, 3, 3);
        g2.fillOval(px+w-8, py+9, 3, 3);
        // Bomba na mão
        g2.setColor(new Color(30,30,40));
        int bombX = facingRight ? px+w-4 : px-6;
        g2.fillOval(bombX, cy-4, 10, 10);
        g2.setColor(new Color(255,200,0,180+pulse));
        g2.fillOval(bombX+3, cy-8, 4, 5); // Fusível
        // Borda
        g2.setColor(new Color(255,120,0,90));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(px+2, py+4, w-4, h-6, 8, 8);
        g2.setStroke(new BasicStroke(1f));
    }

    // TYPE 6 — Turret: canhão fixo no chão
    private void drawTurret(Graphics2D g2, int px, int py, long t) {
        int cx = px + w/2, cy = py + h/2;
        int pulse = (int)(Math.sin(t*0.005)*18);
        // Base sólida
        g2.setPaint(new GradientPaint(px, py+h/2, new Color(40,20,80), px, py+h, new Color(15,5,35)));
        g2.fillRect(px, py+h/2, w, h/2);
        g2.setPaint(null);
        // Corpo giratório
        g2.setPaint(new GradientPaint(px, py, new Color(80,40,160), px, py+h/2, new Color(40,15,80)));
        g2.fillRoundRect(px+2, py+4, w-4, h/2, 8, 8);
        g2.setPaint(null);
        // Canhão
        int canDir = facingRight ? 1 : -1;
        int canX = cx + canDir*4;
        g2.setColor(new Color(60,30,120));
        g2.fillRect(canX - (facingRight ? 0 : 12), cy-3, 12, 6);
        // Boca do canhão pulsante
        g2.setColor(new Color(180,80,255,140+pulse));
        g2.fillOval(canX + (facingRight ? 10 : -8), cy-4, 8, 8);
        // Olho central
        g2.setColor(new Color(200,100,255, 160+pulse));
        g2.fillOval(cx-4, py+8, 8, 8);
        g2.setColor(Color.WHITE);
        g2.fillOval(cx-2, py+10, 4, 4);
        // Borda
        g2.setColor(new Color(120,60,200,80));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(px+2, py+4, w-4, h/2, 8, 8);
        g2.setStroke(new BasicStroke(1f));
    }
}
