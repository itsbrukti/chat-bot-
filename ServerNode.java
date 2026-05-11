import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class ServerNode {
    
    // Use CopyOnWriteArrayList for thread-safe iteration
    private static List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private int port;
    private String dbName;
    
    public ServerNode(int port, String dbName) {
        this.port = port;
        this.dbName = dbName;
    }
    
    public void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("🚀 Server running on port " + port);
            System.out.println("📡 Sync port: " + (port + 1000));
            
            // Start sync server
            new Thread(new SyncManager(port + 1000, dbName)).start();
            
            while (true) {
                Socket socket = serverSocket.accept();
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
        System.out.println("📢 Broadcasting to " + clients.size() + " clients: " + message);
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
        
        // Server 1 on port 1234
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
        
        // Server 2 on port 1235
        new Thread(() -> {
            ServerNode server2 = new ServerNode(1235, "chat_system_2");
            server2.startServer();
        }).start();
        
        System.out.println("=================================");
        System.out.println("✅ Both servers are running!");
        System.out.println("Server 1: Port 1234 (Sync: 2234)");
        System.out.println("Server 2: Port 1235 (Sync: 2235)");
        System.out.println("=================================");
    }
}