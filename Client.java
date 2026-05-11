import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Client {

    static BufferedReader in;
    static PrintWriter out;
    static JTextPane chatPane;
    static JTextField inputField;
    static JFrame frame;
    static String currentUsername;
    static String currentRole;
    static DefaultListModel<String> userListModel;
    static JList<String> userList;
    static SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

            // Connect to load balancer
            Socket lbSocket = new Socket("localhost", 5000);
            BufferedReader lbIn = new BufferedReader(new InputStreamReader(lbSocket.getInputStream()));
            int serverPort = Integer.parseInt(lbIn.readLine());
            lbSocket.close();

            // Connect to server
            Socket socket = new Socket("localhost", serverPort);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // Show login dialog
            showLoginDialog();

            // Login/Register
            String[] options = { "Login", "Register" };
            int choice = JOptionPane.showOptionDialog(null,
                    "Welcome to Chat System",
                    "Authentication",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.INFORMATION_MESSAGE,
                    null, options, options[0]);

            String username = JOptionPane.showInputDialog("Username:");
            String password = JOptionPane.showInputDialog("Password:");

            if (choice == 1) {
                out.println("REGISTER");
                out.println(username);
                out.println(password);
                String response = in.readLine();
                if (response.equals("REGISTER_SUCCESS")) {
                    JOptionPane.showMessageDialog(null, "✅ Registration successful! Please login.");
                    return;
                } else {
                    JOptionPane.showMessageDialog(null, "❌ Username already exists!");
                    return;
                }
            }

            // Login
            out.println("LOGIN");
            out.println(username);
            out.println(password);
            String response = in.readLine();

            if (!response.equals("LOGIN_SUCCESS")) {
                JOptionPane.showMessageDialog(null, "❌ Login failed!");
                return;
            }

            currentUsername = username;
            currentRole = in.readLine();

            // Create beautiful GUI
            createTelegramStyleGUI();

            // Load previous messages
            String[] msgData;
            while (!(msgData = in.readLine().split("\\|"))[0].equals("END_MESSAGES")) {
                if (msgData.length == 3) {
                    addMessageToChat(msgData[0], msgData[1], msgData[2]);
                }
            }

            // Start receiving thread
            new Thread(() -> {
                try {
                    String line;
                    while ((line = in.readLine()) != null) {
                        String[] parts = line.split("\\|", 3);
                        if (parts.length == 3) {
                            addMessageToChat(parts[0], parts[1], parts[2]);
                        } else if (line.startsWith("USER_LIST|")) {
                            updateUserList(line.substring(10));
                        } else if (line.equals("PING")) {
                            out.println("PONG");
                        }
                    }
                } catch (Exception e) {
                    addMessageToChat("System", "Disconnected from server", timeFormat.format(new Date()));
                }
            }).start();

            // Open admin panel if admin
            // Open admin panel if admin
            if (currentRole.equals("admin")) {
                SwingUtilities.invokeLater(() -> {
                    String dbName = (serverPort == 1234) ? "chat_system_1" : "chat_system_2";
                    new AdminPanel(serverPort, dbName).setVisible(true);
                });
            }

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "❌ Connection failed!");
        }
    }

    private static void showLoginDialog() {
        // Just a placeholder for any pre-login setup
    }

    private static void createTelegramStyleGUI() {
        frame = new JFrame("Telegram Chat - " + currentUsername);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 700);
        frame.setLocationRelativeTo(null);
        frame.setBackground(new Color(50, 50, 50));

        // Main panel
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(new Color(50, 50, 50));

        // Header panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(34, 34, 34));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JLabel titleLabel = new JLabel("💬 Chat Room");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(Color.WHITE);

        JLabel userLabel = new JLabel("👤 " + currentUsername + " • " + currentRole);
        userLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        userLabel.setForeground(new Color(150, 150, 150));

        JLabel onlineLabel = new JLabel("● Online");
        onlineLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        onlineLabel.setForeground(new Color(46, 204, 113));

        JPanel rightHeader = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightHeader.setBackground(new Color(34, 34, 34));
        rightHeader.add(userLabel);
        rightHeader.add(onlineLabel);

        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(rightHeader, BorderLayout.EAST);

        // Chat area panel
        JPanel chatPanel = new JPanel(new BorderLayout());
        chatPanel.setBackground(new Color(50, 50, 50));
        chatPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        chatPane = new JTextPane();
        chatPane.setEditable(false);
        chatPane.setBackground(new Color(50, 50, 50));
        chatPane.setForeground(Color.WHITE);
        chatPane.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        JScrollPane scrollPane = new JScrollPane(chatPane);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(new Color(50, 50, 50));

        // User list panel
        JPanel userListPanel = new JPanel(new BorderLayout());
        userListPanel.setBackground(new Color(45, 45, 45));
        userListPanel.setPreferredSize(new Dimension(200, 0));
        userListPanel.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, new Color(60, 60, 60)));

        JLabel usersTitle = new JLabel("  📱 Online Users");
        usersTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        usersTitle.setForeground(Color.WHITE);
        usersTitle.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        usersTitle.setBackground(new Color(45, 45, 45));

        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        userList.setBackground(new Color(45, 45, 45));
        userList.setForeground(Color.WHITE);
        userList.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        userList.setBorder(null);
        userList.setCellRenderer(new UserListRenderer());

        JScrollPane userScroll = new JScrollPane(userList);
        userScroll.setBorder(null);
        userScroll.getViewport().setBackground(new Color(45, 45, 45));

        userListPanel.add(usersTitle, BorderLayout.NORTH);
        userListPanel.add(userScroll, BorderLayout.CENTER);

        // Input panel
        JPanel inputPanel = new JPanel(new BorderLayout(10, 0));
        inputPanel.setBackground(new Color(45, 45, 45));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        inputField = new JTextField();
        inputField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        inputField.setBackground(new Color(60, 60, 60));
        inputField.setForeground(Color.WHITE);
        inputField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(70, 70, 70)),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        inputField.setCaretColor(Color.WHITE);

        JButton sendButton = new JButton("📤 Send");
        sendButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        sendButton.setBackground(new Color(0, 122, 255));
        sendButton.setForeground(Color.WHITE);
        sendButton.setFocusPainted(false);
        sendButton.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        sendButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        sendButton.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                sendButton.setBackground(new Color(0, 102, 215));
            }

            public void mouseExited(MouseEvent e) {
                sendButton.setBackground(new Color(0, 122, 255));
            }
        });

        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        // Assemble
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, chatPanel, userListPanel);
        splitPane.setDividerLocation(650);
        splitPane.setDividerSize(1);
        splitPane.setBorder(null);

        chatPanel.add(scrollPane, BorderLayout.CENTER);

        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(splitPane, BorderLayout.CENTER);
        mainPanel.add(inputPanel, BorderLayout.SOUTH);

        frame.add(mainPanel);
        frame.setVisible(true);

        // Send message action
        sendButton.addActionListener(e -> sendMessage());
        inputField.addActionListener(e -> sendMessage());

        // Initial user list update
        userListModel.addElement(currentUsername + " (You)");
    }

    private static void sendMessage() {
        String text = inputField.getText().trim();
        if (!text.isEmpty()) {
            out.println(text);
            inputField.setText("");
        }
    }

    private static void addMessageToChat(String username, String message, String timestamp) {
        SwingUtilities.invokeLater(() -> {
            try {
                javax.swing.text.Document doc = chatPane.getDocument();
                javax.swing.text.StyledDocument styledDoc = (javax.swing.text.StyledDocument) doc;

                javax.swing.text.StyleContext styleContext = javax.swing.text.StyleContext.getDefaultStyleContext();

                // Timestamp style
                javax.swing.text.Style timestampStyle = styleContext.addStyle("timestamp", null);
                timestampStyle.addAttribute(javax.swing.text.StyleConstants.FontFamily, "Segoe UI");
                timestampStyle.addAttribute(javax.swing.text.StyleConstants.FontSize, 10);
                timestampStyle.addAttribute(javax.swing.text.StyleConstants.Foreground, new Color(150, 150, 150));

                // Username style
                javax.swing.text.Style usernameStyle = styleContext.addStyle("username", null);
                usernameStyle.addAttribute(javax.swing.text.StyleConstants.FontFamily, "Segoe UI");
                usernameStyle.addAttribute(javax.swing.text.StyleConstants.FontSize, 12);
                usernameStyle.addAttribute(javax.swing.text.StyleConstants.Bold, true);
                usernameStyle.addAttribute(javax.swing.text.StyleConstants.Foreground,
                        username.equals(currentUsername) ? new Color(0, 122, 255) : new Color(46, 204, 113));

                // Message style
                javax.swing.text.Style messageStyle = styleContext.addStyle("message", null);
                messageStyle.addAttribute(javax.swing.text.StyleConstants.FontFamily, "Segoe UI");
                messageStyle.addAttribute(javax.swing.text.StyleConstants.FontSize, 13);
                messageStyle.addAttribute(javax.swing.text.StyleConstants.Foreground, Color.WHITE);

                styledDoc.insertString(styledDoc.getLength(), "[" + timestamp + "] ", timestampStyle);
                styledDoc.insertString(styledDoc.getLength(), username + ": ", usernameStyle);
                styledDoc.insertString(styledDoc.getLength(), message + "\n", messageStyle);

                // Auto-scroll to bottom
                chatPane.setCaretPosition(styledDoc.getLength());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private static void updateUserList(String users) {
        SwingUtilities.invokeLater(() -> {
            userListModel.clear();
            String[] userArray = users.split(",");
            for (String user : userArray) {
                if (user.equals(currentUsername)) {
                    userListModel.addElement(user + " (You)");
                } else {
                    userListModel.addElement(user);
                }
            }
        });
    }

    static class UserListRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {
            Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (!isSelected) {
                setBackground(new Color(45, 45, 45));
                setForeground(Color.WHITE);
            }
            setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
            setFont(new Font("Segoe UI", Font.PLAIN, 12));

            String text = value.toString();
            if (text.contains("(You)")) {
                setIcon(new ImageIcon()); // Would need actual icon
                setForeground(new Color(0, 122, 255));
            } else {
                setIcon(new ImageIcon());
            }

            return c;
        }
    }
}