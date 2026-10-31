package com.madnesscreator.mavenproject1;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Properties;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public class NewJPanel extends javax.swing.JPanel {

    private JLayeredPane layeredPane;
    private JPanel mainMenuPanel;
    private BufferedImage backgroundImage;
    private int secretExitTaps = 0;
    private Timer idleTimer;

    private static final int IDLE_TIMEOUT = 60000;

    public NewJPanel() {
        this.setLayout(new BorderLayout());
        this.setOpaque(false);
        
        layeredPane = new JLayeredPane();
        this.add(layeredPane, BorderLayout.CENTER);

        BufferedImage cursorImg = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Cursor blankCursor = Toolkit.getDefaultToolkit().createCustomCursor(
                cursorImg, new Point(0, 0), "blank cursor"
        );
        this.setCursor(blankCursor);

        checkFirstRunBranchSetup();
        SwingUtilities.invokeLater(this::applyClientBranding);

        loadLocalBackground();
        buildMainMenuLayer();

        layeredPane.add(mainMenuPanel, JLayeredPane.DEFAULT_LAYER);
        setupIdleDetector();

        // KioskConfigServer na nagpapadala ng screen metrics at button dimensions
        KioskConfigServer server = new KioskConfigServer(8080, () -> {
            loadLocalBackground();
            applyClientBranding();
            rebuildMainMenuUI();
        });

        startCloudHeartbeat();
    }

    private void checkFirstRunBranchSetup() {
        SwingUtilities.invokeLater(() -> {
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

                String existingBranch = props.getProperty("branch_name");
                if (existingBranch == null || existingBranch.trim().isEmpty() || existingBranch.equals("A&F Kiosk Terminal")) {
                    String branchName = JOptionPane.showInputDialog(
                        null, 
                        "Enter the Branch Name for this Kiosk Terminal (e.g., Fairview Branch):", 
                        "A&F Kiosk - Initial Setup", 
                        JOptionPane.QUESTION_MESSAGE
                    );

                    if (branchName != null && !branchName.trim().isEmpty()) {
                        props.setProperty("branch_name", branchName.trim());
                    } else {
                        props.setProperty("branch_name", "A&F Kiosk Terminal");
                    }

                    try (FileOutputStream out = new FileOutputStream(file)) {
                        props.store(out, "Initialized Kiosk Configuration");
                    }
                }
            } catch (Exception e) {
                System.err.println("First-run setup error: " + e.getMessage());
            }
        });
    }

    private void applyClientBranding() {
        try {
            File file = new File("kiosk_config/settings.properties");
            Properties props = new Properties();
            if (file.exists()) {
                try (FileInputStream in = new FileInputStream(file)) {
                    props.load(in);
                }
            }

            String clientTitle = props.getProperty("client_title", "A&F Computer Store - Kiosk Terminal");
            java.awt.Window window = SwingUtilities.getWindowAncestor(this);
            if (window instanceof JFrame) {
                ((JFrame) window).setTitle(clientTitle);
            }

            File iconFile = new File("kiosk_config/logo.png");
            if (iconFile.exists() && window instanceof JFrame) {
                ImageIO.setUseCache(false);
                try (FileInputStream fis = new FileInputStream(iconFile)) {
                    BufferedImage originalIcon = ImageIO.read(fis);
                    java.awt.Image scaledIcon = originalIcon.getScaledInstance(32, 32, java.awt.Image.SCALE_SMOOTH);
                    ((JFrame) window).setIconImage(scaledIcon);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to apply client branding: " + e.getMessage());
        }
    }

    private void loadLocalBackground() {
        new Thread(() -> {
            try {
                File bgFile = new File("kiosk_config/background.png");
                if (!bgFile.exists()) {
                    bgFile = new File("kiosk_config/banner.png");
                }

                if (bgFile.exists()) {
                    ImageIO.setUseCache(false);
                    try (FileInputStream fis = new FileInputStream(bgFile)) {
                        backgroundImage = ImageIO.read(fis);
                    }
                } else {
                    backgroundImage = null; 
                }
                SwingUtilities.invokeLater(() -> {
                    if (mainMenuPanel != null) {
                        mainMenuPanel.revalidate();
                        mainMenuPanel.repaint();
                    }
                    repaint();
                });
            } catch (Exception e) {
                System.err.println("Background image failed to load: " + e.getMessage());
            }
        }).start();
    }

    private String getSavedUrl(String key, String defaultUrl) {
        File file = new File("kiosk_config/settings.properties");
        if (file.exists()) {
            try (FileInputStream in = new FileInputStream(file)) {
                Properties props = new Properties();
                props.load(in);
                return props.getProperty(key, defaultUrl);
            } catch (Exception e) {
                return defaultUrl;
            }
        }
        return defaultUrl;
    }

    private void terminateEdgeOnly() {
        try {
            ProcessBuilder pb = new ProcessBuilder("taskkill", "/F", "/IM", "msedge.exe");
            pb.start();
        } catch (Exception ex) {
            System.err.println("Failed to terminate Edge: " + ex.getMessage());
        }
    }

    private void setupIdleDetector() {
        idleTimer = new Timer(IDLE_TIMEOUT, e -> {
            SwingUtilities.invokeLater(() -> {
                terminateEdgeOnly();
                java.awt.Window window = SwingUtilities.getWindowAncestor(this);
                if (window != null) {
                    window.setAlwaysOnTop(true);
                    window.toFront();
                    window.requestFocus();
                }
                layeredPane.revalidate();
                layeredPane.repaint();
            });
        });
        idleTimer.setRepeats(true);
        idleTimer.start();

        Toolkit.getDefaultToolkit().addAWTEventListener(event -> {
            if (idleTimer != null) {
                idleTimer.restart();
            }
        }, AWTEvent.MOUSE_EVENT_MASK | AWTEvent.KEY_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);
    }

    private void startCloudHeartbeat() {
        Timer heartbeatTimer = new Timer(5000, e -> {
            new Thread(() -> {
                try {
                    String branchName = getSavedUrl("branch_name", "A&F Kiosk Branch");
                    String clientIp = InetAddress.getLocalHost().getHostAddress();

                    if ("0.0.0.0".equals(clientIp) || "127.0.0.1".equals(clientIp)) {
                        clientIp = "Localhost";
                    }

                    String urlString = "https://mavenproject1-bzdx.onrender.com/api/register?branchName="
                            + URLEncoder.encode(branchName, "UTF-8")
                            + "&clientIp=" + URLEncoder.encode(clientIp, "UTF-8");

                    URL url = new URL(urlString);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(4000);
                    conn.getResponseCode();
                } catch (Exception ignored) {}
            }).start();
        });
        heartbeatTimer.start();
    }

    @Override
    public void doLayout() {
        super.doLayout();
        if (mainMenuPanel != null) {
            mainMenuPanel.setBounds(0, 0, getWidth(), getHeight());
        }
        
        if (getWidth() > 0 && getHeight() > 0) {
            updateClientMetricsAndSizesToProperties(getWidth(), getHeight());
        }
    }

    private void updateClientMetricsAndSizesToProperties(int width, int height) {
        new Thread(() -> {
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
                props.setProperty("client_width", String.valueOf(width));
                props.setProperty("client_height", String.valueOf(height));
                props.setProperty("pricelist_width", "340");
                props.setProperty("pricelist_height", "230");
                props.setProperty("registration_width", "340");
                props.setProperty("registration_height", "230");
                props.setProperty("custom_btn_width", "220");
                props.setProperty("custom_btn_height", "220");

                try (FileOutputStream out = new FileOutputStream(file)) {
                    props.store(out, "Updated Client Screen Metrics & Standard Button Sizes");
                }
            } catch (Exception ignored) {}
        }).start();
    }

    private void rebuildMainMenuUI() {
        SwingUtilities.invokeLater(() -> {
            if (mainMenuPanel != null) {
                layeredPane.remove(mainMenuPanel);
            }
            buildMainMenuLayer();
            layeredPane.add(mainMenuPanel, JLayeredPane.DEFAULT_LAYER);
            layeredPane.revalidate();
            layeredPane.repaint();
        });
    }

    private void buildMainMenuLayer() {
        mainMenuPanel = new JPanel(null) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                if (backgroundImage != null) {
                    g2d.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    g2d.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
                } else {
                    g2d.setColor(new Color(20, 24, 30));
                    g2d.fillRect(0, 0, getWidth(), getHeight());
                }
                g2d.dispose();
            }
        };

        mainMenuPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int hitZoneWidth = mainMenuPanel.getWidth() - 120;
                if (e.getX() > hitZoneWidth && e.getY() < 120) {
                    secretExitTaps++;
                    if (secretExitTaps >= 5) {
                        System.exit(0);
                    }
                } else {
                    secretExitTaps = 0; 
                }
            }
        });

        // 1. PRICELIST BUTTON (Standard Size: 340 x 230)
        int priceX = 200, priceY = 500;
        try { priceX = Integer.parseInt(getSavedUrl("pricelist_x", "200")); } catch (Exception ignored) {}
        try { priceY = Integer.parseInt(getSavedUrl("pricelist_y", "500")); } catch (Exception ignored) {}

        JButton pricelistBtn = createFlexibleButton("btn_pricelist", 340, 230, "Pricelist", e -> {
            String defaultSheetUrl = "https://docs.google.com/spreadsheets/d/1AncL3i3fKC3IEvIXtPYmAplX1oee9CTm5I7yNmocsuM/edit?gid=1864218333#gid=1864218333";
            String targetUrl = getSavedUrl("pricelist_url", defaultSheetUrl);
            launchEdgeKioskScript("launch_pricelist.bat", targetUrl, "EdgePricelist");
        });
        pricelistBtn.setBounds(priceX, priceY, 340, 230);
        mainMenuPanel.add(pricelistBtn);

        // 2. REGISTRATION BUTTON (Standard Size: 340 x 230 - Tugma sa Pricelist)
        int regX = 750, regY = 500;
        try { regX = Integer.parseInt(getSavedUrl("registration_x", "750")); } catch (Exception ignored) {}
        try { regY = Integer.parseInt(getSavedUrl("registration_y", "500")); } catch (Exception ignored) {}

        JButton registrationBtn = createFlexibleButton("btn_registration", 340, 230, "Client Registration", e -> {
            String defaultFormUrl = "https://docs.google.com/forms/d/e/1FAIpQLScf4maR6JYgOIuceat-F43K6Exhn8-wqwvFBPhNo4tU7chZDA/viewform";
            String targetUrl = getSavedUrl("registration_url", defaultFormUrl);
            launchEdgeKioskScript("launch_registration.bat", targetUrl, "EdgeRegistration");
        });
        registrationBtn.setBounds(regX, regY, 340, 230);
        mainMenuPanel.add(registrationBtn);

        // 3. DYNAMIC CUSTOM BUTTONS (Standard Size: 220 x 220 with exact limit -1 parsing)
        String savedNames = getSavedUrl("custom_btn_names", "");
        String savedUrls = getSavedUrl("custom_btn_urls", "");
        String savedXs = getSavedUrl("custom_btn_xs", "");
        String savedYs = getSavedUrl("custom_btn_ys", "");

        if (!savedUrls.isEmpty()) {
            String[] names = savedNames.split("\\|\\|", -1);
            String[] urls = savedUrls.split("\\|\\|", -1);
            String[] xs = savedXs.split("\\|\\|", -1);
            String[] ys = savedYs.split("\\|\\|", -1);

            for (int i = 0; i < urls.length; i++) {
                String customUrl = urls[i].trim();
                if (!customUrl.isEmpty() && !customUrl.equals("https://") && !customUrl.equals("null")) {
                    int btnIndex = i + 1;
                    String btnName = (i < names.length && !names[i].trim().isEmpty() && !names[i].equals("null")) ? names[i].trim() : "Custom " + btnIndex;
                    
                    int posX = 80;
                    int posY = 150;
                    int customSize = 220;

                    try {
                        if (i < xs.length && !xs[i].trim().isEmpty() && !xs[i].equals("null")) {
                            posX = Integer.parseInt(xs[i].trim());
                        }
                        if (i < ys.length && !ys[i].trim().isEmpty() && !ys[i].equals("null")) {
                            posY = Integer.parseInt(ys[i].trim());
                        }
                    } catch (NumberFormatException ignored) {}

                    JButton customBtn = createFlexibleButton("btn_custom_" + btnIndex, customSize, customSize, btnName, e -> {
                        launchEdgeKioskScript("launch_custom_" + btnIndex + ".bat", customUrl, "EdgeCustom" + btnIndex);
                    });
                    
                    customBtn.setBounds(posX, posY, customSize, customSize);
                    mainMenuPanel.add(customBtn);
                }
            }
        }
    }

    private JButton createFlexibleButton(String basePrefix, int targetWidth, int targetHeight, String fallbackText, java.awt.event.ActionListener actionListener) {
        JButton button = new JButton();
        button.setPreferredSize(new Dimension(targetWidth, targetHeight));
        button.setSize(targetWidth, targetHeight);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setBorder(null);
        button.setFocusPainted(false);
        button.setMargin(new java.awt.Insets(0, 0, 0, 0));

        boolean imageLoaded = false;
        try {
            File dir = new File("kiosk_config");
            File foundFile = null;

            if (dir.exists()) {
                File[] files = dir.listFiles((d, name) -> name.toLowerCase().startsWith(basePrefix.toLowerCase() + "."));
                if (files != null && files.length > 0) {
                    foundFile = files[0];
                }
            }

            if (foundFile != null && foundFile.exists()) {
                String ext = foundFile.getName().substring(foundFile.getName().lastIndexOf(".") + 1).toLowerCase();
                if (ext.equals("gif")) {
                    ImageIcon gifIcon = new ImageIcon(foundFile.getAbsolutePath());
                    Image scaledImg = gifIcon.getImage().getScaledInstance(targetWidth, targetHeight, Image.SCALE_DEFAULT);
                    ImageIcon scaledGifIcon = new ImageIcon(scaledImg);
                    scaledGifIcon.setImageObserver(button);
                    button.setIcon(scaledGifIcon);
                    imageLoaded = true;
                } else {
                    ImageIO.setUseCache(false);
                    try (FileInputStream fis = new FileInputStream(foundFile)) {
                        BufferedImage img = ImageIO.read(fis);
                        if (img != null) {
                            Image scaledImg = img.getScaledInstance(targetWidth, targetHeight, Image.SCALE_SMOOTH);
                            button.setIcon(new ImageIcon(scaledImg));
                            imageLoaded = true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Could not load button image for " + basePrefix + ": " + e.getMessage());
        }

        if (!imageLoaded) {
            button.setBorderPainted(true);
            button.setContentAreaFilled(true);
            button.setOpaque(true);
            button.setForeground(Color.WHITE);
            button.setBackground(new Color(50, 50, 50));
            button.setText(fallbackText);
        }

        if (actionListener != null) {
            button.addActionListener(actionListener);
        }

        return button;
    }

    private void launchEdgeKioskScript(String scriptName, String fallbackUrl, String profileName) {
        new Thread(() -> {
            try {
                terminateEdgeOnly();
                Thread.sleep(300);

                SwingUtilities.invokeLater(() -> {
                    java.awt.Window window = SwingUtilities.getWindowAncestor(this);
                    if (window instanceof JFrame) {
                        ((JFrame) window).setAlwaysOnTop(false);
                    }
                });

                File scriptFile = new File(scriptName);
                if (!scriptFile.exists()) {
                    scriptFile = new File("kiosk_config/" + scriptName);
                }

                Process process;
                if (scriptFile.exists()) {
                    ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", scriptFile.getAbsolutePath());
                    process = pb.start();
                } else {
                    GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                    GraphicsDevice[] screens = ge.getScreenDevices();
                    
                    int targetX = 0; 
                    int targetY = 0;

                    if (screens.length > 1) {
                        Rectangle secBounds = screens[1].getDefaultConfiguration().getBounds();
                        targetX = secBounds.x;
                        targetY = secBounds.y;
                    }

                    ProcessBuilder pb = new ProcessBuilder(
                        "C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe",
                        "--kiosk", fallbackUrl,
                        "--window-position=" + targetX + "," + targetY,
                        "--edge-kiosk-type=fullscreen",
                        "--user-data-dir=C:\\KioskProfiles\\" + profileName,
                        "--disable-pinch",
                        "--no-first-run"
                    );
                    process = pb.start();
                }

                process.waitFor();

            } catch (Exception e) {
                System.err.println("Failed to execute Edge kiosk launcher: " + e.getMessage());
            } finally {
                SwingUtilities.invokeLater(() -> {
                    java.awt.Window window = SwingUtilities.getWindowAncestor(this);
                    if (window instanceof JFrame) {
                        ((JFrame) window).setAlwaysOnTop(true);
                        window.toFront();
                        window.requestFocus();
                    }
                });
            }
        }).start();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("A&F Computer Store - Kiosk Terminal");
            frame.setUndecorated(true);
            frame.setResizable(false);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            
            frame.setAlwaysOnTop(true);
            
            NewJPanel kioskPanel = new NewJPanel();
            frame.add(kioskPanel);

            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice[] screens = ge.getScreenDevices();

            Rectangle bounds;
            if (screens.length > 1) {
                bounds = screens[1].getDefaultConfiguration().getBounds();
            } else {
                bounds = ge.getDefaultScreenDevice().getDefaultConfiguration().getBounds();
            }

            frame.setLocation(bounds.x, bounds.y);
            frame.setSize(bounds.width, bounds.height);
            frame.setBounds(bounds);

            frame.setVisible(true);
            frame.toFront();
            frame.requestFocus();
        });
    }
}