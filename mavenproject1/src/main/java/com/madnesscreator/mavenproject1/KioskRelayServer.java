package com.madnesscreator.mavenproject1;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class KioskRelayServer {
    
    private static final int PORT = getPort();

    // Store active registered branches: Key = IP, Value = BranchName
    private static final ConcurrentHashMap<String, String> activeBranches = new ConcurrentHashMap<>();

    private static int getPort() {
        String envPort = System.getenv("PORT");
        if (envPort != null && !envPort.isEmpty()) {
            try {
                return Integer.parseInt(envPort);
            } catch (NumberFormatException e) {
                System.err.println("Invalid PORT environment variable. Falling back to 7070.");
            }
        }
        return 7070;
    }

    public static void main(String[] args) {
        System.out.println("Starting Kiosk Cloud Relay Server...");
        
        ExecutorService pool = Executors.newCachedThreadPool();

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Kiosk Cloud Relay Server active and listening on port " + PORT + "...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                pool.execute(new ClientHandler(clientSocket));
            }
        } catch (IOException e) {
            System.err.println("Server exception encountered on port " + PORT);
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
                if (requestLine == null) return;

                if (requestLine.contains("/api/register")) {
                    String clientIp = socket.getInetAddress().getHostAddress();
                    String branchName = "A&F Kiosk";

                    try {
                        String query = requestLine.split(" ")[1];
                        if (query.contains("?")) {
                            String[] params = query.split("\\?")[1].split("&");
                            for (String param : params) {
                                String[] kv = param.split("=");
                                if (kv.length == 2) {
                                    String key = kv[0];
                                    String val = java.net.URLDecoder.decode(kv[1], "UTF-8");
                                    if ("branchName".equals(key)) branchName = val;
                                    if ("clientIp".equals(key) && !val.isEmpty()) clientIp = val;
                                }
                            }
                        }
                    } catch (Exception ignored) {}

                    activeBranches.put(clientIp, branchName);

                    String httpResponse = "HTTP/1.1 200 OK\r\nContent-Type: text/plain\r\n\r\nOK";
                    out.write(httpResponse.getBytes());

                } else if (requestLine.contains("/api/branches")) {
                    StringBuilder json = new StringBuilder("[");
                    int i = 0;
                    for (ConcurrentHashMap.Entry<String, String> entry : activeBranches.entrySet()) {
                        if (i > 0) json.append(",");
                        json.append("{\"branchName\":\"").append(entry.getValue())
                            .append("\",\"clientIp\":\"").append(entry.getKey()).append("\"}");
                        i++;
                    }
                    json.append("]");

                    String httpResponse = "HTTP/1.1 200 OK\r\nContent-Type: application/json\r\n\r\n" + json.toString();
                    out.write(httpResponse.getBytes());
                } else {
                    String httpResponse = "HTTP/1.1 404 Not Found\r\n\r\n";
                    out.write(httpResponse.getBytes());
                }

            } catch (Exception e) {
                System.err.println("Error handling client connection: " + e.getMessage());
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    // Ignored
                }
            }
        }
    }
}
