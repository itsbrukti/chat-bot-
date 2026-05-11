import javax.swing.*;
import javax.swing.table.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.*;
import java.util.*;
import javax.swing.border.*;

public class AdminPanel extends JFrame {
    
    private JTable userTable;
    private DefaultTableModel tableModel;
    private JButton refreshButton;
    private JButton deleteButton;
    private JButton broadcastButton;
    private JLabel statusLabel;
    private Timer refreshTimer;
    private int serverPort;
    private String dbName;
    
    public AdminPanel(int serverPort, String dbName) {
        this.serverPort = serverPort;
        this.dbName = dbName;
        
        setTitle("Admin Dashboard - " + dbName);
        setSize(1000, 650);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        
        // Main panel
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(new Color(34, 34, 34));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(0, 122, 255));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        JLabel titleLabel = new JLabel("👑 ADMIN CONTROL PANEL - " + dbName);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(Color.WHITE);
        
        statusLabel = new JLabel("● Loading users...");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLabel.setForeground(Color.WHITE);
        
        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(statusLabel, BorderLayout.EAST);
        
        // Table panel
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBackground(new Color(45, 45, 45));
        tablePanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(0, 122, 255), 2),
            "📋 Registered Users",
            TitledBorder.LEFT,
            TitledBorder.TOP,
            new Font("Segoe UI", Font.BOLD, 14),
            new Color(0, 122, 255)
        ));
        
        // Create table with proper columns (without created_at)
        String[] columns = {"ID", "Username", "Role"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        userTable = new JTable(tableModel);
        userTable.setRowHeight(35);
        userTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        userTable.setBackground(new Color(60, 60, 60));
        userTable.setForeground(Color.WHITE);
        userTable.setSelectionBackground(new Color(0, 122, 255));
        userTable.setSelectionForeground(Color.WHITE);
        userTable.setGridColor(new Color(70, 70, 70));
        userTable.setShowGrid(true);
        
        // Set column widths
        userTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        userTable.getColumnModel().getColumn(1).setPreferredWidth(250);
        userTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        
        userTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        userTable.getTableHeader().setBackground(new Color(0, 122, 255));
        userTable.getTableHeader().setForeground(Color.WHITE);
        userTable.getTableHeader().setReorderingAllowed(false);
        
        JScrollPane scrollPane = new JScrollPane(userTable);
        scrollPane.getViewport().setBackground(new Color(60, 60, 60));
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(70, 70, 70)));
        tablePanel.add(scrollPane, BorderLayout.CENTER);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        buttonPanel.setBackground(new Color(45, 45, 45));
        
        refreshButton = createStyledButton("🔄 Refresh Users", new Color(0, 122, 255));
        deleteButton = createStyledButton("🗑️ Delete Selected User", new Color(231, 76, 60));
        broadcastButton = createStyledButton("📢 Send Broadcast", new Color(46, 204, 113));
        
        deleteButton.setEnabled(false);
        
        buttonPanel.add(refreshButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(broadcastButton);
        
        // Stats panel
        JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        statsPanel.setBackground(new Color(45, 45, 45));
        
        JLabel statsLabel = new JLabel("Total Users: 0");
        statsLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        statsLabel.setForeground(new Color(0, 122, 255));
        statsPanel.add(statsLabel);
        
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(tablePanel, BorderLayout.CENTER);
        
        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.setBackground(new Color(45, 45, 45));
        southPanel.add(buttonPanel, BorderLayout.WEST);
        southPanel.add(statsPanel, BorderLayout.EAST);
        mainPanel.add(southPanel, BorderLayout.SOUTH);
        
        add(mainPanel);
        
        // Add listeners
        refreshButton.addActionListener(e -> loadUsersFromDatabase());
        deleteButton.addActionListener(e -> deleteSelectedUser());
        broadcastButton.addActionListener(e -> showBroadcastDialog());
        
        userTable.getSelectionModel().addListSelectionListener(e -> {
            int selectedRow = userTable.getSelectedRow();
            if (selectedRow != -1) {
                String username = (String) tableModel.getValueAt(selectedRow, 1);
                String role = (String) tableModel.getValueAt(selectedRow, 2);
                deleteButton.setEnabled(!username.equals("admin") && !role.equals("admin"));
            } else {
                deleteButton.setEnabled(false);
            }
        });
        
        tableModel.addTableModelListener(e -> {
            statsLabel.setText("Total Users: " + tableModel.getRowCount());
        });
        
        // Load users initially
        loadUsersFromDatabase();
        
        // Setup auto-refresh timer
        refreshTimer = new Timer(5000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loadUsersFromDatabase();
            }
        });
        refreshTimer.start();
        
        // Clean up timer when window closes
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (refreshTimer != null) {
                    refreshTimer.stop();
                }
            }
        });
    }
    
    private JButton createStyledButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                button.setBackground(bgColor.darker());
            }
            public void mouseExited(MouseEvent e) {
                button.setBackground(bgColor);
            }
        });
        
        return button;
    }
    
    private void loadUsersFromDatabase() {
        SwingUtilities.invokeLater(() -> {
            try {
                statusLabel.setText("● Loading users from " + dbName + "...");
                statusLabel.setForeground(Color.YELLOW);
                
                tableModel.setRowCount(0);
                int userCount = 0;
                
                // Connect directly to the database
                String url = "jdbc:mysql://localhost:3306/" + dbName + "?useSSL=false";
                Connection conn = DriverManager.getConnection(url, "root", "");
                
                Statement stmt = conn.createStatement();
                // Removed created_at column from query
                ResultSet rs = stmt.executeQuery("SELECT id, username, role FROM users ORDER BY id");
                
                while (rs.next()) {
                    tableModel.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("role")
                    });
                    userCount++;
                }
                
                rs.close();
                stmt.close();
                conn.close();
                
                statusLabel.setText("● Connected - " + userCount + " users found");
                statusLabel.setForeground(new Color(46, 204, 113));
                
                System.out.println("✅ Loaded " + userCount + " users from " + dbName);
                
            } catch (SQLException e) {
                statusLabel.setText("⚠ Database error: " + e.getMessage());
                statusLabel.setForeground(Color.RED);
                System.err.println("❌ Database error: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    private void deleteSelectedUser() {
        int selectedRow = userTable.getSelectedRow();
        if (selectedRow == -1) return;
        
        int userId = (Integer) tableModel.getValueAt(selectedRow, 0);
        String username = (String) tableModel.getValueAt(selectedRow, 1);
        String role = (String) tableModel.getValueAt(selectedRow, 2);
        
        if (role.equals("admin") || username.equals("admin")) {
            JOptionPane.showMessageDialog(this,
                "❌ Cannot delete the main administrator account!",
                "Delete Denied",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        int confirm = JOptionPane.showConfirmDialog(this,
            "⚠️ Are you sure you want to delete user '" + username + "'?\n\n" +
            "This action will:\n" +
            "• Remove the user account permanently\n" +
            "• Delete all messages from this user\n" +
            "• Cannot be undone!",
            "Confirm Deletion",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
        
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                statusLabel.setText("● Deleting user...");
                statusLabel.setForeground(Color.YELLOW);
                
                // Delete from current database
                String url = "jdbc:mysql://localhost:3306/" + dbName + "?useSSL=false";
                Connection conn = DriverManager.getConnection(url, "root", "");
                
                // First delete user's messages
                PreparedStatement ps1 = conn.prepareStatement("DELETE FROM messages WHERE user_id=?");
                ps1.setInt(1, userId);
                int messagesDeleted = ps1.executeUpdate();
                
                // Then delete the user
                PreparedStatement ps2 = conn.prepareStatement("DELETE FROM users WHERE id=? AND username != 'admin'");
                ps2.setInt(1, userId);
                int affected = ps2.executeUpdate();
                
                conn.close();
                
                if (affected > 0) {
                    JOptionPane.showMessageDialog(this,
                        "✅ User '" + username + "' has been deleted successfully!\n" +
                        "Deleted " + messagesDeleted + " messages.",
                        "Delete Successful",
                        JOptionPane.INFORMATION_MESSAGE);
                    
                    // Refresh user list
                    loadUsersFromDatabase();
                    
                    // Also delete from other database
                    deleteFromOtherDatabase(userId, username);
                } else {
                    JOptionPane.showMessageDialog(this,
                        "❌ Failed to delete user '" + username + "'!",
                        "Delete Failed",
                        JOptionPane.ERROR_MESSAGE);
                }
                
            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this,
                    "❌ Database error: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void deleteFromOtherDatabase(int userId, String username) {
        String otherDb = dbName.equals("chat_system_1") ? "chat_system_2" : "chat_system_1";
        
        try {
            String url = "jdbc:mysql://localhost:3306/" + otherDb + "?useSSL=false";
            Connection conn = DriverManager.getConnection(url, "root", "");
            
            PreparedStatement ps1 = conn.prepareStatement("DELETE FROM messages WHERE user_id=?");
            ps1.setInt(1, userId);
            ps1.executeUpdate();
            
            PreparedStatement ps2 = conn.prepareStatement("DELETE FROM users WHERE id=? AND username != 'admin'");
            ps2.setInt(1, userId);
            ps2.executeUpdate();
            
            conn.close();
            System.out.println("✅ Also deleted from " + otherDb);
            
        } catch (SQLException e) {
            System.out.println("⚠️ Could not delete from " + otherDb + ": " + e.getMessage());
        }
    }
    
    private void showBroadcastDialog() {
        String message = JOptionPane.showInputDialog(this, 
            "Enter broadcast message to send to all users:", 
            "Send Broadcast", 
            JOptionPane.QUESTION_MESSAGE);
            
        if (message != null && !message.trim().isEmpty()) {
            try {
                Socket socket = new Socket("localhost", serverPort);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                out.println("BROADCAST|" + message);
                socket.close();
                JOptionPane.showMessageDialog(this, 
                    "✅ Broadcast sent to all connected users!",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, 
                    "❌ Failed to send broadcast: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}