import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.Map;

public class SyncHandler implements Runnable {
    
    private Socket socket;
    private DatabaseManager db;
    
    public SyncHandler(Socket socket, String dbName) {
        this.socket = socket;
        this.db = new DatabaseManager(dbName);
    }
    
    @Override
    public void run() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            
            String data = in.readLine();
            if (data == null) {
                socket.close();
                return;
            }
            
            System.out.println("📥 Sync received: " + data);
            String[] parts = data.split("\\|");
            
            switch (parts[0]) {
                case "REGISTER":
                    boolean regSuccess = db.registerUser(parts[1], parts[2]);
                    System.out.println("✅ Synced registration: " + parts[1] + " - " + (regSuccess ? "Success" : "Failed"));
                    break;
                    
                case "MESSAGE":
                    int userId = db.getUserId(parts[1]);
                    db.saveMessage(userId, parts[1], parts[2]);
                    System.out.println("💬 Synced message from " + parts[1] + ": " + parts[2]);
                    break;
                    
                case "GET_USERS":
                    System.out.println("📋 Sending user list...");
                    List<Map<String, Object>> users = db.getAllUsers();
                    int userCount = 0;
                    for (Map<String, Object> user : users) {
                        String userData = user.get("id") + "|" + 
                                         user.get("username") + "|" + 
                                         user.get("role") + "|" + 
                                         user.get("created_at");
                        out.println(userData);
                        userCount++;
                        System.out.println("   Sent user: " + user.get("username"));
                    }
                    out.println("END_USERS");
                    System.out.println("✅ User list sent. Total: " + userCount);
                    break;
                    
                case "DELETE_USER":
                    int userIdToDelete = Integer.parseInt(parts[1]);
                    String usernameToDelete = parts[2];
                    boolean deleted = db.deleteUser(userIdToDelete, usernameToDelete);
                    out.println(deleted ? "DELETE_SUCCESS" : "DELETE_FAILED");
                    System.out.println("🗑️ Delete user: " + usernameToDelete + " - " + (deleted ? "Success" : "Failed"));
                    break;
                    
                default:
                    System.out.println("⚠️ Unknown sync command: " + parts[0]);
                    break;
            }
            
            socket.close();
            
        } catch (Exception e) {
            System.err.println("❌ Error in SyncHandler:");
            e.printStackTrace();
        }
    }
}