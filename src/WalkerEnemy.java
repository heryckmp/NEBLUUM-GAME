import java.awt.*;

/** Walker — patrulha o chão revirando ao encontrar buracos ou paredes. */
public class WalkerEnemy extends Enemy {

    private static final float WALK_SPEED = 1.6f;

    public WalkerEnemy(float x, float y, int hp) {
        super(x, y, 24, 28, hp);
    }

    @Override
    protected void updateAI(Room room, Player player) {
        boolean holeAhead = onGround && checkHoleAhead(room);
        if (stateTimer-- <= 0 || holeAhead) {
            facingRight = !facingRight;
            stateTimer = 50 + (int)(Math.random() * 60);
        }
        vx = facingRight ? WALK_SPEED : -WALK_SPEED;
    }

    @Override
    public void draw(Graphics2D g2) {
        if (state == 2) return;
        if (invincibleTimer > 0 && (invincibleTimer / 3) % 2 == 0) return;
        int px = (int)x, py = (int)y;
        int cx = px + w/2, cy = py + h/2;
        long t = System.currentTimeMillis();
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
        g2.setColor(new Color(200,160,255, 80+pulse));
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

        drawHPBar(g2, px, py);
        drawRiddenIndicator(g2, px, py);
    }
}
