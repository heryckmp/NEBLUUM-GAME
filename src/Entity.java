
import java.awt.*;

public abstract class Entity {
    public float x, y;
    public float vx, vy;
    public int w, h;
    public boolean onGround = false;

    protected float friction = 0.85f;
    protected float gravity  = 0.45f;

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
        if (vy > 14) vy = 14; // terminal velocity
    }

    // ---------------------------------------------------------------
    // moveAndCollide — move em X primeiro, depois em Y
    // ---------------------------------------------------------------
    protected void moveAndCollide(Room room, float dx, float dy) {
        // --- Movimento horizontal ---
        x += dx;
        resolveAxis(room, true);

        // --- Movimento vertical (pode precisar de sub-steps para velocidades altas) ---
        onGround = false;
        float remaining = dy;
        int steps = Math.max(1, (int)(Math.abs(dy) / 8) + 1); // sub-step se vy alto
        float step = remaining / steps;
        for (int s = 0; s < steps; s++) {
            y += step;
            resolveAxis(room, false);
            if (onGround && dy > 0) break; // pousou, para sub-steps
        }
    }

    // ---------------------------------------------------------------
    // resolveAxis — percorre todos os tiles em colisão e resolve
    // ---------------------------------------------------------------
    private void resolveAxis(Room room, boolean isX) {
        final int ts = 32;
        int tx1 = (int)(x / ts);
        int tx2 = (int)((x + w - 0.01f) / ts);
        int ty1 = (int)(y / ts);
        int ty2 = (int)((y + h - 0.01f) / ts);

        for (int ty = ty1; ty <= ty2; ty++) {
            for (int tx = tx1; tx <= tx2; tx++) {
                Tile t = room.getTile(tx, ty);
                if (t == null) continue;

                if (t.isSolid()) {
                    resolveOverlap(tx * ts, ty * ts, ts, isX, false);
                } else if (t.isPlatform() || t.isSpike()) {
                    resolveOverlap(tx * ts, ty * ts, ts, isX, true);
                }
            }
        }
    }

    // ---------------------------------------------------------------
    // resolveOverlap — corrigido: funciona mesmo com vx/vy == 0
    // ---------------------------------------------------------------
    private void resolveOverlap(int tileX, int tileY, int ts, boolean isX, boolean isPlatform) {
        // Verifica sobreposição real (com epsilon para evitar erros de precisão float)
        float overlapX = Math.min(x + w, tileX + ts) - Math.max(x, tileX);
        float overlapY = Math.min(y + h, tileY + ts) - Math.max(y, tileY);
        if (overlapX <= 0.05f || overlapY <= 0.05f) return; // sem sobreposição

        if (isPlatform) {
            // Plataforma one-way: só bloqueia vindo de cima
            if (!isX && vy >= 0 && (y + h - vy) <= tileY + 1) {
                y = tileY - h;
                vy = 0;
                onGround = true;
            }
            return;
        }

        if (isX) {
            // Empurra para o lado correto baseado na posição relativa
            if (x + w / 2f < tileX + ts / 2f) {
                // entidade está à esquerda do tile
                x = tileX - w;
            } else {
                // entidade está à direita do tile
                x = tileX + ts;
            }
            vx = 0;
        } else {
            if (y + h / 2f < tileY + ts / 2f) {
                // entidade está acima do tile → pousa
                y = tileY - h;
                vy = 0;
                onGround = true;
            } else {
                // entidade está abaixo → bate no teto
                y = tileY + ts;
                if (vy < 0) vy = 0;
            }
        }
    }

    // ---------------------------------------------------------------
    // handleTileCollision — mantido para compatibilidade com updateDash
    // ---------------------------------------------------------------
    protected void handleTileCollision(Room room, boolean isX) {
        resolveAxis(room, isX);
    }

    // ---------------------------------------------------------------
    // clampToBounds — impede saída dos limites do mapa
    // ---------------------------------------------------------------
    protected void clampToBounds(Room room) {
        int mapW = room.getCols() * 32;
        if (x < 0)        { x = 0;        if (vx < 0) vx = 0; }
        if (x + w > mapW) { x = mapW - w; if (vx > 0) vx = 0; }
    }
}
