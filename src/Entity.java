
import java.awt.*;

public abstract class Entity {
    public float x, y;
    public float vx, vy;
    public int w, h;
    public boolean onGround = false;
    
    protected float friction = 0.85f;
    protected float gravity = 0.45f;

    public Entity(float x, float y, int w, int h) {
        this.x = x; this.y = y; this.w = w; this.h = h;
    }

    public abstract void update(Room room, double deltaTime);
    public abstract void draw(Graphics2D g2);

    public Rectangle getBounds() {
        return new Rectangle((int) x, (int) y, w, h);
    }

    protected void applyGravity(float amount) {
        vy += amount;
        if (vy > 12) vy = 12;
    }

    protected void moveAndCollide(Room room, float dx, float dy) {
        x += dx;
        handleTileCollision(room, true);
        y += dy;
        onGround = false;
        handleTileCollision(room, false);
    }

    protected void handleTileCollision(Room room, boolean isX) {
        int ts = 32;
        int tx1 = (int) (x / ts);
        int tx2 = (int) ((x + w - 0.1f) / ts);
        int ty1 = (int) (y / ts);
        int ty2 = (int) ((y + h - 0.1f) / ts);

        for (int ty = ty1; ty <= ty2; ty++) {
            for (int tx = tx1; tx <= tx2; tx++) {
                Tile t = room.getTile(tx, ty);
                if (t != null && (t.isSolid() || t.isPlatform())) {
                    resolveOverlap(tx * ts, ty * ts, ts, isX, t.isPlatform());
                }
            }
        }
    }

    private void resolveOverlap(int tileX, int tileY, int ts, boolean isX, boolean isPlatform) {
        if (isPlatform) {
            if (!isX && vy > 0 && (y + h - vy) <= tileY) {
                y = tileY - h;
                vy = 0;
                onGround = true;
            }
            return;
        }

        if (isX) {
            if (vx > 0) x = tileX - w;
            else if (vx < 0) x = tileX + ts;
            vx = 0;
        } else {
            if (vy > 0) { y = tileY - h; onGround = true; }
            else if (vy < 0) { y = tileY + ts; }
            vy = 0;
        }
    }
}
