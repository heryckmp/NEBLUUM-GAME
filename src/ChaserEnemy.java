import java.awt.*;

/** Chaser — persegue agressivamente o player, pula sobre buracos. */
public class ChaserEnemy extends Enemy {

    private static final float CHASE_SPEED = 2.8f;
    private static final float JUMP_VX     = 4.5f;
    private static final float JUMP_VY     = -9.0f;

    public ChaserEnemy(float x, float y, int hp) {
        super(x, y, 24, 28, hp);
    }

    @Override
    protected void updateAI(Room room, Player player) {
        float dx = player.x - x;
        boolean holeAhead = onGround && checkHoleAhead(room);

        if (onGround) facingRight = dx > 0;

        if (holeAhead && onGround) {
            vy = JUMP_VY;
            vx = facingRight ? JUMP_VX : -JUMP_VX;
        } else if (onGround) {
            vx = dx > 0 ? CHASE_SPEED : -CHASE_SPEED;
        }
        // No ar: mantém impulso horizontal
    }

    @Override
    public void draw(Graphics2D g2) {
        if (state == 2) return;
        if (invincibleTimer > 0 && (invincibleTimer / 3) % 2 == 0) return;
        int px = (int)x, py = (int)y;
        long t = System.currentTimeMillis();

        float speed = Math.abs(vx);
        if (speed > 0.8f) {
            int trailDir = vx > 0 ? -1 : 1;
            for (int i = 1; i <= 4; i++) {
                int alpha = 55 - i*12;
                int r = 200 - i*20, gb = 60 - i*10;
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
        int ep   = (int)(Math.sin(t*0.009)*25);
        g2.setColor(new Color(255, 180+ep, 40));
        g2.fillOval(eyeX, py+h/2-4, 7, 7);
        g2.setColor(Color.WHITE);
        g2.fillOval(eyeX+2, py+h/2-2, 3, 3);

        drawHPBar(g2, px, py);
        drawRiddenIndicator(g2, px, py);
    }
}
