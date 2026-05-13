import java.awt.*;

/** Boss — persegue o player e atira projéteis em leque triplo. Imune a armadilhas. */
public class BossEnemy extends Enemy {

    private static final float BOSS_SPEED = 2.2f;

    public BossEnemy(float x, float y, int hp) {
        super(x, y, 48, 48, hp);
        this.points = 500;
    }

    @Override
    public boolean isMountable()      { return false; }

    @Override
    protected boolean isImmuneToTraps() { return true; }

    @Override
    protected void updateAI(Room room, Player player) {
        float dx = player.x - x;
        boolean holeAhead = onGround && checkHoleAhead(room);

        if (holeAhead) {
            vx = 0;
            facingRight = dx > 0;
        } else {
            facingRight = dx > 0;
            vx = dx > 0 ? BOSS_SPEED : -BOSS_SPEED;
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

    @Override
    public void draw(Graphics2D g2) {
        if (state == 2) return;
        if (invincibleTimer > 0 && (invincibleTimer / 3) % 2 == 0) return;
        int px = (int)x, py = (int)y;
        int bw = w, bh = h;
        int bcx = px + bw/2, bcy = py + bh/2;
        long t = System.currentTimeMillis();
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

        drawHPBar(g2, px, py);
    }
}
