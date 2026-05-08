import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.event.*;
import java.util.Random;

public class Game extends JPanel implements Runnable {
    public static final int GS = 32;
    private final int W, H;

    public enum State { MENU, STARTING, PLAYING, PAUSED, GAMEOVER, WIN, CREDITS }
    private State currentState = State.MENU;
    private double tutorialTimer = 0;
    private double creditsTimer = 0;
    private double enterCooldown = 0;

    private static final int MAX_LEVEL = 3;

    private int currentLevel = 1;
    private Player player;
    private Room currentRoom;
    private final Camera camera;
    private final LevelManager levelManager;
    private final InputHandler inputHandler;
    private final SoundPlayer sound;

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
            double deltaTime = updateTime / 1_000_000_000.0;
            update(deltaTime);
            repaint();
            long wait = (TARGET_TIME - (System.nanoTime() - now)) / 1_000_000;
            if (wait > 0) { try { Thread.sleep(wait); } catch (Exception ignored) {} }
        }
    }

    private void startLevel(int level) {
        this.currentLevel = level;
        currentRoom = levelManager.buildLevel(level);
        if (player == null) {
            player = new Player(100, 100);
        } else if (level == 1) {
            player.respawn(100, 100);
        } else {
            // Mantém stats, reseta apenas posição e estado temporário
            player.x = 100; player.y = 100;
            player.vx = 0;  player.vy = 0;
            player.invincible = 120;
            player.dismount();
        }
        camera.target = player;
    }

    private void update(double dt) {
        if (enterCooldown > 0) enterCooldown -= dt;
        handleGlobalInput();
        if (currentState == State.PLAYING) {
            updatePhysics(dt);
        } else if (currentState == State.CREDITS) {
            creditsTimer += dt;
        }
    }

    private void handleGlobalInput() {
        if (inputHandler.isKeyPressed(KeyEvent.VK_ENTER) && enterCooldown <= 0) {
            enterCooldown = 0.5;
            if (currentState == State.MENU || currentState == State.GAMEOVER) {
                currentState = State.PLAYING;
                startLevel(1);
            } else if (currentState == State.WIN) {
                currentState = State.CREDITS;
                creditsTimer = 0;
            } else if (currentState == State.CREDITS) {
                if (creditsTimer > 2.0) { // Pequeno delay antes de poder pular os créditos
                    currentState = State.MENU;
                }
            }
        }
        if (inputHandler.isKeyPressed(KeyEvent.VK_P) || inputHandler.isKeyPressed(KeyEvent.VK_ESCAPE)) {
            if (currentState == State.PLAYING) {
                currentState = State.PAUSED;
                try { Thread.sleep(200); } catch (Exception ignored) {}
            } else if (currentState == State.PAUSED) {
                currentState = State.PLAYING;
                try { Thread.sleep(200); } catch (Exception ignored) {}
            }
        }
    }

    private void updatePhysics(double dt) {
        player.inputLeft    = inputHandler.isKeyPressed(KeyEvent.VK_A) || inputHandler.isKeyPressed(KeyEvent.VK_LEFT);
        player.inputRight   = inputHandler.isKeyPressed(KeyEvent.VK_D) || inputHandler.isKeyPressed(KeyEvent.VK_RIGHT);
        player.inputJump    = inputHandler.isKeyPressed(KeyEvent.VK_W) || inputHandler.isKeyPressed(KeyEvent.VK_SPACE) || inputHandler.isKeyPressed(KeyEvent.VK_UP);
        player.inputAttack  = inputHandler.isKeyPressed(KeyEvent.VK_J) || inputHandler.isKeyPressed(KeyEvent.VK_Z);
        player.inputDash    = inputHandler.isKeyPressed(KeyEvent.VK_K) || inputHandler.isKeyPressed(KeyEvent.VK_X);
        player.inputLightning = inputHandler.isKeyPressed(KeyEvent.VK_L) || inputHandler.isKeyPressed(KeyEvent.VK_C);

        player.update(currentRoom, dt);
        currentRoom.update(player, sound, new Random());
        camera.update();

        if (player.hp <= 0) {
            currentState = State.GAMEOVER;
            return;
        }

        if (currentRoom.checkExit(player)) {
            int next = currentLevel + 1;
            if (next > MAX_LEVEL) {
                currentState = State.WIN;
            } else {
                startLevel(next);
            }
        }
    }

    // ── Render ──────────────────────────────────────────────────────────
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (currentState == State.MENU) {
            drawMenu(g2);
            return;
        }

        renderWorld(g2);

        switch (currentState) {
            case STARTING  -> drawTutorial(g2, true);
            case PAUSED    -> drawTutorial(g2, false);
            case GAMEOVER  -> drawOverlay(g2, "GAME OVER",  "PRESSIONE ENTER PARA REINICIAR");
            case WIN       -> drawWin(g2);
            case CREDITS   -> drawCredits(g2);
            case PLAYING   -> drawHUD(g2);
            default        -> {}
        }
    }

    private void renderWorld(Graphics2D g2) {
        g2.translate(-camera.x, -camera.y);
        currentRoom.drawBackground(g2, camera, W, H);
        currentRoom.drawTiles(g2, camera);
        currentRoom.drawCollectibles(g2);
        currentRoom.drawEnemies(g2);
        currentRoom.drawProjectiles(g2);
        currentRoom.drawParticles(g2);
        player.draw(g2);
        g2.translate(camera.x, camera.y);
    }

    // ── HUD ─────────────────────────────────────────────────────────────
    private void drawHUD(Graphics2D g2) {
        int pad = 12;
        // Painel esquerdo
        g2.setColor(new Color(5, 8, 20, 180));
        g2.fillRoundRect(pad, pad, 255, 80, 10, 10);

        // HP
        int bx = pad+10, by = pad+10, bw = 180, bh = 12;
        g2.setFont(new Font("Monospaced", Font.BOLD, 9));
        g2.setColor(new Color(180,220,255,200));
        g2.drawString("HP", bx, by+9);
        bx += 20;
        g2.setColor(new Color(30,10,10,200));
        g2.fillRoundRect(bx, by, bw, bh, 6, 6);
        float hpRatio = (float)player.hp / player.maxHp;
        if (hpRatio > 0) {
            int fillW = (int)(bw * hpRatio);
            g2.setPaint(new GradientPaint(bx, by, new Color(40,220,80), bx+fillW, by, new Color(20,160,60)));
            g2.fillRoundRect(bx, by, fillW, bh, 6, 6);
            g2.setPaint(null);
        }
        g2.setColor(Color.WHITE);
        g2.drawString(player.hp+"/"+player.maxHp, bx+bw+5, by+9);

        // DASH energy
        int ey = pad+28;
        g2.setColor(new Color(30,30,30,180));
        g2.fillRoundRect(pad+30, ey, 160, 8, 4, 4);
        float enRatio = (float)player.energy / player.maxEnergy;
        if (enRatio > 0) {
            g2.setPaint(new GradientPaint(pad+30, ey, new Color(0,200,255), pad+30+(int)(160*enRatio), ey, new Color(0,120,200)));
            g2.fillRoundRect(pad+30, ey, (int)(160*enRatio), 8, 4, 4);
            g2.setPaint(null);
        }
        g2.setColor(new Color(0,200,255,160));
        g2.drawString("DASH", pad+10, ey+8);

        // Ícones (coins, raio)
        int iconY = pad+44;
        g2.setColor(new Color(255,215,0));
        g2.fillOval(pad+10, iconY, 10, 10);
        g2.setColor(Color.WHITE);
        g2.drawString("x"+player.coins, pad+23, iconY+9);

        g2.setColor(Color.CYAN);
        g2.fillRect(pad+70, iconY+1, 3, 9);
        g2.setColor(Color.WHITE);
        g2.drawString("x"+player.lightningAmmo, pad+80, iconY+9);

        // Star power
        if (player.starPower > 0) {
            g2.setColor(new Color(255,220,50));
            g2.fillOval(pad+145, iconY, 10, 10);
            g2.setColor(Color.WHITE);
            g2.drawString("x"+player.starPower, pad+158, iconY+9);
        }

        // Score e fase (canto superior direito)
        g2.setColor(new Color(5, 8, 20, 180));
        g2.fillRoundRect(W-160, pad, 148, 40, 10, 10);
        g2.setFont(new Font("Monospaced", Font.BOLD, 9));
        g2.setColor(new Color(180,220,255,200));
        g2.drawString("SCORE  "+player.score, W-148, pad+14);
        g2.setColor(new Color(0,220,255,220));
        g2.drawString("FASE "+currentLevel+"/"+MAX_LEVEL, W-148, pad+30);

        // Inimigos restantes
        int remaining = currentRoom.enemies.size();
        if (remaining > 0) {
            g2.setColor(new Color(255,80,80,200));
            g2.drawString("INIMIGOS: "+remaining, W-148, pad+44);
        } else {
            g2.setColor(new Color(80,255,120,200));
            g2.drawString("VA AO EXIT! ->", W-148, pad+44);
        }
    }

    // ── Tela de vitória ──────────────────────────────────────────────────
    private void drawWin(Graphics2D g2) {
        long t = System.currentTimeMillis();
        g2.setPaint(new GradientPaint(0, 0, new Color(0,8,30,230), 0, H, new Color(20,0,40,240)));
        g2.fillRect(0, 0, W, H);

        // Raios de vitória
        g2.setColor(new Color(255,220,50,30));
        for (int i = 0; i < 12; i++) {
            double a = t*0.001 + i*Math.PI/6;
            int len = W/2;
            g2.setStroke(new BasicStroke(6f));
            g2.drawLine(W/2, H/2, W/2+(int)(Math.cos(a)*len), H/2+(int)(Math.sin(a)*len));
        }
        g2.setStroke(new BasicStroke(1f));

        g2.setFont(new Font("Monospaced", Font.BOLD, 58));
        FontMetrics fm = g2.getFontMetrics();
        String title = "VITÓRIA!";
        int pulse = (int)(Math.sin(t*0.004)*8);
        // Sombra
        g2.setColor(new Color(255,180,0,80));
        g2.drawString(title, W/2 - fm.stringWidth(title)/2 + 3, H/2 - 50 + 3 + pulse);
        // Título
        g2.setPaint(new GradientPaint(0, H/2-80, new Color(255,240,50), 0, H/2-20, new Color(255,150,0)));
        g2.drawString(title, W/2 - fm.stringWidth(title)/2, H/2 - 50 + pulse);
        g2.setPaint(null);

        g2.setFont(new Font("Monospaced", Font.PLAIN, 16));
        fm = g2.getFontMetrics();
        g2.setColor(new Color(200,230,255,200));
        String sub1 = "Você derrotou Nebluum e sobreviveu às 3 fases!";
        String sub2 = "SCORE FINAL: "+player.score;
        String sub3 = "PRESSIONE ENTER PARA CONTINUAR";
        g2.drawString(sub1, W/2 - fm.stringWidth(sub1)/2, H/2 + 20);
        g2.setColor(new Color(255,220,50,220));
        g2.setFont(new Font("Monospaced", Font.BOLD, 18));
        fm = g2.getFontMetrics();
        g2.drawString(sub2, W/2 - fm.stringWidth(sub2)/2, H/2 + 55);
        g2.setFont(new Font("Monospaced", Font.PLAIN, 13));
        fm = g2.getFontMetrics();
        g2.setColor(new Color(180,200,255,160+(int)(Math.sin(t*0.003)*60)));
        g2.drawString(sub3, W/2 - fm.stringWidth(sub3)/2, H - 60);
    }

    // ── Menu ─────────────────────────────────────────────────────────────
    private void drawMenu(Graphics2D g2) {
        long t = System.currentTimeMillis();
        g2.setPaint(new GradientPaint(0,0,new Color(0,5,20,255),0,H,new Color(10,0,30,255)));
        g2.fillRect(0, 0, W, H);

        // Estrelas de fundo simples
        java.util.Random r = new java.util.Random(42);
        for (int i = 0; i < 80; i++) {
            int sx = r.nextInt(W), sy = r.nextInt(H);
            int ba = 80 + (int)(Math.sin(t*0.001+i*0.5)*40);
            g2.setColor(new Color(200,210,255,Math.max(20,Math.min(200,ba))));
            g2.fillRect(sx, sy, 2, 2);
        }

        g2.setFont(new Font("Monospaced", Font.BOLD, 72));
        FontMetrics fm = g2.getFontMetrics();
        String title = "NEBLUUM";
        int pulse = (int)(Math.sin(t*0.003)*6);
        g2.setColor(new Color(0,100,200,80));
        g2.drawString(title, W/2 - fm.stringWidth(title)/2 + 4, H/2 - 60 + 4);
        g2.setPaint(new GradientPaint(0, H/2-110, new Color(0,200,255), 0, H/2-40, new Color(120,60,255)));
        g2.drawString(title, W/2 - fm.stringWidth(title)/2, H/2 - 60 + pulse);
        g2.setPaint(null);

        g2.setFont(new Font("Monospaced", Font.PLAIN, 15));
        fm = g2.getFontMetrics();
        g2.setColor(new Color(180,220,255,180+(int)(Math.sin(t*0.004)*60)));
        String sub = "PRESSIONE ENTER PARA COMEÇAR";
        g2.drawString(sub, W/2 - fm.stringWidth(sub)/2, H/2 + 30);

        // Dicas de controles
        g2.setFont(new Font("Monospaced", Font.PLAIN, 11));
        g2.setColor(new Color(120,150,200,140));
        String[] tips = { "A/D — MOVER", "W/ESPAÇO — PULAR", "J/Z — ESPADA", "L/C — RAIOS", "K/X — DASH" };
        for (int i = 0; i < tips.length; i++)
            g2.drawString(tips[i], W/2 - 60, H/2 + 80 + i*20);
    }

    private void drawTutorial(Graphics2D g2, boolean showTimer) {
        g2.setPaint(new GradientPaint(0,0,new Color(0,5,20,220),0,H,new Color(10,0,30,240)));
        g2.fillRect(0, 0, W, H);
        g2.setColor(new Color(0,200,255));
        g2.setFont(new Font("Monospaced", Font.BOLD, 36));
        g2.drawString("COMANDOS", W/2 - 100, 100);
        g2.setFont(new Font("Monospaced", Font.PLAIN, 18));
        g2.setColor(Color.WHITE);
        int y = 180, x = W/2 - 180;
        g2.drawString("A / D / SETAS       — MOVER",       x, y); y += 38;
        g2.drawString("W / ESPAÇO / CIMA   — PULAR",       x, y); y += 38;
        g2.drawString("J / Z               — ESPADA",      x, y); y += 38;
        g2.drawString("L / C               — RAIOS",       x, y); y += 38;
        g2.drawString("K / X               — DASH",        x, y); y += 38;
        g2.drawString("CAIR SOBRE INIMIGO  — RODEO!",      x, y); y += 38;
        g2.drawString("P / ESC             — PAUSAR",      x, y);
        if (showTimer) {
            g2.setFont(new Font("Monospaced", Font.BOLD, 50));
            g2.setColor(new Color(0,255,200));
            g2.drawString(""+(int)(tutorialTimer+1), W/2 - 15, H - 80);
        } else {
            g2.setFont(new Font("Monospaced", Font.BOLD, 14));
            g2.setColor(new Color(255,255,255,150));
            g2.drawString("PRESSIONE P PARA VOLTAR", W/2 - 120, H - 80);
        }
    }

    private void drawOverlay(Graphics2D g2, String title, String sub) {
        g2.setPaint(new GradientPaint(0,0,new Color(0,5,20,210),0,H,new Color(10,0,30,230)));
        g2.fillRect(0, 0, W, H);
        g2.setColor(new Color(0,230,255));
        g2.setFont(new Font("Monospaced", Font.BOLD, 62));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(title, W/2 - fm.stringWidth(title)/2, H/2 - 30);
        g2.setFont(new Font("Monospaced", Font.PLAIN, 16));
        fm = g2.getFontMetrics();
        g2.setColor(Color.WHITE);
        g2.drawString(sub, W/2 - fm.stringWidth(sub)/2, H/2 + 46);
        // Score final
        g2.setFont(new Font("Monospaced", Font.BOLD, 14));
        g2.setColor(new Color(255,220,50,200));
        String sc = "SCORE: "+player.score;
        fm = g2.getFontMetrics();
        g2.drawString(sc, W/2 - fm.stringWidth(sc)/2, H/2 + 76);
    }

    // ── Tela de Créditos ────────────────────────────────────────────────
    private void drawCredits(Graphics2D g2) {
        long t = System.currentTimeMillis();
        // Background do espaço profundo
        g2.setPaint(new GradientPaint(0, 0, new Color(5,5,15,255), 0, H, new Color(15,5,30,255)));
        g2.fillRect(0, 0, W, H);
        
        // Estrelas
        java.util.Random r = new java.util.Random(42);
        for (int i = 0; i < 100; i++) {
            int sx = r.nextInt(W), sy = r.nextInt(H);
            int ba = 100 + (int)(Math.sin(t*0.001+i)*55);
            g2.setColor(new Color(220, 230, 255, Math.max(0,Math.min(255,ba))));
            g2.fillRect(sx, sy, 2, 2);
        }

        int cx = W/2;
        int cy = H/2 - 50;

        // Animação do Personagem se tornando uma estrela
        if (creditsTimer < 3.0) {
            // Personagem flutuando
            float hover = (float)Math.sin(creditsTimer * 4) * 10;
            player.x = cx - player.w/2;
            player.y = cy - player.h/2 + hover;
            player.starPower = 7; // Aura máxima de estrela
            player.draw(g2);
        } else {
            // Explosão e Estrela
            double starTime = creditsTimer - 3.0;
            if (starTime < 1.0) {
                // Flash de luz expandindo
                int flashSize = (int)(starTime * 1000);
                g2.setColor(new Color(255, 255, 255, (int)((1.0 - starTime)*255)));
                g2.fillOval(cx - flashSize/2, cy - flashSize/2, flashSize, flashSize);
            }
            
            // Estrela Brilhante final
            int pulse = (int)(Math.sin(t * 0.005) * 15);
            g2.setColor(new Color(255, 255, 200, 150));
            g2.fillOval(cx - 40 - pulse, cy - 40 - pulse, 80 + pulse*2, 80 + pulse*2);
            g2.setColor(Color.WHITE);
            for(int i=0; i<8; i++) {
                double a = t * 0.002 + i * Math.PI/4;
                int len = 50 + pulse*2;
                int x2 = cx + (int)(Math.cos(a) * len);
                int y2 = cy + (int)(Math.sin(a) * len);
                g2.setStroke(new BasicStroke(4f));
                g2.drawLine(cx, cy, x2, y2);
            }
            g2.setColor(new Color(255, 255, 220));
            g2.fillOval(cx - 20, cy - 20, 40, 40);
            g2.setColor(Color.WHITE);
            g2.fillOval(cx - 10, cy - 10, 20, 20);
            g2.setStroke(new BasicStroke(1f));
        }

        // Textos rolando
        double scrollTime = Math.max(0, creditsTimer - 2.0);
        int textY = H + 100 - (int)(scrollTime * 50);

        g2.setFont(new Font("Monospaced", Font.BOLD, 22));
        g2.setColor(Color.WHITE);
        String[] lines = {
            "NEBLUUM",
            "",
            "UMA JORNADA ESTELAR",
            "",
            "DESENVOLVIDO POR:",
            "Heryckmp",
            "",
            "GITHUB:",
            "github.com/heryckmp",
            "",
            "AGRADECIMENTOS ESPECIAIS:",
            "A você por jogar!",
            "",
            "\"O personagem se tornou uma estrela...\"",
            "",
            "",
            "PRESSIONE ENTER PARA VOLTAR AO MENU"
        };
        
        FontMetrics fm = g2.getFontMetrics();
        for (String line : lines) {
            if (textY > 0 && textY < H + 50) {
                if (line.contains("PRESSIONE ENTER")) {
                    g2.setFont(new Font("Monospaced", Font.PLAIN, 14));
                    g2.setColor(new Color(255,255,255,150 + (int)(Math.sin(t*0.005)*100)));
                    fm = g2.getFontMetrics();
                } else if (line.equals("NEBLUUM")) {
                    g2.setFont(new Font("Monospaced", Font.BOLD, 36));
                    g2.setColor(new Color(0,200,255));
                    fm = g2.getFontMetrics();
                } else if (line.contains("GITHUB")) {
                    g2.setFont(new Font("Monospaced", Font.BOLD, 22));
                    g2.setColor(new Color(200, 200, 255));
                    fm = g2.getFontMetrics();
                } else {
                    g2.setFont(new Font("Monospaced", Font.BOLD, 22));
                    g2.setColor(Color.WHITE);
                    fm = g2.getFontMetrics();
                }
                g2.drawString(line, W/2 - fm.stringWidth(line)/2, textY);
            }
            textY += 45;
        }
    }
}
