import java.awt.*;

/** Ghost — voa livremente sem colisão com tiles, persegue o player. */
public class GhostEnemy extends Enemy {

    public GhostEnemy(float x, float y, int hp) {
        super(x, y, 24, 28, hp);
    }

    @Override
    protected boolean usesGravity() { return false; }

    @Override
    protected boolean isImmuneToTraps() { return true; }

    @Override
    protected void updateAI(Room room, Player player) {
        float dx   = player.x - x;
        float dy   = player.y - y;
        float dist = (float)Math.sqrt(dx*dx + dy*dy);
        float spd  = dist < 150 ? 2.4f : 1.6f;

        vx += (dx > 0 ? spd : -spd) * 0.12f;
        vy += (dy > 0 ? spd : -spd) * 0.12f;

        float maxV = spd;
        if (Math.abs(vx) > maxV) vx = vx > 0 ? maxV : -maxV;
        if (Math.abs(vy) > maxV) vy = vy > 0 ? maxV : -maxV;

        if (isRidden) vy *= 0.5f;
        facingRight = dx > 0;
        clampToBounds(room);
    }

    @Override
    public void draw(Graphics2D g2) {
        if (state == 2) return;
        if (invincibleTimer > 0 && (invincibleTimer / 3) % 2 == 0) return;
        int px = (int)x, py = (int)y;
        long t = System.currentTimeMillis();
        float wave = (float)(Math.sin(t*0.003 + px*0.04)*5);
        int gy = py + (int)wave;

        g2.setColor(new Color(140,20,200,25));
        g2.fillOval(px-6, gy-6, w+12, h+8);
        g2.setColor(new Color(120,0,200,70));
        g2.fillOval(px, gy, w, h-4);
        g2.setColor(new Color(180,30,255,55));
        g2.fillOval(px+2, gy+2, w-4, h-8);

        int eyeA = 150 + (int)(Math.sin(t*0.006)*70);
        g2.setColor(new Color(240,140,255, eyeA));
        g2.fillOval(px+4, gy+7, 6, 6);
        g2.fillOval(px+w-10, gy+7, 6, 6);
        g2.setColor(new Color(255,220,255,210));
        g2.fillOval(px+5, gy+8, 3, 3);
        g2.fillOval(px+w-9, gy+8, 3, 3);

        g2.setColor(new Color(200,60,255,100));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawArc(px+5, gy+14, w-10, 7, 200, 140);
        g2.setStroke(new BasicStroke(1f));

        drawHPBar(g2, px, py);
        drawRiddenIndicator(g2, px, py);
    }
}
