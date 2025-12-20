package planningpoker.client;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class ResultDialog extends JDialog {

    // Renk Teması
    private static final Color BG_COLOR = Color.decode("#FAF3E1"); // Oyun Arka Planı
    private static final Color PRIMARY_COLOR = Color.decode("#FF6D1F"); // Turuncu (Buton İçin)
    private static final Color BORDER_COLOR = Color.BLACK;         // Çerçeve Rengi
    private static final Color CARD_BG = Color.WHITE;              // Kart İçi
    private static final Color TEXT_COLOR = Color.BLACK;           // Yazı Rengi

    public ResultDialog(JFrame parent, String serverMsg) {
        super(parent, "Voting Results", true);
        setSize(650, 500);
        setLocationRelativeTo(parent);
        setResizable(false);

        // Veriyi Ayrıştır (Parsing)
        String taskName = extractValue(serverMsg, "TASK=\"", "\"");
        String minVal = extractValue(serverMsg, "MIN=", ",");
        String maxVal = extractValue(serverMsg, "MAX=", ",");
        String avgVal = extractValue(serverMsg, "AVG=", " ");
        String countVal = extractValue(serverMsg, "TOTAL VOTES=", ")");

        if (taskName.equals("?")) taskName = "General Voting";

        //  Ana Panel
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(BG_COLOR);
        mainPanel.setBorder(new EmptyBorder(30, 40, 30, 40));

        // 1. ÜST KISIM (HEADER)
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setBackground(BG_COLOR);
        headerPanel.setBorder(new EmptyBorder(0, 0, 30, 0));

        // Başlık
        JLabel titleLabel = new JLabel("VOTING RESULTS", SwingConstants.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 28));
        titleLabel.setForeground(TEXT_COLOR);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Alt Başlık (Görev Adı)
        JLabel taskLabel = new JLabel(taskName, SwingConstants.CENTER);
        taskLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        taskLabel.setForeground(Color.DARK_GRAY);
        taskLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        headerPanel.add(titleLabel);
        headerPanel.add(Box.createVerticalStrut(10));
        headerPanel.add(taskLabel);

        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // 2. SONUÇ KARTLARI
        JPanel gridPanel = new JPanel(new GridLayout(2, 2, 20, 20));
        gridPanel.setBackground(BG_COLOR);

        gridPanel.add(new PixelResultCard("Average", avgVal));
        gridPanel.add(new PixelResultCard("Total Votes", countVal));
        gridPanel.add(new PixelResultCard("Minimum", minVal));
        gridPanel.add(new PixelResultCard("Maximum", maxVal));

        mainPanel.add(gridPanel, BorderLayout.CENTER);

        // 3. KAPAT BUTONU
        JPanel footerPanel = new JPanel();
        footerPanel.setBackground(BG_COLOR);
        footerPanel.setBorder(new EmptyBorder(20, 0, 0, 0));

        //  OVAL TURUNCU BUTON
        JButton closeBtn = new JButton("Close") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(PRIMARY_COLOR);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 30, 30);
                super.paintComponent(g);
            }
        };

        closeBtn.setFont(new Font("SansSerif", Font.BOLD, 16));
        closeBtn.setForeground(Color.WHITE);
        closeBtn.setPreferredSize(new Dimension(150, 45));

        closeBtn.setFocusPainted(false);
        closeBtn.setContentAreaFilled(false);
        closeBtn.setBorderPainted(false);
        closeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        closeBtn.addActionListener(e -> dispose());
        footerPanel.add(closeBtn);

        mainPanel.add(footerPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    // INNER CLASS: PİKSEL ÇERÇEVELİ KART
    class PixelResultCard extends JPanel {

        public PixelResultCard(String title, String value) {
            setOpaque(false);
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBorder(new EmptyBorder(10, 10, 10, 10));

            JLabel lblTitle = new JLabel(title);
            lblTitle.setFont(new Font("SansSerif", Font.PLAIN, 16));
            lblTitle.setForeground(Color.DARK_GRAY);
            lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

            JLabel lblValue = new JLabel(value);
            lblValue.setFont(new Font("SansSerif", Font.BOLD, 42));
            lblValue.setForeground(TEXT_COLOR);
            lblValue.setAlignmentX(Component.CENTER_ALIGNMENT);

            add(Box.createVerticalGlue());
            add(lblTitle);
            add(Box.createVerticalStrut(5));
            add(lblValue);
            add(Box.createVerticalGlue());
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

            int w = getWidth();
            int h = getHeight();
            int pixelSize = 4;
            int arcSize = 20;

            g2.setColor(CARD_BG);
            g2.fillRoundRect(0, 0, w - pixelSize, h - pixelSize, arcSize, arcSize);

            g2.setStroke(new BasicStroke(pixelSize));
            g2.setColor(BORDER_COLOR);
            g2.drawRoundRect(pixelSize/2, pixelSize/2, w - pixelSize - pixelSize/2, h - pixelSize - pixelSize/2, arcSize, arcSize);
        }
    }

    private String extractValue(String source, String startKey, String endKey) {
        try {
            int start = source.indexOf(startKey);
            if (start == -1) return "?";
            start += startKey.length();

            int end = source.indexOf(endKey, start);
            if (end == -1) {
                String temp = source.substring(start);
                if(temp.contains("(")) temp = temp.substring(0, temp.indexOf("("));
                return temp.trim();
            }
            return source.substring(start, end).trim();
        } catch (Exception e) {
            return "-";
        }
    }
}