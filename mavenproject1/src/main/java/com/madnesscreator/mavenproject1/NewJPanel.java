package com.madnesscreator.mavenproject1;

import java.awt.AWTEvent;
import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;
import javax.imageio.ImageIO;
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
    private JPanel floatingClosePanel;
    private BufferedImage backgroundImage;
    private int secretExitTaps = 0;
    private Timer idleTimer;

    // Inactivity timeout in milliseconds (60,000 ms = 60 seconds)
    private static final int IDLE_TIMEOUT = 60000;

    private final Color ACCENT_GOLD = new Color(241, 196, 15);     
    private final Color TEXT_WHITE = new Color(255, 255, 255);          

    public NewJPanel() {
        this.setLayout(new BorderLayout());
        this.setOpaque(false);
        
        layeredPane = new JLayeredPane();
        this.add(layeredPane, BorderLayout.CENTER);

        // Hide mouse cursor arrow completely for touchscreen compliance
        BufferedImage cursorImg = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Cursor blankCursor = Toolkit.getDefaultToolkit().createCustomCursor(
                cursorImg, new Point(0, 0), "blank cursor"
        );
        this.setCursor(blankCursor);

        // Check for first-time installation / branch name setup
        checkFirstRunBranchSetup();

        // Load client window title and icon onto parent frame
        SwingUtilities.invokeLater(this::applyClientBranding);

        loadLocalBackground();
        buildMainMenuLayer();
        buildFloatingClosePanel();

        layeredPane.add(mainMenuPanel, JLayeredPane.DEFAULT_LAYER);

        // Setup the automatic idle detector to kill Edge on timeout
        setupIdleDetector();

        // Start background network server to listen for Admin App remote config pushes
        KioskConfigServer server = new KioskConfigServer(() -> {
            loadLocalBackground(); // Reload background image on network update
            applyClientBranding(); // Refresh branding/title on network update
            repaint();
        });
        server.startServer();
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
                BufferedImage originalIcon = ImageIO.read(iconFile);
                java.awt.Image scaledIcon = originalIcon.getScaledInstance(32, 32, java.awt.Image.SCALE_SMOOTH);
                ((JFrame) window).setIconImage(scaledIcon);
            }
        } catch (Exception e) {
            System.err.println("Failed to apply client branding: " + e.getMessage());
        }
    }

    private void loadLocalBackground() {
        new Thread(() -> {
            try {
                File bgFile = new File("kiosk_config/background.png");
                if (bgFile.exists()) {
                    backgroundImage = ImageIO.read(bgFile);
                } else {
                    backgroundImage = null; 
                }
                SwingUtilities.invokeLater(() -> repaint());
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

    /**
     * Inactivity idle detector.
     * Resets when touchscreen/keyboard is used. If inactive for 60 seconds,
     * it force-closes Microsoft Edge and restores focus to the main menu.
     */
    private void setupIdleDetector() {
        idleTimer = new Timer(IDLE_TIMEOUT, e -> {
            SwingUtilities.invokeLater(() -> {
                java.awt.Window window = SwingUtilities.getWindowAncestor(this);
                if (window != null) {
                    window.toFront();
                    window.requestFocus();
                }

                try {
                    Runtime.getRuntime().exec("taskkill /F /IM msedge.exe");
                    System.err.println("Idle timeout reached: Force-closed Edge and returned to menu.");
                } catch (Exception ex) {
                    System.err.println("Failed to force-close Edge: " + ex.getMessage());
                }

                layeredPane.revalidate();
                layeredPane.repaint();
            });
        });
        idleTimer.setRepeats(true);
        idleTimer.start();

        // Listen globally across the application to reset the idle countdown
        Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener() {
            @Override
            public void eventDispatched(AWTEvent event) {
                if (idleTimer != null) {
                    idleTimer.restart();
                }
            }
        }, AWTEvent.MOUSE_EVENT_MASK | AWTEvent.KEY_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);
    }

    @Override
    public void doLayout() {
        super.doLayout();
        mainMenuPanel.setBounds(0, 0, getWidth(), getHeight());
        
        int btnWidth = 240;
        int btnHeight = 60;
        int padding = 25;
        floatingClosePanel.setBounds(getWidth() - btnWidth - padding, padding, btnWidth, btnHeight);
    }

    private void buildMainMenuLayer() {
        mainMenuPanel = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                if (backgroundImage != null) {
                    g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
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

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 40, 0, 40); 
        gbc.gridx = 0;
        gbc.gridy = 0;

        // Pricelist Menu Button
        JButton pricelistBtn = createStyledButton("Pricelist Menu");
        pricelistBtn.addActionListener(e -> {
            String defaultSheetUrl = "https://docs.google.com/spreadsheets/d/1AncL3i3fKC3IEvIXtPYmAplX1oee9CTm5I7yNmocsuM/edit?gid=1864218333#gid=1864218333";
            String targetUrl = getSavedUrl("pricelist_url", defaultSheetUrl);
            launchEdgeKioskScript("launch_pricelist.bat", targetUrl, "EdgePricelist");
        });
        mainMenuPanel.add(pricelistBtn, gbc);

        gbc.gridx = 1;
        // Client Registration Button
        JButton registrationBtn = createStyledButton("Client Registration");
        registrationBtn.addActionListener(e -> {
            String defaultFormUrl = "https://docs.google.com/forms/d/e/1FAIpQLScf4maR6JYgOIuceat-F43K6Exhn8-wqwvFBPhNo4tU7chZDA/viewform";
            String targetUrl = getSavedUrl("registration_url", defaultFormUrl);
            launchEdgeKioskScript("launch_registration.bat", targetUrl, "EdgeRegistration");
        });
        mainMenuPanel.add(registrationBtn, gbc);
    }

    private void buildFloatingClosePanel() {
        floatingClosePanel = new JPanel(new BorderLayout());
        floatingClosePanel.setOpaque(false);

        JButton closeBtn = new JButton("✖ Back to Menu") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.90f));
                g2d.setColor(getModel().isPressed() ? new Color(192, 57, 43) : new Color(231, 76, 60)); 
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                
                g2d.dispose();
                super.paintComponent(g);
            }
        };
        closeBtn.setFont(new Font("Segoe UI", Font.BOLD, 15));
        closeBtn.setForeground(TEXT_WHITE);
        closeBtn.setBorderPainted(false);
        closeBtn.setFocusPainted(false);
        closeBtn.setContentAreaFilled(false);
        
        floatingClosePanel.add(closeBtn, BorderLayout.CENTER);
    }

    private JButton createStyledButton(String text) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.88f));
                g2d.setColor(getModel().isPressed() ? new Color(20, 24, 30) : new Color(30, 35, 45));
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 24, 24);
                
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
                g2d.setColor(getModel().isRollover() ? ACCENT_GOLD.brighter() : ACCENT_GOLD);
                g2d.setStroke(new java.awt.BasicStroke(3.5f));
                g2d.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 24, 24);
                
                g2d.dispose();
                super.paintComponent(g);
            }
        };
        
        button.setFont(new Font("Segoe UI", Font.BOLD, 22));
        button.setForeground(TEXT_WHITE);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setContentAreaFilled(false); 
        button.setPreferredSize(new Dimension(340, 140)); 
        return button;
    }

    private void launchEdgeKioskScript(String scriptName, String fallbackUrl, String profileName) {
        new Thread(() -> {
            try {
                File scriptFile = new File(scriptName);
                Process process;
                
                if (scriptFile.exists()) {
                    ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", scriptFile.getAbsolutePath());
                    process = pb.start();
                } else {
                    String command = "cmd.exe /c start /wait \"\" \"C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe\" "
                            + fallbackUrl + " "
                            + "--window-position=1920,0 --kiosk --edge-kiosk-type=fullscreen "
                            + "--user-data-dir=\"C:\\" + profileName + "\" --disable-pinch --no-first-run";
                    process = Runtime.getRuntime().exec(command);
                }
                
                process.waitFor();

                SwingUtilities.invokeLater(() -> {
                    java.awt.Window window = SwingUtilities.getWindowAncestor(this);
                    if (window != null) {
                        window.toFront();
                        window.requestFocus();
                    }
                    layeredPane.revalidate();
                    layeredPane.repaint();
                });

            } catch (Exception e) {
                System.err.println("Failed to execute Edge kiosk launcher for " + scriptName + ": " + e.getMessage());
            }
        }).start();
    }
}