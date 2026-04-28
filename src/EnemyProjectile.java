
import java.awt.*;

public class EnemyProjectile extends Entity {
    public int damage = 1;
    public boolean dead = false;
    public int life = 200;

    public EnemyProjectile(float x, float y, float vx, float vy) {
        super(x, y, 8, 8);
        this.vx = vx;
        this.vy = vy;
    }

    @Override
    public void update(Room room, double dt) {
        x += vx;
        y += vy;
        life--;
        if (life <= 0) dead = true;

        int tx = (int) (x / 32);
        int ty = (int) (y / 32);
        Tile t = room.getTile(tx, ty);
        if (t != null && t.isSolid()) dead = true;
    }

    @Override
    public void draw(Graphics2D g2) {
        g2.setColor(Color.RED);
        g2.fillOval((int) x, (int) y, w, h);
    }
}
