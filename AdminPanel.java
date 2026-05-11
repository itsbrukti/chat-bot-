import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.Map;
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
    
    public AdminPanel(int serverPort) {
        this.serverPort = serverPort;
        
        setTitle("Admin Dashboard");
        setSize(900, 600);
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
        
        JLabel titleLabel = new JLabel("👑 ADMIN CONTROL PANEL");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setForeground(Color.WHITE);
        
        statusLabel = new JLabel("● System Online");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLabel.setForeground(Color.WHITE);
        
        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(statusLabel, BorderLayout.EAST);
        
        // Table panel
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBackground(new Color(45, 45, 45));
        tablePanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(0, 122, 255)),
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
        userTable.setRowHeight(30);
        userTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        userTable.setBackground(new Color(60, 60, 60));
        userTable.setForeground(Color.WHITE);
        userTable.setSelectionBackground(new Color(0, 122, 255));
        userTable.setSelectionForeground(Color.WHITE);
        userTable.setGridColor(new Color(70, 70, 70));
        
        userTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
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
        statsLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
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
        refreshButton.addActionListener(e -> loadUsersFromServer());
        deleteButton.addActionListener(e -> deleteSelectedUser());
        broadcastButton.addActionListener(e -> showBroadcastDialog());
        
        userTable.getSelectionModel().addListSelectionListener(e -> {
            deleteButton.setEnabled(userTable.getSelectedRow() != -1);
        });
        
        tableModel.addTableModelListener(e -> {
            statsLabel.setText("Total Users: " + tableModel.getRowCount());
        });
        
        // Load users
        loadUsersFromServer();
        
        // Auto refresh every 3 seconds
        refreshTimer = new Timer(3000, e -> loadUsersFromServer());
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
    
    private JButton createStyledButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
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
    
    private void loadUsersFromServer() {
        SwingUtilities.invokeLater(() -> {
            try {
                Socket socket = new Socket("localhost", serverPort + 1000);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                
                out.println("GET_USERS");
                
                tableModel.setRowCount(0);
                String response;
                
                while (!(response = in.readLine()).equals("END_USERS")) {
                    String[] userData = response.split("\\|");
                    if (userData.length == 4) {
                        tableModel.addRow(new Object[]{
                            userData[0], userData[1], userData[2], userData[3]
                        });
                    }
                }
                
                socket.close();
                statusLabel.setText("● Updated: " + new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date()));
                
            } catch (Exception e) {
                statusLabel.setText("⚠ Connection Error");
                e.printStackTrace();
            }
        });
    }
    
    private void deleteSelectedUser() {
        int selectedRow = userTable.getSelectedRow();
        if (selectedRow == -1) return;
        
        String username = (String) tableModel.getValueAt(selectedRow, 1);
        String userId = (String) tableModel.getValueAt(selectedRow, 0);
        
        if (username.equals("admin")) {
            JOptionPane.showMessageDialog(this,
                "Cannot delete admin account!",
                "Error",
                JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        int confirm = JOptionPane.showConfirmDialog(this,
            "Delete user '" + username + "'?",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION);
        
        if (confirm == JOptionPane.YES_OPTION) {
            try {
                Socket socket = new Socket("localhost", serverPort + 1000);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                
                out.println("DELETE_USER|" + userId + "|" + username);
                
                String response = in.readLine();
                if (response.equals("DELETE_SUCCESS")) {
                    JOptionPane.showMessageDialog(this, "✅ User deleted successfully!");
                    loadUsersFromServer();
                }
                
                socket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    private void showBroadcastDialog() {
        String message = JOptionPane.showInputDialog(this, "Enter broadcast message:", "Send Broadcast", JOptionPane.QUESTION_MESSAGE);
        if (message != null && !message.trim().isEmpty()) {
            try {
                Socket socket = new Socket("localhost", serverPort);
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                out.println("BROADCAST|" + message);
                socket.close();
                JOptionPane.showMessageDialog(this, "✅ Broadcast sent to all users!");
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "❌ Failed to send broadcast!");
            }
        }
    }
}