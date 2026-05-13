import java.awt.*;

/** Turret — fixo no chão, atira rajadas triplas em ângulos variados. Imune a armadilhas. */
public class TurretEnemy extends Enemy {

    private final float spawnX;

    public TurretEnemy(float x, float y, int hp) {
        super(x, y, 24, 28, hp);
        this.spawnX = x;
    }

    @Override
    protected boolean isImmuneToTraps() { return true; }

    @Override
    protected void afterMove(Room room) {
        // Reseta posição horizontal — a Turret nunca se move lateralmente
        x = spawnX;
        vx = 0;
    }

    @Override
    protected void updateAI(Room room, Player player) {
        float dx = player.x - x;
        facingRight = dx > 0;
        vx = 0; vy = 0;

        if (stateTimer-- <= 0) {
            float bx  = x + w/2f, by = y + h/3f;
            float dir = facingRight ? 1 : -1;
            room.addProjectile(bx, by, dir * 5.0f);
            room.addArcProjectile(bx, by, dir * 4.0f, -3.0f, 1);
            room.addArcProjectile(bx, by, dir * 4.0f,  3.0f, 1);
            stateTimer = 80 + (int)(Math.random() * 40);
        }
    }

    @Override
    public void draw(Graphics2D g2) {
        if (state == 2) return;
        if (invincibleTimer > 0 && (invincibleTimer / 3) % 2 == 0) return;
        int px = (int)x, py = (int)y;
        int cx = px + w/2, cy = py + h/2;
        long t = System.currentTimeMillis();
        int pulse = (int)(Math.sin(t*0.005)*18);

        g2.setPaint(new GradientPaint(px, py+h/2, new Color(40,20,80), px, py+h, new Color(15,5,35)));
        g2.fillRect(px, py+h/2, w, h/2);
        g2.setPaint(null);
        g2.setPaint(new GradientPaint(px, py, new Color(80,40,160), px, py+h/2, new Color(40,15,80)));
        g2.fillRoundRect(px+2, py+4, w-4, h/2, 8, 8);
        g2.setPaint(null);

        int canDir = facingRight ? 1 : -1;
        int canX = cx + canDir*4;
        g2.setColor(new Color(60,30,120));
        g2.fillRect(canX - (facingRight ? 0 : 12), cy-3, 12, 6);
        g2.setColor(new Color(180,80,255, 140+pulse));
        g2.fillOval(canX + (facingRight ? 10 : -8), cy-4, 8, 8);

        g2.setColor(new Color(200,100,255, 160+pulse));
        g2.fillOval(cx-4, py+8, 8, 8);
        g2.setColor(Color.WHITE);
        g2.fillOval(cx-2, py+10, 4, 4);

        g2.setColor(new Color(120,60,200,80));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(px+2, py+4, w-4, h/2, 8, 8);
        g2.setStroke(new BasicStroke(1f));

        drawHPBar(g2, px, py);
    }
}
