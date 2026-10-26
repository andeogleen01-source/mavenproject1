package com.madnesscreator.mavenproject1;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

public class KioskRelayServer {

    // Thread-safe map to store active branches simultaneously: Key = BranchName, Value = Branch Data
    private static final ConcurrentHashMap<String, BranchInfo> activeBranches = new ConcurrentHashMap<>();

    public static void main(String[] args) throws IOException {
        int port = 7070; // Relay server port
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        
        // CRITICAL: Enables concurrent multi-threading so multiple clients can ping at the exact same time
        server.setExecutor(Executors.newCachedThreadPool());

        // Endpoint for remote clients to send simultaneous heartbeat/registration pings
        server.createContext("/api/register", new RegisterHandler());
        
        // Endpoint for the Admin panel to fetch the live list of all active branches
        server.createContext("/api/branches", new GetBranchesHandler());

        server.start();
        System.out.println("Kiosk Cloud Relay Server active and listening concurrently on port " + port + "...");
    }

    static class BranchInfo {
        String branchName;
        String clientIp;
        long lastSeen;

        public BranchInfo(String branchName, String clientIp, long lastSeen) {
            this.branchName = branchName;
            this.clientIp = clientIp;
            this.lastSeen = lastSeen;
        }
    }

    // Handles simultaneous incoming registrations from any branch
    static class RegisterHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                try (InputStream is = exchange.getRequestBody()) {
                    String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    
                    String branchName = extractJsonField(body, "branchName");
                    String clientIp = extractJsonField(body, "clientIp");

                    if (branchName != null && !branchName.isEmpty()) {
                        activeBranches.put(branchName, new BranchInfo(branchName, clientIp, System.currentTimeMillis()));
                    }
                }
                String response = "Registered successfully";
                exchange.sendResponseHeaders(200, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes());
                }
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }

    // Handles requests from the Admin panel to get all online branches
    static class GetBranchesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                long now = System.currentTimeMillis();
                
                // Automatically clean up branches that haven't sent a heartbeat in 15 seconds (marked offline)
                activeBranches.entrySet().removeIf(entry -> now - entry.getValue().lastSeen > 15000);

                // Build lightweight JSON response array for the admin panel
                StringBuilder json = new StringBuilder("[");
                int i = 0;
                for (BranchInfo b : activeBranches.values()) {
                    if (i > 0) json.append(",");
                    json.append("{\"branchName\":\"").append(b.branchName)
                        .append("\",\"clientIp\":\"").append(b.clientIp).append("\"}");
                    i++;
                }
                json.append("]");

                exchange.getResponseHeaders().set("Content-Type", "application/json");
                byte[] responseBytes = json.toString().getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, responseBytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseBytes);
                }
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }

    private static String extractJsonField(String json, String field) {
        try {
            String search = "\"" + field + "\":\"";
            int start = json.indexOf(search);
            if (start == -1) return "";
            start += search.length();
            int end = json.indexOf("\"", start);
            return json.substring(start, end);
        } catch (Exception e) {
            return "";
        }
    }
}