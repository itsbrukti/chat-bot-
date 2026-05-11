import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.io.*;

public class ServerNode {
    
    private static List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private int port;
    private String dbName;
    
    public ServerNode(int port, String dbName) {
        this.port = port;
        this.dbName = dbName;
    }
    
    private boolean isFailoverActive() {
        File markerFile = new File("failover_mode.txt");
        return markerFile.exists();
    }
    
    public void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            String serverType = (port == 1234) ? "MAIN" : "BACKUP";
            
            if (isFailoverActive() && port == 1234) {
                System.out.println("🔴 MAIN Server running on port " + port + " - BUT FAILOVER IS ACTIVE!");
                System.out.println("⚠️ This server will NOT accept registrations or sync data");
            } else {
                System.out.println("🚀 " + serverType + " Server running on port " + port);
            }
            System.out.println("📡 Sync port: " + (port + 1000));
            
            // Start sync server
            new Thread(new SyncManager(port + 1000, dbName)).start();
            
            while (true) {
                Socket socket = serverSocket.accept();
                
                if (isFailoverActive() && port == 1234) {
                    System.out.println("⚠️ Connection attempt to FAILED main server on port " + port);
                    // Still accept but warn that operations will be blocked
                }
                
                System.out.println("✅ New connection accepted on port " + port);
                ClientHandler handler = new ClientHandler(socket, dbName, port);
                Thread clientThread = new Thread(handler);
                clientThread.start();
            }
        } catch (Exception e) {
            System.err.println("❌ Server error on port " + port);
            e.printStackTrace();
        }
    }
    
    public static synchronized void addClient(ClientHandler client) {
        if (client != null && client.getUsername() != null) {
            clients.add(client);
            System.out.println("📱 Client added: " + client.getUsername() + " | Total: " + clients.size());
        } else {
            System.out.println("⚠️ Attempted to add client with null username");
        }
    }
    
    public static synchronized void removeClient(ClientHandler client) {
        if (client != null && client.getUsername() != null) {
            clients.remove(client);
            System.out.println("👋 Client removed: " + client.getUsername() + " | Total: " + clients.size());
        } else {
            clients.remove(client);
            System.out.println("👋 Client removed (unknown) | Total: " + clients.size());
        }
    }
    
    public static synchronized List<ClientHandler> getClients() {
        return new ArrayList<>(clients);
    }
    
    public static synchronized void broadcastToAll(String message) {
        System.out.println("📢 Broadcasting to " + clients.size() + " clients");
        for (ClientHandler client : clients) {
            try {
                client.send(message);
            } catch (Exception e) {
                System.err.println("❌ Failed to send message to client: " + client.getUsername());
            }
        }
    }
    
    public static synchronized void broadcast(String message, ClientHandler sender) {
        System.out.println("📢 Broadcasting (excluding sender) to " + clients.size() + " clients");
        for (ClientHandler client : clients) {
            if (client != sender && client.getUsername() != null) {
                try {
                    client.send(message);
                } catch (Exception e) {
                    System.err.println("❌ Failed to send message to client: " + client.getUsername());
                }
            }
        }
    }
    
    public static void main(String[] args) {
        System.out.println("🎯 Starting Chat Servers...");
        System.out.println("=================================");
        
        // Check if failover is active
        File failoverFile = new File("failover_mode.txt");
        if (failoverFile.exists()) {
            System.out.println("⚠️⚠️⚠️ FAILOVER MODE IS ACTIVE ⚠️⚠️⚠️");
            System.out.println("Main database (port 1234) will NOT accept writes!");
            System.out.println("All operations will go to backup database (port 1235)");
            System.out.println("=================================");
        }
        
        // Server 1 on port 1234 (MAIN)
        new Thread(() -> {
            ServerNode server1 = new ServerNode(1234, "chat_system_1");
            server1.startServer();
        }).start();
        
        // Small delay to ensure server 1 starts properly
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        // Server 2 on port 1235 (BACKUP)
        new Thread(() -> {
            ServerNode server2 = new ServerNode(1235, "chat_system_2");
            server2.startServer();
        }).start();
        
        System.out.println("=================================");
        if (failoverFile.exists()) {
            System.out.println("✅ Servers running in FAILOVER MODE!");
            System.out.println("Server 1 (MAIN): Port 1234 - BLOCKED (Simulated Failure)");
            System.out.println("Server 2 (BACKUP): Port 1235 - ACTIVE");
        } else {
            System.out.println("✅ Both servers running in NORMAL MODE!");
            System.out.println("Server 1 (MAIN): Port 1234 (Sync: 2234)");
            System.out.println("Server 2 (BACKUP): Port 1235 (Sync: 2235)");
        }
        System.out.println("=================================");
    }
}