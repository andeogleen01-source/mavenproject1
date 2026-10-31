package com.madnesscreator.mavenproject1;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Properties;

public class KioskConfigServer {

    private HttpServer server;
    private static Runnable onConfigUpdatedCallback;

    public KioskConfigServer(int port, Runnable onUpdate) {
        onConfigUpdatedCallback = onUpdate;
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/api/config", new ConfigGetHandler());
            server.createContext("/api/update", new ConfigUpdateHandler());
            server.createContext("/api/revert", new SafeModeRevertHandler());
            server.createContext("/api/upload", new FileUploadHandler()); // Endpoint para sa Admin file uploads
            server.createContext("/kiosk_config/", new StaticFileHandler());
            server.setExecutor(null);
            server.start();
            System.out.println("KioskConfigServer started successfully on port " + port);
        } catch (IOException e) {
            System.err.println("Failed to start KioskConfigServer: " + e.getMessage());
        }
    }

    private static void backupCurrentSettings() {
        try {
            File dir = new File("kiosk_config");
            File source = new File(dir, "settings.properties");
            File backup = new File(dir, "settings_backup.properties");
            if (source.exists()) {
                Files.copy(source.toPath(), backup.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            System.err.println("Failed to create settings backup: " + e.getMessage());
        }
    }

    // 1. Handler para ibigay ang LAHAT ng live settings, sukat ng screen, at button dimensions sa Admin Panel
    private static class ConfigGetHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            File file = new File("kiosk_config/settings.properties");
            
            Properties props = new Properties();
            if (file.exists()) {
                try (FileInputStream in = new FileInputStream(file)) {
                    props.load(in);
                } catch (Exception ignored) {}
            }

            // Maglagay ng default values kung sakaling kulang o wala pa sa properties file
            if (!props.containsKey("client_width")) props.setProperty("client_width", "1920");
            if (!props.containsKey("client_height")) props.setProperty("client_height", "1080");
            if (!props.containsKey("grid_columns")) props.setProperty("grid_columns", "16");
            if (!props.containsKey("grid_rows")) props.setProperty("grid_rows", "9");
            if (!props.containsKey("pricelist_width")) props.setProperty("pricelist_width", "340");
            if (!props.containsKey("pricelist_height")) props.setProperty("pricelist_height", "230");
            if (!props.containsKey("registration_width")) props.setProperty("registration_width", "340");
            if (!props.containsKey("registration_height")) props.setProperty("registration_height", "230");
            if (!props.containsKey("custom_btn_width")) props.setProperty("custom_btn_width", "220");
            if (!props.containsKey("custom_btn_height")) props.setProperty("custom_btn_height", "220");

            StringBuilder sb = new StringBuilder("{");
            for (String name : props.stringPropertyNames()) {
                sb.append("\"").append(name).append("\":\"").append(props.getProperty(name)).append("\",");
            }
            if (props.size() > 0) sb.deleteCharAt(sb.length() - 1);
            sb.append("}");

            String response = sb.toString();
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
                os.flush();
            }
        }
    }

    // 2. Handler para tanggapin ang POST updates mula sa Admin Panel
    private static class ConfigUpdateHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                backupCurrentSettings();

                InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
                BufferedReader reader = new BufferedReader(isr);
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }

                String rawBody = sb.toString();
                String decoded = URLDecoder.decode(rawBody, "UTF-8");

                File dir = new File("kiosk_config");
                if (!dir.exists()) dir.mkdirs();
                File file = new File(dir, "settings.properties");
                
                Properties props = new Properties();
                if (file.exists()) {
                    try (FileInputStream in = new FileInputStream(file)) {
                        props.load(in);
                    }
                }

                for (String param : decoded.split("&")) {
                    String[] pair = param.split("=", 2);
                    if (pair.length == 2) {
                        props.setProperty(pair[0], pair[1]);
                    }
                }

                try (FileOutputStream out = new FileOutputStream(file)) {
                    props.store(out, "Updated via KioskConfigServer from Admin Panel");
                }

                String response = "SUCCESS";
                exchange.sendResponseHeaders(200, response.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }

                if (onConfigUpdatedCallback != null) {
                    onConfigUpdatedCallback.run();
                }
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }

    // 3. Handler para sa File Uploads (Background, Logo, Button Images mula sa Admin)
    private static class FileUploadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                String query = exchange.getRequestURI().getQuery();
                String fileName = "uploaded_file.png";
                if (query != null && query.contains("filename=")) {
                    for (String param : query.split("&")) {
                        if (param.startsWith("filename=")) {
                            fileName = param.substring(9);
                        }
                    }
                }

                File dir = new File("kiosk_config");
                if (!dir.exists()) dir.mkdirs();
                File targetFile = new File(dir, fileName);

                try (InputStream is = exchange.getRequestBody();
                     FileOutputStream fos = new FileOutputStream(targetFile)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, bytesRead);
                    }
                }

                String response = "SUCCESS";
                exchange.sendResponseHeaders(200, response.getBytes().length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }

                if (onConfigUpdatedCallback != null) {
                    onConfigUpdatedCallback.run();
                }
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }

    // 4. Handler para sa Safe Mode Revert
    private static class SafeModeRevertHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            File dir = new File("kiosk_config");
            File backup = new File(dir, "settings_backup.properties");
            File target = new File(dir, "settings.properties");

            if (backup.exists()) {
                try {
                    Files.copy(backup.toPath(), target.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    String response = "REVERTED";
                    exchange.sendResponseHeaders(200, response.getBytes().length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(response.getBytes());
                    }
                    if (onConfigUpdatedCallback != null) {
                        onConfigUpdatedCallback.run();
                    }
                    return;
                } catch (Exception ignored) {}
            }

            exchange.sendResponseHeaders(404, -1);
        }
    }

    // 5. Handler para i-serve ang mga lokal na file sa Admin Panel Preview
    private static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            String path = exchange.getRequestURI().getPath().replace("/kiosk_config/", "");
            File file = new File("kiosk_config/" + path);

            if (file.exists() && !file.isDirectory()) {
                byte[] bytes = Files.readAllBytes(file.toPath());
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } else {
                exchange.sendResponseHeaders(404, -1);
            }
        }
    }
}