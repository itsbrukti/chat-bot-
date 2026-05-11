import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.io.File;  // Add this import

public class DatabaseManager {
    
    private Connection conn;
    private String dbName;
    private boolean isMaster = false;
    private static String activeMaster = "chat_system_1";
    
    // Add this method to check failover status
    public static boolean isFailoverActive() {
        File markerFile = new File("failover_mode.txt");
        return markerFile.exists();
    }
    
    // Modify registerUser to block during failover on main DB
    public boolean registerUser(String username, String password) {
        // BLOCK registration on main database during failover
        if (isFailoverActive() && dbName.equals("chat_system_1")) {
            System.out.println("🚫 BLOCKED: Cannot register user on MAIN database during failover");
            return false;
        }
        
        try {
            PreparedStatement ps1 = conn.prepareStatement("SELECT * FROM users WHERE username=?");
            ps1.setString(1, username);
            ResultSet rs = ps1.executeQuery();
            
            if (rs.next()) {
                return false;
            }
            
            PreparedStatement ps = conn.prepareStatement("INSERT INTO users(username, password, role) VALUES(?,?, 'user')");
            ps.setString(1, username);
            ps.setString(2, password);
            ps.executeUpdate();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // Modify saveMessage to block during failover on main DB
    public void saveMessage(int userId, String username, String message) {
        // BLOCK saving messages on main database during failover
        if (isFailoverActive() && dbName.equals("chat_system_1")) {
            System.out.println("🚫 BLOCKED: Cannot save message to MAIN database during failover");
            return;
        }
        
        try {
            PreparedStatement ps = conn.prepareStatement("INSERT INTO messages(user_id, username, message) VALUES(?,?,?)");
            ps.setInt(1, userId);
            ps.setString(2, username);
            ps.setString(3, message);
            ps.executeUpdate();
            System.out.println("💾 Message saved to: " + dbName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // The rest of your existing methods remain the same...
    // (keep your existing connectToDatabase, createTables, loginUser, getUserId, 
    // loadMessages, getAllUsers, deleteUser, syncDatabaseFrom, testConnection methods)
    
    // Make sure you keep all your existing methods below
    public DatabaseManager(String dbName) {
        this.dbName = dbName;
        connectToDatabase();
        createTables();
    }
    
    private void connectToDatabase() {
        try {
            String url = "jdbc:mysql://localhost:3306/" + dbName + "?autoReconnect=true&useSSL=false";
            conn = DriverManager.getConnection(url, "root", "");
            System.out.println("✅ Connected to database: " + dbName);
        } catch (SQLException e) {
            System.err.println("❌ Failed to connect to database: " + dbName);
            e.printStackTrace();
        }
    }
    
    public void setAsMaster(boolean master) {
        this.isMaster = master;
        if (master) {
            activeMaster = dbName;
            System.out.println("👑 " + dbName + " is now the MASTER database");
        } else {
            System.out.println("📀 " + dbName + " is now the BACKUP database");
        }
    }
    
    public boolean isMaster() {
        return isMaster;
    }
    
    public static String getActiveMaster() {
        return activeMaster;
    }
    
    private void createTables() {
        try {
            Statement stmt = conn.createStatement();
            
            String createUsers = "CREATE TABLE IF NOT EXISTS users (" +
                    "id INT PRIMARY KEY AUTO_INCREMENT," +
                    "username VARCHAR(50) UNIQUE NOT NULL," +
                    "password VARCHAR(100) NOT NULL," +
                    "role VARCHAR(20) DEFAULT 'user'," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ")";
            stmt.execute(createUsers);
            
            String createMessages = "CREATE TABLE IF NOT EXISTS messages (" +
                    "id INT PRIMARY KEY AUTO_INCREMENT," +
                    "user_id INT," +
                    "username VARCHAR(50)," +
                    "message TEXT," +
                    "timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ")";
            stmt.execute(createMessages);
            
            // Create admin if not exists
            ResultSet rs = stmt.executeQuery("SELECT * FROM users WHERE username='admin'");
            if (!rs.next()) {
                stmt.execute("INSERT INTO users(username, password, role) VALUES('admin', 'admin123', 'admin')");
                System.out.println("👑 Admin created - Username: admin, Password: admin123");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public String loginUser(String username, String password) {
        try {
            PreparedStatement ps = conn.prepareStatement("SELECT role FROM users WHERE username=? AND password=?");
            ps.setString(1, username);
            ps.setString(2, password);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                return rs.getString("role");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public int getUserId(String username) {
        try {
            PreparedStatement ps = conn.prepareStatement("SELECT id FROM users WHERE username=?");
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            
            if (rs.next()) {
                return rs.getInt("id");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }
    
    public ArrayList<String[]> loadMessages() {
        ArrayList<String[]> list = new ArrayList<>();
        try {
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery("SELECT username, message, timestamp FROM messages ORDER BY id ASC");
            
            while (rs.next()) {
                String[] msg = new String[3];
                msg[0] = rs.getString("username");
                msg[1] = rs.getString("message");
                msg[2] = rs.getTimestamp("timestamp").toString();
                list.add(msg);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }
    
    public List<Map<String, Object>> getAllUsers() {
        List<Map<String, Object>> users = new ArrayList<>();
        try {
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery("SELECT id, username, role, created_at FROM users ORDER BY id");
            
            while (rs.next()) {
                Map<String, Object> user = new HashMap<>();
                user.put("id", rs.getInt("id"));
                user.put("username", rs.getString("username"));
                user.put("role", rs.getString("role"));
                user.put("created_at", rs.getTimestamp("created_at"));
                users.add(user);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return users;
    }
    
    public boolean deleteUser(int userId, String username) {
        try {
            if (username.equals("admin")) {
                return false;
            }
            
            PreparedStatement ps1 = conn.prepareStatement("DELETE FROM messages WHERE user_id=?");
            ps1.setInt(1, userId);
            ps1.executeUpdate();
            
            PreparedStatement ps2 = conn.prepareStatement("DELETE FROM users WHERE id=? AND username != 'admin'");
            ps2.setInt(1, userId);
            int affected = ps2.executeUpdate();
            return affected > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public void syncDatabaseFrom(String sourceDb) {
        try {
            System.out.println("🔄 Syncing " + dbName + " from " + sourceDb + "...");
            
            // Clear current database
            Statement clearStmt = conn.createStatement();
            clearStmt.execute("SET FOREIGN_KEY_CHECKS=0");
            clearStmt.execute("TRUNCATE TABLE messages");
            clearStmt.execute("TRUNCATE TABLE users");
            clearStmt.execute("SET FOREIGN_KEY_CHECKS=1");
            
            // Connect to source database
            String sourceUrl = "jdbc:mysql://localhost:3306/" + sourceDb + "?useSSL=false";
            Connection sourceConn = DriverManager.getConnection(sourceUrl, "root", "");
            
            // Copy users
            Statement sourceStmt = sourceConn.createStatement();
            ResultSet usersRs = sourceStmt.executeQuery("SELECT * FROM users");
            PreparedStatement insertUser = conn.prepareStatement(
                "INSERT INTO users(id, username, password, role, created_at) VALUES(?,?,?,?,?)"
            );
            
            while (usersRs.next()) {
                insertUser.setInt(1, usersRs.getInt("id"));
                insertUser.setString(2, usersRs.getString("username"));
                insertUser.setString(3, usersRs.getString("password"));
                insertUser.setString(4, usersRs.getString("role"));
                insertUser.setTimestamp(5, usersRs.getTimestamp("created_at"));
                insertUser.executeUpdate();
            }
            
            // Copy messages
            ResultSet messagesRs = sourceStmt.executeQuery("SELECT * FROM messages");
            PreparedStatement insertMessage = conn.prepareStatement(
                "INSERT INTO messages(id, user_id, username, message, timestamp) VALUES(?,?,?,?,?)"
            );
            
            while (messagesRs.next()) {
                insertMessage.setInt(1, messagesRs.getInt("id"));
                insertMessage.setInt(2, messagesRs.getInt("user_id"));
                insertMessage.setString(3, messagesRs.getString("username"));
                insertMessage.setString(4, messagesRs.getString("message"));
                insertMessage.setTimestamp(5, messagesRs.getTimestamp("timestamp"));
                insertMessage.executeUpdate();
            }
            
            sourceConn.close();
            System.out.println("✅ Sync complete! " + dbName + " now matches " + sourceDb);
            
        } catch (Exception e) {
            System.err.println("❌ Sync failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public boolean testConnection() {
        try {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
}