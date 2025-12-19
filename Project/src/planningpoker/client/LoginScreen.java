package planningpoker.client;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.Socket;

public class LoginScreen extends JFrame {

    private JTextField nameField;
    private JPasswordField passwordField;
    private JRadioButton workerRadio, ownerRadio;
    private JPanel passwordContainer;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    // Renk Teması
    private static final Color PRIMARY_COLOR = Color.decode("#FF6D1F");

    public LoginScreen() {
        setTitle("UniPoker Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(700, 600);
        setLocationRelativeTo(null);

        // Arka Plan
        JPanel background = new JPanel();
        background.setBackground(Color.decode("#FAF3E1"));
        background.setLayout(new GridBagLayout());
        add(background);

        // İçerik Paneli
        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(new EmptyBorder(40, 40, 40, 40));

        // === 1. İKON (ImageIO ile JAR Uyumlu) ===
        JLabel iconLabel;
        try {
            InputStream imgStream = getClass().getResourceAsStream("/card.png");
            if (imgStream != null) {
                BufferedImage img = ImageIO.read(imgStream);
                Image scaledImg = img.getScaledInstance(120, 120, Image.SCALE_SMOOTH);
                iconLabel = new JLabel(new ImageIcon(scaledImg));
            } else {
                iconLabel = new JLabel("♠️", SwingConstants.CENTER);
                iconLabel.setFont(new Font("SansSerif", Font.BOLD, 100));
                iconLabel.setForeground(PRIMARY_COLOR);
            }
        } catch (Exception e) {
            iconLabel = new JLabel("♠️", SwingConstants.CENTER);
            iconLabel.setFont(new Font("SansSerif", Font.BOLD, 100));
            iconLabel.setForeground(PRIMARY_COLOR);
        }
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // === 2. BAŞLIK ===
        JLabel title = new JLabel("UNIPOKER", SwingConstants.CENTER);
        title.setForeground(new Color(20, 20, 20));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Font Yükleme (JAR Uyumlu)
        try {
            InputStream fontStream = getClass().getResourceAsStream("/fonts/Jersey15-Regular.ttf");
            if (fontStream != null) {
                Font jersey = Font.createFont(Font.TRUETYPE_FONT, fontStream).deriveFont(85f);
                GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                ge.registerFont(jersey);
                title.setFont(jersey);
            } else {
                title.setFont(new Font("SansSerif", Font.BOLD, 60));
            }
        } catch (Exception e) {
            title.setFont(new Font("SansSerif", Font.BOLD, 60));
        }

        // === 3. GİRİŞ ALANLARI ===

        // Kullanıcı Adı
        JLabel nameLabel = new JLabel("Username:");
        nameLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        nameField = new JTextField();
        nameField.setPreferredSize(new Dimension(320, 42));
        nameField.setMaximumSize(new Dimension(320, 42));
        nameField.setFont(new Font("SansSerif", Font.PLAIN, 18));
        nameField.setBorder(new RoundedBorder(20, PRIMARY_COLOR, 2));
        nameField.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Rol Seçimi
        JPanel rolePanel = new JPanel();
        rolePanel.setOpaque(false);
        rolePanel.setLayout(new FlowLayout(FlowLayout.CENTER, 20, 10));
        rolePanel.setMaximumSize(new Dimension(320, 50));

        workerRadio = new JRadioButton("Worker");
        ownerRadio = new JRadioButton("Owner");
        workerRadio.setFont(new Font("SansSerif", Font.BOLD, 16));
        ownerRadio.setFont(new Font("SansSerif", Font.BOLD, 16));
        workerRadio.setOpaque(false);
        ownerRadio.setOpaque(false);
        workerRadio.setSelected(true);

        ButtonGroup bg = new ButtonGroup();
        bg.add(workerRadio);
        bg.add(ownerRadio);
        rolePanel.add(workerRadio);
        rolePanel.add(ownerRadio);
        rolePanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Şifre Alanı
        JLabel passwordLabel = new JLabel("Owner Password:");
        passwordLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        passwordLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        passwordField = new JPasswordField();
        passwordField.setPreferredSize(new Dimension(320, 42));
        passwordField.setMaximumSize(new Dimension(320, 42));
        passwordField.setFont(new Font("SansSerif", Font.PLAIN, 18));
        passwordField.setBorder(new RoundedBorder(20, new Color(200, 200, 200), 1));
        passwordField.setAlignmentX(Component.CENTER_ALIGNMENT);

        passwordContainer = new JPanel();
        passwordContainer.setOpaque(false);
        passwordContainer.setLayout(new BoxLayout(passwordContainer, BoxLayout.Y_AXIS));
        passwordContainer.setMaximumSize(new Dimension(350, 100));
        passwordContainer.add(passwordLabel);
        passwordContainer.add(Box.createVerticalStrut(5));
        passwordContainer.add(passwordField);
        passwordContainer.setVisible(false); // Başlangıçta gizli
        passwordContainer.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Radyo butonu dinleyicisi
        java.awt.event.ActionListener roleListener = e -> {
            passwordContainer.setVisible(ownerRadio.isSelected());
            revalidate();
            repaint();
        };
        ownerRadio.addActionListener(roleListener);
        workerRadio.addActionListener(roleListener);

        // === 4. CONNECT BUTONU ===
        JButton connectBtn = new JButton("Connect") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(PRIMARY_COLOR);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                super.paintComponent(g);
            }
        };
        connectBtn.setFocusPainted(false);
        connectBtn.setForeground(Color.WHITE);
        connectBtn.setFont(new Font("SansSerif", Font.BOLD, 18));
        connectBtn.setPreferredSize(new Dimension(200, 45));
        connectBtn.setMaximumSize(new Dimension(200, 45));
        connectBtn.setContentAreaFilled(false);
        connectBtn.setBorderPainted(false);
        connectBtn.setAlignmentX(Component.CENTER_ALIGNMENT);

        connectBtn.addActionListener(e -> performLogin());

        // === 5. YERLEŞİM (Burası Çok Önemli!) ===
        content.add(iconLabel);
        content.add(Box.createVerticalStrut(10));
        content.add(title);
        content.add(Box.createVerticalStrut(25));

        content.add(nameLabel);
        content.add(Box.createVerticalStrut(5));
        content.add(nameField);

        content.add(Box.createVerticalStrut(20));
        content.add(rolePanel);

        content.add(Box.createVerticalStrut(20));
        content.add(passwordContainer);

        content.add(Box.createVerticalStrut(30));
        content.add(connectBtn);

        background.add(content);
        setVisible(true);
    }

    // === SUNUCUYA BAĞLANMA ===
    private void performLogin() {
        String username = nameField.getText().trim();
        if (username.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a username!");
            return;
        }

        // Kullanıcının seçtiği rol (sunucuya gidecek istek)
        String desiredRole = workerRadio.isSelected() ? "WORKER" : "OWNER";
        String secret = new String(passwordField.getPassword());

        if (desiredRole.equals("OWNER") && secret.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Owner password is required!");
            return;
        }

        new Thread(() -> {
            Socket tempSocket = null;
            try {
                // Her denemede yeni bir soket açılır
                tempSocket = new Socket("localhost", 5007);
                PrintWriter tempOut = new PrintWriter(tempSocket.getOutputStream(), true);
                BufferedReader tempIn = new BufferedReader(new InputStreamReader(tempSocket.getInputStream()));

                // 1) Sunucudan "Kullanıcı adını gir:" bekle
                String lineFromServer = tempIn.readLine();
                tempOut.println(username);

                // 2) Sunucudan "Rolünü yaz (OWNER veya WORKER):" bekle
                lineFromServer = tempIn.readLine();
                tempOut.println(desiredRole);

                // 3) Eğer OWNER istendiyse şifre prompt'unu bekle, şifreyi yolla
                if ("OWNER".equals(desiredRole)) {
                    lineFromServer = tempIn.readLine();
                    tempOut.println(secret);
                }

                // 4) Sunucunun durum cevabını al (başarılı / başarısız)
                String response = tempIn.readLine();

                if (response != null &&
                        response.contains("Patron (OWNER) olarak giriş yaptın.")) {
                    // GERÇEKTEN OWNER OLARAK ALINDI
                    Socket finalSocket = tempSocket;
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(this, "Successfully logged in as OWNER!");
                        this.dispose();
                        new GameScreen(finalSocket, username, true);
                    });

                } else if (response != null &&
                        response.contains("WORKER olarak giriş yaptın.")) {
                    // GERÇEKTEN WORKER OLARAK ALINDI
                    Socket finalSocket = tempSocket;
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(this, "Successfully logged in as WORKER!");
                        this.dispose();
                        new GameScreen(finalSocket, username, false);
                    });

                } else {
                    // BAŞARISIZ GİRİŞ
                    String finalResponse = (response != null) ? response : "Connection lost.";
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                            this,
                            "Login failed: " + finalResponse,
                            "Login Error",
                            JOptionPane.ERROR_MESSAGE
                    ));

                    try {
                        tempSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            } catch (IOException ex) {
                Socket finalTempSocket = tempSocket;
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                        this,
                        "Could not connect to server! Is the server running on port 5005?",
                        "Connection Error",
                        JOptionPane.ERROR_MESSAGE
                ));

                if (finalTempSocket != null) {
                    try {
                        finalTempSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }


    // Yuvarlak Kenarlık Sınıfı
    class RoundedBorder extends javax.swing.border.AbstractBorder {
        private int radius;
        private Color borderColor;
        private int strokeWidth;

        RoundedBorder(int radius, Color color, int strokeWidth) {
            this.radius = radius;
            this.borderColor = color;
            this.strokeWidth = strokeWidth;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setStroke(new BasicStroke(strokeWidth));
            g2.setColor(borderColor);
            g2.drawRoundRect(x + strokeWidth / 2, y + strokeWidth / 2, width - strokeWidth, height - strokeWidth, radius, radius);
        }

        @Override
        public Insets getBorderInsets(Component c) {
            int padding = 12 + strokeWidth;
            return new Insets(padding, padding, padding, padding);
        }
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception e) {}
        new LoginScreen();
    }
}