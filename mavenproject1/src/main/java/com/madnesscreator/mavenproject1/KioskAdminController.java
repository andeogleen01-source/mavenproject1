package com.madnesscreator.mavenproject1;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Properties;

public class KioskAdminController {

    public static boolean sendSettingsToKiosk(String kioskIp, String branchName, String pricelistUrl, String registrationUrl, String clientTitle) {
        try {
            URL url = new URL("http://" + kioskIp + ":8080/update-settings");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(2000);

            Properties props = new Properties();
            props.setProperty("branch_name", branchName);
            props.setProperty("pricelist_url", pricelistUrl);
            props.setProperty("registration_url", registrationUrl);
            if (clientTitle != null && !clientTitle.trim().isEmpty()) {
                props.setProperty("client_title", clientTitle.trim());
            }

            try (OutputStream os = conn.getOutputStream()) {
                props.store(os, "Admin Push Update");
            }

            return conn.getResponseCode() == 200;
        } catch (Exception e) {
            System.err.println("Failed to send settings to kiosk " + kioskIp + ": " + e.getMessage());
            return false;
        }
    }

    public static boolean sendBackgroundImageToKiosk(String kioskIp, File imageFile) {
        try {
            URL url = new URL("http://" + kioskIp + ":8080/update-background");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            conn.setRequestProperty("Content-Type", "image/png");

            try (FileInputStream fis = new FileInputStream(imageFile);
                 OutputStream os = conn.getOutputStream()) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
            }

            return conn.getResponseCode() == 200;
        } catch (Exception e) {
            System.err.println("Failed to send background to kiosk " + kioskIp + ": " + e.getMessage());
            return false;
        }
    }

    public static boolean sendClientIconToKiosk(String kioskIp, File iconFile) {
        try {
            URL url = new URL("http://" + kioskIp + ":8080/update-icon");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);
            conn.setRequestProperty("Content-Type", "image/png");

            try (FileInputStream fis = new FileInputStream(iconFile);
                 OutputStream os = conn.getOutputStream()) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
            }

            return conn.getResponseCode() == 200;
        } catch (Exception e) {
            System.err.println("Failed to send icon to kiosk " + kioskIp + ": " + e.getMessage());
            return false;
        }
    }
}