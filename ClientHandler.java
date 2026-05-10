import java.io.*;
import java.net.*;
import java.sql.*;

public class ClientHandler implements Runnable {

    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String username;

    Connection conn;

    public ClientHandler(Socket socket) {
        this.socket = socket;

        try {
            conn = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/chat_system",
                "root",
                ""
            );
        } catch (Exception e) {
            e.printStackTrace();
            System.out.print("not connected to databasae");
        }
    }

    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            out.println("Enter username:");
            username = in.readLine();

            String msg;

            while ((msg = in.readLine()) != null) {

                String fullMessage = username + ": " + msg;

                System.out.println(fullMessage);

                // 💾 SAVE TO MYSQL
                String sql = "INSERT INTO messages(username, message) VALUES(?, ?)";
                PreparedStatement ps = conn.prepareStatement(sql);
                ps.setString(1, username);
                ps.setString(2, msg);
                ps.executeUpdate();

                // 📡 BROADCAST
                Server.broadcast(fullMessage, this);
            }

        } catch (Exception e) {
            System.out.println(username + " disconnected");
        }
    }

    public void send(String msg) {
        out.println(msg);
    }
}