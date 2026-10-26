package com.madnesscreator.mavenproject1;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Properties;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

public class AdminFrame extends JFrame {

    private JTextField txtBranchName;
    private JTextField txtKioskIp;
    private JTextField txtPricelistUrl;
    private JTextField txtRegistrationUrl;
    private JTextField txtClientTitle;
    private JLabel lblSelectedImage;
    private JLabel lblSelectedIcon;
    private File selectedImageFile = null;
    private File selectedIconFile = null;
    private JTable branchTable;
    private DefaultTableModel tableModel;
    private BufferedImage adminBackgroundImage;

    // Pointed directly to your active Render cloud deployment
    private static final String CLOUD_RELAY_URL = "https://mavenproject1-bzdx.onrender.com";

    private final Color PANEL_BG_GLASS = new Color(18, 22, 30, 235);
    private final Color ACCENT_GOLD = new Color(241, 196, 15);
    private final Color ACCENT_BLUE = new Color(52, 152, 219);
    private final Color TEXT_WHITE = new Color(245, 247, 250);
    private final Color ONLINE_GREEN = new Color(46, 204, 113);
    private final Color OFFLINE_RED = new Color(231, 76, 60);

    public AdminFrame() {
        String customTitle = getSavedSetting("admin_title", "A&F Computer Store - Admin Control Center");
        setTitle(customTitle);
        
        setSize(940, 720);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        loadCustomWindowIcon();
        loadAdminBackground();
        initComponents();
        startCloudSyncHeartbeat();     
    }

    private void loadCustomWindowIcon() {
        try {
            File iconFile = new File("kiosk_config/logo.png");
            if (iconFile.exists()) {
                BufferedImage originalIcon = ImageIO.read(iconFile);
                java.awt.Image scaledIcon = originalIcon.getScaledInstance(32, 32, java.awt.Image.SCALE_SMOOTH);
                setIconImage(scaledIcon);
            }
        } catch (Exception e) {
            System.err.println("Failed to load window icon: " + e.getMessage());
        }
    }

    private void loadAdminBackground() {
        new Thread(() -> {
            try {
                File bgFile = new File("kiosk_config/AdminBG.png");
                if (bgFile.exists()) {
                    adminBackgroundImage = ImageIO.read(bgFile);
                } else {
                    adminBackgroundImage = null;
                }
                SwingUtilities.invokeLater(() -> repaint());
            } catch (Exception e) {
                System.err.println("Admin background image failed to load: " + e.getMessage());
            }
        }).start();
    }

    private void initComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout(15, 15)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                if (adminBackgroundImage != null) {
                    g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    g2d.drawImage(adminBackgroundImage, 0, 0, getWidth(), getHeight(), this);
                } else {
                    g2d.setColor(new Color(12, 16, 24));
                    g2d.fillRect(0, 0, getWidth(), getHeight());
                }
                g2d.dispose();
                super.paintComponent(g); // Safe rendering pass for child components
            }
        };
        mainPanel.setOpaque(false);
        mainPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(15, 20, 15, 20));

        // Top Header Panel
        JPanel topHeaderPanel = new JPanel(new BorderLayout());
        topHeaderPanel.setOpaque(false);

        JLabel lblTitle = new JLabel("A&F KIOSK MANAGEMENT CENTER");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitle.setForeground(ACCENT_GOLD);
        topHeaderPanel.add(lblTitle, BorderLayout.WEST);

        JPanel rightHeaderButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightHeaderButtons.setOpaque(false);

        JButton btnRefresh = createSleekButton("🔄 REFRESH", ACCENT_BLUE);
        btnRefresh.setPreferredSize(new Dimension(115, 34));
        btnRefresh.addActionListener(e -> triggerManualRefresh());
        rightHeaderButtons.add(btnRefresh);

        JButton btnSettingsGear = createSleekButton("⚙ SETTINGS", ACCENT_GOLD);
        btnSettingsGear.setPreferredSize(new Dimension(120, 34));
        btnSettingsGear.addActionListener(e -> openAdminSettingsMenu());
        rightHeaderButtons.add(btnSettingsGear);

        topHeaderPanel.add(rightHeaderButtons, BorderLayout.EAST);
        mainPanel.add(topHeaderPanel, BorderLayout.NORTH);

        // Center Glassmorphism Form Card
        JPanel centerCard = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(PANEL_BG_GLASS);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2d.setColor(new Color(241, 196, 15, 50));
                g2d.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 16, 16);
                g2d.dispose();
                super.paintComponent(g);
            }
        };
        centerCard.setOpaque(false);
        centerCard.setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 20, 12, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 8, 5, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Branch Identifier
        gbc.gridy = 0; gbc.gridx = 0;
        centerCard.add(createStyledLabel("Branch Name:"), gbc);
        txtBranchName = new JTextField();
        styleTextField(txtBranchName);
        gbc.gridx = 1;
        centerCard.add(txtBranchName, gbc);

        // Kiosk IP Address
        gbc.gridy = 1; gbc.gridx = 0;
        centerCard.add(createStyledLabel("Kiosk Local IP:"), gbc);
        txtKioskIp = new JTextField();
        styleTextField(txtKioskIp);
        gbc.gridx = 1;
        centerCard.add(txtKioskIp, gbc);

        // Pricelist URL
        gbc.gridy = 2; gbc.gridx = 0;
        centerCard.add(createStyledLabel("Pricelist URL:"), gbc);
        txtPricelistUrl = new JTextField("https://docs.google.com/spreadsheets/d/1AncL3i3fKC3IEvIXtPYmAplX1oee9CTm5I7yNmocsuM/edit?gid=1864218333#gid=1864218333");
        styleTextField(txtPricelistUrl);
        gbc.gridx = 1;
        centerCard.add(txtPricelistUrl, gbc);

        // Registration Form URL
        gbc.gridy = 3; gbc.gridx = 0;
        centerCard.add(createStyledLabel("Registration URL:"), gbc);
        txtRegistrationUrl = new JTextField("https://docs.google.com/forms/d/e/1FAIpQLScf4maR6JYgOIuceat-F43K6Exhn8-wqwvFBPhNo4tU7chZDA/viewform");
        styleTextField(txtRegistrationUrl);
        gbc.gridx = 1;
        centerCard.add(txtRegistrationUrl, gbc);

        // Client Window Title
        gbc.gridy = 4; gbc.gridx = 0;
        centerCard.add(createStyledLabel("Client Window Title:"), gbc);
        txtClientTitle = new JTextField("A&F Computer Store - Kiosk Terminal");
        styleTextField(txtClientTitle);
        gbc.gridx = 1;
        centerCard.add(txtClientTitle, gbc);

        // Background Image Selection (for Kiosk Banner)
        gbc.gridy = 5; gbc.gridx = 0;
        centerCard.add(createStyledLabel("Kiosk Banner:"), gbc);
        JPanel imgPanel = new JPanel(new BorderLayout(10, 0));
        imgPanel.setOpaque(false);
        JButton btnBrowse = createSleekButton("Browse Banner", ACCENT_BLUE);
        btnBrowse.setPreferredSize(new Dimension(130, 28));
        btnBrowse.addActionListener(e -> chooseBackgroundImage());
        lblSelectedImage = new JLabel("No file selected");
        lblSelectedImage.setForeground(new Color(180, 190, 205));
        lblSelectedImage.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        imgPanel.add(btnBrowse, BorderLayout.WEST);
        imgPanel.add(lblSelectedImage, BorderLayout.CENTER);
        gbc.gridx = 1;
        centerCard.add(imgPanel, gbc);

        // Client Window Icon Selection
        gbc.gridy = 6; gbc.gridx = 0;
        centerCard.add(createStyledLabel("Client Window Icon:"), gbc);
        JPanel iconPanel = new JPanel(new BorderLayout(10, 0));
        iconPanel.setOpaque(false);
        JButton btnBrowseIcon = createSleekButton("Browse Icon", ACCENT_BLUE);
        btnBrowseIcon.setPreferredSize(new Dimension(130, 28));
        btnBrowseIcon.addActionListener(e -> chooseClientIconImage());
        lblSelectedIcon = new JLabel("No file selected");
        lblSelectedIcon.setForeground(new Color(180, 190, 205));
        lblSelectedIcon.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        iconPanel.add(btnBrowseIcon, BorderLayout.WEST);
        iconPanel.add(lblSelectedIcon, BorderLayout.CENTER);
        gbc.gridx = 1;
        centerCard.add(iconPanel, gbc);

        // Push Updates Button
        JButton btnPushAll = createSleekButton("PUSH UPDATES TO CLIENT", ACCENT_GOLD);
        btnPushAll.setPreferredSize(new Dimension(380, 42));
        btnPushAll.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnPushAll.addActionListener(e -> pushToSelectedBranch());
        gbc.gridy = 7; gbc.gridx = 0; gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(10, 8, 4, 8);
        centerCard.add(btnPushAll, gbc);

        mainPanel.add(centerCard, BorderLayout.CENTER);

        // Bottom Table: Branch Monitoring List
        String[] columns = {"Branch Name", "IP Address", "Status"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        branchTable = new JTable(tableModel);
        branchTable.setRowHeight(26);
        branchTable.setBackground(new Color(15, 20, 28, 230));
        branchTable.setForeground(TEXT_WHITE);
        branchTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        branchTable.setSelectionBackground(new Color(241, 196, 15, 50));
        branchTable.setSelectionForeground(ACCENT_GOLD);
        
        branchTable.getTableHeader().setBackground(new Color(22, 28, 38));
        branchTable.getTableHeader().setForeground(ACCENT_GOLD);
        branchTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        
        branchTable.getSelectionModel().addListSelectionListener(e -> {
            int selectedRow = branchTable.getSelectedRow();
            if (selectedRow >= 0) {
                txtBranchName.setText((String) tableModel.getValueAt(selectedRow, 0));
                txtKioskIp.setText((String) tableModel.getValueAt(selectedRow, 1));
            }
        });

        branchTable.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                java.awt.Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if ("ONLINE".equals(value)) {
                    c.setForeground(ONLINE_GREEN);
                    setFont(new Font("Segoe UI", Font.BOLD, 12));
                } else {
                    c.setForeground(OFFLINE_RED);
                    setFont(new Font("Segoe UI", Font.BOLD, 12));
                }
                return c;
            }
        });

        JScrollPane scrollPane = new JScrollPane(branchTable);
        scrollPane.setPreferredSize(new Dimension(880, 140));
        scrollPane.getViewport().setBackground(new Color(15, 20, 28, 200));
        scrollPane.setBorder(javax.swing.BorderFactory.createLineBorder(new Color(241, 196, 15, 80), 1));
        mainPanel.add(scrollPane, BorderLayout.SOUTH);

        add(mainPanel);
    }

    private void openAdminSettingsMenu() {
        String[] options = {"Change Admin Background Image", "Change Admin Window Icon", "Change Admin Window Title", "Cancel"};
        int choice = JOptionPane.showOptionDialog(
            this,
            "Admin Control Center Settings",
            "Settings",
            JOptionPane.DEFAULT_OPTION,
            JOptionPane.INFORMATION_MESSAGE,
            null,
            options,
            options[0]
        );

        if (choice == 0) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileFilter(new FileNameExtensionFilter("PNG Images", "png"));
            int result = fileChooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File sourceFile = fileChooser.getSelectedFile();
                try {
                    File dir = new File("kiosk_config");
                    if (!dir.exists()) dir.mkdirs();
                    File destFile = new File(dir, "AdminBG.png");
                    Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    loadAdminBackground(); 
                    JOptionPane.showMessageDialog(this, "Admin Background updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Failed to update background: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        } else if (choice == 1) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileFilter(new FileNameExtensionFilter("Image Files", "png", "jpg", "ico"));
            int result = fileChooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File sourceFile = fileChooser.getSelectedFile();
                try {
                    File dir = new File("kiosk_config");
                    if (!dir.exists()) dir.mkdirs();
                    File destFile = new File(dir, "logo.png");
                    Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    loadCustomWindowIcon();
                    JOptionPane.showMessageDialog(this, "Admin Window Icon updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Failed to update icon: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        } else if (choice == 2) {
            String newTitle = JOptionPane.showInputDialog(this, "Enter new Admin Window Title:", getTitle());
            if (newTitle != null && !newTitle.trim().isEmpty()) {
                setTitle(newTitle.trim());
                saveSetting("admin_title", newTitle.trim());
                JOptionPane.showMessageDialog(this, "Admin Window Title updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    private String getSavedSetting(String key, String defaultValue) {
        File file = new File("kiosk_config/settings.properties");
        if (file.exists()) {
            try (FileInputStream in = new FileInputStream(file)) {
                Properties props = new Properties();
                props.load(in);
                return props.getProperty(key, defaultValue);
            } catch (Exception e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private void saveSetting(String key, String value) {
        try {
            File dir = new File("kiosk_config");
            if (!dir.exists()) dir.mkdirs();
            File file = new File(dir, "settings.properties");
            Properties props = new Properties();
            if (file.exists()) {
                try (FileInputStream in = new FileInputStream(file)) {
                    props.load(in);
                }
            }
            props.setProperty(key, value);
            try (FileOutputStream out = new FileOutputStream(file)) {
                props.store(out, "Admin Settings Updated");
            }
        } catch (Exception e) {
            System.err.println("Failed to save setting: " + e.getMessage());
        }
    }

    private JLabel createStyledLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 12));
        label.setForeground(TEXT_WHITE);
        return label;
    }

    private void styleTextField(JTextField field) {
        field.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        field.setBackground(new Color(22, 28, 38, 230));
        field.setForeground(ACCENT_GOLD);
        field.setCaretColor(ACCENT_GOLD);
        field.setBorder(javax.swing.BorderFactory.createCompoundBorder(
            javax.swing.BorderFactory.createLineBorder(new Color(241, 196, 15, 100), 1),
            javax.swing.BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
        field.setPreferredSize(new Dimension(380, 28));
    }

    private JButton createSleekButton(String text, Color accentColor) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                if (getModel().isPressed()) {
                    g2d.setColor(accentColor.darker());
                } else if (getModel().isRollover()) {
                    g2d.setColor(new Color(30, 38, 50));
                } else {
                    g2d.setColor(new Color(20, 26, 36, 220));
                }
                
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2d.setColor(accentColor);
                g2d.setStroke(new java.awt.BasicStroke(1.5f));
                g2d.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 10, 10);
                g2d.dispose();
                super.paintComponent(g);
            }
        };
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setForeground(TEXT_WHITE);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        return button;
    }

    private void chooseBackgroundImage() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("PNG Images", "png"));
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            selectedImageFile = fileChooser.getSelectedFile();
            lblSelectedImage.setText(selectedImageFile.getName());
            lblSelectedImage.setForeground(ACCENT_GOLD);
        }
    }

    private void chooseClientIconImage() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("Image Files", "png", "jpg"));
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            selectedIconFile = fileChooser.getSelectedFile();
            lblSelectedIcon.setText(selectedIconFile.getName());
            lblSelectedIcon.setForeground(ACCENT_GOLD);
        }
    }

    private void upsertBranchInTable(String branchName, String ip, String status) {
        if ("localhost".equalsIgnoreCase(ip)) {
            ip = "127.0.0.1";
        }

        // Check if this IP address already exists in the table. Update in place to prevent duplicates.
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            String existingIp = (String) tableModel.getValueAt(i, 1);

            if (existingIp.equals(ip)) {
                tableModel.setValueAt(branchName, i, 0);
                tableModel.setValueAt(ip, i, 1);
                tableModel.setValueAt(status, i, 2);
                return;
            }
        }

        tableModel.addRow(new Object[]{branchName, ip, status});
    }

    private void triggerManualRefresh() {
        new Thread(() -> {
            try {
                URL url = new URL(CLOUD_RELAY_URL + "/api/branches");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(3000);

                if (conn.getResponseCode() == 200) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }

                        String json = response.toString();
                        if (json.startsWith("[") && json.endsWith("]")) {
                            String content = json.substring(1, json.length() - 1).trim();
                            if (!content.isEmpty()) {
                                String[] objects = content.split("\\},\\{");
                                for (String obj : objects) {
                                    obj = obj.replace("[{", "").replace("}]", "").replace("{", "").replace("}", "");
                                    String branchName = "Unknown";
                                    String clientIp = "127.0.0.1";

                                    for (String pair : obj.split(",")) {
                                        String[] kv = pair.split(":");
                                        if (kv.length == 2) {
                                            String key = kv[0].replace("\"", "").trim();
                                            String val = kv[1].replace("\"", "").trim();
                                            if ("branchName".equals(key)) branchName = val;
                                            if ("clientIp".equals(key)) clientIp = val;
                                        }
                                    }

                                    final String bName = branchName;
                                    final String cIp = clientIp;
                                    SwingUtilities.invokeLater(() -> upsertBranchInTable(bName, cIp, "ONLINE"));
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // Fallback to offline if relay server cannot be reached
                for (int i = 0; i < tableModel.getRowCount(); i++) {
                    final int rowIndex = i;
                    SwingUtilities.invokeLater(() -> tableModel.setValueAt("OFFLINE", rowIndex, 2));
                }
            }
        }).start();
    }

    private void startCloudSyncHeartbeat() {
        Timer timer = new Timer(5000, e -> triggerManualRefresh());
        timer.start();
    }

    private void pushToSelectedBranch() {
        String ip = txtKioskIp.getText().trim();
        String branchName = txtBranchName.getText().trim();
        String priceUrl = txtPricelistUrl.getText().trim();
        String regUrl = txtRegistrationUrl.getText().trim();
        String clientTitle = txtClientTitle.getText().trim();

        if (ip.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select or enter a valid Kiosk IP address.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        new Thread(() -> {
            boolean settingsSuccess = KioskAdminController.sendSettingsToKiosk(ip, branchName, priceUrl, regUrl, clientTitle);
            boolean bgSuccess = true;
            if (selectedImageFile != null) {
                bgSuccess = KioskAdminController.sendBackgroundImageToKiosk(ip, selectedImageFile);
            }
            boolean iconSuccess = true;
            if (selectedIconFile != null) {
                iconSuccess = KioskAdminController.sendClientIconToKiosk(ip, selectedIconFile);
            }

            final boolean finalSuccess = settingsSuccess && bgSuccess && iconSuccess;
            SwingUtilities.invokeLater(() -> {
                if (finalSuccess) {
                    JOptionPane.showMessageDialog(this, "Successfully updated " + branchName + "!", "Success", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this, "Failed to reach kiosk at " + ip + ".", "Error", JOptionPane.ERROR_MESSAGE);
                }
            });
        }).start();
    }

    public static void main(String[] args) {
        try {
            javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new AdminFrame().setVisible(true));
    }
}