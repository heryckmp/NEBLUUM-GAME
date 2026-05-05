
import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.event.*;
import java.util.Random;

public class Game extends JPanel implements Runnable {
    // Configurações de Display
    public static final int GS = 32;
    private final int W, H;
    
    // Estados do Jogo
    public enum State { MENU, STARTING, PLAYING, PAUSED, GAMEOVER }
    private State currentState = State.MENU;
    private double tutorialTimer = 0;

    // Core Components
    private int currentLevel = 1;
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
        while (running) {
            long now = System.nanoTime();
            long updateTime = now - lastTime;
            lastTime = now;
            double deltaTime = updateTime / 1000000000.0;

            update(deltaTime);
            repaint();

            long wait = (TARGET_TIME - (System.nanoTime() - now)) / 1000000;
            if (wait > 0) {
                try { Thread.sleep(wait); } catch (Exception e) {}
            }
        }
    }

    private void startLevel(int level) {
        this.currentLevel = level;
        currentRoom = levelManager.buildLevel(level);
        if (player == null) {
            player = new Player(100, 100);
        } else if (level == 1) {
            player.respawn(100, 100); // Game Over ou início do jogo reseta tudo
        } else {
            // Passou de fase: mantém HP, itens e efeito de estrela. Reseta só posição e status temporários
            player.x = 100; player.y = 100;
            player.vx = 0; player.vy = 0;
            player.invincible = 120;
            player.dismount();
        }
        camera.target = player;
    }

    private void update(double dt) {
        handleGlobalInput();

        if (currentState == State.STARTING) {
            tutorialTimer -= dt;
            if (tutorialTimer <= 0) currentState = State.PLAYING;
        } else if (currentState == State.PLAYING) {
            updatePhysics(dt);
        }
    }

    private void handleGlobalInput() {
        if (inputHandler.isKeyPressed(KeyEvent.VK_ENTER)) {
            if (currentState == State.MENU || currentState == State.GAMEOVER) {
                currentState = State.STARTING;
                tutorialTimer = 5.0; // 5 segundos de tutorial
                startLevel(1);
            }
        }
        if (inputHandler.isKeyPressed(KeyEvent.VK_P) || inputHandler.isKeyPressed(KeyEvent.VK_ESCAPE)) {
            if (currentState == State.PLAYING) {
                currentState = State.PAUSED;
                try { Thread.sleep(200); } catch (Exception e) {}
            } else if (currentState == State.PAUSED) {
                currentState = State.PLAYING;
                try { Thread.sleep(200); } catch (Exception e) {}
            }
        }
    }

    private void updatePhysics(double dt) {
        player.inputLeft = inputHandler.isKeyPressed(KeyEvent.VK_A) || inputHandler.isKeyPressed(KeyEvent.VK_LEFT);
        player.inputRight = inputHandler.isKeyPressed(KeyEvent.VK_D) || inputHandler.isKeyPressed(KeyEvent.VK_RIGHT);
        player.inputJump = inputHandler.isKeyPressed(KeyEvent.VK_W) || inputHandler.isKeyPressed(KeyEvent.VK_SPACE) || inputHandler.isKeyPressed(KeyEvent.VK_UP);
        player.inputAttack = inputHandler.isKeyPressed(KeyEvent.VK_J) || inputHandler.isKeyPressed(KeyEvent.VK_Z);
        player.inputDash = inputHandler.isKeyPressed(KeyEvent.VK_K) || inputHandler.isKeyPressed(KeyEvent.VK_X);
        player.inputLightning = inputHandler.isKeyPressed(KeyEvent.VK_L) || inputHandler.isKeyPressed(KeyEvent.VK_C);

        player.update(currentRoom, dt);
        currentRoom.update(player, sound, new Random());
        camera.update();

        if (player.hp <= 0) currentState = State.GAMEOVER;
        if (currentRoom.checkExit(player)) {
            startLevel(currentLevel + 1); 
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
        } else if (currentState == State.STARTING) {
            drawTutorial(g2, true);
        } else if (currentState == State.PAUSED) {
            drawTutorial(g2, false);
        } else if (currentState == State.GAMEOVER) {
            drawOverlay(g2, "GAME OVER", "PRESS ENTER TO RESTART");
        } else {
            drawHUD(g2);
        }
    }

    private void drawTutorial(Graphics2D g2, boolean showTimer) {
        g2.setPaint(new GradientPaint(0, 0, new Color(0, 5, 20, 220), 0, H, new Color(10, 0, 30, 240)));
        g2.fillRect(0, 0, W, H);
        
        g2.setColor(new Color(0, 200, 255));
        g2.setFont(new Font("Monospaced", Font.BOLD, 36));
        g2.drawString("COMANDOS", W/2 - 100, 100);

        g2.setFont(new Font("Monospaced", Font.PLAIN, 18));
        g2.setColor(Color.WHITE);
        int y = 180;
        int x = W/2 - 150;
        g2.drawString("A / D / SETAS     - MOVER", x, y); y += 40;
        g2.drawString("W / ESPAÇO / CIMA - PULAR (2X)", x, y); y += 40;
        g2.drawString("J / Z             - ESPADA", x, y); y += 40;
        g2.drawString("L / C             - RAIOS", x, y); y += 40;
        g2.drawString("K / X             - DASH", x, y); y += 40;
        g2.drawString("P / ESC           - PAUSAR", x, y); y += 40;

        if (showTimer) {
            g2.setFont(new Font("Monospaced", Font.BOLD, 50));
            g2.setColor(new Color(0, 255, 200));
            g2.drawString("" + (int)(tutorialTimer + 1), W/2 - 15, H - 80);
        } else {
            g2.setFont(new Font("Monospaced", Font.BOLD, 20));
            g2.setColor(new Color(255, 255, 255, 150));
            g2.drawString("PRESSIONE P PARA VOLTAR", W/2 - 120, H - 80);
        }
    }

    private void drawHUD(Graphics2D g2) {
        int pad = 12;
        long t = System.currentTimeMillis();
        g2.setColor(new Color(5, 8, 20, 170));
        g2.fillRoundRect(pad, pad, 240, 72, 10, 10);
        
        int bx = pad + 10, by = pad + 10, bw = 180, bh = 12;
        g2.setFont(new Font("Monospaced", Font.BOLD, 9));
        g2.setColor(new Color(180, 220, 255, 200));
        g2.drawString("HP", bx, by + 9);
        bx += 18;
        g2.setColor(new Color(30, 10, 10, 200));
        g2.fillRoundRect(bx, by, bw, bh, 6, 6);
        float hpRatio = (float) player.hp / player.maxHp;
        if (hpRatio > 0) {
            int fillW = (int)(bw * hpRatio);
            g2.setPaint(new GradientPaint(bx, by, new Color(40, 220, 80), bx + fillW, by, new Color(20, 160, 60)));
            g2.fillRoundRect(bx, by, fillW, bh, 6, 6);
        }
        g2.setColor(Color.WHITE);
        g2.drawString(player.hp + "/" + player.maxHp, bx + bw + 5, by + 9);

        // DASH Energy
        int ey = pad + 28;
        g2.setColor(new Color(30, 30, 30, 180));
        g2.fillRoundRect(pad + 28, ey, 160, 8, 4, 4);
        float enRatio = (float) player.energy / player.maxEnergy;
        if (enRatio > 0) {
            g2.setPaint(new GradientPaint(pad+28, ey, new Color(0, 200, 255), pad+28+(int)(160*enRatio), ey, new Color(0, 120, 200)));
            g2.fillRoundRect(pad+28, ey, (int)(160 * enRatio), 8, 4, 4);
        }
        g2.setColor(new Color(0, 200, 255, 160));
        g2.drawString("DASH", pad + 10, ey + 8);

        // Items
        int iconY = pad + 44;
        g2.setColor(new Color(255, 215, 0));
        g2.fillOval(pad + 10, iconY, 10, 10);
        g2.setColor(Color.WHITE);
        g2.drawString("x" + player.coins, pad + 23, iconY + 9);
        
        g2.setColor(Color.CYAN);
        g2.fillRect(pad + 65, iconY + 1, 3, 9);
        g2.setColor(Color.WHITE);
        g2.drawString("x" + player.lightningAmmo, pad + 78, iconY + 9);

        String scoreStr = "SCORE " + player.score;
        g2.drawString(scoreStr, W - 100, pad + 14);
    }

    private void drawOverlay(Graphics2D g2, String title, String sub) {
        g2.setPaint(new GradientPaint(0, 0, new Color(0, 5, 20, 210), 0, H, new Color(10, 0, 30, 230)));
        g2.fillRect(0, 0, W, H);
        g2.setColor(new Color(0, 230, 255));
        g2.setFont(new Font("Monospaced", Font.BOLD, 62));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(title, W/2 - fm.stringWidth(title)/2, H/2 - 30);
        g2.setFont(new Font("Monospaced", Font.PLAIN, 16));
        fm = g2.getFontMetrics();
        g2.setColor(Color.WHITE);
        g2.drawString(sub, W/2 - fm.stringWidth(sub)/2, H/2 + 46);
    }
}
