import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.ArrayList;

public class Game extends JPanel implements ActionListener, KeyListener {

    int W, H;
    static final int GS = 32;
    static final int HUD_H = 40;

    int state = 0;          // 0=menu 1=playing 2=paused 3=gameover
    int level = 1;
    int score = 0;
    int highScore = 0;
    int lives = 5;
    long gameTime = 0;
    boolean paused = false;

    Camera camera;
    Player player;
    LevelManager levelManager;
    Room currentRoom;

    SoundPlayer sound;
    Random random = new Random();
    Timer timer;

    boolean keys[] = new boolean[512];

    public Game(int w, int h) {
        W = w; H = h;
        setPreferredSize(new Dimension(w, h));
        setBackground(new Color(8, 8, 18));
        setFocusable(true);

        camera = new Camera(w, h);
        sound = new SoundPlayer();
        addKeyListener(this);

        levelManager = new LevelManager();
        startLevel();

        timer = new Timer(16, this);
        timer.start();
    }

    void startLevel() {
        currentRoom = levelManager.buildLevel(level);
        if (level == 1 || state != 1) {
            player = new Player(50, 200);
        } else {
            player.respawn(50, 200);
        }
        camera.target = player;
        camera.x = 0; camera.y = 0;
        paused = false;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (state == 1 && !paused) {
            gameTime++;

            player.inputLeft = keys[KeyEvent.VK_LEFT] || keys[KeyEvent.VK_A];
            player.inputRight = keys[KeyEvent.VK_RIGHT] || keys[KeyEvent.VK_D];
            player.inputJump = keys[KeyEvent.VK_UP] || keys[KeyEvent.VK_W] || keys[KeyEvent.VK_SPACE];
            player.inputDown = keys[KeyEvent.VK_DOWN] || keys[KeyEvent.VK_S];
            player.inputAttack = keys[KeyEvent.VK_Z] || keys[KeyEvent.VK_J];
            player.inputDash = keys[KeyEvent.VK_X] || keys[KeyEvent.VK_K];
            player.inputUse = keys[KeyEvent.VK_C] || keys[KeyEvent.VK_L];
            player.inputDrop = keys[KeyEvent.VK_Q];

            player.update(currentRoom);
            currentRoom.update(player, sound, random);
            camera.update();

            score = player.score;

            if (player.hp <= 0) {
                lives--;
                if (lives <= 0) {
                    state = 3;
                    if (score > highScore) highScore = score;
                    sound.playDeath();
                } else {
                    player.respawn(50, 200);
                    camera.x = player.x - W / 2;
                    camera.y = player.y - H / 2;
                    sound.playHurt();
                }
            }

            if (currentRoom.isComplete()) {
                level++;
                sound.playLevelUp();
                player.heal(2);
                startLevel();
            }
        }

        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        if (state == 0) drawMenu(g2);
        else if (state == 1) drawGame(g2);
        else if (state == 2) { drawGame(g2); drawPause(g2); }
        else if (state == 3) { drawGame(g2); drawGameOver(g2); }
    }

    void drawGame(Graphics2D g2) {
        GradientPaint bg = new GradientPaint(0, 0, new Color(6, 6, 18), 0, H, new Color(2, 4, 10));
        g2.setPaint(bg);
        g2.fillRect(0, 0, W, H);

        currentRoom.drawBackground(g2, camera, W, H);

        g2.translate(-camera.x, -camera.y);
        currentRoom.drawTiles(g2, camera);
        currentRoom.drawCollectibles(g2);
        currentRoom.drawEnemies(g2);
        currentRoom.drawProjectiles(g2);
        currentRoom.drawParticles(g2);
        player.draw(g2);
        g2.translate(camera.x, camera.y);

        drawHUD(g2);

        if (paused) {
            g2.setColor(new Color(0, 0, 0, 140));
            g2.fillRect(0, 0, W, H);
        }
    }

    void drawHUD(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 120));
        g2.fillRect(0, 0, W, HUD_H);

        int y = HUD_H / 2 + 4;

        int hpBarW = 120, hpBarH = 10, hpX = 12;
        g2.setColor(Color.decode("#1a1a2a"));
        g2.fillRect(hpX, y - hpBarH/2, hpBarW, hpBarH);
        float hpRatio = Math.max(0, (float)player.hp / player.maxHp);
        Color hpCol = hpRatio > 0.5 ? Color.decode("#00cc88") : Color.decode("#ff4466");
        g2.setColor(hpCol);
        g2.fillRect(hpX, y - hpBarH/2, (int)(hpBarW * hpRatio), hpBarH);
        g2.setColor(Color.decode("#00d4ff"));
        g2.drawRect(hpX, y - hpBarH/2, hpBarW, hpBarH);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Consolas", Font.PLAIN, 9));
        g2.drawString("HP " + player.hp + "/" + player.maxHp, hpX + 2, y - 2);

        if (player.shield > 0) {
            g2.setColor(new Color(100, 200, 255, 180));
            g2.setFont(new Font("Consolas", Font.PLAIN, 10));
            g2.drawString("SH " + player.shield, hpX + 130, y);
        }

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Consolas", Font.BOLD, 14));
        String st = "SCORE " + score;
        g2.drawString(st, W / 2 - g2.getFontMetrics().stringWidth(st) / 2, y);

        g2.setColor(Color.decode("#ffcc00"));
        g2.setFont(new Font("Consolas", Font.BOLD, 11));
        g2.drawString("LVL " + level, W - 60, y - 4);

        g2.setColor(new Color(255, 100, 100, 180));
        g2.drawString("x" + lives, W - 60, y + 10);

        if (player.energy < player.maxEnergy) {
            int enW = 80, enH = 5;
            int enX = W / 2 - enW / 2;
            g2.setColor(new Color(255, 255, 0, 40));
            g2.fillRect(enX, HUD_H - 10, enW, enH);
            g2.setColor(new Color(255, 255, 0, 150));
            g2.fillRect(enX, HUD_H - 10, enW * player.energy / player.maxEnergy, enH);
        }

        // Inventory
        g2.setFont(new Font("Consolas", Font.PLAIN, 10));
        g2.setColor(Color.decode("#ffcc00"));
        g2.drawString("$" + player.coins, 12, HUD_H + 14);
        g2.setColor(Color.decode("#ff8800"));
        g2.drawString("B:" + player.bombs, 42, HUD_H + 14);
        g2.setColor(Color.decode("#ff4488"));
        g2.drawString("P:" + player.healthPotions, 80, HUD_H + 14);
    }

    void drawMenu(Graphics2D g2) {
        GradientPaint bg = new GradientPaint(0, 0, new Color(5, 8, 20), 0, H, Color.BLACK);
        g2.setPaint(bg);
        g2.fillRect(0, 0, W, H);

        g2.setColor(new Color(0, 100, 200, 15));
        int t = (int)(System.currentTimeMillis() / 50);
        int spacing = 40;
        int ox = t % spacing;
        int oy = t % spacing;
        for (int x = -ox; x < W; x += spacing) g2.drawLine(x, 0, x, H);
        for (int y = -oy; y < H; y += spacing) g2.drawLine(0, y, W, y);

        g2.setColor(new Color(0, 200, 255));
        g2.setFont(new Font("Consolas", Font.BOLD, 52));
        String title = "NEBLUUM";
        g2.drawString(title, W / 2 - g2.getFontMetrics().stringWidth(title) / 2, H / 3);

        g2.setColor(new Color(0, 200, 255, 20));
        g2.fillOval(W / 2 - 150, H / 3 - 40, 300, 55);

        g2.setColor(new Color(150, 180, 200));
        g2.setFont(new Font("Consolas", Font.PLAIN, 13));
        String sub = "CYBER PLATFORMER";
        g2.drawString(sub, W / 2 - g2.getFontMetrics().stringWidth(sub) / 2, H / 3 + 25);

        int blink = (int)(System.currentTimeMillis() / 500) % 2;
        if (blink == 0) {
            g2.setColor(new Color(0, 255, 200));
            g2.setFont(new Font("Consolas", Font.BOLD, 15));
            String prompt = ">> ENTER TO START <<";
            g2.drawString(prompt, W / 2 - g2.getFontMetrics().stringWidth(prompt) / 2, H / 2 + 40);
        }

        g2.setColor(new Color(80, 90, 130));
        g2.setFont(new Font("Consolas", Font.PLAIN, 11));
        int cy = H / 2 + 90;
        int cx = W / 2;
        String[] controls = {
            "WASD/Setas - Mover",
            "W/Space/Up - Pular (duplo)",
            "S/Down - Drop-through",
            "Z/J - Atacar com espada",
            "X/K - Dash (esquiva rapida)",
            "C/L - Usar pocao de vida",
            "P/Esc - Pausar",
            "Q - Dropar bomba"
        };
        for (int i = 0; i < controls.length; i++) {
            g2.drawString(controls[i], cx - g2.getFontMetrics().stringWidth(controls[i]) / 2, cy + i * 16);
        }

        if (highScore > 0) {
            g2.setColor(Color.decode("#ffcc00"));
            g2.setFont(new Font("Consolas", Font.BOLD, 14));
            String hs = "High Score: " + highScore;
            g2.drawString(hs, cx - g2.getFontMetrics().stringWidth(hs) / 2, H - 30);
        }
    }

    void drawPause(Graphics2D g2) {
        g2.setColor(new Color(0, 200, 255));
        g2.setFont(new Font("Consolas", Font.BOLD, 36));
        String txt = "PAUSED";
        g2.drawString(txt, W / 2 - g2.getFontMetrics().stringWidth(txt) / 2, H / 2);
        g2.setColor(new Color(150, 150, 180));
        g2.setFont(new Font("Consolas", Font.PLAIN, 14));
        String resume = "P/Esc para continuar";
        g2.drawString(resume, W / 2 - g2.getFontMetrics().stringWidth(resume) / 2, H / 2 + 30);
    }

    void drawGameOver(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 170));
        g2.fillRect(0, 0, W, H);

        g2.setColor(new Color(255, 80, 80));
        g2.setFont(new Font("Consolas", Font.BOLD, 40));
        String go = "GAME OVER";
        g2.drawString(go, W / 2 - g2.getFontMetrics().stringWidth(go) / 2, H / 2 - 20);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Consolas", Font.PLAIN, 18));
        String fs = "Score: " + score + "  |  High: " + highScore + "  |  Level: " + level;
        g2.drawString(fs, W / 2 - g2.getFontMetrics().stringWidth(fs) / 2, H / 2 + 20);

        g2.setColor(new Color(150, 150, 180));
        g2.setFont(new Font("Consolas", Font.PLAIN, 13));
        String restart = "ENTER para reiniciar";
        g2.drawString(restart, W / 2 - g2.getFontMetrics().stringWidth(restart) / 2, H / 2 + 50);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() >= 0 && e.getKeyCode() < keys.length)
            keys[e.getKeyCode()] = true;

        if (e.getKeyCode() == KeyEvent.VK_P || e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            if (state == 1) { paused = true; state = 2; }
            else if (state == 2) { paused = false; state = 1; }
        }
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            if (state == 0) { state = 1; startLevel(); score = 0; }
            else if (state == 3) { state = 0; lives = 5; }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (e.getKeyCode() >= 0 && e.getKeyCode() < keys.length)
            keys[e.getKeyCode()] = false;
    }

    @Override
    public void keyTyped(KeyEvent e) {}
}
