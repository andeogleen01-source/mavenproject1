package com.madnesscreator.mavenproject1;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.*;

public class AdminFrame extends JFrame {

    private JTextField txtBranchName, txtKioskIp, txtPricelistUrl, txtRegistrationUrl, txtClientTitle;
    private JTextField txtPriceX, txtPriceY, txtRegX, txtRegY;
    private JTextField txtClientWidth, txtClientHeight; 

    private JLabel lblSelectedImage, lblSelectedIcon, lblPricelistBtnImage, lblRegistrationBtnImage;
    private File selectedImageFile, selectedIconFile, selectedPricelistBtnFile, selectedRegistrationBtnFile;

    private JPanel dynamicButtonsContainer;
    private JScrollPane customScroll;
    private final List<CustomButtonEntry> customButtonEntries = new ArrayList<>();

    private JTable branchTable;
    private DefaultTableModel tableModel;
    private BufferedImage adminBackgroundImage;

    private LayoutDesignerCanvas layoutCanvas;
    private boolean clientSelected = false;

    private static final String CLOUD_RELAY_URL = "https://mavenproject1-bzdx.onrender.com";

    private final Color PANEL_BG_GLASS = new Color(18, 22, 30, 245);
    private final Color ACCENT_GOLD = new Color(241, 196, 15);
    private final Color ACCENT_BLUE = new Color(52, 152, 219);
    private final Color TEXT_WHITE = new Color(245, 247, 250);
    private final Color ONLINE_GREEN = new Color(46, 204, 113);
    private final Color OFFLINE_RED = new Color(231, 76, 60);

    private static class CustomButtonEntry {
        JTextField nameField, urlField, xField, yField;
        JLabel imgLabel;
        File imageFile;
        int index;

        public CustomButtonEntry(int index, String name, String url, String x, String y) {
            this.index = index;
            this.nameField = new JTextField(name, 5); // Pinaikli para magkasya nang husto
            this.urlField = new JTextField(url, 7);   // Pinaikli para magkasya nang husto
            this.xField = new JTextField(x, 3);
            this.yField = new JTextField(y, 3);
            this.imgLabel = new JLabel("No img");
        }
    }

    public AdminFrame() {
        setTitle(getSavedSetting("admin_title", "A&F Computer Store - Admin Control Center"));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setResizable(true);

        loadCustomWindowIcon();
        loadAdminBackground();
        initComponents();
        startCloudSyncHeartbeat();     
    }

    private void loadCustomWindowIcon() {
        try {
            File f = new File("kiosk_config/logo.png");
            if (f.exists()) setIconImage(ImageIO.read(f).getScaledInstance(32, 32, Image.SCALE_SMOOTH));
        } catch (Exception ignored) {}
    }

    private void loadAdminBackground() {
        new Thread(() -> {
            try {
                File f = new File("kiosk_config/AdminBG.png");
                adminBackgroundImage = f.exists() ? ImageIO.read(f) : null;
                SwingUtilities.invokeLater(this::repaint);
            } catch (Exception ignored) {}
        }).start();
    }

    private void initComponents() {
        JPanel mainPanel = new JPanel(new BorderLayout(8, 8)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                if (adminBackgroundImage != null) {
                    g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    g2d.drawImage(adminBackgroundImage, 0, 0, getWidth(), getHeight(), this);
                } else {
                    g2d.setColor(new Color(12, 16, 24));
                    g2d.fillRect(0, 0, getWidth(), getHeight());
                }
                g2d.dispose();
            }
        };
        mainPanel.setBorder(new EmptyBorder(8, 12, 8, 12));

        JPanel topHeaderPanel = new JPanel(new BorderLayout());
        topHeaderPanel.setOpaque(false);
        JLabel lblTitle = new JLabel("A&F SCREEN MANAGEMENT CENTER - LAYOUT DESIGNER");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblTitle.setForeground(ACCENT_GOLD);
        topHeaderPanel.add(lblTitle, BorderLayout.WEST);

        JPanel rightHeaderButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        rightHeaderButtons.setOpaque(false);

        JButton btnRefresh = createSleekButton("🔄 REFRESH", ACCENT_BLUE, new Dimension(110, 26));
        btnRefresh.addActionListener(e -> triggerManualRefresh());
        rightHeaderButtons.add(btnRefresh);

        JButton btnSettingsGear = createSleekButton("⚙ SETTINGS", ACCENT_GOLD, new Dimension(110, 26));
        btnSettingsGear.addActionListener(e -> openAdminSettingsMenu());
        rightHeaderButtons.add(btnSettingsGear);
        topHeaderPanel.add(rightHeaderButtons, BorderLayout.EAST);
        mainPanel.add(topHeaderPanel, BorderLayout.NORTH);

        JSplitPane masterSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        masterSplitPane.setOpaque(false);
        masterSplitPane.setBorder(null);
        masterSplitPane.setDividerLocation(880); 
        masterSplitPane.setResizeWeight(0.65);

        // --- LEFT SIDE: Visual Canvas Preview ---
        JPanel canvasWrapper = new JPanel(new BorderLayout());
        canvasWrapper.setOpaque(false);
        canvasWrapper.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(ACCENT_GOLD, 1),
                " Client Screen Layout Preview ",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 12),
                ACCENT_GOLD
            ),
            new EmptyBorder(8, 8, 8, 8)
        ));

        layoutCanvas = new LayoutDesignerCanvas();
        canvasWrapper.add(layoutCanvas, BorderLayout.CENTER);
        
        JLabel lblCanvasHint = new JLabel("💡 Awtomatikong naka-sync ang sukat at posisyon batay sa piniling client terminal.");
        lblCanvasHint.setFont(new Font("Segoe UI", Font.ITALIC, 10));
        lblCanvasHint.setForeground(TEXT_WHITE);
        canvasWrapper.add(lblCanvasHint, BorderLayout.SOUTH);

        masterSplitPane.setLeftComponent(canvasWrapper);

        // --- RIGHT SIDE: Configuration Fields & Table ---
        JPanel rightContainer = new JPanel(new GridLayout(2, 1, 0, 8));
        rightContainer.setOpaque(false);

        JPanel centerCard = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(PANEL_BG_GLASS);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2d.setColor(new Color(241, 196, 15, 50));
                g2d.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 14, 14);
                g2d.dispose();
                super.paintComponent(g);
            }
        };
        centerCard.setOpaque(false);
        centerCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(ACCENT_GOLD, 1),
                " Branch Screen Configuration ",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 11),
                ACCENT_GOLD
            ),
            new EmptyBorder(6, 10, 6, 10)
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 4, 3, 4);
        int row = 0;

        addFormRow(centerCard, gbc, row++, "Branch Name:", txtBranchName = styleTextField(new JTextField()));
        addFormRow(centerCard, gbc, row++, "Kiosk Local IP:", txtKioskIp = styleTextField(new JTextField()));
        addFormRow(centerCard, gbc, row++, "Pricelist URL:", txtPricelistUrl = styleTextField(new JTextField()));
        addFormRow(centerCard, gbc, row++, "Registration URL:", txtRegistrationUrl = styleTextField(new JTextField()));
        addFormRow(centerCard, gbc, row++, "Client Window Title:", txtClientTitle = styleTextField(new JTextField()));

        txtPriceX = new JTextField("200", 4);
        txtPriceY = new JTextField("500", 4);
        txtRegX = new JTextField("750", 4);
        txtRegY = new JTextField("500", 4);
        
        txtClientWidth = new JTextField("1920", 4);
        txtClientHeight = new JTextField("1080", 4);

        lblSelectedImage = createGuideLabel("No file selected");
        addUploadRowNicely(centerCard, gbc, row++, "Kiosk Banner BG:", "Browse Banner", e -> {
            chooseFile(fc -> selectedImageFile = fc.getSelectedFile(), lblSelectedImage, "png", "jpg");
            if (selectedImageFile != null) layoutCanvas.setBackgroundImage(selectedImageFile);
        }, lblSelectedImage);

        lblSelectedIcon = createGuideLabel("No file selected");
        addUploadRowNicely(centerCard, gbc, row++, "Client Window Icon:", "Browse Icon", e -> chooseFile(fc -> selectedIconFile = fc.getSelectedFile(), lblSelectedIcon, "png", "ico", "jpg"), lblSelectedIcon);

        lblPricelistBtnImage = createGuideLabel("Default style");
        addUploadRowNicely(centerCard, gbc, row++, "Pricelist Btn Image:", "Browse Pricelist", e -> chooseFile(fc -> selectedPricelistBtnFile = fc.getSelectedFile(), lblPricelistBtnImage, "png", "gif", "jpg"), lblPricelistBtnImage);

        lblRegistrationBtnImage = createGuideLabel("Default style");
        addUploadRowNicely(centerCard, gbc, row++, "Registration Btn Image:", "Browse Reg", e -> chooseFile(fc -> selectedRegistrationBtnFile = fc.getSelectedFile(), lblRegistrationBtnImage, "png", "gif", "jpg"), lblRegistrationBtnImage);

        gbc.gridy = row++; gbc.gridx = 0; gbc.gridwidth = 2; gbc.weightx = 1.0; gbc.fill = GridBagConstraints.HORIZONTAL;
        JPanel customHeaderPanel = new JPanel(new BorderLayout());
        customHeaderPanel.setOpaque(false);
        JLabel lblCustomSec = new JLabel("Custom Buttons Config (Name & URL setup)");
        lblCustomSec.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lblCustomSec.setForeground(ACCENT_GOLD);
        customHeaderPanel.add(lblCustomSec, BorderLayout.WEST);

        JButton btnAddCustom = createSleekButton("+ Add Button", ACCENT_BLUE, new Dimension(110, 20));
        btnAddCustom.addActionListener(e -> {
            if (!clientSelected) {
                JOptionPane.showMessageDialog(this, "Mangyaring pumili muna ng client sa table.", "Paalala", JOptionPane.WARNING_MESSAGE);
                return;
            }
            addCustomButtonRow("Button", "https://", "80", "150", null);
            layoutCanvas.repaint();
        });
        customHeaderPanel.add(btnAddCustom, BorderLayout.EAST);
        centerCard.add(customHeaderPanel, gbc);

        dynamicButtonsContainer = new JPanel();
        dynamicButtonsContainer.setLayout(new BoxLayout(dynamicButtonsContainer, BoxLayout.Y_AXIS));
        dynamicButtonsContainer.setOpaque(false);

        // Tamang sukat at scroll pane para maluwag ang custom buttons list
        customScroll = new JScrollPane(dynamicButtonsContainer);
        customScroll.setPreferredSize(new Dimension(540, 95));
        customScroll.getViewport().setBackground(new Color(15, 20, 28, 180));
        customScroll.setBorder(BorderFactory.createLineBorder(new Color(241, 196, 15, 80), 1));

        gbc.gridy = row++; gbc.gridx = 0; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.BOTH;
        centerCard.add(customScroll, gbc);

        JPanel actionButtonsPanel = new JPanel(new GridLayout(1, 3, 6, 0));
        actionButtonsPanel.setOpaque(false);

        JButton btnUpdateOnly = createColoredActionButton("Update Only", ACCENT_BLUE);
        btnUpdateOnly.addActionListener(e -> pushToSelectedBranch());

        JButton btnUpdateToAll = createColoredActionButton("Update To All", ONLINE_GREEN);
        btnUpdateToAll.addActionListener(e -> pushToAllBranches());

        JButton btnRevert = createColoredActionButton("Revert", OFFLINE_RED);
        btnRevert.addActionListener(e -> triggerSafeModeRevert());

        actionButtonsPanel.add(btnUpdateOnly);
        actionButtonsPanel.add(btnUpdateToAll);
        actionButtonsPanel.add(btnRevert);

        gbc.gridy = row++; gbc.gridx = 0; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.HORIZONTAL;
        centerCard.add(actionButtonsPanel, gbc);

        rightContainer.add(centerCard);

        // Lower Table Panel
        String[] columns = {"Branch Name", "IP Address", "Status"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };
        branchTable = new JTable(tableModel);
        branchTable.setRowHeight(24);
        branchTable.setBackground(new Color(15, 20, 28, 230));
        branchTable.setForeground(TEXT_WHITE);
        branchTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        branchTable.setSelectionBackground(new Color(241, 196, 15, 50));
        branchTable.setSelectionForeground(ACCENT_GOLD);
        
        branchTable.getTableHeader().setBackground(new Color(22, 28, 38));
        branchTable.getTableHeader().setForeground(ACCENT_GOLD);
        branchTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        
        branchTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = branchTable.getSelectedRow();
                if (selectedRow >= 0) {
                    clientSelected = true;
                    String branchName = (String) tableModel.getValueAt(selectedRow, 0);
                    String kioskIp = (String) tableModel.getValueAt(selectedRow, 1);
                    txtBranchName.setText(branchName);
                    txtKioskIp.setText(kioskIp);
                    fetchLiveKioskSettingsAndBackground(kioskIp, branchName);
                }
            }
        });

        branchTable.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                c.setForeground("ONLINE".equals(value) ? ONLINE_GREEN : OFFLINE_RED);
                setFont(new Font("Segoe UI", Font.BOLD, 12));
                return c;
            }
        });

        JScrollPane scrollPane = new JScrollPane(branchTable);
        scrollPane.getViewport().setBackground(new Color(15, 20, 28, 200));
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(241, 196, 15, 80), 1));

        JPanel tableWrapper = new JPanel(new BorderLayout());
        tableWrapper.setOpaque(false);
        tableWrapper.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(ACCENT_GOLD, 1),
                " List of Branches & Terminal Status ",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 12),
                ACCENT_GOLD
            ),
            new EmptyBorder(6, 6, 6, 6)
        ));
        tableWrapper.add(scrollPane, BorderLayout.CENTER);

        rightContainer.add(tableWrapper);
        masterSplitPane.setRightComponent(rightContainer);

        mainPanel.add(masterSplitPane, BorderLayout.CENTER);
        add(mainPanel);
    }

    // ==================== LAYOUT DESIGNER CANVAS ====================
    private class LayoutDesignerCanvas extends JPanel {
        private BufferedImage currentBannerImage = null;
        private int selectedItemType = -1; // 0: Price, 1: Reg, 2: Custom
        private int draggedButtonIndex = -1;
        private int dragOffsetX = 0, dragOffsetY = 0;

        public LayoutDesignerCanvas() {
            setOpaque(true);
            setBackground(new Color(25, 30, 40));
            setBorder(BorderFactory.createLineBorder(ACCENT_GOLD, 1));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    if (!clientSelected) return;
                    selectedItemType = -1;
                    draggedButtonIndex = -1;

                    Rectangle r = getScaledRenderRect();
                    int clientW = 1920, clientH = 1080;
                    try { clientW = Integer.parseInt(txtClientWidth.getText().trim()); } catch (Exception ignored) {}
                    try { clientH = Integer.parseInt(txtClientHeight.getText().trim()); } catch (Exception ignored) {}

                    double scale = (double) r.width / (double) clientW;

                    int mouseX = e.getX() - r.x;
                    int mouseY = e.getY() - r.y;

                    if (mouseX < 0 || mouseY < 0 || mouseX > r.width || mouseY > r.height) return;

                    // 1. Pricelist Button (340x230)
                    try {
                        int px = Integer.parseInt(txtPriceX.getText().trim());
                        int py = Integer.parseInt(txtPriceY.getText().trim());
                        int sx = (int)(px * scale);
                        int sy = (int)(py * scale);
                        int pw = (int)(340 * scale);
                        int ph = (int)(230 * scale);

                        if (new Rectangle(sx, sy, pw, ph).contains(mouseX, mouseY)) {
                            selectedItemType = 0;
                            dragOffsetX = mouseX - sx;
                            dragOffsetY = mouseY - sy;
                            return;
                        }
                    } catch (Exception ignored) {}

                    // 2. Registration Button (340x230)
                    try {
                        int rx = Integer.parseInt(txtRegX.getText().trim());
                        int ry = Integer.parseInt(txtRegY.getText().trim());
                        int sx = (int)(rx * scale);
                        int sy = (int)(ry * scale);
                        int rw = (int)(340 * scale);
                        int rh = (int)(230 * scale);

                        if (new Rectangle(sx, sy, rw, rh).contains(mouseX, mouseY)) {
                            selectedItemType = 1;
                            dragOffsetX = mouseX - sx;
                            dragOffsetY = mouseY - sy;
                            return;
                        }
                    } catch (Exception ignored) {}

                    // 3. Custom Buttons (220x220)
                    for (int i = 0; i < customButtonEntries.size(); i++) {
                        CustomButtonEntry entry = customButtonEntries.get(i);
                        try {
                            int bx = Integer.parseInt(entry.xField.getText().trim());
                            int by = Integer.parseInt(entry.yField.getText().trim());
                            int sx = (int)(bx * scale);
                            int sy = (int)(by * scale);
                            int bw = (int)(220 * scale);
                            int bh = (int)(220 * scale);

                            if (new Rectangle(sx, sy, bw, bh).contains(mouseX, mouseY)) {
                                selectedItemType = 2;
                                draggedButtonIndex = i;
                                dragOffsetX = mouseX - sx;
                                dragOffsetY = mouseY - sy;
                                break;
                            }
                        } catch (Exception ignored) {}
                    }
                }
            });

            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    if (!clientSelected) return;

                    Rectangle r = getScaledRenderRect();
                    int clientW = 1920, clientH = 1080;
                    try { clientW = Integer.parseInt(txtClientWidth.getText().trim()); } catch (Exception ignored) {}
                    try { clientH = Integer.parseInt(txtClientHeight.getText().trim()); } catch (Exception ignored) {}

                    double scale = (double) r.width / (double) clientW;

                    int mouseX = e.getX() - r.x;
                    int mouseY = e.getY() - r.y;

                    int newX = (int)((mouseX - dragOffsetX) / scale);
                    int newY = (int)((mouseY - dragOffsetY) / scale);

                    if (selectedItemType == 0) {
                        txtPriceX.setText(String.valueOf(Math.max(0, Math.min(clientW - 340, newX))));
                        txtPriceY.setText(String.valueOf(Math.max(0, Math.min(clientH - 230, newY))));
                        repaint();
                    } else if (selectedItemType == 1) {
                        txtRegX.setText(String.valueOf(Math.max(0, Math.min(clientW - 340, newX))));
                        txtRegY.setText(String.valueOf(Math.max(0, Math.min(clientH - 230, newY))));
                        repaint();
                    } else if (selectedItemType == 2 && draggedButtonIndex >= 0 && draggedButtonIndex < customButtonEntries.size()) {
                        CustomButtonEntry entry = customButtonEntries.get(draggedButtonIndex);
                        entry.xField.setText(String.valueOf(Math.max(0, Math.min(clientW - 220, newX))));
                        entry.yField.setText(String.valueOf(Math.max(0, Math.min(clientH - 220, newY))));
                        repaint();
                    }
                }
            });
        }

        private Rectangle getScaledRenderRect() {
            int panelWidth = getWidth();
            int panelHeight = getHeight();
            
            int clientW = 1920, clientH = 1080;
            try { clientW = Integer.parseInt(txtClientWidth.getText().trim()); } catch (Exception ignored) {}
            try { clientH = Integer.parseInt(txtClientHeight.getText().trim()); } catch (Exception ignored) {}

            double clientAspect = (double) clientW / (double) clientH;

            int targetWidth = panelWidth;
            int targetHeight = (int) (panelWidth / clientAspect);

            if (targetHeight > panelHeight) {
                targetHeight = panelHeight;
                targetWidth = (int) (panelHeight * clientAspect);
            }

            int x = (panelWidth - targetWidth) / 2;
            int y = (panelHeight - targetHeight) / 2;
            return new Rectangle(x, y, targetWidth, targetHeight);
        }

        public void setBackgroundImage(File file) {
            try {
                currentBannerImage = ImageIO.read(file);
                repaint();
            } catch (Exception ignored) {}
        }

        public void setBackgroundImageDirect(BufferedImage img) {
            currentBannerImage = img;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            Rectangle r = getScaledRenderRect();

            if (currentBannerImage != null) {
                g2d.drawImage(currentBannerImage, r.x, r.y, r.width, r.height, this);
            } else {
                g2d.setColor(new Color(30, 35, 45));
                g2d.fillRect(r.x, r.y, r.width, r.height);
            }

            if (!clientSelected) {
                g2d.setColor(new Color(18, 22, 30, 210));
                g2d.fillRect(0, 0, getWidth(), getHeight());
                g2d.setColor(ACCENT_GOLD);
                g2d.setFont(new Font("Segoe UI", Font.BOLD, 14));
                String msg = "Pumili ng client sa table para sa preview layout";
                g2d.drawString(msg, (getWidth() - g2d.getFontMetrics().stringWidth(msg)) / 2, getHeight() / 2);
                g2d.dispose();
                return;
            }

            int clientW = 1920;
            try { clientW = Integer.parseInt(txtClientWidth.getText().trim()); } catch (Exception ignored) {}
            double scale = (double) r.width / (double) clientW;

            // 1. Pricelist Preview (340x230)
            try {
                int px = Integer.parseInt(txtPriceX.getText().trim());
                int py = Integer.parseInt(txtPriceY.getText().trim());
                g2d.setColor(new Color(46, 204, 113, 210));
                g2d.fillRoundRect(r.x + (int)(px * scale), r.y + (int)(py * scale), (int)(340 * scale), (int)(230 * scale), 6, 6);
                g2d.setColor(ACCENT_GOLD);
                g2d.drawRoundRect(r.x + (int)(px * scale), r.y + (int)(py * scale), (int)(340 * scale), (int)(230 * scale), 6, 6);
                g2d.setColor(TEXT_WHITE);
                g2d.drawString("Pricelist (340x230)", r.x + (int)(px * scale) + 8, r.y + (int)(py * scale) + 20);
            } catch (Exception ignored) {}

            // 2. Registration Preview (340x230)
            try {
                int rx = Integer.parseInt(txtRegX.getText().trim());
                int ry = Integer.parseInt(txtRegY.getText().trim());
                g2d.setColor(new Color(231, 76, 60, 210));
                g2d.fillRoundRect(r.x + (int)(rx * scale), r.y + (int)(ry * scale), (int)(340 * scale), (int)(230 * scale), 6, 6);
                g2d.setColor(ACCENT_GOLD);
                g2d.drawRoundRect(r.x + (int)(rx * scale), r.y + (int)(ry * scale), (int)(340 * scale), (int)(230 * scale), 6, 6);
                g2d.setColor(TEXT_WHITE);
                g2d.drawString("Registration (340x230)", r.x + (int)(rx * scale) + 8, r.y + (int)(ry * scale) + 20);
            } catch (Exception ignored) {}

            // 3. Custom Buttons Preview (220x220)
            for (CustomButtonEntry entry : customButtonEntries) {
                try {
                    int bx = Integer.parseInt(entry.xField.getText().trim());
                    int by = Integer.parseInt(entry.yField.getText().trim());
                    g2d.setColor(new Color(52, 152, 219, 210));
                    g2d.fillRoundRect(r.x + (int)(bx * scale), r.y + (int)(by * scale), (int)(220 * scale), (int)(220 * scale), 6, 6);
                    g2d.setColor(ACCENT_GOLD);
                    g2d.drawRoundRect(r.x + (int)(bx * scale), r.y + (int)(by * scale), (int)(220 * scale), (int)(220 * scale), 6, 6);
                    g2d.setColor(TEXT_WHITE);
                    g2d.drawString(entry.nameField.getText() + " (220x220)", r.x + (int)(bx * scale) + 6, r.y + (int)(by * scale) + 20);
                } catch (Exception ignored) {}
            }

            g2d.dispose();
        }
    }

    private void fetchLiveKioskSettingsAndBackground(String ip, String branchName) {
        if ("127.0.0.1".equals(ip) || ip.isEmpty()) {
            txtClientTitle.setText(branchName + " - Kiosk Terminal");
            return;
        }

        new Thread(() -> {
            try {
                Properties liveProps = KioskAdminController.fetchKioskLiveConfig(ip);
                
                String liveTitle = liveProps.getProperty("client_title", branchName + " - Kiosk Terminal");
                String livePrice = liveProps.getProperty("pricelist_url", "");
                String liveReg = liveProps.getProperty("registration_url", "");
                
                String pX = liveProps.getProperty("pricelist_x", "200");
                String pY = liveProps.getProperty("pricelist_y", "500");
                String rX = liveProps.getProperty("registration_x", "750");
                String rY = liveProps.getProperty("registration_y", "500");
                
                String cW = liveProps.getProperty("client_width", "1920");
                String cH = liveProps.getProperty("client_height", "1080");

                String liveNames = liveProps.getProperty("custom_btn_names", "");
                String liveUrls = liveProps.getProperty("custom_btn_urls", "");
                String liveXs = liveProps.getProperty("custom_btn_xs", "");
                String liveYs = liveProps.getProperty("custom_btn_ys", "");

                SwingUtilities.invokeLater(() -> {
                    txtClientTitle.setText(liveTitle);
                    if (!livePrice.isEmpty()) txtPricelistUrl.setText(livePrice);
                    if (!liveReg.isEmpty()) txtRegistrationUrl.setText(liveReg);
                    
                    txtPriceX.setText(pX);
                    txtPriceY.setText(pY);
                    txtRegX.setText(rX);
                    txtRegY.setText(rY);
                    
                    txtClientWidth.setText(cW);
                    txtClientHeight.setText(cH);

                    customButtonEntries.clear();
                    if (!liveUrls.isEmpty()) {
                        String[] nArr = liveNames.split("\\|\\|", -1);
                        String[] uArr = liveUrls.split("\\|\\|", -1);
                        String[] xArr = liveXs.split("\\|\\|", -1);
                        String[] yArr = liveYs.split("\\|\\|", -1);

                        for (int i = 0; i < uArr.length; i++) {
                            String urlVal = uArr[i].trim();
                            if (!urlVal.isEmpty() && !urlVal.equals("https://") && !urlVal.equals("null")) {
                                String nameVal = (i < nArr.length && !nArr[i].trim().isEmpty() && !nArr[i].equals("null")) ? nArr[i].trim() : "Custom " + (i + 1);
                                String xVal = (i < xArr.length && !xArr[i].trim().isEmpty() && !xArr[i].equals("null")) ? xArr[i].trim() : "80";
                                String yVal = (i < yArr.length && !yArr[i].trim().isEmpty() && !yArr[i].equals("null")) ? yArr[i].trim() : "150";
                                
                                addCustomButtonRow(nameVal, urlVal, xVal, yVal, null);
                            }
                        }
                    }
                    layoutCanvas.repaint();
                });

                boolean bgLoaded = false;
                String[] possibleBgNames = {"background.png", "banner.png"};
                for (String bgName : possibleBgNames) {
                    try {
                        URL imgUrl = new URL("http://" + ip + ":8080/kiosk_config/" + bgName);
                        BufferedImage liveBg = ImageIO.read(imgUrl);
                        if (liveBg != null) {
                            SwingUtilities.invokeLater(() -> {
                                layoutCanvas.setBackgroundImageDirect(liveBg);
                                lblSelectedImage.setText("Loaded from " + branchName);
                                lblSelectedImage.setForeground(ONLINE_GREEN);
                            });
                            bgLoaded = true;
                            break;
                        }
                    } catch (Exception ignored) {}
                }

                if (!bgLoaded) {
                    SwingUtilities.invokeLater(() -> {
                        lblSelectedImage.setText("No background found on client");
                        lblSelectedImage.setForeground(OFFLINE_RED);
                    });
                }

            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> txtClientTitle.setText(branchName + " - Kiosk Terminal"));
            }
        }).start();
    }

    private void addFormRow(JPanel panel, GridBagConstraints gbc, int row, String labelText, JTextField textField) {
        gbc.gridy = row; gbc.gridx = 0; gbc.gridwidth = 1; gbc.weightx = 0.0; gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.WEST;
        panel.add(createStyledLabel(labelText), gbc);
        gbc.gridx = 1; gbc.gridwidth = 1; gbc.weightx = 1.0; gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(textField, gbc);
    }

    private void addUploadRowNicely(JPanel panel, GridBagConstraints gbc, int row, String labelText, String btnText, java.awt.event.ActionListener action, JLabel guideLabel) {
        gbc.gridy = row; gbc.gridx = 0; gbc.gridwidth = 1; gbc.weightx = 0.0; gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.WEST;
        panel.add(createStyledLabel(labelText), gbc);

        JPanel rightPanel = new JPanel(new BorderLayout(6, 0));
        rightPanel.setOpaque(false);
        JButton btn = createSleekButton(btnText, ACCENT_BLUE, new Dimension(145, 20));
        btn.addActionListener(action);

        guideLabel.setForeground(new Color(180, 190, 205));
        guideLabel.setFont(new Font("Segoe UI", Font.ITALIC, 9));

        rightPanel.add(btn, BorderLayout.WEST);
        rightPanel.add(guideLabel, BorderLayout.CENTER);

        gbc.gridx = 1; gbc.gridwidth = 1; gbc.weightx = 1.0; gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(rightPanel, gbc);
    }

    private JLabel createGuideLabel(String text) { return new JLabel(text); }

    private void chooseFile(java.util.function.Consumer<JFileChooser> setter, JLabel label, String... extensions) {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("Supported Formats", extensions));
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            setter.accept(fc);
            File f = fc.getSelectedFile();
            label.setText(f.getName());
            label.setForeground(ACCENT_GOLD);
        }
    }

    private void addCustomButtonRow(String name, String url, String xPos, String yPos, File imgFile) {
        CustomButtonEntry entry = new CustomButtonEntry(customButtonEntries.size() + 1, name, url, xPos, yPos);
        if (imgFile != null) {
            entry.imageFile = imgFile;
            entry.imgLabel.setText(imgFile.getName());
            entry.imgLabel.setForeground(ACCENT_GOLD);
        }
        entry.nameField.addKeyListener(new KeyAdapter() { public void keyReleased(KeyEvent e) { layoutCanvas.repaint(); } });

        customButtonEntries.add(entry);
        rebuildCustomButtonsUI();
    }

    private void rebuildCustomButtonsUI() {
        dynamicButtonsContainer.removeAll();
        for (int i = 0; i < customButtonEntries.size(); i++) {
            CustomButtonEntry entry = customButtonEntries.get(i);
            entry.index = i + 1;

            JPanel rowCard = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 2)) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2d = (Graphics2D) g.create();
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2d.setColor(new Color(22, 28, 38, 230));
                    g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 5, 5);
                    g2d.setColor(new Color(241, 196, 15, 50));
                    g2d.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 5, 5);
                    g2d.dispose();
                    super.paintComponent(g);
                }
            };
            rowCard.setOpaque(false);
            rowCard.setPreferredSize(new Dimension(525, 28));
            rowCard.setMaximumSize(new Dimension(530, 28));

            rowCard.add(createMiniLabel("N:")); rowCard.add(entry.nameField); styleTextField(entry.nameField);
            rowCard.add(createMiniLabel("U:")); rowCard.add(entry.urlField); styleTextField(entry.urlField);

            JButton btnBrowse = createSleekButton("Img", ACCENT_BLUE, new Dimension(30, 18));
            btnBrowse.addActionListener(e -> {
                JFileChooser fc = new JFileChooser();
                if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                    entry.imageFile = fc.getSelectedFile();
                    entry.imgLabel.setText(entry.imageFile.getName());
                    entry.imgLabel.setForeground(ACCENT_GOLD);
                }
            });
            rowCard.add(btnBrowse);

            entry.imgLabel.setPreferredSize(new Dimension(42, 16));
            entry.imgLabel.setFont(new Font("Segoe UI", Font.ITALIC, 8));
            rowCard.add(entry.imgLabel);

            JButton btnDel = createSleekButton("✖", OFFLINE_RED, new Dimension(18, 18));
            btnDel.addActionListener(e -> {
                customButtonEntries.remove(entry);
                rebuildCustomButtonsUI();
                layoutCanvas.repaint();
            });
            rowCard.add(btnDel);

            dynamicButtonsContainer.add(rowCard);
            dynamicButtonsContainer.add(Box.createRigidArea(new Dimension(0, 2)));
        }
        dynamicButtonsContainer.revalidate();
        dynamicButtonsContainer.repaint();
    }

    private JLabel createMiniLabel(String text) {
        JLabel l = new JLabel(text); l.setForeground(TEXT_WHITE); l.setFont(new Font("Segoe UI", Font.BOLD, 9)); return l;
    }

    private void openAdminSettingsMenu() {
        String[] options = {"Change Admin Background", "Change Admin Icon", "Change Admin Title", "Cancel"};
        int choice = JOptionPane.showOptionDialog(this, "Settings", "Settings", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);
        if (choice == 0 || choice == 1) {
            JFileChooser fc = new JFileChooser();
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                try {
                    File dir = new File("kiosk_config");
                    if (!dir.exists()) dir.mkdirs();
                    Files.copy(fc.getSelectedFile().toPath(), new File(dir, choice == 0 ? "AdminBG.png" : "logo.png").toPath(), StandardCopyOption.REPLACE_EXISTING);
                    if (choice == 0) loadAdminBackground(); else loadCustomWindowIcon();
                    JOptionPane.showMessageDialog(this, "Updated successfully!");
                } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage()); }
            }
        } else if (choice == 2) {
            String newTitle = JOptionPane.showInputDialog(this, "Enter new Admin Title:", getTitle());
            if (newTitle != null && !newTitle.trim().isEmpty()) { setTitle(newTitle.trim()); saveSetting("admin_title", newTitle.trim()); }
        }
    }

    private String getSavedSetting(String key, String def) {
        File f = new File("kiosk_config/settings.properties");
        if (f.exists()) {
            try (FileInputStream in = new FileInputStream(f)) { Properties p = new Properties(); p.load(in); return p.getProperty(key, def); } catch (Exception ignored) {}
        }
        return def;
    }

    private void saveSetting(String key, String val) {
        try {
            File dir = new File("kiosk_config"); if (!dir.exists()) dir.mkdirs();
            File f = new File(dir, "settings.properties"); Properties p = new Properties();
            if (f.exists()) { try (FileInputStream in = new FileInputStream(f)) { p.load(in); } }
            p.setProperty(key, val);
            try (FileOutputStream out = new FileOutputStream(f)) { p.store(out, "Updated"); }
        } catch (Exception ignored) {}
    }

    private JLabel createStyledLabel(String text) {
        JLabel l = new JLabel(text); l.setFont(new Font("Segoe UI", Font.BOLD, 11)); l.setForeground(TEXT_WHITE); return l;
    }

    private JTextField styleTextField(JTextField f) {
        f.setFont(new Font("Segoe UI", Font.PLAIN, 11)); f.setBackground(new Color(22, 28, 38, 230)); f.setForeground(ACCENT_GOLD); f.setCaretColor(ACCENT_GOLD);
        f.setPreferredSize(new Dimension(0, 22));
        f.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(241, 196, 15, 100), 1), new EmptyBorder(1, 4, 1, 4)));
        return f;
    }

    private JButton createSleekButton(String text, Color accent, Dimension dim) {
        JButton b = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(getModel().isPressed() ? accent.darker() : getModel().isRollover() ? new Color(30, 38, 50) : new Color(20, 26, 36, 220));
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 5, 5); g2d.setColor(accent); g2d.setStroke(new BasicStroke(1.2f));
                g2d.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 5, 5); g2d.dispose();
                super.paintComponent(g);
            }
        };
        b.setFont(new Font("Segoe UI", Font.BOLD, 11)); b.setForeground(TEXT_WHITE); b.setFocusPainted(false); b.setContentAreaFilled(false); b.setBorderPainted(false);
        if (dim != null) b.setPreferredSize(dim);
        return b;
    }

    private JButton createColoredActionButton(String text, Color baseColor) {
        JButton b = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(getModel().isPressed() ? baseColor.darker() : getModel().isRollover() ? new Color(30, 38, 50) : new Color(20, 26, 36, 220));
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6); g2d.setColor(baseColor); g2d.setStroke(new BasicStroke(1.5f));
                g2d.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 6, 6); g2d.dispose();
                super.paintComponent(g);
            }
        };
        b.setFont(new Font("Segoe UI", Font.BOLD, 12)); b.setForeground(TEXT_WHITE); b.setFocusPainted(false); b.setContentAreaFilled(false); b.setBorderPainted(false);
        b.setPreferredSize(new Dimension(0, 28));
        return b;
    }

    private void triggerManualRefresh() {
        new Thread(() -> {
            try {
                HttpURLConnection conn = (HttpURLConnection) new URL(CLOUD_RELAY_URL + "/api/branches").openConnection();
                conn.setRequestMethod("GET"); conn.setConnectTimeout(3000);
                if (conn.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder(); String line; while ((line = reader.readLine()) != null) sb.append(line);
                    reader.close();
                    String json = sb.toString(); Set<String> activeIps = new HashSet<>();
                    if (json.startsWith("[") && json.endsWith("]")) {
                        for (String obj : json.substring(1, json.length() - 1).split("\\},\\{")) {
                            String bName = "Unknown", cIp = "127.0.0.1";
                            for (String pair : obj.replace("[{", "").replace("}]", "").replace("{", "").replace("}", "").split(",")) {
                                String[] kv = pair.split(":");
                                if (kv.length == 2) {
                                    String k = kv[0].replace("\"", "").trim(); String v = kv[1].replace("\"", "").trim();
                                    if ("branchName".equals(k)) bName = v; if ("clientIp".equals(k)) cIp = "localhost".equalsIgnoreCase(v) ? "127.0.0.1" : v;
                                }
                            }
                            if (!"127.0.0.1".equals(cIp)) {
                                activeIps.add(cIp);
                                final String fName = bName; final String fIp = cIp;
                                SwingUtilities.invokeLater(() -> upsertBranchInTable(fName, fIp, "ONLINE"));
                            }
                        }
                    }
                    SwingUtilities.invokeLater(() -> {
                        for (int i = 0; i < tableModel.getRowCount(); i++) {
                            if (!activeIps.contains(tableModel.getValueAt(i, 1))) tableModel.setValueAt("OFFLINE", i, 2);
                        }
                    });
                }
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> { for (int i = 0; i < tableModel.getRowCount(); i++) tableModel.setValueAt("OFFLINE", i, 2); });
            }
        }).start();
    }

    private void upsertBranchInTable(String name, String ip, String status) {
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            if (tableModel.getValueAt(i, 1).equals(ip)) { tableModel.setValueAt(name, i, 0); tableModel.setValueAt(status, i, 2); return; }
        }
        tableModel.addRow(new Object[]{name, ip, status});
    }

    private void startCloudSyncHeartbeat() { new javax.swing.Timer(5000, e -> triggerManualRefresh()).start(); }

    private void pushToSelectedBranch() {
        if (!clientSelected) {
            JOptionPane.showMessageDialog(this, "Pumili muna ng client mula sa table.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String ip = txtKioskIp.getText().trim(); 
        String branchName = txtBranchName.getText().trim(); 
        String clientTitle = txtClientTitle.getText().trim();
        if (ip.isEmpty()) { JOptionPane.showMessageDialog(this, "Pumili muna ng Kiosk IP.", "Error", JOptionPane.ERROR_MESSAGE); return; }

        StringBuilder names = new StringBuilder();
        StringBuilder urls = new StringBuilder();
        StringBuilder xs = new StringBuilder();
        StringBuilder ys = new StringBuilder();
        
        for (CustomButtonEntry e : customButtonEntries) {
            String n = e.nameField.getText().trim();
            String u = e.urlField.getText().trim();
            if (!n.isEmpty() && !u.isEmpty()) {
                names.append(n).append("||"); 
                urls.append(u).append("||");
                xs.append(e.xField.getText().trim()).append("||"); 
                ys.append(e.yField.getText().trim()).append("||");
            }
        }

        new Thread(() -> {
            boolean ok = KioskAdminController.sendSettingsToKiosk(
                ip, branchName, txtPricelistUrl.getText().trim(), txtRegistrationUrl.getText().trim(), 
                clientTitle, names.toString(), urls.toString(), xs.toString(), ys.toString(),
                txtPriceX.getText().trim(), txtPriceY.getText().trim(), 
                txtRegX.getText().trim(), txtRegY.getText().trim()
            );

            if (selectedImageFile != null) ok &= KioskAdminController.sendBackgroundImageToKiosk(ip, selectedImageFile);
            if (selectedIconFile != null) ok &= KioskAdminController.sendClientIconToKiosk(ip, selectedIconFile);
            if (selectedPricelistBtnFile != null) {
                String ext = selectedPricelistBtnFile.getName().contains(".") ? selectedPricelistBtnFile.getName().substring(selectedPricelistBtnFile.getName().lastIndexOf(".")) : ".gif";
                ok &= KioskAdminController.sendCustomButtonImageToKiosk(ip, selectedPricelistBtnFile, "btn_pricelist" + ext);
            }
            if (selectedRegistrationBtnFile != null) {
                String ext = selectedRegistrationBtnFile.getName().contains(".") ? selectedRegistrationBtnFile.getName().substring(selectedRegistrationBtnFile.getName().lastIndexOf(".")) : ".gif";
                ok &= KioskAdminController.sendCustomButtonImageToKiosk(ip, selectedRegistrationBtnFile, "btn_registration" + ext);
            }
            for (int i = 0; i < customButtonEntries.size(); i++) {
                CustomButtonEntry e = customButtonEntries.get(i);
                if (e.imageFile != null) {
                    String ext = e.imageFile.getName().contains(".") ? e.imageFile.getName().substring(e.imageFile.getName().lastIndexOf(".")) : ".gif";
                    ok &= KioskAdminController.sendCustomButtonImageToKiosk(ip, e.imageFile, "btn_custom_" + (i + 1) + ext);
                }
            }
            final boolean success = ok;
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, success ? "Successfully updated " + branchName + "!" : "Failed to reach kiosk.", success ? "Success" : "Error", success ? JOptionPane.INFORMATION_MESSAGE : JOptionPane.ERROR_MESSAGE));
        }).start();
    }

    private void pushToAllBranches() {
        int rowCount = tableModel.getRowCount();
        if (rowCount == 0) { JOptionPane.showMessageDialog(this, "Walang nakitang branch sa table.", "Error", JOptionPane.ERROR_MESSAGE); return; }
        int confirm = JOptionPane.showConfirmDialog(this, "I-broadcast at i-update ang LAHAT ng online branches?", "Push To All", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        StringBuilder names = new StringBuilder(), urls = new StringBuilder(), xs = new StringBuilder(), ys = new StringBuilder();
        for (CustomButtonEntry e : customButtonEntries) {
            String n = e.nameField.getText().trim();
            String u = e.urlField.getText().trim();
            if (!n.isEmpty() && !u.isEmpty()) {
                names.append(n).append("||"); urls.append(u).append("||");
                xs.append(e.xField.getText().trim()).append("||"); ys.append(e.yField.getText().trim()).append("||");
            }
        }

        new Thread(() -> {
            int successCount = 0;
            for (int i = 0; i < rowCount; i++) {
                String bName = (String) tableModel.getValueAt(i, 0); String ip = (String) tableModel.getValueAt(i, 1); String status = (String) tableModel.getValueAt(i, 2);
                if ("ONLINE".equals(status) && !ip.equals("127.0.0.1")) {
                    boolean ok = KioskAdminController.sendSettingsToKiosk(
                        ip, bName, txtPricelistUrl.getText().trim(), txtRegistrationUrl.getText().trim(), 
                        txtClientTitle.getText().trim(), names.toString(), urls.toString(), xs.toString(), ys.toString(),
                        txtPriceX.getText().trim(), txtPriceY.getText().trim(), 
                        txtRegX.getText().trim(), txtRegY.getText().trim()
                    );
                    
                    if (selectedImageFile != null) ok &= KioskAdminController.sendBackgroundImageToKiosk(ip, selectedImageFile);
                    if (ok) successCount++;
                }
            }
            final int finalCount = successCount;
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Naka-broadcast sa " + finalCount + " na online branch(es)!", "Success", JOptionPane.INFORMATION_MESSAGE));
        }).start();
    }

    private void triggerSafeModeRevert() {
        if (!clientSelected) {
            JOptionPane.showMessageDialog(this, "Pumili muna ng client.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String ip = txtKioskIp.getText().trim(); String branchName = txtBranchName.getText().trim();
        if (ip.isEmpty()) { JOptionPane.showMessageDialog(this, "Pumili muna ng Kiosk IP.", "Error", JOptionPane.ERROR_MESSAGE); return; }
        int confirm = JOptionPane.showConfirmDialog(this, "I-revert sa huling maayos na setting (Safe Mode) ang " + branchName + "?", "Safe Mode", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            new Thread(() -> {
                try {
                    URL url = new URL("http://" + ip + ":8080/api/revert");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET"); conn.setConnectTimeout(3000);
                    if (conn.getResponseCode() == 200) {
                        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Successfully reverted to Safe Mode!"));
                    } else {
                        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Walang nakitang backup sa client.", "Error", JOptionPane.ERROR_MESSAGE));
                    }
                } catch (Exception ex) { SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage())); }
            }).start();
        }
    }

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); } catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new AdminFrame().setVisible(true));
    }
}