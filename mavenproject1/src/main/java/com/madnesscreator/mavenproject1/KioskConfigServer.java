package com.madnesscreator.mavenproject1;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import javax.swing.SwingUtilities;

public class KioskConfigServer {

    private final Runnable onConfigUpdated;

    // Must match the Relay Server URL and port (Change to your cloud URL when deployed live)
    private static final String RELAY_SERVER_URL = "http://localhost:7070";

    public KioskConfigServer(Runnable onConfigUpdated) {
        this.onConfigUpdated = onConfigUpdated;
    }

    public void startServer() {
        new Thread(() -> {
            try {
                HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
                server.createContext("/update-settings", new SettingsHandler());
                server.createContext("/update-background", new BackgroundHandler());
                server.createContext("/update-icon", new IconHandler());
                server.createContext("/", new RootHandler());
                server.setExecutor(null); 
                server.start();
                System.out.println("Kiosk Network Config Server active on port 8080...");

                // Start continuous background auto-registration loop pointing to the Relay Server
                startRelayRegistrationLoop();

            } catch (IOException e) {
                System.err.println("Failed to start Kiosk Config Server: " + e.getMessage());
            }
        }).start();
    }

    private class RootHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = "A&F Kiosk Client Active";
            exchange.sendResponseHeaders(200, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    private class SettingsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                InputStream is = exchange.getRequestBody();
                Properties props = new Properties();

                File dir = new File("kiosk_config");
                if (!dir.exists()) dir.mkdirs();
                File configFile = new File(dir, "settings.properties");

                if (configFile.exists()) {
                    try (FileInputStream fis = new FileInputStream(configFile)) {
                        props.load(fis);
                    }
                }

                Properties newProps = new Properties();
                newProps.load(is);
                props.putAll(newProps);

                try (FileOutputStream fos = new FileOutputStream(configFile)) {
                    props.store(fos, "Updated via Admin App over Network");
                }

                String response = "Settings updated successfully.";
                exchange.sendResponseHeaders(200, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();

                if (onConfigUpdated != null) {
                    SwingUtilities.invokeLater(onConfigUpdated);
                }
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }

    private class BackgroundHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                File dir = new File("kiosk_config");
                if (!dir.exists()) dir.mkdirs();

                File bgFile = new File(dir, "background.png");
                try (InputStream is = exchange.getRequestBody();
                     FileOutputStream fos = new FileOutputStream(bgFile)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, bytesRead);
                    }
                }

                String response = "Background image updated successfully.";
                exchange.sendResponseHeaders(200, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();

                if (onConfigUpdated != null) {
                    SwingUtilities.invokeLater(onConfigUpdated);
                }
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }

    private class IconHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                File dir = new File("kiosk_config");
                if (!dir.exists()) dir.mkdirs();

                File iconFile = new File(dir, "logo.png");
                try (InputStream is = exchange.getRequestBody();
                     FileOutputStream fos = new FileOutputStream(iconFile)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, bytesRead);
                    }
                }

                String response = "Client window icon updated successfully.";
                exchange.sendResponseHeaders(200, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();

                if (onConfigUpdated != null) {
                    SwingUtilities.invokeLater(onConfigUpdated);
                }
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }

    /**
     * Continuously pings the Relay Server every 5 seconds so remote branches 
     * automatically register themselves without manual IP setup.
     */
    private static void startRelayRegistrationLoop() {
        new Thread(() -> {
            while (true) {
                try {
                    String branchName = "A&F Kiosk Terminal";
                    File file = new File("kiosk_config/settings.properties");
                    if (file.exists()) {
                        try (FileInputStream in = new FileInputStream(file)) {
                            Properties props = new Properties();
                            props.load(in);
                            branchName = props.getProperty("branch_name", "A&F Kiosk Terminal");
                        }
                    }

                    String localIp = InetAddress.getLocalHost().getHostAddress();
                    
                    URL url = new URL(RELAY_SERVER_URL + "/api/register");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setDoOutput(true);
                    conn.setConnectTimeout(3000);
                    conn.setRequestProperty("Content-Type", "application/json");
                    
                    String jsonPayload = "{\"branchName\":\"" + branchName + "\", \"clientIp\":\"" + localIp + "\"}";
                    try (OutputStream os = conn.getOutputStream()) {
                        os.write(jsonPayload.getBytes(StandardCharsets.UTF_8));
                    }
                    
                    conn.getResponseCode(); 
                } catch (Exception e) {
                    // Relay server unreachable; will automatically retry on the next cycle
                }
                
                try {
                    Thread.sleep(5000); // Pulse every 5 seconds
                } catch (InterruptedException ignored) {}
            }
        }).start();
    }
}