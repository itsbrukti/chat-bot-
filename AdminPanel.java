import javax.swing.*;
import javax.swing.table.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.*;
import java.io.*;
import javax.swing.border.*;

public class AdminPanel extends JFrame {
    
    private JTable userTable;
    private DefaultTableModel tableModel;
    private JButton refreshButton;
    private JButton deleteButton;
    private JButton failoverButton;
    private JButton recoverButton;
    private JLabel statusLabel;
    private Timer refreshTimer;
    private int serverPort;
    private String dbName;
    private JTextArea logArea;
    private JLabel masterLabel;
    private static boolean failoverActive = false;
    private JLabel failoverStatusLabel;
    
    public AdminPanel(int serverPort, String dbName) {
        this.serverPort = serverPort;
        this.dbName = dbName;
        
        setTitle("Admin Dashboard - Failover Management");
        setSize(1200, 750);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        
        // Main panel
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBackground(new Color(34, 34, 34));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Header
        JPanel headerPanel = createHeaderPanel();
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        
        // Create split pane for users and logs
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setResizeWeight(0.6);
        splitPane.setDividerSize(5);
        
        // Top panel - User table
        JPanel topPanel = createUserTablePanel();
        
        // Bottom panel - Logs and controls
        JPanel bottomPanel = createBottomPanel();
        
        splitPane.setTopComponent(topPanel);
        splitPane.setBottomComponent(bottomPanel);
        
        mainPanel.add(splitPane, BorderLayout.CENTER);
        
        add(mainPanel);
        
        // Check failover status from file
        checkFailoverStatusFromFile();
        
        // Load users initially
        loadUsersFromDatabase();
        checkMasterStatus();
        
        // Auto refresh every 5 seconds
        refreshTimer = new Timer(5000, e -> {
            loadUsersFromDatabase();
            checkMasterStatus();
        });
        refreshTimer.start();
        
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (refreshTimer != null) {
                    refreshTimer.stop();
                }
            }
        });
    }
    
    private void checkFailoverStatusFromFile() {
        File markerFile = new File("failover_mode.txt");
        if (markerFile.exists()) {
            failoverActive = true;
            addLog("⚠️ System started in FAILOVER MODE - Main database is OFFLINE");
        }
    }
    
    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(0, 122, 255));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        JLabel titleLabel = new JLabel("🏥 DATABASE FAILOVER MANAGEMENT SYSTEM");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(Color.WHITE);
        
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        rightPanel.setBackground(new Color(0, 122, 255));
        
        masterLabel = new JLabel("● Checking master status...");
        masterLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        masterLabel.setForeground(Color.WHITE);
        
        failoverStatusLabel = new JLabel("");
        failoverStatusLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        
        statusLabel = new JLabel("● System Online");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        statusLabel.setForeground(Color.WHITE);
        
        rightPanel.add(masterLabel);
        rightPanel.add(failoverStatusLabel);
        rightPanel.add(statusLabel);
        
        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(rightPanel, BorderLayout.EAST);
        
        return headerPanel;
    }
    
    private JPanel createUserTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(45, 45, 45));
        panel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(0, 122, 255), 2),
            "📋 Registered Users",
            TitledBorder.LEFT,
            TitledBorder.TOP,
            new Font("Segoe UI", Font.BOLD, 14),
            new Color(0, 122, 255)
        ));
        
        String[] columns = {"ID", "Username", "Role", "Registration Date"};
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
        
        userTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        userTable.getColumnModel().getColumn(1).setPreferredWidth(200);
        userTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        userTable.getColumnModel().getColumn(3).setPreferredWidth(200);
        
        userTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        userTable.getTableHeader().setBackground(new Color(0, 122, 255));
        userTable.getTableHeader().setForeground(Color.WHITE);
        
        JScrollPane scrollPane = new JScrollPane(userTable);
        scrollPane.getViewport().setBackground(new Color(60, 60, 60));
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createBottomPanel() {
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 10));
        bottomPanel.setBackground(new Color(45, 45, 45));
        bottomPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(0, 122, 255), 2),
            "🛠️ System Controls & Logs",
            TitledBorder.LEFT,
            TitledBorder.TOP,
            new Font("Segoe UI", Font.BOLD, 14),
            new Color(0, 122, 255)
        ));
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        buttonPanel.setBackground(new Color(45, 45, 45));
        
        refreshButton = createStyledButton("🔄 Refresh Users", new Color(0, 122, 255));
        deleteButton = createStyledButton("🗑️ Delete User", new Color(231, 76, 60));
        failoverButton = createStyledButton("⚠️ SIMULATE FAILOVER - Main DB FAILS", new Color(243, 156, 18));
        recoverButton = createStyledButton("🔧 RECOVER & SYNC - Restore Main DB", new Color(46, 204, 113));
        
        deleteButton.setEnabled(false);
        
        buttonPanel.add(refreshButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(failoverButton);
        buttonPanel.add(recoverButton);
        
        // Stats panel
        JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        statsPanel.setBackground(new Color(45, 45, 45));
        
        JLabel statsLabel = new JLabel("Total Users: 0");
        statsLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        statsLabel.setForeground(new Color(0, 122, 255));
        statsPanel.add(statsLabel);
        
        tableModel.addTableModelListener(e -> {
            statsLabel.setText("Total Users: " + tableModel.getRowCount());
        });
        
        // Log area
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setBackground(new Color(30, 30, 30));
        logArea.setForeground(new Color(46, 204, 113));
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        logArea.setBorder(BorderFactory.createLineBorder(new Color(70, 70, 70)));
        
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setPreferredSize(new Dimension(0, 200));
        logScroll.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(0, 122, 255)),
            "System Log",
            TitledBorder.LEFT,
            TitledBorder.TOP,
            new Font("Segoe UI", Font.BOLD, 11),
            new Color(0, 122, 255)
        ));
        
        // Add listeners
        refreshButton.addActionListener(e -> loadUsersFromDatabase());
        deleteButton.addActionListener(e -> deleteSelectedUser());
        failoverButton.addActionListener(e -> simulateFailover());
        recoverButton.addActionListener(e -> recoverAndSync());
        
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
        
        JPanel topControls = new JPanel(new BorderLayout());
        topControls.setBackground(new Color(45, 45, 45));
        topControls.add(buttonPanel, BorderLayout.WEST);
        topControls.add(statsPanel, BorderLayout.EAST);
        
        bottomPanel.add(topControls, BorderLayout.NORTH);
        bottomPanel.add(logScroll, BorderLayout.CENTER);
        
        return bottomPanel;
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
    
    private void addLog(String message) {
        SwingUtilities.invokeLater(() -> {
            String timestamp = new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date());
            logArea.append("[" + timestamp + "] " + message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }
    
    private void loadUsersFromDatabase() {
        SwingUtilities.invokeLater(() -> {
            try {
                String targetDb;
                
                // If failover is active, ONLY show users from backup database
                if (failoverActive) {
                    targetDb = "chat_system_2";
                    addLog("📊 Showing users from BACKUP database (Main is OFFLINE)");
                } else {
                    targetDb = dbName;
                }
                
                String url = "jdbc:mysql://localhost:3306/" + targetDb + "?useSSL=false";
                Connection conn = DriverManager.getConnection(url, "root", "");
                
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT id, username, role, created_at FROM users ORDER BY id");
                
                tableModel.setRowCount(0);
                int userCount = 0;
                
                while (rs.next()) {
                    tableModel.addRow(new Object[]{
                        rs.getInt("id"),
                        rs.getString("username"),
                        rs.getString("role"),
                        rs.getTimestamp("created_at")
                    });
                    userCount++;
                }
                
                rs.close();
                stmt.close();
                conn.close();
                
                if (failoverActive) {
                    statusLabel.setText("● BACKUP DATABASE ACTIVE - " + userCount + " users");
                    statusLabel.setForeground(Color.YELLOW);
                    failoverStatusLabel.setText("🔴 FAILOVER ACTIVE");
                    failoverStatusLabel.setForeground(Color.RED);
                } else {
                    statusLabel.setText("● Online - " + userCount + " users");
                    statusLabel.setForeground(new Color(46, 204, 113));
                    failoverStatusLabel.setText("");
                }
                
            } catch (SQLException e) {
                statusLabel.setText("⚠ Database Connection Error");
                statusLabel.setForeground(Color.RED);
                addLog("❌ Database connection failed: " + e.getMessage());
            }
        });
    }
    
    private void checkMasterStatus() {
        try {
            // Try to connect to main database
            String url = "jdbc:mysql://localhost:3306/chat_system_1?useSSL=false";
            Connection conn = DriverManager.getConnection(url, "root", "");
            conn.close();
            
            if (failoverActive) {
                masterLabel.setText("⚠ MAIN DB: FAILED (Simulated) - Using BACKUP");
                masterLabel.setForeground(Color.RED);
            } else {
                masterLabel.setText("● MAIN DB: ACTIVE");
                masterLabel.setForeground(new Color(46, 204, 113));
            }
        } catch (SQLException e) {
            masterLabel.setText("🔴 MAIN DB: OFFLINE - Using BACKUP");
            masterLabel.setForeground(Color.RED);
        }
    }
    
    private void simulateFailover() {
        int confirm = JOptionPane.showConfirmDialog(this,
            "⚠️ SIMULATE COMPLETE MAIN DATABASE FAILURE\n\n" +
            "THIS WILL:\n" +
            "• Completely BLOCK access to main database (chat_system_1)\n" +
            "• Prevent ANY operations (read/write) on main database\n" +
            "• Force ALL operations to use backup database (chat_system_2)\n" +
            "• Main database will receive NO new data\n\n" +
            "NEW USERS REGISTERED DURING FAILOVER WILL ONLY APPEAR IN BACKUP\n\n" +
            "Continue with failover simulation?",
            "Simulate Database Failure",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
        
        if (confirm == JOptionPane.YES_OPTION) {
            failoverActive = true;
            
            addLog("🔴 🔴 🔴 COMPLETE MAIN DATABASE FAILURE SIMULATED 🔴 🔴 🔴");
            addLog("❌ MAIN DATABASE (chat_system_1) is now COMPLETELY INACCESSIBLE");
            addLog("🚫 NO read or write operations allowed on main database");
            addLog("🟢 ALL operations forced to BACKUP database (chat_system_2)");
            addLog("📝 New users registered will ONLY appear in backup database");
            addLog("⚠️ Main database will receive NO updates until recovery");
            
            // Create marker file to indicate failover
            try {
                FileWriter fw = new FileWriter("failover_mode.txt");
                fw.write("active");
                fw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            
            // Try to drop the main database connection to simulate failure
            try {
                Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/chat_system_1?useSSL=false", "root", "");
                Statement stmt = conn.createStatement();
                // This will effectively block writes to main db by renaming it
                stmt.execute("RENAME TABLE users TO users_failed");
                stmt.execute("RENAME TABLE messages TO messages_failed");
                conn.close();
                addLog("🔒 Main database tables have been locked/renamed - writes blocked");
            } catch (SQLException e) {
                addLog("✅ Main database is now OFFLINE (simulated)");
            }
            
            JOptionPane.showMessageDialog(this,
                "✅ COMPLETE MAIN DATABASE FAILURE SIMULATED!\n\n" +
                "chat_system_1 is now COMPLETELY INACCESSIBLE.\n" +
                "• NO reads allowed\n" +
                "• NO writes allowed\n" +
                "• NO new users can be added to main DB\n\n" +
                "chat_system_2 is now the ONLY active database.\n\n" +
                "All operations are using the backup database.\n\n" +
                "Click 'RECOVER & SYNC' to restore the main database.",
                "Failover Active",
                JOptionPane.WARNING_MESSAGE);
            
            checkMasterStatus();
            loadUsersFromDatabase();
        }
    }
    
    private void recoverAndSync() {
        int confirm = JOptionPane.showConfirmDialog(this,
            "🔧 DATABASE RECOVERY AND SYNC\n\n" +
            "This will RECOVER the main database and SYNC all data:\n\n" +
            "STEP 1: Restore main database tables\n" +
            "STEP 2: Copy ALL data from backup (chat_system_2) to main (chat_system_1)\n" +
            "STEP 3: This includes ALL users registered during failure\n" +
            "STEP 4: This includes ALL messages sent during failure\n" +
            "STEP 5: Make chat_system_1 the ACTIVE MASTER again\n\n" +
            "No data will be lost - everything from backup will be synced.\n\n" +
            "Proceed with recovery?",
            "Recover and Sync",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.INFORMATION_MESSAGE);
        
        if (confirm == JOptionPane.YES_OPTION) {
            addLog("🔄 STARTING DATABASE RECOVERY PROCESS...");
            addLog("📥 Preparing to copy ALL data from backup to main database");
            
            try {
                // First, restore the main database tables
                addLog("🔧 Restoring main database tables...");
                Connection mainConn = DriverManager.getConnection("jdbc:mysql://localhost:3306/chat_system_1?useSSL=false", "root", "");
                Statement restoreStmt = mainConn.createStatement();
                
                try {
                    restoreStmt.execute("RENAME TABLE users_failed TO users");
                    restoreStmt.execute("RENAME TABLE messages_failed TO messages");
                    addLog("✅ Main database tables restored");
                } catch (SQLException e) {
                    addLog("ℹ️ Tables already in normal state, proceeding with sync");
                }
                
                // Clear main database
                addLog("🗑️ Clearing main database for fresh sync...");
                restoreStmt.execute("SET FOREIGN_KEY_CHECKS=0");
                restoreStmt.execute("TRUNCATE TABLE messages");
                restoreStmt.execute("TRUNCATE TABLE users");
                restoreStmt.execute("SET FOREIGN_KEY_CHECKS=1");
                
                // Connect to backup database
                Connection backupConn = DriverManager.getConnection("jdbc:mysql://localhost:3306/chat_system_2?useSSL=false", "root", "");
                
                // Copy users from backup to main
                addLog("📋 Copying users from backup database...");
                Statement backupStmt = backupConn.createStatement();
                ResultSet usersRs = backupStmt.executeQuery("SELECT * FROM users");
                PreparedStatement insertUser = mainConn.prepareStatement(
                    "INSERT INTO users(id, username, password, role, created_at) VALUES(?,?,?,?,?)"
                );
                
                int userCount = 0;
                while (usersRs.next()) {
                    insertUser.setInt(1, usersRs.getInt("id"));
                    insertUser.setString(2, usersRs.getString("username"));
                    insertUser.setString(3, usersRs.getString("password"));
                    insertUser.setString(4, usersRs.getString("role"));
                    insertUser.setTimestamp(5, usersRs.getTimestamp("created_at"));
                    insertUser.executeUpdate();
                    userCount++;
                }
                addLog("✅ Copied " + userCount + " users to main database");
                
                // Copy messages from backup to main
                addLog("💬 Copying messages from backup database...");
                ResultSet messagesRs = backupStmt.executeQuery("SELECT * FROM messages");
                PreparedStatement insertMessage = mainConn.prepareStatement(
                    "INSERT INTO messages(id, user_id, username, message, timestamp) VALUES(?,?,?,?,?)"
                );
                
                int msgCount = 0;
                while (messagesRs.next()) {
                    insertMessage.setInt(1, messagesRs.getInt("id"));
                    insertMessage.setInt(2, messagesRs.getInt("user_id"));
                    insertMessage.setString(3, messagesRs.getString("username"));
                    insertMessage.setString(4, messagesRs.getString("message"));
                    insertMessage.setTimestamp(5, messagesRs.getTimestamp("timestamp"));
                    insertMessage.executeUpdate();
                    msgCount++;
                }
                addLog("✅ Copied " + msgCount + " messages to main database");
                
                backupConn.close();
                mainConn.close();
                
                // Remove failover marker
                File markerFile = new File("failover_mode.txt");
                if (markerFile.exists()) {
                    markerFile.delete();
                }
                
                failoverActive = false;
                
                addLog("✅ ✅ ✅ RECOVERY COMPLETE! ✅ ✅ ✅");
                addLog("👑 chat_system_1 is now the ACTIVE MASTER again");
                addLog("🔄 Both databases are now FULLY SYNCHRONIZED");
                addLog("📊 Total users in main DB: " + userCount);
                addLog("💬 Total messages in main DB: " + msgCount);
                addLog("💡 All data from the failure period has been restored!");
                
                JOptionPane.showMessageDialog(this,
                    "✅ DATABASE RECOVERY COMPLETE!\n\n" +
                    "chat_system_1 has been FULLY RESTORED and SYNCHRONIZED.\n" +
                    "chat_system_1 is now the ACTIVE MASTER database again.\n\n" +
                    "Data recovered from backup:\n" +
                    "• Users: " + userCount + "\n" +
                    "• Messages: " + msgCount + "\n\n" +
                    "BOTH DATABASES ARE NOW IDENTICAL!",
                    "Recovery Complete",
                    JOptionPane.INFORMATION_MESSAGE);
                
                checkMasterStatus();
                loadUsersFromDatabase();
                
            } catch (SQLException e) {
                addLog("❌ Recovery failed: " + e.getMessage());
                e.printStackTrace();
                JOptionPane.showMessageDialog(this,
                    "❌ Recovery failed: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
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
            "⚠️ Delete user '" + username + "'?\nThis cannot be undone!",
            "Confirm Deletion",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
        
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                // During failover, only delete from backup database
                String targetDb = failoverActive ? "chat_system_2" : "chat_system_1";
                String url = "jdbc:mysql://localhost:3306/" + targetDb + "?useSSL=false";
                Connection conn = DriverManager.getConnection(url, "root", "");
                
                PreparedStatement ps1 = conn.prepareStatement("DELETE FROM messages WHERE user_id=?");
                ps1.setInt(1, userId);
                ps1.executeUpdate();
                
                PreparedStatement ps2 = conn.prepareStatement("DELETE FROM users WHERE id=? AND username != 'admin'");
                ps2.setInt(1, userId);
                int affected = ps2.executeUpdate();
                
                conn.close();
                
                if (affected > 0) {
                    if (failoverActive) {
                        addLog("🗑️ Deleted user: " + username + " from BACKUP database only (Main is OFFLINE)");
                    } else {
                        addLog("🗑️ Deleted user: " + username);
                    }
                    JOptionPane.showMessageDialog(this, "✅ User deleted from " + targetDb);
                    loadUsersFromDatabase();
                }
                
            } catch (SQLException e) {
                addLog("❌ Delete failed: " + e.getMessage());
                JOptionPane.showMessageDialog(this, "❌ Delete failed: " + e.getMessage());
            }
        }
    }
}