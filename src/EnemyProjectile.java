import java.awt.*;

public class EnemyProjectile extends Entity {
    public int     damage    = 1;
    public boolean dead      = false;
    public boolean hasGravity = false; // true para projéteis em arco (Bomber/Turret)
    public int     life      = 220;

    public EnemyProjectile(float x, float y, float vx, float vy) {
        super(x, y, 10, 10);
        this.vx = vx;
        this.vy = vy;
    }

    @Override
    public void update(Room room, double dt) {
        if (hasGravity) vy += 0.35f; // gravidade para projéteis em arco
        x += vx;
        y += vy;
        life--;
        if (life <= 0) { dead = true; return; }

        // Morre ao colidir com tile sólido
        int tx = (int)(x / 32);
        int ty = (int)(y / 32);
        Tile t = room.getTile(tx, ty);
        if (t != null && t.isSolid()) dead = true;

        // Morre se sair do mapa
        if (x < 0 || x > room.getCols() * 32 || y > room.getRows() * 32) dead = true;
    }

    @Override
    public void draw(Graphics2D g2) { /* desenhado em Room.drawProjectiles */ }
}
