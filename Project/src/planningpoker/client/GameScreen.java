package planningpoker.client;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class GameScreen extends JFrame {

    private final Socket socket;
    private final String username;
    private final boolean isOwner;

    private PrintWriter out;
    private BufferedReader in;

    // UI Bileşenleri
    private JLabel headerTitle;
    private JTextArea logArea;
    private JPanel centerPanel;
    private JPanel mainContainer;

    // Renk Paleti
    private static final Color BG_COLOR = Color.decode("#FAF3E1"); // Bej Arka Plan
    private static final Color PRIMARY_COLOR = Color.decode("#FF6D1F"); // Turuncu
    private static final Color ACCENT_COLOR = Color.BLACK; // Siyah
    private static final Color CARD_HOVER = Color.decode("#FFE8D6");

    // Kart Renk Paleti
    private static final Color[] CARD_PALETTE = {
            Color.decode("#55b78b"),
            Color.decode("#646b99"),
            Color.decode("#ffb31a"),
            Color.decode("#f94668"),
            Color.decode("#55b78b"),
            Color.decode("#646b99"),
            Color.decode("#ffb31a"),
            Color.decode("#f94668"),
            Color.decode("#55b78b")
    };

    private static final int[] POKER_VALUES = {1, 2, 3, 5, 8, 13, 20, 40, 100};
    private int selectedValue = -1;

    //  KİLİT MEKANİZMASI
    private boolean hasVoted = false;
    private boolean isTaskActive = false;

    private List<CardButton> cardButtons = new ArrayList<>();

    public GameScreen(Socket socket, String username, boolean isOwner) {
        this.socket = socket;
        this.username = username;
        this.isOwner = isOwner;

        setTitle("PLANNING POKER  - " + username);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(950, 800);
        setLocationRelativeTo(null);

        setupIO();
        initializeUI();
        startMessageListener();

        setVisible(true);
    }

    private void setupIO() {
        try {
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Connection Error!");
            System.exit(0);
        }
    }

    private void sendMessage(String msg) {
        if (out != null) out.println(msg);
    }

    private void initializeUI() {
        mainContainer = new JPanel(new BorderLayout());
        mainContainer.setBackground(BG_COLOR);
        add(mainContainer);

        // 1. HEADER (ÜST KISIM)
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBackground(BG_COLOR);
        headerPanel.setBorder(new EmptyBorder(30, 20, 10, 20));

        // 1. BAŞLIK
        headerTitle = new JLabel("PLANNING POKER ");
        headerTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        headerTitle.setForeground(ACCENT_COLOR);
        loadCustomFont(headerTitle, 70f);

        // 2. KULLANICI ROLÜ
        JLabel userInfo = new JLabel(username + " (" + (isOwner ? "Owner" : "Worker") + ")");
        userInfo.setAlignmentX(Component.CENTER_ALIGNMENT);
        userInfo.setFont(new Font("SansSerif", Font.BOLD, 16));
        userInfo.setForeground(Color.GRAY);

        // 3. WELCOME MESAJI (Turuncu)
        JLabel welcomeLabel = new JLabel("WELCOME " + username.toUpperCase() + "!");
        welcomeLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        welcomeLabel.setFont(new Font("SansSerif", Font.BOLD, 26));
        welcomeLabel.setForeground(PRIMARY_COLOR);
        welcomeLabel.setBorder(new EmptyBorder(15, 0, 15, 0));

        headerPanel.add(headerTitle);
        headerPanel.add(Box.createVerticalStrut(5));
        headerPanel.add(userInfo);
        headerPanel.add(welcomeLabel);

        mainContainer.add(headerPanel, BorderLayout.NORTH);

        //  2. CENTER (ORTA ALAN)
        centerPanel = new JPanel();
        centerPanel.setBackground(BG_COLOR);
        centerPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        if (isOwner) {
            setupOwnerPanel();
        } else {
            setupWorkerPanel();
        }

        JScrollPane centerScroll = new JScrollPane(centerPanel);
        centerScroll.setBorder(null);
        centerScroll.getViewport().setBackground(BG_COLOR);
        mainContainer.add(centerScroll, BorderLayout.CENTER);

        //  3. FOOTER (LOG ALANI)
        JPanel footerPanel = new JPanel(new BorderLayout());
        footerPanel.setBackground(BG_COLOR);
        footerPanel.setBorder(new EmptyBorder(10, 40, 30, 40));
        footerPanel.setPreferredSize(new Dimension(getWidth(), 250));

        JLabel logLabel = new JLabel("Game History:");
        logLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        logLabel.setForeground(ACCENT_COLOR);
        logLabel.setBorder(new EmptyBorder(0, 0, 5, 0));

        PixelCardPanel logWrapper = new PixelCardPanel();
        logWrapper.setBackground(Color.WHITE);

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        logArea.setOpaque(false);

        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setOpaque(false);
        logScroll.getViewport().setOpaque(false);
        logScroll.setBorder(null);

        logWrapper.add(logScroll, BorderLayout.CENTER);

        footerPanel.add(logLabel, BorderLayout.NORTH);
        footerPanel.add(logWrapper, BorderLayout.CENTER);

        mainContainer.add(footerPanel, BorderLayout.SOUTH);
    }

    // WORKER PANELİ
    private void setupWorkerPanel() {
        centerPanel.removeAll();
        centerPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 20));

        cardButtons.clear();
        int colorIndex = 0;

        for (int value : POKER_VALUES) {
            Color cardColor = CARD_PALETTE[colorIndex % CARD_PALETTE.length];
            CardButton card = new CardButton(value, cardColor);

            card.addActionListener(e -> {
                if (!isTaskActive) {
                    appendLog("⚠️ No task active yet! Please wait.");
                    return;
                }
                if (!hasVoted) {
                    selectedValue = value;
                    hasVoted = true;
                    sendMessage("VOTE:" + value);
                    updateCardVisuals();
                    appendLog("Your selection has been sent: " + value);
                }
            });
            cardButtons.add(card);
            centerPanel.add(card);
            colorIndex++;
        }
        centerPanel.revalidate();
        centerPanel.repaint();
    }

    private void updateCardVisuals() {
        for (CardButton card : cardButtons) {
            card.setSelectedState(card.getValue() == selectedValue);
        }
    }

    //  OWNER PANELİ
    private void setupOwnerPanel() {
        centerPanel.removeAll();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));

        PixelCardPanel controlBox = new PixelCardPanel();
        controlBox.setMaximumSize(new Dimension(600, 250));
        controlBox.setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel lbl = new JLabel("Define New Task");
        lbl.setFont(new Font("SansSerif", Font.BOLD, 18));
        lbl.setHorizontalAlignment(SwingConstants.CENTER);

        JTextField taskField = new JTextField();
        taskField.setFont(new Font("SansSerif", Font.PLAIN, 18));
        taskField.setPreferredSize(new Dimension(300, 50));
        taskField.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(10, Color.GRAY, 2),
                new EmptyBorder(5, 10, 5, 10)
        ));

        JPanel buttonGroup = new JPanel(new GridLayout(1, 2, 20, 0));
        buttonGroup.setOpaque(false);

        JButton btnSend = createStyledButton("START", ACCENT_COLOR);
        JButton btnReset = createStyledButton("RESET", PRIMARY_COLOR);

        buttonGroup.add(btnSend);
        buttonGroup.add(btnReset);

        btnSend.addActionListener(e -> {
            String task = taskField.getText().trim();
            if (!task.isEmpty()) {
                sendMessage("TASK:" + task);
                appendLog("New task defined: " + task);
                taskField.setText("");
            }
        });

        btnReset.addActionListener(e -> {
            sendMessage("RESET");
            appendLog("Voting has been reset.");
        });

        gbc.gridx = 0; gbc.gridy = 0;
        controlBox.add(lbl, gbc);
        gbc.gridy = 1;
        controlBox.add(taskField, gbc);
        gbc.gridy = 2;
        controlBox.add(Box.createVerticalStrut(10), gbc);
        gbc.gridy = 3;
        controlBox.add(buttonGroup, gbc);

        centerPanel.add(Box.createVerticalGlue());
        centerPanel.add(controlBox);
        centerPanel.add(Box.createVerticalGlue());

        centerPanel.revalidate();
        centerPanel.repaint();
    }

    private void loadCustomFont(JLabel label, float size) {
        try {
            InputStream is = getClass().getResourceAsStream("/fonts/Jersey15-Regular.ttf");
            if (is != null) {
                Font font = Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(size);
                GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
                label.setFont(font);
            } else {
                label.setFont(new Font("SansSerif", Font.BOLD, (int)size));
            }
        } catch (Exception e) {
            label.setFont(new Font("SansSerif", Font.BOLD, (int)size));
        }
    }

    private JButton createStyledButton(String text, Color color) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(color);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
                super.paintComponent(g);
            }
        };
        btn.setFocusPainted(false);
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("SansSerif", Font.BOLD, 18));
        btn.setPreferredSize(new Dimension(150, 50));
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void appendLog(String msg) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(msg + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private void startMessageListener() {
        new Thread(() -> {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    final String msg = line;
                    SwingUtilities.invokeLater(() -> processMessage(msg));
                }
            } catch (IOException e) {  }
        }).start();
    }

    private void processMessage(String msg) {
        if (msg.startsWith("[TASK]")) {
            if (!isOwner) {
                isTaskActive = true;
                selectedValue = -1;
                hasVoted = false;
                updateCardVisuals();
            }
        } else if (msg.contains("Oylar sıfırlandı")) {
            if (!isOwner) {
                isTaskActive = true;
                selectedValue = -1;
                hasVoted = false;
                updateCardVisuals();
            }
        } else if (msg.startsWith("RESULT:")) {
            if (!isOwner) {
                isTaskActive = false;
                updateCardVisuals();
            }
            new ResultDialog(this, msg).setVisible(true);
        }
        logArea.append(">> " + msg + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }


    // INNER CLASSES


    // 1. KART GÖRÜNÜMLÜ BUTON
    class CardButton extends JButton {
        private int value;
        private Color myColor;
        private boolean isSelected = false;
        private boolean isHovered = false;

        public CardButton(int value, Color color) {
            this.value = value;
            this.myColor = color;
            setText(String.valueOf(value));
            setFont(new Font("SansSerif", Font.BOLD, 48));
            setPreferredSize(new Dimension(140, 200));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));

            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) {
                    if (isTaskActive && !hasVoted) {
                        isHovered = true;
                        repaint();
                    }
                }
                public void mouseExited(MouseEvent e) {
                    isHovered = false;
                    repaint();
                }
            });
        }
        public int getValue() { return value; }
        public void setSelectedState(boolean sel) { this.isSelected = sel; repaint(); }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            int w = getWidth(); int h = getHeight();
            int pixelSize = 4; int arcSize = 30;

            g2.setColor(Color.BLACK);
            g2.fillRoundRect(pixelSize, pixelSize, w - pixelSize, h - pixelSize, arcSize, arcSize);

            if (!isTaskActive) {
                g2.setColor(Color.LIGHT_GRAY);
            } else if (isSelected) {
                g2.setColor(PRIMARY_COLOR);
            } else if (hasVoted) {
                g2.setColor(new Color(220, 220, 220));
            } else if (isHovered) {
                g2.setColor(myColor.brighter());
            } else {
                g2.setColor(myColor);
            }

            g2.fillRoundRect(0, 0, w - pixelSize, h - pixelSize, arcSize, arcSize);
            g2.setStroke(new BasicStroke(pixelSize));
            g2.setColor(Color.BLACK);
            g2.drawRoundRect(pixelSize/2, pixelSize/2, w - pixelSize*2 + pixelSize/2, h - pixelSize*2 + pixelSize/2, arcSize, arcSize);

            if (!isTaskActive) setForeground(Color.GRAY);
            else setForeground(isSelected ? Color.WHITE : Color.BLACK);

            super.paintComponent(g);
        }
    }

    // 2. PIXEL PANEL
    class PixelCardPanel extends JPanel {
        public PixelCardPanel() {
            setOpaque(false);
            setLayout(new BorderLayout());
            setBorder(new EmptyBorder(15, 20, 15, 20));
        }
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            int w = getWidth(); int h = getHeight();
            int pixelSize = 4; int arcSize = 30;

            g2.setColor(Color.BLACK);
            g2.fillRoundRect(pixelSize, pixelSize, w - pixelSize, h - pixelSize, arcSize, arcSize);
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(0, 0, w - pixelSize, h - pixelSize, arcSize, arcSize);

            g2.setStroke(new BasicStroke(pixelSize));
            g2.setColor(Color.BLACK);
            g2.drawRoundRect(pixelSize/2, pixelSize/2, w - pixelSize*2 + pixelSize/2, h - pixelSize*2 + pixelSize/2, arcSize, arcSize);
            super.paintComponent(g);
        }
    }

    // 3. YUVARLAK KENARLIK (Owner Input İçin)
    class RoundedBorder extends javax.swing.border.AbstractBorder {
        private int radius;
        private Color color;
        private int thickness;
        RoundedBorder(int radius, Color color, int thickness) {
            this.radius = radius; this.color = color; this.thickness = thickness;
        }
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(thickness));
            g2.drawRoundRect(x + thickness / 2, y + thickness / 2, w - thickness, h - thickness, radius, radius);
        }
        public Insets getBorderInsets(Component c) { return new Insets(10, 10, 10, 10); }
    }
}