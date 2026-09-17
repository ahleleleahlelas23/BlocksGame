import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Улучшенный игровой движок с поддержкой:
 * - Плавного игрового цикла (delta time)
 * - Обработки ввода с клавиатуры и мыши
 * - Отслеживания состояния клавиш
 */
public abstract class GameEngine extends JPanel implements ActionListener, KeyListener, MouseListener, MouseMotionListener {

    protected final Set<Integer> pressedKeys = new HashSet<>();
    protected Point mousePoint = new Point(0, 0);
    protected boolean mouseLeftPressed = false;
    protected boolean mouseRightPressed = false;
    protected boolean isDragging = false;

    private final Timer timer;
    private final int fps;
    private long lastFrameTime;

    public GameEngine(int fps) {
        this.fps = fps;
        setDoubleBuffered(true);
        setFocusable(true);
        setBackground(Color.BLACK);

        addKeyListener(this);
        addMouseListener(this);
        addMouseMotionListener(this);

        timer = new Timer(1000 / fps, this);
        lastFrameTime = System.nanoTime();
    }

    public void start() {
        timer.start();
        requestFocusInWindow();
        lastFrameTime = System.nanoTime();
    }

    public void stop() {
        timer.stop();
    }

    public boolean isKeyDown(int keyCode) {
        return pressedKeys.contains(keyCode);
    }

    /**
     * Обновление состояния игры
     * @param deltaTime время в секундах с последнего кадра
     */
    protected abstract void update(double deltaTime);
    
    /**
     * Отрисовка игры
     * @param g контекст графики
     */
    protected abstract void render(Graphics2D g);

    @Override
    public void actionPerformed(ActionEvent e) {
        long currentTime = System.nanoTime();
        double deltaTime = (currentTime - lastFrameTime) / 1_000_000_000.0;
        lastFrameTime = currentTime;
        
        // Ограничиваем deltaTime чтобы избежать проблем при лагах
        if (deltaTime > 0.1) deltaTime = 0.1;
        
        update(deltaTime);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // Включаем сглаживание для лучшей графики
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        render(g2d);
    }

    @Override 
    public void keyPressed(KeyEvent e) { 
        pressedKeys.add(e.getKeyCode()); 
        // Предотвращаем стандартное поведение для игровых клавиш
        if (isGameKey(e.getKeyCode())) {
            e.consume();
        }
    }
    
    @Override 
    public void keyReleased(KeyEvent e) { 
        pressedKeys.remove(e.getKeyCode()); 
    }
    
    @Override 
    public void keyTyped(KeyEvent e) {}
    
    private boolean isGameKey(int keyCode) {
        return keyCode == KeyEvent.VK_W || keyCode == KeyEvent.VK_A || 
               keyCode == KeyEvent.VK_S || keyCode == KeyEvent.VK_D ||
               keyCode == KeyEvent.VK_SPACE || keyCode == KeyEvent.VK_UP ||
               keyCode == KeyEvent.VK_DOWN || keyCode == KeyEvent.VK_LEFT ||
               keyCode == KeyEvent.VK_RIGHT;
    }

    @Override 
    public void mousePressed(MouseEvent e) {
        mousePoint = e.getPoint();
        if (SwingUtilities.isLeftMouseButton(e)) {
            mouseLeftPressed = true;
            isDragging = false;
        } else if (SwingUtilities.isRightMouseButton(e)) {
            mouseRightPressed = true;
            isDragging = false;
        }
    }
    
    @Override 
    public void mouseReleased(MouseEvent e) {
        mousePoint = e.getPoint();
        if (SwingUtilities.isLeftMouseButton(e)) {
            mouseLeftPressed = false;
        } else if (SwingUtilities.isRightMouseButton(e)) {
            mouseRightPressed = false;
        }
    }
    
    @Override 
    public void mouseClicked(MouseEvent e) {}
    
    @Override 
    public void mouseMoved(MouseEvent e) { 
        mousePoint = e.getPoint(); 
    }
    
    @Override 
    public void mouseDragged(MouseEvent e) { 
        mousePoint = e.getPoint();
        isDragging = true;
    }
    
    @Override 
    public void mouseEntered(MouseEvent e) {}
    
    @Override 
    public void mouseExited(MouseEvent e) {}
}
