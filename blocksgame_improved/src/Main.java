import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Улучшенная главная точка входа в игру Blocks Game
 * С улучшенным меню и настройками игры
 */
public class Main {

    public static void main(String[] args) {
        // Настройка внешнего вида
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        SwingUtilities.invokeLater(Main::showMenu);
    }

    private static void showMenu() {
        JFrame menuFrame = new JFrame("Blocks Game - Improved Edition");
        menuFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        menuFrame.setResizable(false);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(30, 30, 50));
        panel.setPreferredSize(new Dimension(700, 500));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.insets = new Insets(20, 30, 20, 30);

        // Заголовок с градиентом
        JLabel title = new JLabel("BLOCKS");
        title.setFont(new Font("SansSerif", Font.BOLD, 80));
        title.setForeground(new Color(100, 200, 255));
        gbc.gridy = 0;
        panel.add(title, gbc);

        // Подзаголовок
        JLabel subtitle = new JLabel("IMPROVED EDITION");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 24));
        subtitle.setForeground(new Color(180, 180, 200));
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 30, 30, 30);
        panel.add(subtitle, gbc);

        // Кнопка играть
        JButton playButton = createStyledButton("Играть", 32);
        gbc.gridy = 2;
        gbc.insets = new Insets(15, 30, 15, 30);
        panel.add(playButton, gbc);

        // Кнопка управления
        JButton controlsButton = createStyledButton("Управление", 24);
        gbc.gridy = 3;
        panel.add(controlsButton, gbc);

        // Информация об управлении
        JLabel controlsInfo = new JLabel("<html><center>WASD или Стрелки - Движение<br>" +
                "ЛКМ - Разрушить блок | ПКМ - Поставить блок<br>" +
                "Клавиши 1-9 - Выбор блока | ESC - Меню</center></html>");
        controlsInfo.setFont(new Font("SansSerif", Font.PLAIN, 16));
        controlsInfo.setForeground(new Color(150, 150, 180));
        gbc.gridy = 4;
        gbc.insets = new Insets(30, 50, 20, 50);
        panel.add(controlsInfo, gbc);

        menuFrame.add(panel);
        menuFrame.pack();
        menuFrame.setLocationRelativeTo(null);
        menuFrame.setVisible(true);

        playButton.addActionListener(e -> {
            menuFrame.setVisible(false);
            startGame(menuFrame);
        });
        
        controlsButton.addActionListener(e -> {
            showControlsDialog(menuFrame);
        });
    }

    private static JButton createStyledButton(String text, int fontSize) {
        JButton button = new JButton(text);
        button.setFont(new Font("SansSerif", Font.PLAIN, fontSize));
        button.setFocusPainted(false);
        button.setBackground(new Color(60, 120, 200));
        button.setForeground(Color.WHITE);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(40, 80, 140), 2),
                BorderFactory.createEmptyBorder(10, 30, 10, 30)
        ));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Эффект при наведении
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(80, 140, 220));
            }
            
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(60, 120, 200));
            }
        });
        
        return button;
    }

    private static void startGame(JFrame menuFrame) {
        JFrame gameFrame = new JFrame("Blocks Game - Improved");
        gameFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        gameFrame.setResizable(false);
        gameFrame.setExtendedState(JFrame.MAXIMIZED_BOTH); // Полноэкранный режим

        BlocksGame game = new BlocksGame();
        gameFrame.add(game);
        gameFrame.setVisible(true); // Сначала показываем
        gameFrame.pack(); // Затем подстраиваем размер
        gameFrame.setLocationRelativeTo(null);
        
        game.init();
        game.start();

        gameFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                game.stop();
                menuFrame.setVisible(true);
            }
        });
    }
    
    private static void showControlsDialog(JFrame parent) {
        JDialog dialog = new JDialog(parent, "Управление", true);
        dialog.setLayout(new BorderLayout());
        
        JPanel content = new JPanel(new GridBagLayout());
        content.setBackground(new Color(40, 40, 60));
        content.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 5, 5, 5);
        
        String[][] controls = {
            {"W / Стрелка Вверх", "Прыжок"},
            {"A / Стрелка Влево", "Движение влево"},
            {"D / Стрелка Вправо", "Движение вправо"},
            {"S / Стрелка Вниз", "Приседание"},
            {"ЛКМ (удерживать)", "Разрушить блок"},
            {"ПКМ (удерживать)", "Поставить блок"},
            {"Клавиши 1-9", "Выбор блока из инвентаря"},
            {"ESC", "Выход в меню"}
        };
        
        for (int i = 0; i < controls.length; i++) {
            JLabel key = new JLabel(controls[i][0]);
            key.setFont(new Font("SansSerif", Font.BOLD, 14));
            key.setForeground(new Color(100, 200, 255));
            gbc.gridy = i;
            content.add(key, gbc);
            
            JLabel action = new JLabel("- " + controls[i][1]);
            action.setFont(new Font("SansSerif", Font.PLAIN, 14));
            action.setForeground(Color.WHITE);
            gbc.gridx = 1;
            content.add(action, gbc);
            gbc.gridx = 0;
        }
        
        dialog.add(content, BorderLayout.CENTER);
        
        JButton closeBtn = new JButton("Закрыть");
        closeBtn.setFont(new Font("SansSerif", Font.PLAIN, 16));
        closeBtn.setBackground(new Color(60, 120, 200));
        closeBtn.setForeground(Color.WHITE);
        closeBtn.setFocusPainted(false);
        closeBtn.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
        closeBtn.addActionListener(e -> dialog.dispose());
        
        JPanel btnPanel = new JPanel();
        btnPanel.setBackground(new Color(40, 40, 60));
        btnPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        btnPanel.add(closeBtn);
        dialog.add(btnPanel, BorderLayout.SOUTH);
        
        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }
}
