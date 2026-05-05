
import java.awt.*;
import java.awt.geom.*;

public class Collectible extends Entity {
    public int type;
    public boolean collected = false;
    private int bobOffset;

    public Collectible(float x, float y, int type) {
        super(x, y, 20, 20);
        this.type = type;
        this.bobOffset = (int)(Math.random() * 100);
    }

    @Override
    public void update(Room room, double dt) { bobOffset++; }

    @Override
    public void draw(Graphics2D g2) {
        if (collected) return;
        int by = (int)(y + Math.sin(bobOffset * 0.08) * 3);
        int px = (int)x;
        long t = System.currentTimeMillis();

        switch (type) {
            case 0 -> drawHeal(g2, px, by, t);
            case 1 -> drawCoin(g2, px, by, t);
            case 2 -> drawShield(g2, px, by, t);
            case 3 -> drawPotion(g2, px, by, t);
            case 4 -> drawBomb(g2, px, by, t);
            case 5 -> drawAttackUp(g2, px, by, t);
            case 6 -> drawLightningAmmo(g2, px, by, t);
            case 7 -> drawStarFragment(g2, px, by, t);
            case 8 -> drawJumpUp(g2, px, by, t);
            default -> drawCoin(g2, px, by, t);
        }
    }

    private void drawJumpUp(Graphics2D g2, int px, int py, long t) {
        int cx = px + w / 2, cy = py + h / 2;
        int pulse = (int)(Math.sin(t * 0.008) * 15);
        // Aura formato vento
        g2.setColor(new Color(200, 255, 255, 60 + pulse));
        g2.fillOval(px - 4, py, w + 8, h);
        
        // Seta p/ cima
        int[] sx = { cx, cx + 6, cx + 2, cx + 2, cx - 2, cx - 2, cx - 6 };
        int[] sy = { py + 2, py + 8, py + 8, py + h - 2, py + h - 2, py + 8, py + 8 };
        g2.setPaint(new GradientPaint(cx, py, new Color(255, 255, 255), cx, py + h, new Color(100, 200, 255)));
        g2.fillPolygon(sx, sy, 7);
        g2.setPaint(null);
        g2.setColor(new Color(0, 150, 255, 180));
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawPolygon(sx, sy, 7);
        g2.setStroke(new BasicStroke(1f));
        
        // Asas
        g2.setColor(new Color(255, 255, 255, 120 + pulse));
        g2.fillOval(cx - 10, cy, 8, 4);
        g2.fillOval(cx + 2, cy, 8, 4);
    }

    private void drawStarFragment(Graphics2D g2, int px, int py, long t) {
        int cx = px + w / 2, cy = py + h / 2;
        int pulse = (int)(Math.sin(t * 0.01) * 15);
        
        // Brilho intenso
        g2.setColor(new Color(255, 255, 100, 100 + pulse));
        g2.fillOval(cx - 15, cy - 15, 30, 30);
        
        // Estrela interna
        int[] sx = {cx, cx+4, cx+12, cx+6, cx+8, cx, cx-8, cx-6, cx-12, cx-4};
        int[] sy = {cy-12, cy-4, cy-4, cy+2, cy+12, cy+6, cy+12, cy+2, cy-4, cy-4};
        g2.setColor(new Color(255, 255, 220));
        g2.fillPolygon(sx, sy, 10);
        g2.setColor(Color.WHITE);
        g2.drawPolygon(sx, sy, 10);
        
        // Faíscas
        for (int i=0; i<4; i++) {
            double a = t * 0.005 + i * Math.PI/2;
            int x2 = cx + (int)(Math.cos(a) * 12);
            int y2 = cy + (int)(Math.sin(a) * 12);
            g2.drawLine(cx, cy, x2, y2);
        }
    }

    private void drawLightningAmmo(Graphics2D g2, int px, int py, long t) {
        int pulse = (int)(Math.sin(t * 0.01) * 10);
        g2.setColor(new Color(0, 255, 255, 50 + pulse));
        g2.fillOval(px - 3, py - 3, w + 6, h + 6);
        g2.setColor(Color.CYAN);
        g2.fillRect(px + w / 2 - 2, py + 2, 4, h - 4);
        g2.setColor(Color.WHITE);
        g2.fillRect(px + w / 2 - 1, py + 4, 2, h - 8);
    }

    // Tipo 0 — Cruz de cura verde pulsante
    private void drawHeal(Graphics2D g2, int px, int py, long t) {
        int pulse = (int)(Math.sin(t * 0.006) * 15);
        // Halo
        g2.setColor(new Color(0, 200, 80, 40 + pulse));
        g2.fillOval(px - 4, py - 4, w + 8, h + 8);
        // Cruz
        g2.setColor(new Color(40, 220, 100));
        g2.fillRoundRect(px + w / 2 - 3, py + 2, 6, h - 4, 2, 2);
        g2.fillRoundRect(px + 2, py + h / 2 - 3, w - 4, 6, 2, 2);
        // Brilho interior
        g2.setColor(new Color(180, 255, 200, 160));
        g2.fillRoundRect(px + w / 2 - 1, py + 4, 2, 4, 1, 1);
    }

    // Tipo 1 — Hexágono dourado rotacionando
    private void drawCoin(Graphics2D g2, int px, int py, long t) {
        double angle = (t * 0.003 + bobOffset * 0.05);
        int cx = px + w / 2, cy = py + h / 2;
        int r = w / 2 - 1;
        // Glow
        g2.setColor(new Color(255, 215, 0, 50));
        g2.fillOval(px - 3, py - 3, w + 6, h + 6);
        // Hexágono
        int[] hx = new int[6], hy = new int[6];
        for (int i = 0; i < 6; i++) {
            double a = angle + Math.PI / 3 * i;
            hx[i] = cx + (int)(Math.cos(a) * r);
            hy[i] = cy + (int)(Math.sin(a) * r * 0.55); // achatado (efeito 3D)
        }
        g2.setPaint(new GradientPaint(px, py, new Color(255, 230, 50), px + w, py + h, new Color(200, 140, 0)));
        g2.fillPolygon(hx, hy, 6);
        g2.setPaint(null);
        // Brilho
        g2.setColor(new Color(255, 255, 200, 150));
        g2.fillOval(cx - 3, cy - 3, 5, 4);
        // Borda
        g2.setColor(new Color(255, 200, 0, 180));
        g2.setStroke(new BasicStroke(1f));
        g2.drawPolygon(hx, hy, 6);
        g2.setStroke(new BasicStroke(1f));
    }

    // Tipo 2 — Diamante azul com ondas
    private void drawShield(Graphics2D g2, int px, int py, long t) {
        int cx = px + w / 2, cy = py + h / 2;
        int pulse = (int)(Math.sin(t * 0.005) * 10);
        // Ondas
        g2.setColor(new Color(0, 180, 255, 30 + pulse));
        g2.fillOval(px - 5, py - 5, w + 10, h + 10);
        // Diamante
        int[] dx = { cx, px + w - 1, cx, px + 1 };
        int[] dy = { py + 1, cy, py + h - 1, cy };
        g2.setPaint(new GradientPaint(px, py, new Color(80, 200, 255), px + w, py + h, new Color(0, 80, 200)));
        g2.fillPolygon(dx, dy, 4);
        g2.setPaint(null);
        // Reflexo
        g2.setColor(new Color(200, 240, 255, 130));
        g2.fillPolygon(new int[]{ cx - 3, cx, cx + 3, cx }, new int[]{ cy, py + 3, cy, py + h / 3 }, 4);
        // Borda
        g2.setColor(new Color(0, 200, 255, 160));
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawPolygon(dx, dy, 4);
        g2.setStroke(new BasicStroke(1f));
    }

    // Tipo 3 — Frasco vermelho com borbulhas
    private void drawPotion(Graphics2D g2, int px, int py, long t) {
        int cx = px + w / 2;
        // Frasco
        g2.setPaint(new GradientPaint(px + 3, py, new Color(220, 30, 30, 200), px + w - 3, py + h, new Color(140, 0, 40, 220)));
        g2.fillRoundRect(px + 3, py + 4, w - 6, h - 4, 6, 6);
        g2.setPaint(null);
        // Gargalo
        g2.setColor(new Color(160, 100, 100));
        g2.fillRect(cx - 3, py, 6, 5);
        // Rolha
        g2.setColor(new Color(180, 130, 80));
        g2.fillRoundRect(cx - 2, py - 2, 4, 4, 2, 2);
        // Borbulhas animadas
        for (int i = 0; i < 3; i++) {
            int bx = px + 5 + i * 4;
            int bb = (int)((t * 0.002 + i * 0.7) % 1.0 * (h - 8));
            int bby = py + h - 4 - bb;
            if (bby > py + 5) {
                g2.setColor(new Color(255, 120, 120, 100));
                g2.fillOval(bx, bby, 3, 3);
            }
        }
        // Brilho
        g2.setColor(new Color(255, 180, 180, 80));
        g2.fillRoundRect(px + 4, py + 5, 4, h / 2 - 2, 2, 2);
        // Contorno
        g2.setColor(new Color(255, 80, 80, 120));
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(px + 3, py + 4, w - 6, h - 4, 6, 6);
        g2.setStroke(new BasicStroke(1f));
    }

    // Tipo 4 — Bomba preta com fusível aceso
    private void drawBomb(Graphics2D g2, int px, int py, long t) {
        // Corpo
        g2.setPaint(new GradientPaint(px, py + 4, new Color(50, 50, 60), px + w, py + h, new Color(20, 20, 30)));
        g2.fillOval(px + 1, py + 4, w - 2, h - 4);
        g2.setPaint(null);
        // Fusível
        g2.setColor(new Color(140, 100, 60));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawLine(px + w / 2, py + 4, px + w / 2 + 4, py - 2);
        g2.setStroke(new BasicStroke(1f));
        // Chama do fusível pulsante
        int fPulse = (int)(Math.sin(t * 0.02) * 2);
        g2.setColor(new Color(255, 200, 0, 200));
        g2.fillOval(px + w / 2 + 2, py - 4 - fPulse, 5, 5);
        g2.setColor(new Color(255, 100, 0, 160));
        g2.fillOval(px + w / 2 + 3, py - 3 - fPulse, 3, 3);
        // Reflexo
        g2.setColor(new Color(120, 120, 140, 80));
        g2.fillOval(px + 4, py + 7, 6, 5);
        // Contorno
        g2.setColor(new Color(100, 100, 120, 100));
        g2.setStroke(new BasicStroke(1f));
        g2.drawOval(px + 1, py + 4, w - 2, h - 4);
        g2.setStroke(new BasicStroke(1f));
    }

    // Tipo 5 — ATK+ raio dourado brilhante
    private void drawAttackUp(Graphics2D g2, int px, int py, long t) {
        int cx = px + w / 2, cy = py + h / 2;
        int pulse = (int)(Math.sin(t * 0.007) * 20);
        // Glow amarelo
        g2.setColor(new Color(255, 230, 0, 35 + pulse));
        g2.fillOval(px - 5, py - 5, w + 10, h + 10);
        // Raio (zigzag)
        int[] lx = { cx + 2, cx - 4, cx + 2, cx - 4 };
        int[] ly = { py + 1, cy - 2, cy + 1, py + h - 1 };
        g2.setPaint(new GradientPaint(cx, py, new Color(255, 240, 0), cx, py + h, new Color(255, 140, 0)));
        g2.fillPolygon(lx, ly, 4);
        g2.setPaint(null);
        // Brilho interior
        g2.setColor(new Color(255, 255, 200, 140));
        g2.fillPolygon(new int[]{ cx + 1, cx - 2, cx + 1 }, new int[]{ py + 2, cy, cy - 1 }, 3);
        // Contorno
        g2.setColor(new Color(255, 200, 0, 160));
        g2.setStroke(new BasicStroke(1f));
        g2.drawPolygon(lx, ly, 4);
        g2.setStroke(new BasicStroke(1f));
    }
}
