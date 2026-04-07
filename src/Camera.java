class Camera {
    float x, y;
    public int W, H;
    Object target; // Player
    float smooth = 0.08f;

    Camera(int w, int h) {
        W = w; H = h;
    }

    void update() {
        if (target instanceof Player p) {
            float tx = p.x + p.w / 2 - W / 2;
            float ty = p.y + p.h / 2 - H / 2;
            x += (tx - x) * smooth;
            y += (ty - y) * smooth;

            // Clamp
            if (x < 0) x = 0;
            if (y < 0) y = 0;
        }
    }
}

class Map {
    static class InputState {
        boolean left, right, up, down, attack, dash, use, drop;
    }
}
