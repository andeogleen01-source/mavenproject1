package com.madnesscreator.mavenproject1;

import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class Mavenproject1 {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Kiosk Application");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setUndecorated(true); // Optional: removes title bar for true kiosk mode
            
            // Add your custom panel
            frame.add(new NewJPanel());

            // Detect monitors and position on the second screen
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice[] screens = ge.getScreenDevices();

            if (screens.length > 1) {
                // Get bounds of the second monitor
                Rectangle secondScreenBounds = screens[1].getDefaultConfiguration().getBounds();
                frame.setLocation(secondScreenBounds.x, secondScreenBounds.y);
                frame.setSize(secondScreenBounds.width, secondScreenBounds.height);
            } else {
                // Fallback to primary screen if only one monitor is connected
                frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            }

            frame.setVisible(true);
        });
    }
}