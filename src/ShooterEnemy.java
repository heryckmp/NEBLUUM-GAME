import java.awt.*;

/** Shooter — mantém distância do player e atira projéteis horizontais. */
public class ShooterEnemy extends Enemy {

    private static final float APPROACH_SPEED = 1.4f;
    private static final float RETREAT_SPEED  = 1.2f;
    private static final float MAX_RANGE      = 260f;
    private static final float MIN_RANGE      = 100f;

    public ShooterEnemy(float x, float y, int hp) {
        super(x, y, 24, 28, hp);
    }

    @Override
    protected void updateAI(Room room, Player player) {
        float dx   = player.x - x;
        float dist = Math.abs(dx);
        boolean holeAhead = onGround && checkHoleAhead(room);

        if (holeAhead) {
            vx = 0;
            facingRight = dx > 0;
        } else {
            facingRight = dx > 0;
            if (dist > MAX_RANGE)      vx = dx > 0 ?  APPROACH_SPEED : -APPROACH_SPEED;
            else if (dist < MIN_RANGE) vx = dx > 0 ? -RETREAT_SPEED  :  RETREAT_SPEED;
            else                       vx *= 0.85f;
        }

        if (stateTimer-- <= 0) {
            room.addProjectile(x + w/2f, y + h/2f, facingRight ? 5 : -5);
            stateTimer = 70 + (int)(Math.random() * 30);
        }
    }

    @Override
    public void draw(Graphics2D g2) {
        if (state == 2) return;
        if (invincibleTimer > 0 && (invincibleTimer / 3) % 2 == 0) return;
        int px = (int)x, py = (int)y;
        int cx = px + w/2, cy = py + h/2;
        long t = System.currentTimeMillis();
        int pulse = (int)(Math.sin(t * 0.004) * 20);

        g2.setColor(new Color(255,120,0, 22+pulse/2));
        g2.fillOval(px-6, py-6, w+12, h+12);
        g2.setPaint(new GradientPaint(px, py, new Color(220,110,20), px, py+h, new Color(130,55,5)));
        g2.fillOval(px+2, py+4, w-4, h-6);
        g2.setPaint(null);
        g2.setStroke(new BasicStroke(2f));
        g2.setColor(new Color(255,160,40,70));
        g2.drawOval(px-5, cy-4, w+10, 8);
        g2.setStroke(new BasicStroke(1f));

        int eyeP = (int)(Math.sin(t*0.007)*35);
        g2.setColor(new Color(255, 50+eyeP, 0));
        g2.fillOval(px+4, py+6, 5, 5);
        g2.fillOval(px+w-9, py+6, 5, 5);

        int canX = facingRight ? px+w : px-6;
        g2.setColor(new Color(140,70,0));
        g2.fillRect(canX, cy-2, 6, 4);
        g2.setColor(new Color(255,180,0, 120+pulse));
        g2.fillOval(canX+(facingRight?4:-3), cy-4, 6, 8);

        drawHPBar(g2, px, py);
        drawRiddenIndicator(g2, px, py);
    }
}
