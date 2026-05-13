import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.Random;

/**
 * PlayerRenderer — responsável exclusivamente por renderizar o Player.
 * Extrai as ~260 linhas de Graphics2D do método draw() de Player.java,
 * aplicando Separação de Responsabilidades (SRP).
 */
public class PlayerRenderer {

    public void draw(Graphics2D g2, Player p) {
        if (p.invincible > 0 && !p.isDashing && (p.invincible / 3) % 2 == 0) return;

        int px = (int)p.x, py = (int)p.y;
        int cx = px + p.w/2, cy = py + p.h/2;
        long t = System.currentTimeMillis();

        drawStarAura(g2, p, cx, cy, t);
        drawStarBurst(g2, p, cx, cy);
        drawEmeraldBurst(g2, p, cx, cy);
        drawLightningOrbit(g2, p, cx, cy, t);
        drawHealFlash(g2, p, cx, cy, t);
        drawRodeoIndicator(g2, p, px, py);

        float lRatio    = Math.min(1.0f, p.lightningItems / 10f);
        float sRatio    = p.starPower / 7f;
        float totalRatio = Math.max(lRatio, sRatio);

        Color suitTop = Color.decode("#261e55");
        Color suitBot = Color.decode("#120e32");
        if (totalRatio > 0) {
            int r = (int)(suitTop.getRed()   * (1-totalRatio) + 255 * totalRatio);
            int gr = (int)(suitTop.getGreen() * (1-totalRatio) + 255 * totalRatio);
            int b  = (int)(suitTop.getBlue()  * (1-totalRatio) + 100 * totalRatio);
            suitTop = new Color(r, gr, b);
        }

        if (p.starPower >= GameConstants.STAR_POWER_MAX) {
            drawStarForm(g2, px, py, t, p.w, p.h);
            return;
        }

        drawDashTrail(g2, p, px, py);
        drawBody(g2, px, py, p.w, p.h, suitTop, suitBot, totalRatio);
        drawAttackArc(g2, p, px, py);
        drawLightningBeam(g2, p, px, py, cy);
    }

    private void drawStarAura(Graphics2D g2, Player p, int cx, int cy, long t) {
        if (p.starPower <= 0) return;
        float sp = p.starPower / 7f;
        float pulse1 = (float)(Math.sin(t * 0.006) * 0.5 + 0.5);
        float pulse2 = (float)(Math.sin(t * 0.013 + 1.2) * 0.5 + 0.5);

        int haloR = 18 + (int)(sp * 32) + (int)(pulse1 * (4 + sp * 8));
        for (int layer = 3; layer >= 0; layer--) {
            int lr    = haloR + layer * 7;
            int alpha = (int)((0.18f + sp * 0.35f) * 255 / (layer + 1));
            int rC = 255, gC = (int)(230 - sp*30 + pulse2*20), bC = (int)(60 + sp*120 + pulse1*60);
            g2.setColor(new Color(rC, Math.min(255,gC), Math.min(255,bC), Math.max(0,alpha)));
            g2.fillOval(cx - lr, cy - lr, lr*2, lr*2);
        }

        g2.setStroke(new BasicStroke(1.5f + sp * 2f));
        int ringR = 22 + (int)(sp * 20) + (int)(pulse1 * 5);
        g2.setColor(new Color(255, 240, 80, (int)(100 + sp*155)));
        g2.drawOval(cx - ringR, cy - ringR, ringR*2, ringR*2);
        g2.setStroke(new BasicStroke(1f));

        int numRays = Math.max(4, p.starPower * 2);
        float speed = 0.003f + sp * 0.007f;
        for (int i = 0; i < numRays; i++) {
            double angle = t * speed + i * (Math.PI * 2.0 / numRays);
            float rayBase = 22 + sp * 20;
            float rayVar  = (float)(Math.sin(t * 0.009 + i * 1.3) * (4 + sp * 8));
            int rLen = (int)(rayBase + rayVar);
            int rx = cx + (int)(Math.cos(angle) * rLen);
            int ry = cy + (int)(Math.sin(angle) * rLen);
            g2.setStroke(new BasicStroke(3f + sp * 3f));
            g2.setColor(new Color(255, 220, 50, (int)(30 + sp*60)));
            g2.drawLine(cx, cy, rx, ry);
            g2.setStroke(new BasicStroke(1f));
            g2.setColor(new Color(255, 255, 220, (int)(120 + sp*135)));
            g2.drawLine(cx, cy, rx, ry);
            int dotR = 2 + (int)(sp * 3);
            g2.setColor(new Color(255, 255, 255, (int)(180 + sp*75)));
            g2.fillOval(rx - dotR, ry - dotR, dotR*2, dotR*2);
        }
        g2.setStroke(new BasicStroke(1f));

        int numOrb = p.starPower;
        float orbR = 28 + sp * 18;
        for (int i = 0; i < numOrb; i++) {
            double angle = -t * 0.004 + i * (Math.PI * 2.0 / numOrb);
            int ox = cx + (int)(Math.cos(angle) * orbR);
            int oy = cy + (int)(Math.sin(angle) * orbR);
            int dotSize = 3 + (int)(sp * 3);
            g2.setColor(new Color(255, 255, 180, (int)(160 + sp*95)));
            g2.fillOval(ox - dotSize/2, oy - dotSize/2, dotSize, dotSize);
            g2.setColor(Color.WHITE);
            g2.fillOval(ox - 1, oy - 1, 2, 2);
        }

        if (p.starPower >= 4) {
            int ringR2 = ringR + 10 + (int)(pulse2 * 6);
            g2.setStroke(new BasicStroke(1f));
            g2.setColor(new Color(255, 200, 255, (int)(60 + sp*80)));
            g2.drawOval(cx - ringR2, cy - ringR2, ringR2*2, ringR2*2);
        }
    }

    private void drawStarBurst(Graphics2D g2, Player p, int cx, int cy) {
        if (p.starBurstTimer <= 0) return;
        float prog = p.starBurstTimer / 75f;
        float sPow = Math.min(1f, p.starPower / 7f);
        for (int i = 0; i < 4; i++) {
            int ringSize = (int)((1-prog)*(180+i*30)) + (i*15);
            int alpha = (int)(prog * 140 / (i+1));
            Color rc = i%2==0 ? new Color(255,230,60,alpha) : new Color(255,255,255,alpha/2);
            g2.setColor(rc);
            g2.setStroke(new BasicStroke(3.5f * prog));
            g2.drawOval(cx-ringSize/2, cy-ringSize/2, ringSize, ringSize);
        }
        for (int i = 0; i < 12; i++) {
            double angle = i * Math.PI / 6;
            int len = (int)(prog * (100 + sPow*60));
            int rx = cx + (int)(Math.cos(angle)*len), ry = cy + (int)(Math.sin(angle)*len);
            g2.setStroke(new BasicStroke(8f*prog));
            g2.setColor(new Color(255,200,50,(int)(prog*50)));
            g2.drawLine(cx, cy, rx, ry);
            g2.setStroke(new BasicStroke(2f*prog));
            g2.setColor(new Color(255,255,220,(int)(prog*255)));
            g2.drawLine(cx, cy, rx, ry);
        }
        int coreSize = (int)(20*prog);
        g2.setColor(new Color(255,255,255,(int)(prog*255)));
        g2.fillOval(cx-coreSize, cy-coreSize, coreSize*2, coreSize*2);
        g2.setStroke(new BasicStroke(1f));
    }

    private void drawEmeraldBurst(Graphics2D g2, Player p, int cx, int cy) {
        if (p.emeraldBurstTimer <= 0) return;
        float prog = p.emeraldBurstTimer / 60f;
        for (int i = 0; i < 3; i++) {
            int ringSize = (int)((1-prog)*160) + (i*25);
            g2.setColor(new Color(0,255,120,(int)(prog*120/(i+1))));
            g2.setStroke(new BasicStroke(3f*prog));
            g2.drawOval(cx-ringSize/2, cy-ringSize/2, ringSize, ringSize);
        }
        for (int i = 0; i < 8; i++) {
            double angle = i * Math.PI / 4;
            int len = (int)(prog*90);
            int rx = cx+(int)(Math.cos(angle)*len), ry = cy+(int)(Math.sin(angle)*len);
            g2.setStroke(new BasicStroke(10f*prog));
            g2.setColor(new Color(0,255,120,(int)(prog*60)));
            g2.drawLine(cx,cy,rx,ry);
            g2.setStroke(new BasicStroke(2f*prog));
            g2.setColor(new Color(220,255,240,(int)(prog*255)));
            g2.drawLine(cx,cy,rx,ry);
        }
        g2.setColor(Color.WHITE);
        int coreSize = (int)(12*prog);
        g2.fillOval(cx-coreSize/2, cy-coreSize/2, coreSize, coreSize);
        g2.setStroke(new BasicStroke(1f));
    }

    private void drawLightningOrbit(Graphics2D g2, Player p, int cx, int cy, long t) {
        if (p.lightningItems <= 0) return;
        g2.setColor(new Color(255,255,100,200));
        g2.setStroke(new BasicStroke(2f));
        for (int i = 0; i < p.lightningItems; i++) {
            double angle = t*0.005 + (i*(Math.PI*2/p.lightningItems));
            int rLen = 18 + (int)(Math.sin(t*0.01+i)*4);
            int x2 = cx+(int)(Math.cos(angle)*rLen), y2 = cy+(int)(Math.sin(angle)*rLen);
            g2.setColor(new Color(255,255,0,150));
            g2.drawLine(cx,cy,x2,y2);
            g2.setColor(Color.WHITE);
            g2.fillOval(x2-1,y2-1,3,3);
        }
        g2.setStroke(new BasicStroke(1f));
    }

    private void drawHealFlash(Graphics2D g2, Player p, int cx, int cy, long t) {
        if (p.healFlash <= 0) return;
        float prog = p.healFlash / 55f;
        int size = (int)((1-prog)*120);
        g2.setColor(new Color(255,255,180,(int)(prog*220)));
        g2.setStroke(new BasicStroke(2.5f*prog));
        for (int i = 0; i < 12; i++) {
            double angle = i*Math.PI/6 + (1-prog)*2;
            int x2 = cx+(int)(Math.cos(angle)*size), y2 = cy+(int)(Math.sin(angle)*size);
            g2.drawLine(cx,cy,x2,y2);
        }
        g2.setStroke(new BasicStroke(1f));
    }

    private void drawRodeoIndicator(Graphics2D g2, Player p, int px, int py) {
        if (p.ridingEnemy == null) return;
        long t = System.currentTimeMillis();
        float rPulse = (float)(Math.sin(t*0.01)*0.5+0.5);
        int rAlpha = (int)(120+rPulse*100);
        g2.setColor(new Color(255,200,50,rAlpha));
        g2.setStroke(new BasicStroke(2.5f));
        g2.drawRoundRect(px-2, py-2, p.w+4, p.h+4, 8, 8);
        g2.setStroke(new BasicStroke(1f));
        int filled = Math.min(p.w, p.rodeoTimer * p.w / GameConstants.MAX_RODEO_FRAMES);
        g2.setColor(new Color(0,0,0,100));
        g2.fillRect(px, py-7, p.w, 4);
        g2.setColor(new Color(255,180,0,200));
        g2.fillRect(px, py-7, filled, 4);
    }

    private void drawDashTrail(Graphics2D g2, Player p, int px, int py) {
        if (!p.isDashing) return;
        for (int i = 1; i < p.dashTrailX.length; i++) {
            int idx = (p.dashTrailHead - i + p.dashTrailX.length * 10) % p.dashTrailX.length;
            if (p.dashTrailX[idx] == 0) continue;
            int alpha = Math.max(0, 115 - i*22);
            g2.setColor(new Color(130,210,255,alpha));
            g2.fillRoundRect((int)p.dashTrailX[idx]+3, (int)p.dashTrailY[idx]+8, p.w-6, p.h-16, 8, 8);
        }
    }

    private void drawBody(Graphics2D g2, int px, int py, int w, int h,
                          Color suitTop, Color suitBot, float totalRatio) {
        g2.setPaint(new GradientPaint(px, py+10, suitTop, px, py+h-10, suitBot));
        g2.fillRoundRect(px+1, py+10, w-2, h-18, 6, 6);
        g2.setPaint(null);
        g2.setColor(suitTop.brighter());
        g2.fillRoundRect(px, py, w, 18, 10, 10);
        g2.setColor(new Color(30, 200, 255, (int)(200*(1-totalRatio))));
        g2.fillRoundRect(px+2, py+3, w-4, 10, 6, 6);
    }

    private void drawAttackArc(Graphics2D g2, Player p, int px, int py) {
        if (!p.attacking) return;
        g2.setColor(new Color(255,255,255,220));
        g2.setStroke(new BasicStroke(3f));
        int arcW = 32, arcH = 40;
        int ax = px + (p.facingRight ? p.w : -arcW);
        int ay = py - 5;
        int startAngle = p.facingRight ? 45 : 135;
        int extent     = p.facingRight ? -90 : 90;
        float prog = 1.0f - (p.attackDuration / 8.0f);
        g2.drawArc(ax, ay, arcW, arcH, startAngle, (int)(extent*prog));
        g2.setStroke(new BasicStroke(1f));
    }

    private void drawLightningBeam(Graphics2D g2, Player p, int px, int py, int cy) {
        if (!p.firingLightning) return;
        g2.setColor(new Color(255,255,0,200));
        int lx = px + (p.facingRight ? p.w : 0);
        g2.setStroke(new BasicStroke(2f));
        Random rand = new Random();
        int curX = lx, curY = cy;
        for (int i = 0; i < 5; i++) {
            int nextX = lx + (p.facingRight ? 30 : -30) * (i+1);
            int nextY = cy + rand.nextInt(20) - 10;
            g2.drawLine(curX, curY, nextX, nextY);
            curX = nextX; curY = nextY;
        }
        g2.setStroke(new BasicStroke(1f));
    }

    private void drawStarForm(Graphics2D g2, int px, int py, long t, int w, int h) {
        int cx = px + w/2, cy = py + h/2;
        int pulse = (int)(Math.sin(t*0.01)*8);
        g2.setColor(new Color(255,255,200,100));
        g2.fillOval(cx-30-pulse, cy-30-pulse, 60+pulse*2, 60+pulse*2);
        g2.setColor(Color.WHITE);
        for (int i = 0; i < 4; i++) {
            double a = t*0.005 + i*Math.PI/2;
            int len = 25+pulse;
            int x2 = cx+(int)(Math.cos(a)*len), y2 = cy+(int)(Math.sin(a)*len);
            g2.setStroke(new BasicStroke(3f));
            g2.drawLine(cx,cy,x2,y2);
        }
        g2.setColor(new Color(255,255,220));
        g2.fillOval(cx-12, cy-12, 24, 24);
        g2.setColor(Color.WHITE);
        g2.fillOval(cx-6, cy-6, 12, 12);
        g2.setStroke(new BasicStroke(1f));
    }
}
