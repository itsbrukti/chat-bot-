import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ServerNode {
    
    private static List<ClientHandler> clients = new ArrayList<>();
    private int port;
    private String dbName;
    
    public ServerNode(int port, String dbName) {
        this.port = port;
        this.dbName = dbName;
    }
    
    public void startServer() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("🚀 Server running on port " + port);
            
            // Start sync server
            new Thread(new SyncManager(port + 1000, dbName)).start();
            
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("✅ Client connected to server " + port);
                ClientHandler handler = new ClientHandler(socket, dbName, port);
                new Thread(handler).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static synchronized void addClient(ClientHandler client) {
        clients.add(client);
        System.out.println("📱 Client added: " + client.getUsername() + " | Total: " + clients.size());
    }
    
    public static synchronized void removeClient(ClientHandler client) {
        clients.remove(client);
        System.out.println("👋 Client removed: " + client.getUsername() + " | Total: " + clients.size());
    }
    
    public static synchronized List<ClientHandler> getClients() {
        return new ArrayList<>(clients);
    }
    
    public static synchronized void broadcastToAll(String message) {
        for (ClientHandler client : clients) {
            client.send(message);
        }
        System.out.println("📢 Broadcast to " + clients.size() + " clients: " + message);
    }
    
    public static synchronized void broadcast(String message, ClientHandler sender) {
        for (ClientHandler client : clients) {
            if (client != sender) {
                client.send(message);
            }
        }
    }
    
    public static void main(String[] args) {
        System.out.println("🎯 Starting Chat Servers...");
        
        // Server 1
        new Thread(() -> new ServerNode(1234, "chat_system_1").startServer()).start();
        
        // Server 2  
        new Thread(() -> new ServerNode(1235, "chat_system_2").startServer()).start();
    }
}