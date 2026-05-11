import java.sql.*;
import java.util.ArrayList;

public class DatabaseManager {

    private Connection conn;

    public DatabaseManager(String dbName) {

        try {

            conn = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/" + dbName,
                    "root",
                    ""
            );

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    // =========================
    // REGISTER USER
    // =========================
    public boolean registerUser(String username,
                                String password) {

        try {

            String check =
                    "SELECT * FROM users WHERE username=?";

            PreparedStatement ps1 =
                    conn.prepareStatement(check);

            ps1.setString(1, username);

            ResultSet rs = ps1.executeQuery();

            if (rs.next()) {
                return false;
            }

            String sql =
                    "INSERT INTO users(username,password) VALUES(?,?)";

            PreparedStatement ps =
                    conn.prepareStatement(sql);

            ps.setString(1, username);
            ps.setString(2, password);

            ps.executeUpdate();

            return true;

        } catch (Exception e) {

            e.printStackTrace();
        }

        return false;
    }

    // =========================
    // LOGIN USER
    // =========================
    public String loginUser(String username,
                            String password) {

        try {

            String sql =
                    "SELECT role FROM users WHERE username=? AND password=?";

            PreparedStatement ps =
                    conn.prepareStatement(sql);

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

    // =========================
    // GET USER ID
    // =========================
    public int getUserId(String username) {

        try {

            String sql =
                    "SELECT id FROM users WHERE username=?";

            PreparedStatement ps =
                    conn.prepareStatement(sql);

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

    // =========================
    // SAVE MESSAGE
    // =========================
    public void saveMessage(int userId,
                            String username,
                            String message) {

        try {

            String sql =
                    "INSERT INTO messages(user_id,username,message) VALUES(?,?,?)";

            PreparedStatement ps =
                    conn.prepareStatement(sql);

            ps.setInt(1, userId);
            ps.setString(2, username);
            ps.setString(3, message);

            ps.executeUpdate();

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    // =========================
    // LOAD MESSAGES
    // =========================
    public ArrayList<String> loadMessages() {

        ArrayList<String> list =
                new ArrayList<>();

        try {

            String sql =
                    "SELECT username,message,timestamp FROM messages ORDER BY timestamp ASC";

            Statement st =
                    conn.createStatement();

            ResultSet rs =
                    st.executeQuery(sql);

            while (rs.next()) {

                String msg =
                        "[" +
                        rs.getTimestamp("timestamp") +
                        "] " +
                        rs.getString("username") +
                        ": " +
                        rs.getString("message");

                list.add(msg);
            }

        } catch (Exception e) {

            e.printStackTrace();
        }

        return list;
    }
}