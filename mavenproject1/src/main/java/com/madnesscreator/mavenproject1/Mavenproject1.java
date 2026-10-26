package com.madnesscreator.mavenproject1;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class Mavenproject1 {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("A&F Computer Store - Kiosk Terminal");
            
            // True Kiosk Mode: Remove borders and title bar completely
            frame.setUndecorated(true);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            
            // Add your custom kiosk client panel
            frame.add(new NewJPanel());

            // Detect monitors and position strictly on the second screen
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice[] screens = ge.getScreenDevices();

            if (screens.length > 1) {
                // Pin exclusively to the second monitor (Index 1)
                GraphicsDevice secondScreen = screens[1];
                Rectangle bounds = secondScreen.getDefaultConfiguration().getBounds();
                
                frame.setLocation(bounds.x, bounds.y);
                frame.setSize(bounds.width, bounds.height);
                frame.setBounds(bounds);
            } else {
                // Fallback to primary screen if only one monitor is connected
                GraphicsDevice defaultScreen = ge.getDefaultScreenDevice();
                Rectangle bounds = defaultScreen.getDefaultConfiguration().getBounds();
                frame.setLocation(bounds.x, bounds.y);
                frame.setSize(bounds.width, bounds.height);
                frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            }

            frame.setVisible(true);
            frame.toFront();
            frame.requestFocus();
        });
    }
}