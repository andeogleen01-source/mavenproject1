package com.madnesscreator.mavenproject1;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class KioskRelayServer {
    
    // Read Render's assigned port from the environment, default to 7070 for local testing
    private static final int PORT = getPort();

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
        
        // Thread pool to handle concurrent Admin Control Center and remote kiosk connections
        ExecutorService pool = Executors.newCachedThreadPool();

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Kiosk Cloud Relay Server active and listening concurrently on port " + PORT + "...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress());
                
                // Hand off the connection to a dedicated worker thread
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
            try {
                System.out.println("Handling communication stream for: " + socket.getRemoteSocketAddress());
            } catch (Exception e) {
                System.err.println("Error handling client connection: " + e.getMessage());
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    // Ignored on close
                }
            }
        }
    }
}
