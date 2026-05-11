import java.io.*;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class ClientHandler implements Runnable {
    
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String username;
    private String role;
    private DatabaseManager db;
    private int serverPort;
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
    private boolean authenticated = false;
    
    public ClientHandler(Socket socket, String dbName, int serverPort) {
        this.socket = socket;
        this.db = new DatabaseManager(dbName);
        this.serverPort = serverPort;
    }
    
    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            
            // Authentication
            while (true) {
                String choice = in.readLine();
                if (choice == null) return;
                
                if ("REGISTER".equals(choice)) {
                    String user = in.readLine();
                    String pass = in.readLine();
                    boolean success = db.registerUser(user, pass);
                    
                    if (success) {
                        syncRegister(user, pass);
                        out.println("REGISTER_SUCCESS");
                        System.out.println("✅ New user registered: " + user);
                    } else {
                        out.println("REGISTER_FAILED");
                    }
                } else if ("LOGIN".equals(choice)) {
                    username = in.readLine();
                    String password = in.readLine();
                    role = db.loginUser(username, password);
                    
                    if (role != null) {
                        out.println("LOGIN_SUCCESS");
                        out.println(role);
                        
                        // Load old messages
                        ArrayList<String[]> oldMessages = db.loadMessages();
                        for (String[] msg : oldMessages) {
                            out.println(msg[0] + "|" + msg[1] + "|" + msg[2]);
                        }
                        out.println("END_MESSAGES");
                        
                        // Mark as authenticated and add to client list
                        authenticated = true;
                        ServerNode.addClient(this);
                        broadcastUserList();
                        
                        System.out.println("✅ User logged in: " + username + " (Role: " + role + ")");
                        break;
                    } else {
                        out.println("LOGIN_FAILED");
                    }
                }
            }
            
            // Chat loop - only if authenticated
            if (authenticated) {
                String msg;
                while ((msg = in.readLine()) != null) {
                    if (msg.startsWith("BROADCAST|") && role.equals("admin")) {
                        String broadcastMsg = msg.substring(10);
                        String timestamp = timeFormat.format(new Date());
                        String fullMsg = "SYSTEM|🔔 ADMIN BROADCAST: " + broadcastMsg + "|" + timestamp;
                        
                        // Save to database
                        db.saveMessage(0, "SYSTEM", "🔔 ADMIN BROADCAST: " + broadcastMsg);
                        syncMessage("SYSTEM", "🔔 ADMIN BROADCAST: " + broadcastMsg);
                        
                        // Broadcast to all clients
                        ServerNode.broadcastToAll(fullMsg);
                    } else {
                        String timestamp = timeFormat.format(new Date());
                        String fullMessage = username + "|" + msg + "|" + timestamp;
                        
                        // Save to database
                        int userId = db.getUserId(username);
                        db.saveMessage(userId, username, msg);
                        
                        // Sync to other server
                        syncMessage(username, msg);
                        
                        // Broadcast to ALL clients (including sender)
                        ServerNode.broadcastToAll(fullMessage);
                    }
                }
            }
            
        } catch (Exception e) {
            System.out.println("❌ Error in ClientHandler for " + username + ": " + e.getMessage());
        } finally {
            if (authenticated) {
                ServerNode.removeClient(this);
                broadcastUserList();
                System.out.println("👋 User disconnected: " + username);
            }
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    public void send(String message) {
        if (out != null && !socket.isClosed()) {
            out.println(message);
        }
    }
    
    public String getUsername() {
        return username;
    }
    
    private void broadcastUserList() {
        StringBuilder users = new StringBuilder();
        for (ClientHandler client : ServerNode.getClients()) {
            if (users.length() > 0) users.append(",");
            users.append(client.getUsername());
        }
        ServerNode.broadcastToAll("USER_LIST|" + users.toString());
        System.out.println("📋 User list broadcasted: " + users.toString());
    }
    
    private void syncRegister(String username, String password) {
        int targetPort = (serverPort == 1234) ? 2235 : 2234;
        try {
            Socket s = new Socket("localhost", targetPort);
            PrintWriter out = new PrintWriter(s.getOutputStream(), true);
            out.println("REGISTER|" + username + "|" + password);
            s.close();
            System.out.println("🔄 Registration synced to port " + targetPort + " for user: " + username);
        } catch (Exception e) {
            System.out.println("⚠️ Register Sync failed for " + username + ": " + e.getMessage());
        }
    }
    
    private void syncMessage(String username, String message) {
        int targetPort = (serverPort == 1234) ? 2235 : 2234;
        try {
            Socket s = new Socket("localhost", targetPort);
            PrintWriter out = new PrintWriter(s.getOutputStream(), true);
            out.println("MESSAGE|" + username + "|" + message);
            s.close();
            System.out.println("🔄 Message synced to port " + targetPort + " from " + username);
        } catch (Exception e) {
            System.out.println("⚠️ Message Sync failed: " + e.getMessage());
        }
    }
}