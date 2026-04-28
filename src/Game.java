

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.event.*;
import java.util.Random;


public class Game extends JPanel implements Runnable {
    // Configurações de Display
    public static final int GS = 32;
    private final int W, H;
    
    // Estados do Jogo (Enum é mais seguro que int)
    public enum State { MENU, PLAYING, PAUSED, GAMEOVER }
    private State currentState = State.MENU;

    // Core Components
    private Player player;
    private Room currentRoom;
    private final Camera camera;
    private final LevelManager levelManager;
    private final InputHandler inputHandler;
    private final SoundPlayer sound;
    
    // Engine Loop
    private Thread gameThread;
    private boolean running = false;
    private final int FPS = 60;
    private final long TARGET_TIME = 1000000000 / FPS;

    public Game(int w, int h) {
        this.W = w; this.H = h;
        setPreferredSize(new Dimension(w, h));
        setBackground(Color.BLACK);
        setFocusable(true);

        camera = new Camera(w, h);
        levelManager = new LevelManager();
        inputHandler = new InputHandler();
        sound = new SoundPlayer();
        
        addKeyListener(inputHandler);
        startLevel(1);
    }

    public synchronized void start() {
        if (running) return;
        running = true;
        gameThread = new Thread(this, "GameLoop");
        gameThread.start();
    }

    @Override
    public void run() {
        long lastTime = System.nanoTime();
        double deltaTime = 0;

        while (running) {
            long now = System.nanoTime();
            long updateTime = now - lastTime;
            lastTime = now;

            deltaTime = updateTime / 1000000000.0;

            update(deltaTime);
            repaint();

            // Sincronização de frames
            long wait = (TARGET_TIME - (System.nanoTime() - now)) / 1000000;
            if (wait > 0) {
                try { Thread.sleep(wait); } catch (Exception e) {}
            }
        }
    }

    private void startLevel(int level) {
        currentRoom = levelManager.buildLevel(level);
        if (player == null) player = new Player(100, 100);
        else player.respawn(100, 100);
        camera.target = player;
    }

    private void update(double dt) {
        handleGlobalInput();

        if (currentState == State.PLAYING) {
            updatePhysics(dt);
        }
    }

    private void handleGlobalInput() {
        if (inputHandler.isKeyPressed(KeyEvent.VK_ENTER)) {
            if (currentState == State.MENU || currentState == State.GAMEOVER) {
                currentState = State.PLAYING;
                startLevel(1);
            }
        }
        if (inputHandler.isKeyPressed(KeyEvent.VK_P) || inputHandler.isKeyPressed(KeyEvent.VK_ESCAPE)) {
            if (currentState == State.PLAYING) currentState = State.PAUSED;
            else if (currentState == State.PAUSED) currentState = State.PLAYING;
            // Pequeno delay para não flutuar o estado
            try { Thread.sleep(100); } catch (Exception e) {}
        }
    }

    private void updatePhysics(double dt) {
        // Sync inputs
        player.inputLeft = inputHandler.isKeyPressed(KeyEvent.VK_A) || inputHandler.isKeyPressed(KeyEvent.VK_LEFT);
        player.inputRight = inputHandler.isKeyPressed(KeyEvent.VK_D) || inputHandler.isKeyPressed(KeyEvent.VK_RIGHT);
        player.inputJump = inputHandler.isKeyPressed(KeyEvent.VK_W) || inputHandler.isKeyPressed(KeyEvent.VK_SPACE) || inputHandler.isKeyPressed(KeyEvent.VK_UP);
        player.inputAttack = inputHandler.isKeyPressed(KeyEvent.VK_J) || inputHandler.isKeyPressed(KeyEvent.VK_Z);
        player.inputDash = inputHandler.isKeyPressed(KeyEvent.VK_K) || inputHandler.isKeyPressed(KeyEvent.VK_X);

        player.update(currentRoom, dt);
        currentRoom.update(player, sound, new Random());
        camera.update();

        if (player.hp <= 0) currentState = State.GAMEOVER;
        if (currentRoom.isComplete()) {
            startLevel(2); // Próxima fase
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        renderWorld(g2);
        renderUI(g2);
    }

    private void renderWorld(Graphics2D g2) {
        if (currentState == State.MENU) return;

        g2.translate(-camera.x, -camera.y);
        currentRoom.drawTiles(g2, camera);
        currentRoom.drawCollectibles(g2);
        currentRoom.drawEnemies(g2);
        player.draw(g2);
        g2.translate(camera.x, camera.y);
    }

    private void renderUI(Graphics2D g2) {
        if (currentState == State.MENU) {
            drawOverlay(g2, "NEBLUUM", "PRESS ENTER TO START");
        } else if (currentState == State.PAUSED) {
            drawOverlay(g2, "PAUSED", "PRESS P TO RESUME");
        } else if (currentState == State.GAMEOVER) {
            drawOverlay(g2, "GAME OVER", "PRESS ENTER TO RESTART");
        } else {
            drawHUD(g2);
        }
    }

    private void drawHUD(Graphics2D g2) {
        int pad = 12;
        long t = System.currentTimeMillis();

        // --- Painel semi-transparente ---
        g2.setColor(new Color(5, 8, 20, 170));
        g2.fillRoundRect(pad, pad, 240, 72, 10, 10);
        g2.setColor(new Color(0, 140, 255, 40));
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(pad, pad, 240, 72, 10, 10);
        g2.setStroke(new BasicStroke(1f));

        // --- Barra de HP ---
        int bx = pad + 10, by = pad + 10, bw = 180, bh = 12;
        // Label
        g2.setFont(new Font("Monospaced", Font.BOLD, 9));
        g2.setColor(new Color(180, 220, 255, 200));
        g2.drawString("HP", bx, by + 9);
        bx += 18;
        // Track
        g2.setColor(new Color(30, 10, 10, 200));
        g2.fillRoundRect(bx, by, bw, bh, 6, 6);
        // Preenchimento com gradiente
        float hpRatio = (float) player.hp / player.maxHp;
        Color hpLeft  = hpRatio > 0.5f ? new Color(40, 220, 80)  : hpRatio > 0.25f ? new Color(230, 180, 0) : new Color(220, 40, 40);
        Color hpRight = hpRatio > 0.5f ? new Color(20, 160, 60)  : hpRatio > 0.25f ? new Color(180, 120, 0) : new Color(160, 20, 20);
        if (hpRatio > 0) {
            int fillW = Math.max(4, (int)(bw * hpRatio));
            g2.setPaint(new GradientPaint(bx, by, hpLeft, bx + fillW, by, hpRight));
            g2.fillRoundRect(bx, by, fillW, bh, 6, 6);
            g2.setPaint(null);
            // Brilho interno
            g2.setColor(new Color(255, 255, 255, 50));
            g2.fillRoundRect(bx + 2, by + 2, Math.max(0, fillW - 4), 3, 2, 2);
        }
        // Borda
        g2.setColor(new Color(255, 255, 255, 30));
        g2.drawRoundRect(bx, by, bw, bh, 6, 6);
        // Texto HP
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Monospaced", Font.BOLD, 9));
        g2.drawString(player.hp + "/" + player.maxHp, bx + bw + 5, by + 9);

        // --- Barra de Energia (Dash) ---
        bx = pad + 28;
        int ey = pad + 28;
        int ewt = 160, eht = 8;
        g2.setColor(new Color(30, 30, 30, 180));
        g2.fillRoundRect(bx, ey, ewt, eht, 4, 4);
        float enRatio = (float) player.energy / player.maxEnergy;
        int energyPulse = (int)(Math.sin(t * 0.005) * 15);
        if (enRatio > 0) {
            int fillW = Math.max(2, (int)(ewt * enRatio));
            g2.setPaint(new GradientPaint(bx, ey, new Color(0, 200, 255), bx + fillW, ey, new Color(0, 120, 200)));
            g2.fillRoundRect(bx, ey, fillW, eht, 4, 4);
            g2.setPaint(null);
            g2.setColor(new Color(150, 240, 255, 60));
            g2.fillRoundRect(bx + 2, ey + 1, Math.max(0, fillW - 4), 2, 1, 1);
        }
        g2.setColor(new Color(0, 180, 255, 35));
        g2.drawRoundRect(bx, ey, ewt, eht, 4, 4);
        // Label DASH
        g2.setFont(new Font("Monospaced", Font.BOLD, 8));
        g2.setColor(new Color(0, 200, 255, 160));
        g2.drawString("DASH", pad + 10, ey + 8);

        // --- Ícones de itens ---
        int iconY = pad + 44;
        int iconX = pad + 10;
        // Moedas
        g2.setColor(new Color(255, 215, 0, 200));
        g2.fillOval(iconX, iconY, 10, 10);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Monospaced", Font.BOLD, 10));
        g2.drawString("x" + player.coins, iconX + 13, iconY + 9);
        // Bombas
        iconX += 55;
        g2.setColor(new Color(80, 80, 100, 200));
        g2.fillOval(iconX, iconY, 10, 10);
        g2.setColor(new Color(255, 200, 0, 220));
        g2.fillOval(iconX + 6, iconY - 3, 4, 4);
        g2.setColor(Color.WHITE);
        g2.drawString("x" + player.bombs, iconX + 13, iconY + 9);
        // Poções
        iconX += 55;
        g2.setColor(new Color(180, 20, 20, 200));
        g2.fillRoundRect(iconX + 2, iconY + 1, 6, 8, 2, 2);
        g2.setColor(new Color(140, 80, 60, 200));
        g2.fillRect(iconX + 3, iconY - 1, 4, 3);
        g2.setColor(Color.WHITE);
        g2.drawString("x" + player.healthPotions, iconX + 13, iconY + 9);

        // --- Score (canto superior direito) ---
        g2.setFont(new Font("Monospaced", Font.BOLD, 11));
        String scoreStr = "SCORE " + player.score;
        int sw = g2.getFontMetrics().stringWidth(scoreStr);
        g2.setColor(new Color(5, 8, 20, 160));
        g2.fillRoundRect(W - sw - 24, pad, sw + 18, 22, 8, 8);
        g2.setColor(new Color(0, 200, 255, 120));
        g2.drawRoundRect(W - sw - 24, pad, sw + 18, 22, 8, 8);
        g2.setColor(new Color(200, 240, 255));
        g2.drawString(scoreStr, W - sw - 15, pad + 14);
    }

    private void drawOverlay(Graphics2D g2, String title, String sub) {
        long t = System.currentTimeMillis();

        // Fundo com gradiente
        g2.setPaint(new GradientPaint(0, 0, new Color(0, 5, 20, 210), 0, H, new Color(10, 0, 30, 230)));
        g2.fillRect(0, 0, W, H);
        g2.setPaint(null);

        // Scanlines
        g2.setColor(new Color(0, 0, 0, 20));
        for (int sy = 0; sy < H; sy += 3) g2.fillRect(0, sy, W, 1);

        // Vinheta nas bordas
        for (int i = 0; i < 40; i++) {
            g2.setColor(new Color(0, 0, 0, 5));
            g2.drawRect(i, i, W - i * 2, H - i * 2);
        }

        // Título com glow
        int titleSize = 62;
        g2.setFont(new Font("Monospaced", Font.BOLD, titleSize));
        FontMetrics fm = g2.getFontMetrics();
        int tx = W / 2 - fm.stringWidth(title) / 2;
        int ty = H / 2 - 30;
        // Camadas de glow
        int titlePulse = (int)(Math.sin(t * 0.003) * 20);
        for (int glow = 6; glow >= 1; glow--) {
            int ga = 15 + glow * 5 + titlePulse;
            g2.setColor(new Color(0, 200, 255, Math.min(255, ga)));
            g2.drawString(title, tx - glow, ty - glow);
            g2.drawString(title, tx + glow, ty + glow);
        }
        // Texto principal
        g2.setColor(new Color(0, 230, 255));
        g2.drawString(title, tx, ty);
        // Brilho interno
        g2.setFont(new Font("Monospaced", Font.BOLD, titleSize));
        g2.setColor(new Color(200, 240, 255, 80));
        g2.drawString(title, tx + 1, ty - 2);

        // Linha decorativa
        int lineW = fm.stringWidth(title) + 20;
        int lineY = ty + 12;
        g2.setColor(new Color(0, 200, 255, 60));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawLine(W / 2 - lineW / 2, lineY, W / 2 + lineW / 2, lineY);
        g2.setStroke(new BasicStroke(1f));

        // Subtexto animado (fade)
        int subAlpha = 160 + (int)(Math.sin(t * 0.004) * 60);
        g2.setFont(new Font("Monospaced", Font.PLAIN, 16));
        fm = g2.getFontMetrics();
        int sx = W / 2 - fm.stringWidth(sub) / 2;
        // Glow do subtexto
        g2.setColor(new Color(255, 255, 255, subAlpha / 4));
        g2.drawString(sub, sx - 1, H / 2 + 48);
        g2.setColor(new Color(255, 255, 255, subAlpha));
        g2.drawString(sub, sx, H / 2 + 46);

        // Pontinho decorativo piscante
        int dotA = (int)(Math.sin(t * 0.006) * 80 + 100);
        g2.setColor(new Color(0, 255, 200, dotA));
        g2.fillOval(W / 2 - 3, H / 2 + 60, 6, 6);
    }
}
