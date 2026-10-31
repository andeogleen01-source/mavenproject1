package com.madnesscreator.mavenproject1;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class KioskRelayServer {
    
    private static final int PORT = getPort();
    
    // Store branch info along with the last heartbeat timestamp
    private static final ConcurrentHashMap<String, BranchInfo> activeBranches = new ConcurrentHashMap<>();

    private static class BranchInfo {
        String branchName;
        long lastSeen;

        BranchInfo(String branchName, long lastSeen) {
            this.branchName = branchName;
            this.lastSeen = lastSeen;
        }
    }

    private static int getPort() {
        String envPort = System.getenv("PORT");
        if (envPort != null && !envPort.isEmpty()) {
            try {
                return Integer.parseInt(envPort);
            } catch (NumberFormatException e) {
                System.err.println("Invalid PORT. Falling back to 7070.");
            }
        }
        return 7070;
    }

    public static void main(String[] args) {
        System.out.println("Starting Kiosk Cloud Relay Server on port " + PORT);
        ExecutorService pool = Executors.newCachedThreadPool();

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Relay Server is running successfully!");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                pool.execute(new ClientHandler(clientSocket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            pool.shutdown();
        }
    }

    private static class ClientHandler implements Runnable {
        private final Socket socket;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 OutputStream out = socket.getOutputStream()) {
                
                String requestLine = reader.readLine();
                if (requestLine == null || requestLine.isEmpty()) return;

                String clientIp = socket.getInetAddress().getHostAddress();
                if (clientIp.equals("0.0.0.0") || clientIp.equals("127.0.0.1")) {
                    clientIp = "Localhost";
                }

                if (requestLine.contains("/api/register")) {
                    String branchName = "A&F Kiosk Branch";

                    try {
                        String[] parts = requestLine.split(" ");
                        if (parts.length > 1) {
                            String uri = parts[1];
                            if (uri.contains("?")) {
                                String query = uri.split("\\?")[1];
                                for (String param : query.split("&")) {
                                    String[] kv = param.split("=");
                                    if (kv.length == 2) {
                                        String val = URLDecoder.decode(kv[1], "UTF-8");
                                        if ("branchName".equals(kv[0])) branchName = val;
                                        if ("clientIp".equals(kv[0]) && !val.isEmpty()) clientIp = val;
                                    }
                                }
                            }
                        }
                    } catch (Exception ignored) {}

                    // Update branch with current timestamp
                    activeBranches.put(clientIp, new BranchInfo(branchName, System.currentTimeMillis()));

                    String httpResponse = "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\nConnection: close\r\n\r\nOK";
                    out.write(httpResponse.getBytes());

                } else if (requestLine.contains("/api/branches")) {
                    long currentTime = System.currentTimeMillis();
                    
                    // Remove branches that haven't sent a heartbeat in the last 15 seconds
                    activeBranches.entrySet().removeIf(entry -> (currentTime - entry.getValue().lastSeen) > 15000);

                    StringBuilder json = new StringBuilder("[");
                    int i = 0;
                    for (ConcurrentHashMap.Entry<String, BranchInfo> entry : activeBranches.entrySet()) {
                        if (i > 0) json.append(",");
                        json.append("{\"branchName\":\"").append(entry.getValue().branchName)
                            .append("\",\"clientIp\":\"").append(entry.getKey()).append("\"}");
                        i++;
                    }
                    json.append("]");

                    String httpResponse = "HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nConnection: close\r\n\r\n" + json.toString();
                    out.write(httpResponse.getBytes());
                } else {
                    String httpResponse = "HTTP/1.1 200 OK\r\nConnection: close\r\n\r\nAlive";
                    out.write(httpResponse.getBytes());
                }

            } catch (Exception ignored) {
            } finally {
                try { socket.close(); } catch (IOException ignored) {}
            }
        }
    }
}