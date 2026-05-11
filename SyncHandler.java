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
            if (data == null) return;
            
            String[] parts = data.split("\\|");
            
            switch (parts[0]) {
                case "REGISTER":
                    db.registerUser(parts[1], parts[2]);
                    System.out.println("Synced registration: " + parts[1]);
                    break;
                    
                case "MESSAGE":
                    int userId = db.getUserId(parts[1]);
                    db.saveMessage(userId, parts[1], parts[2]);
                    System.out.println("Synced message from " + parts[1]);
                    break;
                    
                case "GET_USERS":
                    List<Map<String, Object>> users = db.getAllUsers();
                    for (Map<String, Object> user : users) {
                        out.println(user.get("id") + "|" + 
                                   user.get("username") + "|" + 
                                   user.get("role") + "|" + 
                                   user.get("created_at"));
                    }
                    out.println("END_USERS");
                    break;
                    
                case "DELETE_USER":
                    boolean deleted = db.deleteUser(Integer.parseInt(parts[1]), parts[2]);
                    out.println(deleted ? "DELETE_SUCCESS" : "DELETE_FAILED");
                    System.out.println("Synced delete: " + parts[2]);
                    break;
            }
            
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}