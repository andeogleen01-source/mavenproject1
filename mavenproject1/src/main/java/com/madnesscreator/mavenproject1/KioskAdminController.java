package com.madnesscreator.mavenproject1;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class KioskAdminController {

    // 1. Ipadala ang settings, coordinates, at sukat mula sa Admin papunta sa Kiosk Client
    public static boolean sendSettingsToKiosk(String kioskIp, String branchName, String pricelistUrl, String registrationUrl, String clientTitle, String customNames, String customUrls, String customXs, String customYs, String priceX, String priceY, String regX, String regY) {
        try {
            String targetUrl = "http://" + kioskIp + ":8080/api/update";
            URL url = new URL(targetUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(4000);
            conn.setReadTimeout(4000);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

            String data = "branch_name=" + URLEncoder.encode(branchName, StandardCharsets.UTF_8.name())
                    + "&pricelist_url=" + URLEncoder.encode(pricelistUrl, StandardCharsets.UTF_8.name())
                    + "&registration_url=" + URLEncoder.encode(registrationUrl, StandardCharsets.UTF_8.name())
                    + "&client_title=" + URLEncoder.encode(clientTitle, StandardCharsets.UTF_8.name())
                    + "&custom_btn_names=" + URLEncoder.encode(customNames, StandardCharsets.UTF_8.name())
                    + "&custom_btn_urls=" + URLEncoder.encode(customUrls, StandardCharsets.UTF_8.name())
                    + "&custom_btn_xs=" + URLEncoder.encode(customXs, StandardCharsets.UTF_8.name())
                    + "&custom_btn_ys=" + URLEncoder.encode(customYs, StandardCharsets.UTF_8.name())
                    + "&pricelist_x=" + URLEncoder.encode(priceX, StandardCharsets.UTF_8.name())
                    + "&pricelist_y=" + URLEncoder.encode(priceY, StandardCharsets.UTF_8.name())
                    + "&registration_x=" + URLEncoder.encode(regX, StandardCharsets.UTF_8.name())
                    + "&registration_y=" + URLEncoder.encode(regY, StandardCharsets.UTF_8.name());

            try (OutputStream os = conn.getOutputStream();
                 PrintWriter pw = new PrintWriter(new OutputStreamWriter(os, StandardCharsets.UTF_8))) {
                pw.print(data);
                pw.flush();
            }

            return conn.getResponseCode() == 200;
        } catch (Exception e) {
            System.err.println("Failed to send settings to kiosk: " + e.getMessage());
            return false;
        }
    }

    // 2. [BIDIRECTIONAL FEATURE] Kuhanin ang tumpak na live config at metrics mula sa Kiosk Client /api/config
    public static Properties fetchKioskLiveConfig(String kioskIp) {
        Properties liveProps = new Properties();
        try {
            String targetUrl = "http://" + kioskIp + ":8080/api/config";
            URL url = new URL(targetUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);

            if (conn.getResponseCode() == 200) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }

                    // I-parse ang JSON response galing sa server patungong Properties object
                    String jsonClean = sb.toString().replace("{", "").replace("}", "").replace("\"", "");
                    for (String entry : jsonClean.split(",")) {
                        String[] kv = entry.split(":", 2);
                        if (kv.length == 2) {
                            liveProps.setProperty(kv[0].trim(), kv[1].trim());
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to fetch live config from kiosk " + kioskIp + ": " + e.getMessage());
        }
        return liveProps;
    }

    public static boolean sendBackgroundImageToKiosk(String kioskIp, File imageFile) {
        return sendFileToKiosk(kioskIp, imageFile, "background.png");
    }

    public static boolean sendClientIconToKiosk(String kioskIp, File iconFile) {
        return sendFileToKiosk(kioskIp, iconFile, "logo.png");
    }

    public static boolean sendCustomButtonImageToKiosk(String kioskIp, File imgFile, String fileName) {
        return sendFileToKiosk(kioskIp, imgFile, fileName);
    }

    private static boolean sendFileToKiosk(String kioskIp, File file, String remoteFileName) {
        try {
            String targetUrl = "http://" + kioskIp + ":8080/api/upload?filename=" + remoteFileName;
            URL url = new URL(targetUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(4000);
            conn.setReadTimeout(4000);
            conn.setRequestProperty("Content-Type", "application/octet-stream");

            try (OutputStream os = conn.getOutputStream();
                 FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
                os.flush();
            }

            return conn.getResponseCode() == 200;
        } catch (Exception e) {
            System.err.println("Failed to send file " + remoteFileName + " to kiosk: " + e.getMessage());
            return false;
        }
    }
}