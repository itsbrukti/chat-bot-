import javax.swing.*;
import javax.swing.table.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.*;
import java.io.*;
import javax.swing.border.*;

public class ModernAdminPanel extends JFrame {
    
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
    private static boolean isOpen = false;
    
    public ModernAdminPanel(int serverPort, String dbName) {
        // Prevent multiple instances
        if (isOpen) {
            return;
        }
        isOpen = true;
        
        this.serverPort = serverPort;
        this.dbName = dbName;
        
        setTitle("Admin Dashboard - " + dbName);
        setSize(1300, 800);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        
        // Main panel - Dark elegant background
        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBackground(new Color(28, 28, 32));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        // Header
        JPanel headerPanel = createHeaderPanel();
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        
        // Create split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setResizeWeight(0.65);
        splitPane.setDividerSize(8);
        splitPane.setBorder(null);
        
        // Top panel - User table
        JPanel topPanel = createUserTablePanel();
        splitPane.setTopComponent(topPanel);
        
        // Bottom panel - Logs and controls
        JPanel bottomPanel = createBottomPanel();
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
                isOpen = false;
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
        headerPanel.setBackground(new Color(52, 73, 94));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(20, 25, 20, 25));
        
        JLabel titleLabel = new JLabel("🏥 DATABASE FAILOVER MANAGEMENT SYSTEM");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setForeground(Color.BLACK);
        
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 0));
        rightPanel.setBackground(new Color(52, 73, 94));
        
        masterLabel = new JLabel("● Checking master status...");
        masterLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        masterLabel.setForeground(Color.BLACK);
        
        failoverStatusLabel = new JLabel("");
        failoverStatusLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        
        statusLabel = new JLabel("● System Online");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        statusLabel.setForeground(Color.BLACK);
        
        rightPanel.add(masterLabel);
        rightPanel.add(failoverStatusLabel);
        rightPanel.add(statusLabel);
        
        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(rightPanel, BorderLayout.EAST);
        
        return headerPanel;
    }
    
    private JPanel createUserTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(38, 38, 43));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(52, 73, 94), 2),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        
        JLabel titleLabel = new JLabel("📋 REGISTERED USERS");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setForeground(new Color(52, 152, 219));
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        panel.add(titleLabel, BorderLayout.NORTH);
        
        String[] columns = {"ID", "Username", "Role", "Registration Date"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        userTable = new JTable(tableModel);
        userTable.setRowHeight(40);
        userTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        userTable.setBackground(new Color(45, 45, 50));
        userTable.setForeground(Color.BLACK);
        userTable.setSelectionBackground(new Color(52, 152, 219));
        userTable.setSelectionForeground(Color.BLACK);
        userTable.setGridColor(new Color(60, 60, 65));
        userTable.setShowGrid(true);
        
        // Custom renderer for Role column
        userTable.getColumn("Role").setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, 
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String role = (String) value;
                if (!isSelected) {
                    if (role.equals("admin")) {
                        setBackground(new Color(231, 76, 60));
                        setForeground(Color.BLACK);
                        setFont(getFont().deriveFont(Font.BOLD));
                    } else {
                        setBackground(new Color(46, 204, 113));
                        setForeground(Color.BLACK);
                    }
                    setHorizontalAlignment(JLabel.CENTER);
                }
                return c;
            }
        });
        
        userTable.getColumnModel().getColumn(0).setPreferredWidth(60);
        userTable.getColumnModel().getColumn(1).setPreferredWidth(200);
        userTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        userTable.getColumnModel().getColumn(3).setPreferredWidth(250);
        
        userTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        userTable.getTableHeader().setBackground(new Color(52, 73, 94));
        userTable.getTableHeader().setForeground(Color.BLACK);
        userTable.getTableHeader().setPreferredSize(new Dimension(0, 40));
        
        JScrollPane scrollPane = new JScrollPane(userTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(52, 73, 94)));
        scrollPane.getViewport().setBackground(new Color(45, 45, 50));
        
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }
    
    private JPanel createBottomPanel() {
        JPanel bottomPanel = new JPanel(new BorderLayout(15, 15));
        bottomPanel.setBackground(new Color(38, 38, 43));
        bottomPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(52, 73, 94), 2),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        buttonPanel.setBackground(new Color(38, 38, 43));
        
        refreshButton = createStyledButton("🔄 REFRESH USERS", new Color(52, 152, 219));
        deleteButton = createStyledButton("🗑️ DELETE SELECTED USER", new Color(231, 76, 60));
        failoverButton = createStyledButton("⚠️ SIMULATE FAILOVER", new Color(243, 156, 18));
        recoverButton = createStyledButton("🔧 RECOVER & SYNC", new Color(46, 204, 113));
        
        deleteButton.setEnabled(false);
        
        buttonPanel.add(refreshButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(failoverButton);
        buttonPanel.add(recoverButton);
        
        // Stats panel
        JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        statsPanel.setBackground(new Color(38, 38, 43));
        
        JLabel statsLabel = new JLabel("📊 TOTAL USERS: 0");
        statsLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        statsLabel.setForeground(new Color(52, 152, 219));
        statsPanel.add(statsLabel);
        
        tableModel.addTableModelListener(e -> {
            statsLabel.setText("📊 TOTAL USERS: " + tableModel.getRowCount());
        });
        
        // Log area
        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.setBackground(new Color(38, 38, 43));
        logPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(52, 152, 219)),
            "📝 SYSTEM ACTIVITY LOG",
            TitledBorder.LEFT,
            TitledBorder.TOP,
            new Font("Segoe UI", Font.BOLD, 12),
            new Color(52, 152, 219)
        ));
        
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setBackground(new Color(25, 25, 30));
        logArea.setForeground(new Color(0, 255, 127));
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        logArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(null);
        logScroll.getViewport().setBackground(new Color(25, 25, 30));
        
        logPanel.add(logScroll, BorderLayout.CENTER);
        
        // Button actions
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
        topControls.setBackground(new Color(38, 38, 43));
        topControls.add(buttonPanel, BorderLayout.WEST);
        topControls.add(statsPanel, BorderLayout.EAST);
        
        bottomPanel.add(topControls, BorderLayout.NORTH);
        bottomPanel.add(logPanel, BorderLayout.CENTER);
        
        return bottomPanel;
    }
    
    private JButton createStyledButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setBackground(bgColor);
        button.setForeground(Color.BLACK);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(12, 25, 12, 25));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Add hover effect
        button.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                button.setBackground(bgColor.darker());
                button.setFont(button.getFont().deriveFont(Font.BOLD, 14));
            }
            public void mouseExited(MouseEvent e) {
                button.setBackground(bgColor);
                button.setFont(button.getFont().deriveFont(Font.BOLD, 13));
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
                    statusLabel.setText("● ONLINE - " + userCount + " users");
                    statusLabel.setForeground(new Color(46, 204, 113));
                    failoverStatusLabel.setText("");
                }
                
                addLog("✅ Loaded " + userCount + " users from " + targetDb);
                
            } catch (SQLException e) {
                statusLabel.setText("⚠ DATABASE CONNECTION ERROR");
                statusLabel.setForeground(Color.RED);
                addLog("❌ Database connection failed: " + e.getMessage());
            }
        });
    }
    
    private void checkMasterStatus() {
        try {
            String url = "jdbc:mysql://localhost:3306/chat_system_1?useSSL=false";
            Connection conn = DriverManager.getConnection(url, "root", "");
            conn.close();
            
            if (failoverActive) {
                masterLabel.setText("⚠ MAIN DB: SIMULATED FAILURE - Using BACKUP");
                masterLabel.setForeground(Color.YELLOW);
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
            "⚠️ SIMULATE MAIN DATABASE FAILURE\n\n" +
            "• Main database will be BLOCKED\n" +
            "• All operations go to BACKUP\n" +
            "• No new data to main DB\n\n" +
            "Continue?",
            "Failover Simulation",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
        
        if (confirm == JOptionPane.YES_OPTION) {
            failoverActive = true;
            
            addLog("🔴🔴🔴 MAIN DATABASE FAILURE SIMULATED 🔴🔴🔴");
            addLog("❌ Main database is now COMPLETELY INACCESSIBLE");
            addLog("🟢 ALL operations forced to BACKUP database");
            addLog("📝 New users will ONLY appear in backup database");
            
            try {
                FileWriter fw = new FileWriter("failover_mode.txt");
                fw.write("active");
                fw.close();
            } catch (IOException e) {}
            
            JOptionPane.showMessageDialog(this,
                "✅ FAILOVER SIMULATED!\n\n" +
                "Backup database is now ACTIVE.\n" +
                "Click 'RECOVER & SYNC' to restore.",
                "Failover Active",
                JOptionPane.WARNING_MESSAGE);
            
            checkMasterStatus();
            loadUsersFromDatabase();
        }
    }
    
    private void recoverAndSync() {
        int confirm = JOptionPane.showConfirmDialog(this,
            "🔧 DATABASE RECOVERY AND SYNC\n\n" +
            "• Copy ALL data from backup to main\n" +
            "• Include users registered during failure\n" +
            "• Include messages sent during failure\n" +
            "• Make both databases IDENTICAL\n\n" +
            "Proceed?",
            "Database Recovery",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.INFORMATION_MESSAGE);
        
        if (confirm == JOptionPane.YES_OPTION) {
            addLog("🔄 STARTING DATABASE RECOVERY...");
            addLog("📥 Copying ALL data from backup to main database");
            
            try {
                Connection mainConn = DriverManager.getConnection("jdbc:mysql://localhost:3306/chat_system_1?useSSL=false", "root", "");
                Connection backupConn = DriverManager.getConnection("jdbc:mysql://localhost:3306/chat_system_2?useSSL=false", "root", "");
                
                // Clear main database
                Statement clearStmt = mainConn.createStatement();
                clearStmt.execute("SET FOREIGN_KEY_CHECKS=0");
                clearStmt.execute("TRUNCATE TABLE messages");
                clearStmt.execute("TRUNCATE TABLE users");
                clearStmt.execute("SET FOREIGN_KEY_CHECKS=1");
                
                // Copy users
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
                
                // Copy messages
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
                
                File markerFile = new File("failover_mode.txt");
                if (markerFile.exists()) markerFile.delete();
                
                failoverActive = false;
                
                addLog("✅✅✅ RECOVERY COMPLETE! ✅✅✅");
                addLog("👑 Main database is now ACTIVE MASTER again");
                addLog("🔄 Both databases are now FULLY SYNCHRONIZED");
                addLog("📊 Users: " + userCount + " | Messages: " + msgCount);
                
                JOptionPane.showMessageDialog(this,
                    "✅ RECOVERY COMPLETE!\n\n" +
                    "Main database restored!\n" +
                    "Users: " + userCount + "\n" +
                    "Messages: " + msgCount + "\n\n" +
                    "Both databases are now IDENTICAL!",
                    "Recovery Complete",
                    JOptionPane.INFORMATION_MESSAGE);
                
                checkMasterStatus();
                loadUsersFromDatabase();
                
            } catch (SQLException e) {
                addLog("❌ Recovery failed: " + e.getMessage());
                JOptionPane.showMessageDialog(this,
                    "❌ Recovery failed: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private void deleteSelectedUser() {
        int selectedRow = userTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a user to delete!", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
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
            "⚠️ Delete user '" + username + "'?\nThis will also delete all their messages!\nThis cannot be undone!",
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
                int messagesDeleted = ps1.executeUpdate();
                
                PreparedStatement ps2 = conn.prepareStatement("DELETE FROM users WHERE id=? AND username != 'admin'");
                ps2.setInt(1, userId);
                int affected = ps2.executeUpdate();
                
                conn.close();
                
                if (affected > 0) {
                    if (failoverActive) {
                        addLog("🗑️ Deleted user: " + username + " from BACKUP database only (Deleted " + messagesDeleted + " messages)");
                    } else {
                        addLog("🗑️ Deleted user: " + username + " (Deleted " + messagesDeleted + " messages)");
                    }
                    JOptionPane.showMessageDialog(this, "✅ User '" + username + "' deleted successfully!");
                    loadUsersFromDatabase();
                } else {
                    JOptionPane.showMessageDialog(this, "❌ Failed to delete user!");
                }
                
            } catch (SQLException e) {
                addLog("❌ Delete failed: " + e.getMessage());
                JOptionPane.showMessageDialog(this, "❌ Delete failed: " + e.getMessage());
            }
        }
    }
}