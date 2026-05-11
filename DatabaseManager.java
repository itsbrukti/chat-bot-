import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class DatabaseManager {
    
    private Connection conn;
    
    public DatabaseManager(String dbName) {
        try {
            String url = "jdbc:mysql://localhost:3306/" + dbName + "?autoReconnect=true&useSSL=false";
            conn = DriverManager.getConnection(url, "root", "");
            createTables();
            System.out.println("Database connected: " + dbName);
        } catch (Exception e) {
            e.printStackTrace();
        }
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
                System.out.println("✅ Admin created - Username: admin, Password: admin123");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public boolean registerUser(String username, String password) {
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
    
    public void saveMessage(int userId, String username, String message) {
        try {
            PreparedStatement ps = conn.prepareStatement("INSERT INTO messages(user_id, username, message) VALUES(?,?,?)");
            ps.setInt(1, userId);
            ps.setString(2, username);
            ps.setString(3, message);
            ps.executeUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
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
            
            PreparedStatement ps = conn.prepareStatement("DELETE FROM users WHERE id=? AND username != 'admin'");
            ps.setInt(1, userId);
            int affected = ps.executeUpdate();
            return affected > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}