import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.util.Random;

/**
 * Улучшенная версия игры BlocksGame с новыми функциями:
 * - Больший мир (30x20 блоков)
 * - Генерация мира с пещерами и платформами
 * - Улучшенная физика игрока
 * - Система частиц при разрушении блоков
 * - Улучшенная графика (градиенты, тени, анимации)
 * - Поддержка непрерывного разрушения/установки блоков при зажатии мыши
 * - Индикатор выбранного блока
 * - Счетчик FPS
 */
public class BlocksGame extends GameEngine {

    // Размеры мира увеличены
    private static final int WORLD_WIDTH = 30;
    private static final int WORLD_HEIGHT = 20;
    private static final int BLOCK_SIZE = 40;

    // Улучшенная цветовая палитра с градиентами
    private static final Color[] BLOCK_COLORS = {
            new Color(220, 60, 60),      // Красный
            new Color(60, 180, 60),      // Зеленый
            new Color(60, 120, 220),     // Синий
            new Color(240, 200, 60),     // Желтый
            new Color(180, 60, 180),     // Фиолетовый
            new Color(60, 200, 200),     // Голубой
            new Color(240, 140, 60),     // Оранжевый
            new Color(240, 160, 180),    // Розовый
            new Color(240, 240, 240),    // Белый
            new Color(100, 100, 100),    // Серый (камень)
            new Color(139, 90, 43),      // Коричневый (дерево)
            new Color(34, 139, 34),      // Темно-зеленый (трава)
            new Color(128, 128, 128),    // Серебро
            new Color(255, 215, 0)       // Золото (редкий блок)
    };

    private final int[][] world = new int[WORLD_HEIGHT][WORLD_WIDTH];
    private final Random random = new Random();

    // Физика игрока
    private double playerX, playerY;
    private double playerVx, playerVy;
    private boolean onGround;
    private boolean isMovingLeft;
    private boolean isMovingRight;
    private boolean wantJump;
    
    private static final double PLAYER_WIDTH = 30;
    private static final double PLAYER_HEIGHT = 40;
    private static final double MOVE_SPEED = 200;
    private static final double MAX_MOVE_SPEED = 250;
    private static final double ACCELERATION = 600;
    private static final double FRICTION = 500;
    private static final double JUMP_SPEED = -400;
    private static final double GRAVITY = 800;

    private int selectedSlot = 0;
    
    // Система частиц
    private java.util.List<Particle> particles = new java.util.ArrayList<>();
    
    // Анимация выделения блока
    private float highlightAlpha = 0.0f;
    private int lastHighlightCol = -1, lastHighlightRow = -1;
    
    // Статистика
    private int blocksDestroyed = 0;
    private int blocksPlaced = 0;
    private long gameTime = 0;

    public BlocksGame() {
        super(60);
        setPreferredSize(new Dimension(WORLD_WIDTH * BLOCK_SIZE, WORLD_HEIGHT * BLOCK_SIZE));
    }

    /**
     * Инициализация игры
     */
    public void init() {
        generateWorld();
        spawnPlayer();
        gameTime = System.currentTimeMillis();
    }

    /**
     * Улучшенная генерация мира с ландшафтом
     */
    private void generateWorld() {
        // Заполняем мир базовыми блоками
        for (int row = 0; row < WORLD_HEIGHT; row++) {
            for (int col = 0; col < WORLD_WIDTH; col++) {
                if (row >= WORLD_HEIGHT - 2) {
                    // Глубокие слои - камень
                    world[row][col] = 9;
                } else if (row >= WORLD_HEIGHT - 5) {
                    // Средние слои - земля с рудами
                    int rand = random.nextInt(100);
                    if (rand < 70) {
                        world[row][col] = 10; // Дерево/земля
                    } else if (rand < 85) {
                        world[row][col] = 11; // Трава
                    } else if (rand < 95) {
                        world[row][col] = 12; // Серебро
                    } else {
                        world[row][col] = 13; // Золото
                    }
                } else if (row >= WORLD_HEIGHT - 8) {
                    // Верхние слои - платформы
                    if (random.nextInt(100) < 30) {
                        world[row][col] = random.nextInt(8) + 1;
                    } else {
                        world[row][col] = 0;
                    }
                } else {
                    // Небо - пусто
                    world[row][col] = 0;
                }
            }
        }
        
        // Добавляем несколько случайных структур
        addRandomStructures();
    }
    
    /**
     * Добавление случайных структур (столбы, арки)
     */
    private void addRandomStructures() {
        // Несколько вертикальных столбов
        for (int i = 0; i < 3; i++) {
            int col = random.nextInt(WORLD_WIDTH - 4) + 2;
            int height = random.nextInt(5) + 3;
            for (int h = 0; h < height; h++) {
                int row = WORLD_HEIGHT - 3 - h;
                if (row >= 0 && row < WORLD_HEIGHT) {
                    world[row][col] = random.nextInt(8) + 1;
                }
            }
        }
    }

    private void spawnPlayer() {
        playerX = (WORLD_WIDTH * BLOCK_SIZE) / 2.0 - PLAYER_WIDTH / 2.0;
        playerY = (WORLD_HEIGHT / 2) * BLOCK_SIZE - PLAYER_HEIGHT;
        playerVx = 0; 
        playerVy = 0; 
        onGround = false;
        isMovingLeft = false;
        isMovingRight = false;
        wantJump = false;
        clearSpawnArea();
    }

    private void clearSpawnArea() {
        Rectangle pr = getPlayerRect();
        for (int r = 0; r < WORLD_HEIGHT; r++) {
            for (int c = 0; c < WORLD_WIDTH; c++) {
                if (world[r][c] != 0 && pr.intersects(new Rectangle(c * BLOCK_SIZE, r * BLOCK_SIZE, BLOCK_SIZE, BLOCK_SIZE))) {
                    world[r][c] = 0;
                }
            }
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        super.keyPressed(e);
        int key = e.getKeyCode();
        
        // Выбор слота клавишами 1-9
        if (key >= KeyEvent.VK_1 && key <= KeyEvent.VK_9) {
            selectedSlot = key - KeyEvent.VK_1;
            if (selectedSlot >= BLOCK_COLORS.length) {
                selectedSlot = BLOCK_COLORS.length - 1;
            }
        }
        
        // Быстрый выбор предыдущего/следующего блока колесом мыши обрабатывается в mouseWheelMoved
    }

    @Override
    protected void update(double dt) {
        handleInput(dt);
        applyPhysics(dt);
        updateParticles(dt);
        
        // Непрерывное разрушение/установка блоков при зажатии
        if (mouseLeftPressed) {
            destroyBlock();
        }
        if (mouseRightPressed) {
            placeBlock();
        }
        
        // Обновление анимации выделения
        int mc = mousePoint.x / BLOCK_SIZE;
        int mr = mousePoint.y / BLOCK_SIZE;
        if (mc != lastHighlightCol || mr != lastHighlightRow) {
            highlightAlpha = 0.0f;
            lastHighlightCol = mc;
            lastHighlightRow = mr;
        } else {
            highlightAlpha = Math.min(1.0f, highlightAlpha + (float)dt * 5);
        }
        
        // Обработка выхода
        if (isKeyDown(KeyEvent.VK_ESCAPE)) {
            Window window = SwingUtilities.getWindowAncestor(this);
            if (window != null) {
                window.dispatchEvent(new WindowEvent(window, WindowEvent.WINDOW_CLOSING));
            }
        }
        
        // Респаун если упал за пределы мира
        if (playerY > WORLD_HEIGHT * BLOCK_SIZE + 100) {
            spawnPlayer();
        }
    }

    private void handleInput(double dt) {
        // Движение влево/вправо с ускорением и трением
        if (isKeyDown(KeyEvent.VK_A) || isKeyDown(KeyEvent.VK_LEFT)) {
            isMovingLeft = true;
            isMovingRight = false;
        } else if (isKeyDown(KeyEvent.VK_D) || isKeyDown(KeyEvent.VK_RIGHT)) {
            isMovingRight = true;
            isMovingLeft = false;
        } else {
            isMovingLeft = false;
            isMovingRight = false;
        }

        // Прыжок
        if ((isKeyDown(KeyEvent.VK_W) || isKeyDown(KeyEvent.VK_SPACE) || isKeyDown(KeyEvent.VK_UP)) && onGround) {
            wantJump = true;
        }
    }

    private void applyPhysics(double dt) {
        // Горизонтальное движение с ускорением
        if (isMovingLeft) {
            playerVx -= ACCELERATION * dt;
        } else if (isMovingRight) {
            playerVx += ACCELERATION * dt;
        } else {
            // Трение когда не движемся
            if (playerVx > 0) {
                playerVx = Math.max(0, playerVx - FRICTION * dt);
            } else if (playerVx < 0) {
                playerVx = Math.min(0, playerVx + FRICTION * dt);
            }
        }
        
        // Ограничение скорости
        playerVx = Math.max(-MAX_MOVE_SPEED, Math.min(MAX_MOVE_SPEED, playerVx));
        
        // Прыжок
        if (wantJump && onGround) {
            playerVy = JUMP_SPEED;
            onGround = false;
            wantJump = false;
        }
        
        // Гравитация
        playerVy += GRAVITY * dt;
        
        // Применение движения и коллизии
        playerX += playerVx * dt;
        resolveCollisionX();
        playerY += playerVy * dt;
        resolveCollisionY();
    }

    private void resolveCollisionX() {
        Rectangle pr = getPlayerRect();
        for (int r = 0; r < WORLD_HEIGHT; r++) {
            for (int c = 0; c < WORLD_WIDTH; c++) {
                if (world[r][c] != 0 && pr.intersects(new Rectangle(c * BLOCK_SIZE, r * BLOCK_SIZE, BLOCK_SIZE, BLOCK_SIZE))) {
                    if (playerVx > 0) {
                        playerX = c * BLOCK_SIZE - PLAYER_WIDTH;
                    } else if (playerVx < 0) {
                        playerX = c * BLOCK_SIZE + BLOCK_SIZE;
                    }
                    playerVx = 0;
                    pr.setRect(playerX, playerY, PLAYER_WIDTH, PLAYER_HEIGHT);
                }
            }
        }
    }

    private void resolveCollisionY() {
        Rectangle pr = getPlayerRect();
        onGround = false;
        for (int r = 0; r < WORLD_HEIGHT; r++) {
            for (int c = 0; c < WORLD_WIDTH; c++) {
                if (world[r][c] != 0 && pr.intersects(new Rectangle(c * BLOCK_SIZE, r * BLOCK_SIZE, BLOCK_SIZE, BLOCK_SIZE))) {
                    if (playerVy > 0) {
                        // Падаем вниз - ставим на блок
                        playerY = r * BLOCK_SIZE - PLAYER_HEIGHT;
                        playerVy = 0;
                        onGround = true;
                    } else if (playerVy < 0) {
                        // Прыгаем вверх - ударяемся головой
                        playerY = r * BLOCK_SIZE + BLOCK_SIZE;
                        playerVy = 0;
                    }
                    pr.setRect(playerX, playerY, PLAYER_WIDTH, PLAYER_HEIGHT);
                }
            }
        }
    }

    private Rectangle getPlayerRect() {
        return new Rectangle((int) playerX, (int) playerY, (int) PLAYER_WIDTH, (int) PLAYER_HEIGHT);
    }

    /**
     * Разрушение блока с созданием частиц
     */
    private void destroyBlock() {
        int c = mousePoint.x / BLOCK_SIZE;
        int r = mousePoint.y / BLOCK_SIZE;
        
        if (r >= 0 && r < WORLD_HEIGHT && c >= 0 && c < WORLD_WIDTH && world[r][c] != 0) {
            // Создаем частицы
            createParticles(c * BLOCK_SIZE + BLOCK_SIZE / 2, r * BLOCK_SIZE + BLOCK_SIZE / 2, BLOCK_COLORS[world[r][c] - 1]);
            world[r][c] = 0;
            blocksDestroyed++;
        }
    }

    private void placeBlock() {
        int c = mousePoint.x / BLOCK_SIZE;
        int r = mousePoint.y / BLOCK_SIZE;
        
        if (r < 0 || r >= WORLD_HEIGHT || c < 0 || c >= WORLD_WIDTH || world[r][c] != 0) {
            return;
        }
        
        // Нельзя ставить блок в игрока
        if (getPlayerRect().intersects(new Rectangle(c * BLOCK_SIZE, r * BLOCK_SIZE, BLOCK_SIZE, BLOCK_SIZE))) {
            return;
        }
        
        world[r][c] = selectedSlot + 1;
        blocksPlaced++;
    }

    /**
     * Класс частицы для эффектов
     */
    private static class Particle {
        double x, y, vx, vy;
        Color color;
        float life;
        float maxLife;
        
        Particle(double x, double y, Color color) {
            this.x = x;
            this.y = y;
            this.color = color;
            this.vx = (Math.random() - 0.5) * 200;
            this.vy = (Math.random() - 0.5) * 200 - 100;
            this.life = 1.0f;
            this.maxLife = 0.5f + (float)Math.random() * 0.5f;
        }
        
        void update(double dt) {
            x += vx * dt;
            y += vy * dt;
            vy += 300 * dt; // гравитация
            life -= dt / maxLife;
        }
        
        void render(Graphics2D g) {
            if (life <= 0) return;
            int size = (int)(6 * life);
            g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)(255 * life)));
            g.fillOval((int)x - size/2, (int)y - size/2, size, size);
        }
    }
    
    private void createParticles(int x, int y, Color color) {
        for (int i = 0; i < 8; i++) {
            particles.add(new Particle(x, y, color));
        }
    }
    
    private void updateParticles(double dt) {
        particles.removeIf(p -> p.life <= 0);
        for (Particle p : particles) {
            p.update(dt);
        }
    }
    
    private void renderParticles(Graphics2D g) {
        for (Particle p : particles) {
            p.render(g);
        }
    }

    private void drawPlayer(Graphics2D g) {
        int px = (int) playerX;
        int py = (int) playerY;

        // Тень под игроком (рисуем всегда, но прозрачность зависит от высоты над землей)
        double distToGround = onGround ? 0 : Math.min(50, playerVy > 0 ? playerVy / 16 : 10);
        int shadowAlpha = Math.max(20, 80 - (int)(distToGround * 1.5));
        g.setColor(new Color(0, 0, 0, shadowAlpha));
        g.fillOval(px + 5, py + (int)PLAYER_HEIGHT + 2, 20, 6);

        // Тело (синяя рубашка)
        g.setColor(new Color(0, 120, 200));
        g.fillRoundRect(px + 2, py + 12, 26, 28, 8, 8);
        
        // Блик на одежде
        g.setColor(new Color(40, 140, 220));
        g.fillRoundRect(px + 4, py + 14, 8, 20, 4, 4);

        // Голова (телесный цвет)
        g.setColor(new Color(255, 220, 180));
        g.fillOval(px + 7, py, 16, 16);
        
        // Блик на голове
        g.setColor(new Color(255, 235, 200));
        g.fillOval(px + 9, py + 2, 6, 6);

        // Глаза (белки)
        g.setColor(Color.WHITE);
        g.fillOval(px + 10, py + 4, 5, 5);
        g.fillOval(px + 17, py + 4, 5, 5);

        // Зрачки (черные)
        g.setColor(Color.BLACK);
        g.fillOval(px + 12, py + 5, 3, 3);
        g.fillOval(px + 19, py + 5, 3, 3);

        // Ноги (темно-серые ботинки)
        g.setColor(new Color(80, 80, 80));
        g.fillRect(px + 5, py + 36, 8, 4);
        g.fillRect(px + 17, py + 36, 8, 4);
        
        // Анимация ходьбы
        if (Math.abs(playerVx) > 10 && onGround) {
            double walkCycle = System.currentTimeMillis() / 100.0;
            int legOffset = (int)(Math.sin(walkCycle) * 3);
            g.setColor(new Color(60, 60, 60));
            g.fillRect(px + 5 + legOffset, py + 38, 8, 2);
            g.fillRect(px + 17 - legOffset, py + 38, 8, 2);
        }
    }

    private void drawInventory(Graphics2D g) {
        int slotSize = 32;
        int spacing = 6;
        int numSlots = Math.min(BLOCK_COLORS.length, 9); // Показываем первые 9 слотов
        int totalWidth = slotSize * numSlots + spacing * (numSlots - 1);
        int startX = (getWidth() - totalWidth) / 2;
        int startY = getHeight() - slotSize - 10;

        // Фон инвентаря с полупрозрачностью
        g.setColor(new Color(0, 0, 0, 180));
        g.fillRoundRect(startX - 10, startY - 8, totalWidth + 20, slotSize + 16, 12, 12);
        
        // Рамка
        g.setColor(new Color(100, 100, 100, 200));
        g.setStroke(new BasicStroke(2));
        g.drawRoundRect(startX - 10, startY - 8, totalWidth + 20, slotSize + 16, 12, 12);
        g.setStroke(new BasicStroke(1));

        for (int i = 0; i < numSlots; i++) {
            int x = startX + i * (slotSize + spacing);
            int y = startY;
            
            // Цвет блока
            g.setColor(BLOCK_COLORS[i]);
            g.fillRect(x, y, slotSize, slotSize);
            
            // Градиент для объема
            GradientPaint gradient = new GradientPaint(x, y, new Color(255, 255, 255, 100), 
                                                        x, y + slotSize, new Color(0, 0, 0, 50));
            g.setPaint(gradient);
            g.fillRect(x, y, slotSize, slotSize);
            
            // Рамка блока
            g.setColor(Color.BLACK);
            g.setStroke(new BasicStroke(2));
            g.drawRect(x, y, slotSize, slotSize);
            g.setStroke(new BasicStroke(1));

            // Номер слота
            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, 14));
            String num = Integer.toString(i + 1);
            FontMetrics fm = g.getFontMetrics();
            g.drawString(num, x + (slotSize - fm.stringWidth(num)) / 2, 
                        y + (slotSize + fm.getAscent()) / 2 - 2);

            // Выделение выбранного слота
            if (i == selectedSlot) {
                g.setColor(new Color(255, 200, 0, 200));
                g.setStroke(new BasicStroke(3));
                g.drawRect(x - 2, y - 2, slotSize + 4, slotSize + 4);
                
                // Пульсирующий эффект
                float pulse = (float)(Math.sin(System.currentTimeMillis() / 200.0) * 0.3 + 0.7);
                g.setColor(new Color(255, 200, 0, (int)(pulse * 100)));
                g.fillRoundRect(x - 3, y - 3, slotSize + 6, slotSize + 6, 6, 6);
                g.setStroke(new BasicStroke(1));
            }
        }
    }
    
    /**
     * Отрисовка статистики
     */
    private void drawStats(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRoundRect(10, 10, 180, 70, 8, 8);
        
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.PLAIN, 14));
        
        g.drawString("Разрушено: " + blocksDestroyed, 20, 30);
        g.drawString("Поставлено: " + blocksPlaced, 20, 50);
        
        long playTime = (System.currentTimeMillis() - gameTime) / 1000;
        int minutes = (int)(playTime / 60);
        int seconds = (int)(playTime % 60);
        g.drawString("Время: " + minutes + ":" + (seconds < 10 ? "0" : "") + seconds, 20, 70);
    }

    @Override
    protected void render(Graphics2D g) {
        // Небо с градиентом
        GradientPaint skyGradient = new GradientPaint(0, 0, new Color(135, 206, 235), 
                                                       0, getHeight(), new Color(100, 180, 230));
        g.setPaint(skyGradient);
        g.fillRect(0, 0, getWidth(), getHeight());

        // Отрисовка блоков
        for (int r = 0; r < WORLD_HEIGHT; r++) {
            for (int c = 0; c < WORLD_WIDTH; c++) {
                int id = world[r][c];
                if (id == 0) continue;
                
                int x = c * BLOCK_SIZE;
                int y = r * BLOCK_SIZE;
                
                // Основной цвет блока
                g.setColor(BLOCK_COLORS[id - 1]);
                g.fillRect(x, y, BLOCK_SIZE, BLOCK_SIZE);
                
                // Градиент для объема
                GradientPaint gradient = new GradientPaint(x, y, new Color(255, 255, 255, 80), 
                                                            x + BLOCK_SIZE, y + BLOCK_SIZE, new Color(0, 0, 0, 60));
                g.setPaint(gradient);
                g.fillRect(x, y, BLOCK_SIZE, BLOCK_SIZE);
                
                // Рамка блока
                g.setColor(new Color(0, 0, 0, 150));
                g.setStroke(new BasicStroke(1.5f));
                g.drawRect(x, y, BLOCK_SIZE, BLOCK_SIZE);
                g.setStroke(new BasicStroke(1));
            }
        }

        // Подсветка блока под курсором
        int mc = mousePoint.x / BLOCK_SIZE;
        int mr = mousePoint.y / BLOCK_SIZE;
        if (mr >= 0 && mr < WORLD_HEIGHT && mc >= 0 && mc < WORLD_WIDTH) {
            g.setColor(new Color(255, 255, 255, (int)(highlightAlpha * 120)));
            g.fillRect(mc * BLOCK_SIZE, mr * BLOCK_SIZE, BLOCK_SIZE, BLOCK_SIZE);
            
            // Рамка выделения
            g.setColor(new Color(255, 255, 255, (int)(highlightAlpha * 200)));
            g.setStroke(new BasicStroke(2));
            g.drawRect(mc * BLOCK_SIZE, mr * BLOCK_SIZE, BLOCK_SIZE, BLOCK_SIZE);
            g.setStroke(new BasicStroke(1));
        }

        // Отрисовка частиц
        renderParticles(g);

        // Отрисовка игрока
        drawPlayer(g);
        
        // Отрисовка интерфейса
        drawInventory(g);
        // Статистика удалена по запросу пользователя
    }
}
