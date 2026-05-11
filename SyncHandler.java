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
    
    private boolean isFailoverActive() {
        File markerFile = new File("failover_mode.txt");
        return markerFile.exists();
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
            
            // BLOCK all sync operations during failover
            if (isFailoverActive()) {
                System.out.println("🚫 SYNC BLOCKED: Failover mode is active - No data syncing");
                out.println("SYNC_BLOCKED");
                socket.close();
                return;
            }
            
            switch (parts[0]) {
                case "REGISTER":
                    boolean regSuccess = db.registerUser(parts[1], parts[2]);
                    System.out.println("✅ Synced registration: " + parts[1] + " - " + (regSuccess ? "Success" : "Failed"));
                    break;
                    
                case "MESSAGE":
                    int userId = db.getUserId(parts[1]);
                    db.saveMessage(userId, parts[1], parts[2]);
                    System.out.println("💬 Synced message from " + parts[1]);
                    break;
                    
                case "GET_USERS":
                    System.out.println("📋 Sending user list...");
                    List<Map<String, Object>> users = db.getAllUsers();
                    for (Map<String, Object> user : users) {
                        String userData = user.get("id") + "|" + 
                                         user.get("username") + "|" + 
                                         user.get("role") + "|" + 
                                         user.get("created_at");
                        out.println(userData);
                    }
                    out.println("END_USERS");
                    System.out.println("✅ User list sent. Total: " + users.size());
                    break;
                    
                case "DELETE_USER":
                    int userIdToDelete = Integer.parseInt(parts[1]);
                    String usernameToDelete = parts[2];
                    boolean deleted = db.deleteUser(userIdToDelete, usernameToDelete);
                    out.println(deleted ? "DELETE_SUCCESS" : "DELETE_FAILED");
                    System.out.println("🗑️ Delete user: " + usernameToDelete);
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