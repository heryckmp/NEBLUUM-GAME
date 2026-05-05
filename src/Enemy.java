
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
    public boolean isRidden = false; // true quando o player está montado neste inimigo
    public int guaranteedDrop = -1;

    public Enemy(float x, float y, int type, int hp) {
        super(x, y, 24, 28);
        this.type = type;
        this.maxHp = hp;
        this.hp = hp;
        stateTimer = 60 + (int)(Math.random() * 60);
    }

    @Override
    public void update(Room room, double dt) {}

    public void update(Room room, Player player, double dt) {
        if (state == 2) { stateTimer--; return; }
        animTimer++;

        if (!active) {
            float dx = x - player.x, dy = y - player.y;
            if (Math.sqrt(dx * dx + dy * dy) < 400) active = true;
            return;
        }

        handleAI(room, player);
        if (type != 2) applyGravity(0.4f); // Ghost flutua, sem gravidade
        // Quando montado: velocidade reduzida (player dirige via ataque)
        if (isRidden) { vx *= 0.6f; }
        moveAndCollide(room, vx, vy);
        if (y > 800) { state = 2; stateTimer = 0; }
    }

    private void handleAI(Room room, Player player) {
        float nextX = x + (facingRight ? w + 5 : -5);
        float footY = y + h + 2;
        int tx = (int)(nextX / 32);
        int ty = (int)(footY / 32);
        Tile floorAhead = room.getTile(tx, ty);
        boolean holeAhead = floorAhead == null || (!floorAhead.isSolid() && !floorAhead.isPlatform() && !floorAhead.isSpike());

        if (type == 0) { // Walker — vira mais rápido, velocidade ligeiramente maior
            if (stateTimer-- <= 0 || holeAhead) { 
                facingRight = !facingRight; 
                stateTimer = 40 + (int)(Math.random() * 60); 
            }
            vx = facingRight ? 1.6f : -1.6f;
        } else if (type == 1) { // Shooter — se move lateralmente E atira mais rápido
            float dx = player.x - x;
            float dy = player.y - y;
            facingRight = dx > 0;
            // Strafe: se aproxima do player mantendo distância de 96–250px
            float dist = Math.abs(dx);
            if (dist > 250) {
                vx = dx > 0 ? 1.4f : -1.4f; // avança
            } else if (dist < 96) {
                vx = dx > 0 ? -1.2f : 1.2f; // recua
            } else {
                vx *= 0.85f; // freio suave na faixa ideal
            }
            // Atira com cadência ajustada por distância
            if (stateTimer-- <= 0) {
                room.addProjectile(x + w/2, y + h/2, facingRight ? 5 : -5);
                stateTimer = 70 + (int)(Math.random() * 30);
            }
        } else if (type == 2) { // Ghost — persegue diretamente, acelera ao se aproximar
            float dx = player.x - x;
            float dy = player.y - y;
            float dist = (float)Math.sqrt(dx*dx + dy*dy);
            float spd = dist < 150 ? 2.2f : 1.5f; // mais rápido perto
            vx += (dx > 0 ? spd : -spd) * 0.12f;
            vy += (dy > 0 ? spd : -spd) * 0.12f;
            // Limita velocidade
            float maxV = spd;
            if (Math.abs(vx) > maxV) vx = vx > 0 ? maxV : -maxV;
            if (Math.abs(vy) > maxV) vy = vy > 0 ? maxV : -maxV;
            facingRight = dx > 0;
        } else if (type == 3) { // Chaser — pula sobre buracos largos, mais veloz
            float dx = player.x - x;
            if (onGround) facingRight = dx > 0; // Só muda de direção no chão
            
            if (holeAhead && onGround) {
                vy = -8.5f; // Pulo mais forte
                vx = facingRight ? 4.2f : -4.2f; // Impulso longo
            } else if (onGround) {
                vx = dx > 0 ? 2.6f : -2.6f;
            }
            // No ar, ele mantém o vx do pulo e não reseta
        } else if (type == 4) { // Boss — mais agressivo, atira em leque
            float dx = player.x - x;
            facingRight = dx > 0;
            vx = dx > 0 ? 2.2f : -2.2f;
            if (stateTimer-- <= 0) {
                room.addProjectile(x + w/2, y + h/2, facingRight ? 6 : -6);
                room.addProjectile(x + w/2, y + h/2 - 10, facingRight ? 5.5f : -5.5f);
                room.addProjectile(x + w/2, y + h/2 + 10, facingRight ? 5.5f : -5.5f);
                stateTimer = 55;
            }
        }
    }

    public void takeDamage(int dmg, float kx, float ky) {
        hp -= dmg; vx = kx; vy = ky;
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
            default -> drawWalker(g2, px, py, t);
        }

        drawHPBar(g2, px, py);

        // Sela de rodeo: arco dourado em cima do inimigo
        if (isRidden) {
            long t2 = System.currentTimeMillis();
            float pulse = (float)(Math.sin(t2 * 0.008) * 0.4 + 0.6);
            g2.setColor(new Color(255, 200, 50, (int)(180 * pulse)));
            g2.setStroke(new BasicStroke(3f));
            g2.drawArc(px, py - 8, w, 16, 0, 180); // arco acima
            g2.setStroke(new BasicStroke(1.5f));
            g2.setColor(new Color(255, 255, 150, (int)(120 * pulse)));
            g2.drawLine(px + w/4, py - 2, px + 3*w/4, py - 2); // linha de sela
            g2.setStroke(new BasicStroke(1f));
        }
    }

    private void drawHPBar(Graphics2D g2, int px, int py) {
        if (hp >= maxHp) return;
        int bw = w + 4, bh = 4;
        int bx = px - 2, by = py - 8;
        g2.setColor(new Color(60, 0, 0, 180));
        g2.fillRoundRect(bx, by, bw, bh, 2, 2);
        float ratio = (float)hp / maxHp;
        Color barColor = ratio > 0.5f ? new Color(80, 220, 80) : ratio > 0.25f ? new Color(255, 200, 0) : new Color(255, 60, 60);
        g2.setColor(barColor);
        g2.fillRoundRect(bx, by, (int)(bw * ratio), bh, 2, 2);
        g2.setColor(new Color(255, 255, 255, 40));
        g2.drawRoundRect(bx, by, bw, bh, 2, 2);
    }

    // Tipo 0 — Pulsar Drone: hexagonal, magnetic field aura
    private void drawWalker(Graphics2D g2, int px, int py, long t) {
        int cx = px + w / 2, cy = py + h / 2;
        int pulse = (int)(Math.sin(t * 0.005 + px * 0.04) * 25);
        // Magnetic field aura
        g2.setColor(new Color(80, 40, 200, 18 + pulse / 3));
        g2.fillOval(px - 7, py - 7, w + 14, h + 14);
        // Hexagonal body
        int[] hx = new int[6], hy = new int[6];
        for (int i = 0; i < 6; i++) {
            double a = Math.PI / 6 + i * Math.PI / 3;
            hx[i] = cx + (int)(Math.cos(a) * (w / 2 - 1));
            hy[i] = cy + (int)(Math.sin(a) * (h / 2 - 2));
        }
        g2.setPaint(new GradientPaint(px, py, new Color(60, 30, 160), px, py + h, new Color(20, 8, 80)));
        g2.fillPolygon(hx, hy, 6);
        g2.setPaint(null);
        // Hex edge glow
        g2.setColor(new Color(120, 80, 255, 70));
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawPolygon(hx, hy, 6);
        g2.setStroke(new BasicStroke(1f));
        // Pulsar eye (expanding rings)
        int eyeR = 3 + (int)(Math.sin(t * 0.008) * 2);
        g2.setColor(new Color(200, 160, 255, 80 + pulse));
        g2.fillOval(cx - eyeR - 2, cy - eyeR - 2, (eyeR + 2) * 2, (eyeR + 2) * 2);
        g2.setColor(new Color(240, 220, 255, 180 + pulse));
        g2.fillOval(cx - eyeR, cy - eyeR, eyeR * 2, eyeR * 2);
        g2.setColor(new Color(255, 255, 255, 220));
        g2.fillOval(cx - 2, cy - 2, 4, 4);
        // Orbital legs (3 small dots)
        for (int i = 0; i < 3; i++) {
            double a = t * 0.003 + i * Math.PI * 2 / 3;
            int lx = cx + (int)(Math.cos(a) * (w / 2 + 3));
            int ly = cy + (int)(Math.sin(a) * (h / 2 + 1));
            g2.setColor(new Color(100, 60, 220, 140));
            g2.fillOval(lx - 2, ly - 2, 4, 4);
        }
    }

    // Tipo 1 — Solar Probe: flattened sphere with saturn-rings and solar flare cannon
    private void drawShooter(Graphics2D g2, int px, int py, long t) {
        int cx = px + w / 2, cy = py + h / 2;
        int pulse = (int)(Math.sin(t * 0.004) * 20);
        // Solar glow aura
        g2.setColor(new Color(255, 120, 0, 22 + pulse / 2));
        g2.fillOval(px - 6, py - 6, w + 12, h + 12);
        // Body (flattened sphere)
        g2.setPaint(new GradientPaint(px, py, new Color(220, 110, 20), px, py + h, new Color(130, 55, 5)));
        g2.fillOval(px + 2, py + 4, w - 4, h - 6);
        g2.setPaint(null);
        // Saturn-like ring
        g2.setStroke(new BasicStroke(2f));
        g2.setColor(new Color(255, 160, 40, 70));
        g2.drawOval(px - 5, cy - 4, w + 10, 8);
        g2.setStroke(new BasicStroke(1f));
        // Solar spots (eyes)
        int eyeP = (int)(Math.sin(t * 0.007) * 35);
        g2.setColor(new Color(255, 50 + eyeP, 0));
        g2.fillOval(px + 4, py + 6, 5, 5);
        g2.fillOval(px + w - 9, py + 6, 5, 5);
        g2.setColor(new Color(255, 220, 100, 180));
        g2.fillOval(px + 5, py + 7, 2, 2);
        g2.fillOval(px + w - 8, py + 7, 2, 2);
        // Solar flare cannon
        int canX = facingRight ? px + w : px - 6;
        g2.setColor(new Color(140, 70, 0));
        g2.fillRect(canX, cy - 2, 6, 4);
        g2.setColor(new Color(255, 180, 0, 120 + pulse));
        g2.fillOval(canX + (facingRight ? 4 : -3), cy - 4, 6, 8);
        // Body outline
        g2.setColor(new Color(255, 150, 30, 80));
        g2.drawOval(px + 2, py + 4, w - 4, h - 6);
    }

    // Tipo 2 — Nebula Ghost: translucent gaseous being, violet-rose
    private void drawGhost(Graphics2D g2, int px, int py, long t) {
        float wave = (float)(Math.sin(t * 0.003 + px * 0.04) * 5);
        int gy = py + (int)wave;
        int cx = px + w / 2;
        // Outer gas halo
        g2.setColor(new Color(140, 20, 200, 25));
        g2.fillOval(px - 6, gy - 6, w + 12, h + 8);
        g2.setColor(new Color(200, 40, 255, 18));
        g2.fillOval(px - 3, gy - 3, w + 6, h + 4);
        // Core gas body (layered ellipses)
        g2.setColor(new Color(120, 0, 200, 70));
        g2.fillOval(px, gy, w, h - 4);
        g2.setColor(new Color(180, 30, 255, 55));
        g2.fillOval(px + 2, gy + 2, w - 4, h - 8);
        g2.setColor(new Color(220, 100, 255, 40));
        g2.fillOval(px + 4, gy + 4, w - 8, h - 12);
        // Pulsar eyes inside nebula
        int eyeA = 150 + (int)(Math.sin(t * 0.006) * 70);
        g2.setColor(new Color(240, 140, 255, eyeA));
        g2.fillOval(px + 4, gy + 7, 6, 6);
        g2.fillOval(px + w - 10, gy + 7, 6, 6);
        g2.setColor(new Color(255, 220, 255, 210));
        g2.fillOval(px + 5, gy + 8, 3, 3);
        g2.fillOval(px + w - 9, gy + 8, 3, 3);
        // Sinister smile
        g2.setColor(new Color(200, 60, 255, 100));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawArc(px + 5, gy + 14, w - 10, 7, 200, 140);
        g2.setStroke(new BasicStroke(1f));
        // Gas filament tail
        for (int i = 0; i < 3; i++) {
            float tw = (float)(Math.sin(t * 0.005 + i * 1.6) * 4);
            int tx = px + i * (w / 3) + 3;
            int ty = gy + h - 4;
            g2.setColor(new Color(140, 0, 220, 55 - i * 15));
            g2.fillOval(tx, ty, (int)Math.abs(tw) + 7, 5);
        }
    }

    // Tipo 3 — Flaming Meteorite: irregular space rock with cosmic fire trail
    private void drawChaser(Graphics2D g2, int px, int py, long t) {
        float speed = Math.abs(vx);
        // Cosmic fire trail
        if (speed > 0.8f) {
            int trailDir = vx > 0 ? -1 : 1;
            for (int i = 1; i <= 4; i++) {
                int alpha = 55 - i * 12;
                int r = 200 - i * 20; int gb = 60 - i * 10;
                g2.setColor(new Color(Math.max(0,r), Math.max(0,gb), 80, Math.max(0,alpha)));
                g2.fillOval(px + trailDir * i * 6, py + 5, w - 2, h - 8);
            }
        }
        // Rock body (irregular polygon)
        int[] cx2 = facingRight
            ? new int[]{ px, px + w, px + w - 3, px + w - 5, px + 2 }
            : new int[]{ px + w, px, px + 3, px + 5, px + w - 2 };
        int[] cy2 = { py + h / 2, py + 5, py + 4, py + h - 3, py + h - 4 };
        g2.setPaint(new GradientPaint(px, py, new Color(160, 40, 80), px, py + h, new Color(80, 10, 40)));
        g2.fillPolygon(cx2, cy2, 5);
        g2.setPaint(null);
        // Heat glow on leading edge
        g2.setColor(new Color(255, 120, 20, 60));
        int hx2 = facingRight ? px + w - 6 : px;
        g2.fillOval(hx2, py + 4, 8, h - 8);
        // Single eye (cosmic core)
        int eyeX = facingRight ? px + w - 9 : px + 4;
        int ep = (int)(Math.sin(t * 0.009) * 25);
        g2.setColor(new Color(255, 180 + ep, 40));
        g2.fillOval(eyeX, py + h / 2 - 4, 7, 7);
        g2.setColor(Color.WHITE);
        g2.fillOval(eyeX + 2, py + h / 2 - 2, 3, 3);
        // Rock outline
        g2.setColor(new Color(255, 80, 60, 75));
        g2.setStroke(new BasicStroke(1f));
        g2.drawPolygon(cx2, cy2, 5);
        g2.setStroke(new BasicStroke(1f));
    }

    // Tipo 4 — Black Hole: event horizon, accretion disk, quasar eyes
    private void drawBoss(Graphics2D g2, int px, int py, long t) {
        int bw = w * 2, bh = h * 2;
        int bpx = px - w / 2, bpy = py - h / 2;
        int bcx = bpx + bw / 2, bcy = bpy + bh / 2;
        double diskAngle = t * 0.0015;
        int plasma = (int)(Math.sin(t * 0.003) * 28);
        // Gravitational distortion (outer rings)
        for (int ring = 3; ring >= 0; ring--) {
            int rExtra = ring * 12;
            int rAlpha = 8 + ring * 5;
            g2.setColor(new Color(80, 0, 160, rAlpha));
            g2.fillOval(bpx - rExtra, bpy - rExtra, bw + rExtra * 2, bh + rExtra * 2);
        }
        // Accretion disk (elliptical arc, rotating)
        g2.setStroke(new BasicStroke(5f));
        for (int seg = 0; seg < 12; seg++) {
            float ratio = (float)seg / 12;
            int r2 = (int)(100 + ratio * 155);
            int g3 = (int)(40 + ratio * 120);
            int a2 = (int)(180 - ratio * 100);
            g2.setColor(new Color(r2, g3, 10, Math.max(0, a2 + plasma)));
            double startA = diskAngle * 180 / Math.PI + seg * 30;
            g2.drawArc(bpx - 4, bcy - 10, bw + 8, 20, (int)startA, 28);
        }
        g2.setStroke(new BasicStroke(1f));
        // Main body (dark mass)
        g2.setPaint(new GradientPaint(bpx, bpy, new Color(30, 0, 60), bpx, bpy + bh, new Color(5, 0, 20)));
        g2.fillOval(bpx, bpy + bh / 4, bw, bh * 3 / 4);
        g2.setPaint(null);
        // Head
        g2.setPaint(new GradientPaint(bpx, bpy, new Color(50, 0, 90), bpx, bpy + bh / 3, new Color(20, 0, 50)));
        g2.fillRoundRect(bpx + 4, bpy, bw - 8, bh / 2, 12, 12);
        g2.setPaint(null);
        // Event horizon core (absolute black)
        g2.setColor(Color.BLACK);
        g2.fillOval(bcx - bw / 5, bcy - bh / 5, bw * 2 / 5, bh * 2 / 5);
        // Quasar jets (top and bottom)
        int jetA = 50 + plasma;
        g2.setColor(new Color(220, 180, 255, jetA));
        g2.setStroke(new BasicStroke(2f));
        g2.drawLine(bcx, bpy - 5, bcx, bpy - 18);
        g2.drawLine(bcx, bpy + bh + 5, bcx, bpy + bh + 18);
        g2.setStroke(new BasicStroke(1f));
        // Quasar eyes
        int eyeP = (int)(Math.sin(t * 0.006) * 45);
        g2.setColor(new Color(255, 80 + eyeP, 255));
        g2.fillOval(bpx + 10, bpy + 10, 12, 10);
        g2.fillOval(bpx + bw - 22, bpy + 10, 12, 10);
        g2.setColor(new Color(255, 220, 255, 200));
        g2.fillOval(bpx + 12, bpy + 12, 6, 5);
        g2.fillOval(bpx + bw - 20, bpy + 12, 6, 5);
        // Neon border
        g2.setColor(new Color(160, 0, 255, 90));
        g2.setStroke(new BasicStroke(2f));
        g2.drawRoundRect(bpx, bpy + bh / 4, bw, bh * 3 / 4, 8, 8);
        g2.drawRoundRect(bpx + 4, bpy, bw - 8, bh / 2, 12, 12);
        g2.setStroke(new BasicStroke(1f));
    }
}
