import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class BlocksGame extends JPanel implements ActionListener, KeyListener, MouseListener, MouseMotionListener {
    private static final int TILE_SIZE = 40;
    private static final int WORLD_WIDTH = 40;
    private static final int WORLD_HEIGHT = 25;
    
    private double playerX = 10 * TILE_SIZE;
    private double playerY = 10 * TILE_SIZE;
    private double velocityY = 0;
    private double velocityX = 0;
    private final double GRAVITY = 0.6;
    private final double JUMP_STRENGTH = -11;
    private final double MOVE_SPEED = 0.5;
    private final double MAX_SPEED = 4.0;
    private final double FRICTION = 0.8;
    
    private boolean[] keys = new boolean[256];
    private int selectedBlock = 1;
    private JFrame frame;
    
    private int cameraX = 0;
    private int cameraY = 0;
    
    private int[][] world = new int[WORLD_WIDTH][WORLD_HEIGHT];
    private Random random = new Random();
    private ArrayList<Particle> particles = new ArrayList<>();
    
    private Color[] blockColors = {
        new Color(0, 0, 0), new Color(101, 67, 33), new Color(54, 139, 34),
        new Color(128, 128, 128), new Color(139, 69, 19), new Color(255, 215, 0),
        new Color(192, 192, 192), new Color(245, 245, 220), new Color(70, 130, 180),
        new Color(178, 34, 34), new Color(255, 140, 0), new Color(75, 0, 130),
        new Color(255, 255, 255), new Color(47, 79, 79)
    };

    public BlocksGame() {
        setPreferredSize(new Dimension(800, 600));
        setBackground(Color.CYAN);
        setFocusable(true);
        addKeyListener(this);
        addMouseListener(this);
        addMouseMotionListener(this);
        generateWorld();
        new javax.swing.Timer(16, this).start();
    }
    
    private void generateWorld() {
        for (int x = 0; x < WORLD_WIDTH; x++) {
            int groundLevel = 10 + random.nextInt(3);
            for (int y = 0; y < WORLD_HEIGHT; y++) {
                if (y < groundLevel) world[x][y] = 0;
                else if (y == groundLevel) world[x][y] = 2;
                else if (y < groundLevel + 5) world[x][y] = 1;
                else {
                    int r = random.nextInt(100);
                    if (r < 5) world[x][y] = 5;
                    else if (r < 10) world[x][y] = 6;
                    else if (r < 15) world[x][y] = 4;
                    else world[x][y] = 3;
                }
            }
            if (x > 2 && x < WORLD_WIDTH - 2 && random.nextInt(10) == 0) {
                int t = groundLevel;
                world[x][t] = 2; world[x][t-1] = 4; world[x][t-2] = 4;
                world[x][t-3] = 4; world[x][t-4] = 2; world[x-1][t-3] = 2;
                world[x+1][t-3] = 2; world[x][t-5] = 2;
            }
        }
        playerY = (groundLevelAt(10) - 3) * TILE_SIZE;
    }
    
    private int groundLevelAt(int x) {
        for (int y = 0; y < WORLD_HEIGHT; y++) if (world[x][y] != 0) return y;
        return WORLD_HEIGHT - 1;
    }

    public void actionPerformed(ActionEvent e) {
        updatePhysics();
        repaint();
    }
    
    private void updatePhysics() {
        if (keys[KeyEvent.VK_LEFT] || keys[KeyEvent.VK_A]) velocityX -= MOVE_SPEED;
        if (keys[KeyEvent.VK_RIGHT] || keys[KeyEvent.VK_D]) velocityX += MOVE_SPEED;
        
        velocityX *= FRICTION;
        if (Math.abs(velocityX) < 0.1) velocityX = 0;
        if (velocityX > MAX_SPEED) velocityX = MAX_SPEED;
        if (velocityX < -MAX_SPEED) velocityX = -MAX_SPEED;
        
        velocityY += GRAVITY;
        
        double nextX = playerX + velocityX;
        double nextY = playerY + velocityY;
        int pw = 20, ph = 36;
        
        if (!checkCollision(nextX, playerY, pw, ph)) playerX = nextX;
        else velocityX = 0;
        
        boolean onGround = false;
        if (!checkCollision(playerX, nextY, pw, ph)) {
            playerY = nextY;
        } else {
            if (velocityY > 0) {
                onGround = true;
                int by = (int)((playerY + ph) / TILE_SIZE);
                int bx = (int)((playerX + pw/2) / TILE_SIZE);
                if (bx >= 0 && bx < WORLD_WIDTH && by >= 0 && by < WORLD_HEIGHT && world[bx][by] != 0) {
                    playerY = by * TILE_SIZE - ph - 0.001;
                }
            }
            velocityY = 0;
        }
        
        if ((keys[KeyEvent.VK_SPACE] || keys[KeyEvent.VK_UP] || keys[KeyEvent.VK_W]) && onGround)
            velocityY = JUMP_STRENGTH;
        
        if (playerX < 0) playerX = 0;
        if (playerX > WORLD_WIDTH * TILE_SIZE - pw) playerX = WORLD_WIDTH * TILE_SIZE - pw;
        if (playerY > WORLD_HEIGHT * TILE_SIZE) { playerY = 0; velocityY = 0; }
        
        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            p.update();
            if (p.life <= 0) particles.remove(i);
        }
        
        int sw = getWidth(), sh = getHeight();
        cameraX = (int)(playerX - sw / 2.0);
        cameraY = (int)(playerY - sh / 2.0);
        cameraX = Math.max(0, Math.min(cameraX, WORLD_WIDTH * TILE_SIZE - sw));
        cameraY = Math.max(0, Math.min(cameraY, WORLD_HEIGHT * TILE_SIZE - sh));
    }
    
    private boolean checkCollision(double x, double y, int w, int h) {
        int sx = (int)(x / TILE_SIZE), ex = (int)((x + w) / TILE_SIZE);
        int sy = (int)(y / TILE_SIZE), ey = (int)((y + h) / TILE_SIZE);
        for (int ix = sx; ix <= ex; ix++)
            for (int iy = sy; iy <= ey; iy++)
                if (ix >= 0 && ix < WORLD_WIDTH && iy >= 0 && iy < WORLD_HEIGHT && world[ix][iy] != 0)
                    return true;
        return false;
    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        int w = getWidth(), h = getHeight();
        GradientPaint sky = new GradientPaint(0, 0, new Color(135, 206, 235), 0, h, new Color(255, 255, 224));
        g2d.setPaint(sky);
        g2d.fillRect(0, 0, w, h);
        
        g2d.translate(-cameraX, -cameraY);
        
        int sc = cameraX / TILE_SIZE, ec = sc + w / TILE_SIZE + 2;
        int sr = cameraY / TILE_SIZE, er = sr + h / TILE_SIZE + 2;
        
        for (int x = Math.max(0, sc); x < Math.min(WORLD_WIDTH, ec); x++)
            for (int y = Math.max(0, sr); y < Math.min(WORLD_HEIGHT, er); y++)
                if (world[x][y] != 0) drawBlock(g2d, x * TILE_SIZE, y * TILE_SIZE, world[x][y]);
        
        // Тень
        int pcx = (int)(playerX + 10), pby = (int)(playerY + 36);
        int gy = pby, bx = pcx / TILE_SIZE, by = pby / TILE_SIZE;
        if (by < WORLD_HEIGHT && world[bx][by] == 0) {
            for (int y = by; y < WORLD_HEIGHT; y++)
                if (world[bx][y] != 0) { gy = y * TILE_SIZE; break; }
        } else if (by < WORLD_HEIGHT) gy = by * TILE_SIZE;
        
        double dist = (playerY + 36) - gy;
        float alpha = 1.0f - (float)(dist / 150.0);
        if (alpha < 0) alpha = 0;
        if (alpha > 0.6) alpha = 0.6f;
        int ss = Math.max(5, 20 - (int)(dist / 5));
        
        g2d.setColor(new Color(0, 0, 0, (int)(alpha * 255)));
        g2d.fillOval((int)playerX + (20-ss)/2, gy - 5, ss, 10);
        
        // Игрок
        g2d.setColor(new Color(255, 100, 100));
        g2d.fillRect((int)playerX, (int)playerY, 20, 20);
        g2d.setColor(new Color(255, 200, 150));
        g2d.fillRect((int)playerX + 2, (int)playerY - 10, 16, 10);
        g2d.setColor(new Color(50, 50, 200));
        g2d.fillRect((int)playerX, (int)playerY + 20, 20, 16);
        
        g2d.setColor(Color.WHITE);
        int eo = velocityX >= 0 ? 12 : 2;
        g2d.fillRect((int)playerX + eo, (int)playerY - 8, 4, 4);
        g2d.setColor(Color.BLACK);
        g2d.fillRect((int)playerX + eo + (velocityX >= 0 ? 2 : 0), (int)playerY - 7, 2, 2);
        
        g2d.translate(cameraX, cameraY);
        
        Point mp = getMousePosition();
        if (mp != null) {
            int mx = mp.x + cameraX, my = mp.y + cameraY;
            int bmx = mx / TILE_SIZE, bmy = my / TILE_SIZE;
            if (bmx >= 0 && bmx < WORLD_WIDTH && bmy >= 0 && bmy < WORLD_HEIGHT) {
                g2d.setColor(Color.WHITE);
                g2d.setStroke(new BasicStroke(2));
                g2d.drawRect(bmx * TILE_SIZE - cameraX, bmy * TILE_SIZE - cameraY, TILE_SIZE, TILE_SIZE);
            }
        }
        
        for (Particle p : particles) p.draw(g2d, cameraX, cameraY);
        drawInventory(g2d, w, h);
    }
    
    private void drawBlock(Graphics2D g, int x, int y, int t) {
        Color c = blockColors[t];
        g.setColor(c);
        g.fillRect(x, y, TILE_SIZE, TILE_SIZE);
        g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 100));
        g.fillRect(x + 5, y + 5, TILE_SIZE - 10, TILE_SIZE - 10);
        g.setColor(c.darker());
        g.drawRect(x, y, TILE_SIZE, TILE_SIZE);
    }
    
    private void drawInventory(Graphics2D g, int w, int h) {
        int ph = 60, py = h - ph - 10, pw = 400, px = (w - pw) / 2;
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRoundRect(px, py, pw, ph, 10, 10);
        g.setColor(Color.WHITE);
        g.drawRoundRect(px, py, pw, ph, 10, 10);
        
        int ss = 40, gap = 10, sx = px + 20;
        String[] names = {"Земля", "Трава", "Камень", "Дерево", "Золото", "Серебро", "Песок", "Вода", "Кирпич", "Медь"};
        
        for (int i = 1; i <= 10; i++) {
            int x = sx + (i - 1) * (ss + gap), y = py + 10;
            if (i == selectedBlock) {
                g.setColor(new Color(255, 215, 0, 100));
                g.fillRoundRect(x - 2, y - 2, ss + 4, ss + 4, 5, 5);
                g.setColor(Color.YELLOW);
                g.drawRoundRect(x - 2, y - 2, ss + 4, ss + 4, 5, 5);
            }
            drawBlock(g, x, y, i);
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 12));
            g.drawString(String.valueOf(i), x + 2, y - 5);
        }
        g.setFont(new Font("Arial", Font.PLAIN, 14));
        g.drawString("Выбрано: " + names[selectedBlock-1], px + 20, py - 5);
    }
    
    private void createParticles(int x, int y, int t) {
        for (int i = 0; i < 8; i++) particles.add(new Particle(x, y, blockColors[t]));
    }

    public void keyPressed(KeyEvent e) {
        keys[e.getKeyCode()] = true;
        if (e.getKeyCode() >= KeyEvent.VK_1 && e.getKeyCode() <= KeyEvent.VK_9) selectedBlock = e.getKeyCode() - 48;
        if (e.getKeyCode() == KeyEvent.VK_0) selectedBlock = 10;
        if (e.getKeyCode() == KeyEvent.VK_F11) toggleFullScreen();
    }
    public void keyReleased(KeyEvent e) { keys[e.getKeyCode()] = false; }
    public void keyTyped(KeyEvent e) {}
    
    public void mouseClicked(MouseEvent e) {
        int mx = e.getX() + cameraX, my = e.getY() + cameraY;
        int bx = mx / TILE_SIZE, by = my / TILE_SIZE;
        if (bx < 0 || bx >= WORLD_WIDTH || by < 0 || by >= WORLD_HEIGHT) return;
        
        if (e.getButton() == MouseEvent.BUTTON1 && world[bx][by] != 0) {
            createParticles(bx * TILE_SIZE + 20, by * TILE_SIZE + 20, world[bx][by]);
            world[bx][by] = 0;
        } else if (e.getButton() == MouseEvent.BUTTON3 && world[bx][by] == 0) {
            Rectangle pr = new Rectangle((int)playerX, (int)playerY, 20, 36);
            Rectangle br = new Rectangle(bx * TILE_SIZE, by * TILE_SIZE, TILE_SIZE, TILE_SIZE);
            if (!pr.intersects(br)) world[bx][by] = selectedBlock;
        }
    }
    
    private void toggleFullScreen() {
        frame.dispose();
        if (frame.isUndecorated()) {
            frame.setUndecorated(false);
            frame.setSize(800, 600);
            frame.setLocationRelativeTo(null);
        } else {
            frame.setUndecorated(true);
            frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        }
        frame.setContentPane(this);
        frame.setVisible(true);
        requestFocusInWindow();
    }
    
    public void mousePressed(MouseEvent e) { if (SwingUtilities.isRightMouseButton(e)) mouseClicked(e); }
    public void mouseReleased(MouseEvent e) {}
    public void mouseEntered(MouseEvent e) {}
    public void mouseExited(MouseEvent e) {}
    public void mouseDragged(MouseEvent e) {}
    public void mouseMoved(MouseEvent e) {}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Blocks Game 0.2.2_2");
            BlocksGame g = new BlocksGame();
            g.frame = f;
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setUndecorated(true);
            f.setExtendedState(JFrame.MAXIMIZED_BOTH);
            f.add(g);
            f.setVisible(true);
            g.requestFocusInWindow();
        });
    }
    
    class Particle {
        double x, y, vx, vy;
        Color color;
        int life = 30;
        Particle(int x, int y, Color c) {
            this.x = x; this.y = y; this.color = c;
            vx = (Math.random() - 0.5) * 4; vy = (Math.random() - 0.5) * 4;
        }
        void update() { x += vx; y += vy; vy += 0.2; life--; }
        void draw(Graphics2D g, int cx, int cy) {
            g.setColor(color);
            g.fillRect((int)x - cx, (int)y - cy, 4, 4);
        }
    }
}
