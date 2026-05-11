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
    
    private boolean isFailoverActive() {
        java.io.File markerFile = new java.io.File("failover_mode.txt");
        return markerFile.exists();
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
                    
                    // BLOCK registration on main server (port 1234) during failover
                    if (isFailoverActive() && serverPort == 1234) {
                        out.println("REGISTER_FAILED");
                        System.out.println("🚫 REGISTRATION BLOCKED: Main server is in FAILOVER mode");
                        continue;
                    }
                    
                    boolean success = db.registerUser(user, pass);
                    
                    if (success) {
                        // Only sync if NOT in failover mode
                        if (!isFailoverActive()) {
                            syncRegister(user, pass);
                        } else {
                            System.out.println("⚠️ Sync skipped during failover mode");
                        }
                        out.println("REGISTER_SUCCESS");
                        System.out.println("✅ New user registered on " + (serverPort == 1234 ? "MAIN" : "BACKUP") + ": " + user);
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
                        
                        authenticated = true;
                        ServerNode.addClient(this);
                        broadcastUserList();
                        
                        String serverType = (serverPort == 1234) ? "MAIN" : "BACKUP";
                        if (isFailoverActive() && serverPort == 1234) {
                            System.out.println("⚠️ User logged into FAILED main server - this should not happen!");
                        } else {
                            System.out.println("✅ User logged in: " + username + " (Role: " + role + ") on " + serverType);
                        }
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
                    String timestamp = timeFormat.format(new Date());
                    String fullMessage = username + "|" + msg + "|" + timestamp;
                    
                    int userId = db.getUserId(username);
                    
                    // Save to current database
                    db.saveMessage(userId, username, msg);
                    
                    // Determine if this is main server (1234) and failover is active
                    boolean shouldSkipSync = isFailoverActive() && serverPort == 1234;
                    
                    if (!shouldSkipSync) {
                        // Only sync to other server if not in failover mode OR if this is backup server
                        syncMessage(username, msg);
                        System.out.println("🔄 Message synced from " + (serverPort == 1234 ? "MAIN" : "BACKUP"));
                    } else {
                        System.out.println("🚫 SYNC BLOCKED: Message saved only to " + (serverPort == 1234 ? "MAIN" : "BACKUP") + " during failover");
                    }
                    
                    // Broadcast to ALL clients on this server
                    ServerNode.broadcastToAll(fullMessage);
                    
                    System.out.println("💬 Message from " + username + " saved to " + 
                        (serverPort == 1234 ? "MAIN DB" : "BACKUP DB") + 
                        (shouldSkipSync ? " (Sync blocked due to failover)" : ""));
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
        // Don't sync if failover is active
        if (isFailoverActive()) {
            System.out.println("🚫 Registration sync blocked: Failover active");
            return;
        }
        
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
        // Don't sync if failover is active
        if (isFailoverActive()) {
            System.out.println("🚫 Message sync blocked: Failover active");
            return;
        }
        
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