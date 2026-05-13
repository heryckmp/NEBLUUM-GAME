import java.awt.*;

/** Bomber — caminha no chão e lança projéteis em arco. */
public class BomberEnemy extends Enemy {

    private static final float WALK_SPEED = 1.8f;

    public BomberEnemy(float x, float y, int hp) {
        super(x, y, 24, 28, hp);
    }

    @Override
    protected void updateAI(Room room, Player player) {
        float dx = player.x - x;
        boolean holeAhead = onGround && checkHoleAhead(room);

        if (onGround && !holeAhead) facingRight = dx > 0;

        if (holeAhead && onGround) {
            vx = 0;
            facingRight = dx > 0;
        } else if (onGround) {
            vx = facingRight ? WALK_SPEED : -WALK_SPEED;
        }

        if (stateTimer-- <= 0 && onGround) {
            float bvx = facingRight ? 3.5f : -3.5f;
            room.addArcProjectile(x + w/2f, y, bvx, -6f, 2);
            stateTimer = 90 + (int)(Math.random() * 60);
        }
    }

    @Override
    public void draw(Graphics2D g2) {
        if (state == 2) return;
        if (invincibleTimer > 0 && (invincibleTimer / 3) % 2 == 0) return;
        int px = (int)x, py = (int)y;
        int cy = py + h/2;
        long t = System.currentTimeMillis();
        int pulse = (int)(Math.sin(t*0.006)*20);

        g2.setColor(new Color(255,80,0, 18+pulse/3));
        g2.fillOval(px-6, py-6, w+12, h+12);
        g2.setPaint(new GradientPaint(px, py, new Color(200,80,0), px, py+h, new Color(100,30,0)));
        g2.fillRoundRect(px+2, py+4, w-4, h-6, 8, 8);
        g2.setPaint(null);

        g2.setColor(new Color(255,220,0, 180+pulse));
        g2.fillOval(px+3, py+8, 6, 5);
        g2.fillOval(px+w-9, py+8, 6, 5);
        g2.setColor(Color.WHITE);
        g2.fillOval(px+4, py+9, 3, 3);
        g2.fillOval(px+w-8, py+9, 3, 3);

        int bombX = facingRight ? px+w-4 : px-6;
        g2.setColor(new Color(30,30,40));
        g2.fillOval(bombX, cy-4, 10, 10);
        g2.setColor(new Color(255,200,0, 180+pulse));
        g2.fillOval(bombX+3, cy-8, 4, 5);

        g2.setColor(new Color(255,120,0,90));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(px+2, py+4, w-4, h-6, 8, 8);
        g2.setStroke(new BasicStroke(1f));

        drawHPBar(g2, px, py);
        drawRiddenIndicator(g2, px, py);
    }
}
