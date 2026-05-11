import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ModernClient {

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
            showBeautifulLoginScreen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void showBeautifulLoginScreen() {
        JFrame loginFrame = new JFrame("Chat System");
        loginFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        loginFrame.setSize(480, 620);
        loginFrame.setLocationRelativeTo(null);
        loginFrame.setUndecorated(true);

        // Main gradient panel
        JPanel mainPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                GradientPaint gp = new GradientPaint(0, 0, new Color(30, 30, 35), 0, getHeight(),
                        new Color(20, 20, 25));
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Card panel
        JPanel cardPanel = new JPanel();
        cardPanel.setBackground(new Color(45, 45, 50));
        cardPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(60, 60, 65), 1, true),
                BorderFactory.createEmptyBorder(30, 30, 30, 30)));
        cardPanel.setLayout(new BoxLayout(cardPanel, BoxLayout.Y_AXIS));

        // Logo Section
        JLabel iconLabel = new JLabel("💬");
        iconLabel.setFont(new Font("Segoe UI", Font.PLAIN, 70));
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel titleLabel = new JLabel("Chat System");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        titleLabel.setForeground(new Color(52, 152, 219));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("Modern Distributed Messaging Platform");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitleLabel.setForeground(new Color(150, 150, 155));
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        cardPanel.add(iconLabel);
        cardPanel.add(Box.createVerticalStrut(10));
        cardPanel.add(titleLabel);
        cardPanel.add(Box.createVerticalStrut(5));
        cardPanel.add(subtitleLabel);
        cardPanel.add(Box.createVerticalStrut(30));

        // Username Field
        JLabel userLabel = new JLabel("USERNAME");
        userLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        userLabel.setForeground(new Color(150, 150, 155));
        userLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JTextField usernameField = new JTextField();
        usernameField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        usernameField.setBackground(new Color(55, 55, 60));
        usernameField.setForeground(Color.WHITE);
        usernameField.setCaretColor(Color.WHITE);
        usernameField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(70, 70, 75), 1, true),
                BorderFactory.createEmptyBorder(12, 15, 12, 15)));
        usernameField.setMaximumSize(new Dimension(300, 45));
        usernameField.setPreferredSize(new Dimension(300, 45));
        usernameField.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Password Field
        JLabel passLabel = new JLabel("PASSWORD");
        passLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        passLabel.setForeground(new Color(150, 150, 155));
        passLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPasswordField passwordField = new JPasswordField();
        passwordField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        passwordField.setBackground(new Color(55, 55, 60));
        passwordField.setForeground(Color.WHITE);
        passwordField.setCaretColor(Color.WHITE);
        passwordField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(70, 70, 75), 1, true),
                BorderFactory.createEmptyBorder(12, 15, 12, 15)));
        passwordField.setMaximumSize(new Dimension(300, 45));
        passwordField.setPreferredSize(new Dimension(300, 45));
        passwordField.setAlignmentX(Component.CENTER_ALIGNMENT);

        cardPanel.add(userLabel);
        cardPanel.add(Box.createVerticalStrut(8));
        cardPanel.add(usernameField);
        cardPanel.add(Box.createVerticalStrut(20));
        cardPanel.add(passLabel);
        cardPanel.add(Box.createVerticalStrut(8));
        cardPanel.add(passwordField);
        cardPanel.add(Box.createVerticalStrut(30));

        // Login Button (Black text)
        JButton loginButton = new JButton("LOGIN");
        loginButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        loginButton.setBackground(new Color(52, 152, 219));
        loginButton.setForeground(Color.WHITE);
        loginButton.setFocusPainted(false);
        loginButton.setBorder(BorderFactory.createEmptyBorder(12, 25, 12, 25));
        loginButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        loginButton.setMaximumSize(new Dimension(300, 45));
        loginButton.setPreferredSize(new Dimension(300, 45));
        loginButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        loginButton.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                loginButton.setBackground(new Color(41, 128, 185));
            }

            public void mouseExited(MouseEvent e) {
                loginButton.setBackground(new Color(52, 152, 219));
            }
        });

        loginButton.addActionListener(e -> {
            performLogin(usernameField.getText(), new String(passwordField.getPassword()), loginFrame);
        });

        // Register Button (Black text)
        JButton registerButton = new JButton("CREATE NEW ACCOUNT");
        registerButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        registerButton.setBackground(new Color(60, 60, 65));
        registerButton.setForeground(new Color(52, 152, 219));
        registerButton.setFocusPainted(false);
        registerButton.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        registerButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        registerButton.setMaximumSize(new Dimension(300, 40));
        registerButton.setPreferredSize(new Dimension(300, 40));
        registerButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        registerButton.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                registerButton.setBackground(new Color(70, 70, 75));
            }

            public void mouseExited(MouseEvent e) {
                registerButton.setBackground(new Color(60, 60, 65));
            }
        });

        registerButton.addActionListener(e -> {
            showRegisterDialog(loginFrame);
        });

        cardPanel.add(loginButton);
        cardPanel.add(Box.createVerticalStrut(15));
        cardPanel.add(registerButton);

        // Close button
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);
        JButton closeBtn = new JButton("✕");
        closeBtn.setFont(new Font("Arial", Font.BOLD, 14));
        closeBtn.setForeground(Color.WHITE);
        closeBtn.setBackground(new Color(231, 76, 60));
        closeBtn.setBorderPainted(false);
        closeBtn.setFocusPainted(false);
        closeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        closeBtn.addActionListener(e -> System.exit(0));

        JPanel closePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        closePanel.setOpaque(false);
        closePanel.add(closeBtn);
        topBar.add(closePanel, BorderLayout.EAST);

        mainPanel.add(topBar, BorderLayout.NORTH);
        mainPanel.add(cardPanel, BorderLayout.CENTER);

        loginFrame.add(mainPanel);
        loginFrame.setVisible(true);
    }

    private static void showRegisterDialog(JFrame parent) {
        JDialog registerDialog = new JDialog(parent, "Create Account", true);
        registerDialog.setSize(400, 500);
        registerDialog.setLocationRelativeTo(parent);
        registerDialog.setUndecorated(true);

        JPanel mainPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                GradientPaint gp = new GradientPaint(0, 0, new Color(30, 30, 35), 0, getHeight(),
                        new Color(20, 20, 25));
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel cardPanel = new JPanel();
        cardPanel.setBackground(new Color(45, 45, 50));
        cardPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(60, 60, 65), 1, true),
                BorderFactory.createEmptyBorder(25, 25, 25, 25)));
        cardPanel.setLayout(new BoxLayout(cardPanel, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel("Create Account");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        titleLabel.setForeground(new Color(52, 152, 219));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel usernameLabel = new JLabel("USERNAME");
        usernameLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        usernameLabel.setForeground(new Color(150, 150, 155));
        usernameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JTextField usernameField = new JTextField();
        usernameField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        usernameField.setBackground(new Color(55, 55, 60));
        usernameField.setForeground(Color.WHITE);
        usernameField.setCaretColor(Color.WHITE);
        usernameField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(70, 70, 75), 1, true),
                BorderFactory.createEmptyBorder(12, 15, 12, 15)));
        usernameField.setMaximumSize(new Dimension(300, 45));
        usernameField.setPreferredSize(new Dimension(300, 45));
        usernameField.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel passwordLabel = new JLabel("PASSWORD");
        passwordLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        passwordLabel.setForeground(new Color(150, 150, 155));
        passwordLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPasswordField passwordField = new JPasswordField();
        passwordField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        passwordField.setBackground(new Color(55, 55, 60));
        passwordField.setForeground(Color.WHITE);
        passwordField.setCaretColor(Color.WHITE);
        passwordField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(70, 70, 75), 1, true),
                BorderFactory.createEmptyBorder(12, 15, 12, 15)));
        passwordField.setMaximumSize(new Dimension(300, 45));
        passwordField.setPreferredSize(new Dimension(300, 45));
        passwordField.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel confirmLabel = new JLabel("CONFIRM PASSWORD");
        confirmLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        confirmLabel.setForeground(new Color(150, 150, 155));
        confirmLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPasswordField confirmField = new JPasswordField();
        confirmField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        confirmField.setBackground(new Color(55, 55, 60));
        confirmField.setForeground(Color.WHITE);
        confirmField.setCaretColor(Color.WHITE);
        confirmField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(70, 70, 75), 1, true),
                BorderFactory.createEmptyBorder(12, 15, 12, 15)));
        confirmField.setMaximumSize(new Dimension(300, 45));
        confirmField.setPreferredSize(new Dimension(300, 45));
        confirmField.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton registerBtn = new JButton("REGISTER");
        registerBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        registerBtn.setBackground(new Color(46, 204, 113));
        registerBtn.setForeground(Color.WHITE);
        registerBtn.setFocusPainted(false);
        registerBtn.setBorder(BorderFactory.createEmptyBorder(12, 25, 12, 25));
        registerBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        registerBtn.setMaximumSize(new Dimension(300, 45));
        registerBtn.setPreferredSize(new Dimension(300, 45));
        registerBtn.setAlignmentX(Component.CENTER_ALIGNMENT);

        registerBtn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                registerBtn.setBackground(new Color(39, 174, 96));
            }

            public void mouseExited(MouseEvent e) {
                registerBtn.setBackground(new Color(46, 204, 113));
            }
        });

        registerBtn.addActionListener(e -> {
            String pass = new String(passwordField.getPassword());
            String confirm = new String(confirmField.getPassword());

            if (!pass.equals(confirm)) {
                JOptionPane.showMessageDialog(registerDialog, "Passwords do not match!", "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            performRegister(usernameField.getText(), pass, registerDialog, parent);
        });

        JButton cancelBtn = new JButton("CANCEL");
        cancelBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cancelBtn.setBackground(new Color(60, 60, 65));
        cancelBtn.setForeground(new Color(150, 150, 155));
        cancelBtn.setFocusPainted(false);
        cancelBtn.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        cancelBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        cancelBtn.setMaximumSize(new Dimension(300, 40));
        cancelBtn.setPreferredSize(new Dimension(300, 40));
        cancelBtn.setAlignmentX(Component.CENTER_ALIGNMENT);

        cancelBtn.addActionListener(e -> registerDialog.dispose());

        cardPanel.add(titleLabel);
        cardPanel.add(Box.createVerticalStrut(25));
        cardPanel.add(usernameLabel);
        cardPanel.add(Box.createVerticalStrut(8));
        cardPanel.add(usernameField);
        cardPanel.add(Box.createVerticalStrut(15));
        cardPanel.add(passwordLabel);
        cardPanel.add(Box.createVerticalStrut(8));
        cardPanel.add(passwordField);
        cardPanel.add(Box.createVerticalStrut(15));
        cardPanel.add(confirmLabel);
        cardPanel.add(Box.createVerticalStrut(8));
        cardPanel.add(confirmField);
        cardPanel.add(Box.createVerticalStrut(25));
        cardPanel.add(registerBtn);
        cardPanel.add(Box.createVerticalStrut(10));
        cardPanel.add(cancelBtn);

        // Close button
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);
        JButton closeBtn = new JButton("✕");
        closeBtn.setFont(new Font("Arial", Font.BOLD, 14));
        closeBtn.setForeground(Color.WHITE);
        closeBtn.setBackground(new Color(231, 76, 60));
        closeBtn.setBorderPainted(false);
        closeBtn.setFocusPainted(false);
        closeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        closeBtn.addActionListener(e -> registerDialog.dispose());

        JPanel closePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        closePanel.setOpaque(false);
        closePanel.add(closeBtn);
        topBar.add(closePanel, BorderLayout.EAST);

        mainPanel.add(topBar, BorderLayout.NORTH);
        mainPanel.add(cardPanel, BorderLayout.CENTER);

        registerDialog.add(mainPanel);
        registerDialog.setVisible(true);
    }

    private static void performLogin(String username, String password, JFrame loginFrame) {
        try {
            Socket lbSocket = new Socket("localhost", 5000);
            BufferedReader lbIn = new BufferedReader(new InputStreamReader(lbSocket.getInputStream()));
            int serverPort = Integer.parseInt(lbIn.readLine());
            lbSocket.close();

            Socket socket = new Socket("localhost", serverPort);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            out.println("LOGIN");
            out.println(username);
            out.println(password);
            String response = in.readLine();

            if (response.equals("LOGIN_SUCCESS")) {
                currentUsername = username;
                currentRole = in.readLine();
                loginFrame.dispose();
                createModernChatGUI(serverPort);

                // Load old messages
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
                            }
                        }
                    } catch (Exception e) {
                        addMessageToChat("System", "Disconnected", timeFormat.format(new Date()));
                    }
                }).start();

                // Open admin panel only once
                if (currentRole.equals("admin")) {
                    SwingUtilities.invokeLater(() -> {
                        String dbName = (serverPort == 1234) ? "chat_system_1" : "chat_system_2";
                        new ModernAdminPanel(serverPort, dbName).setVisible(true);
                    });
                }
            } else {
                JOptionPane.showMessageDialog(loginFrame, "Invalid username or password!", "Login Failed",
                        JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(loginFrame, "Connection failed!", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void performRegister(String username, String password, JDialog registerDialog, JFrame parent) {
        try {
            Socket lbSocket = new Socket("localhost", 5000);
            BufferedReader lbIn = new BufferedReader(new InputStreamReader(lbSocket.getInputStream()));
            int serverPort = Integer.parseInt(lbIn.readLine());
            lbSocket.close();

            Socket socket = new Socket("localhost", serverPort);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            out.println("REGISTER");
            out.println(username);
            out.println(password);
            String response = in.readLine();

            if (response.equals("REGISTER_SUCCESS")) {
                JOptionPane.showMessageDialog(registerDialog, "Registration Successful! Please login.", "Success",
                        JOptionPane.INFORMATION_MESSAGE);
                registerDialog.dispose();
            } else {
                JOptionPane.showMessageDialog(registerDialog, "Username already exists!", "Registration Failed",
                        JOptionPane.ERROR_MESSAGE);
            }
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(registerDialog, "Registration failed!", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void createModernChatGUI(int serverPort) {
        frame = new JFrame("Chat System - " + currentUsername);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1100, 700);
        frame.setLocationRelativeTo(null);
        frame.getContentPane().setBackground(new Color(30, 30, 35));

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(280);
        splitPane.setDividerSize(1);
        splitPane.setBorder(null);

        // Sidebar
        JPanel sidebar = createModernSidebar();
        splitPane.setLeftComponent(sidebar);

        // Chat area
        JPanel chatArea = createModernChatArea();
        splitPane.setRightComponent(chatArea);

        frame.add(splitPane);
        frame.setVisible(true);

        userListModel = new DefaultListModel<>();
        userList.setModel(userListModel);
        userListModel.addElement(currentUsername + " (You)");
    }

    private static JPanel createModernSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setBackground(new Color(38, 38, 43));
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(50, 50, 55)));

        // Profile section
        JPanel profilePanel = new JPanel(new BorderLayout(12, 0));
        profilePanel.setBackground(new Color(38, 38, 43));
        profilePanel.setBorder(BorderFactory.createEmptyBorder(25, 20, 25, 20));

        JLabel avatarLabel = new JLabel();
        avatarLabel.setText(String.valueOf(currentUsername.charAt(0)).toUpperCase());
        avatarLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        avatarLabel.setForeground(Color.WHITE);
        avatarLabel.setHorizontalAlignment(SwingConstants.CENTER);
        avatarLabel.setPreferredSize(new Dimension(55, 55));
        avatarLabel.setBackground(new Color(52, 152, 219));
        avatarLabel.setOpaque(true);
        avatarLabel.setBorder(BorderFactory.createLineBorder(new Color(52, 152, 219), 2, true));

        JPanel userInfo = new JPanel(new GridLayout(2, 1, 0, 5));
        userInfo.setBackground(new Color(38, 38, 43));

        JLabel nameLabel = new JLabel(currentUsername);
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        nameLabel.setForeground(Color.WHITE);

        JLabel roleLabel = new JLabel(currentRole.toUpperCase());
        roleLabel.setFont(new Font("Segoe UI", Font.BOLD, 10));
        roleLabel.setForeground(currentRole.equals("admin") ? new Color(231, 76, 60) : new Color(46, 204, 113));

        userInfo.add(nameLabel);
        userInfo.add(roleLabel);

        profilePanel.add(avatarLabel, BorderLayout.WEST);
        profilePanel.add(userInfo, BorderLayout.CENTER);

        // Online users section
        JPanel usersPanel = new JPanel(new BorderLayout());
        usersPanel.setBackground(new Color(38, 38, 43));
        usersPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));

        JLabel usersTitle = new JLabel("🟢 ONLINE USERS");
        usersTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        usersTitle.setForeground(new Color(150, 150, 155));
        usersTitle.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));

        userList = new JList<>();
        userList.setBackground(new Color(38, 38, 43));
        userList.setForeground(Color.WHITE);
        userList.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        userList.setSelectionBackground(new Color(52, 152, 219));
        userList.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JScrollPane userScroll = new JScrollPane(userList);
        userScroll.setBorder(null);
        userScroll.setBackground(new Color(38, 38, 43));
        userScroll.getViewport().setBackground(new Color(38, 38, 43));

        usersPanel.add(usersTitle, BorderLayout.NORTH);
        usersPanel.add(userScroll, BorderLayout.CENTER);

        sidebar.add(profilePanel, BorderLayout.NORTH);
        sidebar.add(usersPanel, BorderLayout.CENTER);

        return sidebar;
    }

    private static JPanel createModernChatArea() {
        JPanel chatPanel = new JPanel(new BorderLayout());
        chatPanel.setBackground(new Color(30, 30, 35));

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(38, 38, 43));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 25, 15, 25));

        JLabel chatTitle = new JLabel("💬 General Chat");
        chatTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        chatTitle.setForeground(Color.WHITE);

        JLabel statusLabel = new JLabel("● Connected");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        statusLabel.setForeground(new Color(46, 204, 113));

        headerPanel.add(chatTitle, BorderLayout.WEST);
        headerPanel.add(statusLabel, BorderLayout.EAST);

        // Messages area
        chatPane = new JTextPane();
        chatPane.setEditable(false);
        chatPane.setBackground(new Color(30, 30, 35));
        chatPane.setForeground(Color.WHITE);
        chatPane.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        chatPane.setMargin(new Insets(20, 25, 20, 25));

        JScrollPane scrollPane = new JScrollPane(chatPane);
        scrollPane.setBorder(null);
        scrollPane.setBackground(new Color(30, 30, 35));
        scrollPane.getViewport().setBackground(new Color(30, 30, 35));

        // Input area
        JPanel inputPanel = new JPanel(new BorderLayout(12, 0));
        inputPanel.setBackground(new Color(38, 38, 43));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(15, 25, 20, 25));

        inputField = new JTextField();
        inputField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        inputField.setBackground(new Color(45, 45, 50));
        inputField.setForeground(Color.WHITE);
        inputField.setCaretColor(new Color(52, 152, 219));
        inputField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(55, 55, 60), 1, true),
                BorderFactory.createEmptyBorder(12, 15, 12, 15)));

        JButton sendButton = new JButton("SEND");
        sendButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        sendButton.setBackground(new Color(52, 152, 219));
        sendButton.setForeground(Color.WHITE);
        sendButton.setBorder(BorderFactory.createEmptyBorder(0, 25, 0, 25));
        sendButton.setFocusPainted(false);
        sendButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        sendButton.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                sendButton.setBackground(new Color(41, 128, 185));
            }

            public void mouseExited(MouseEvent e) {
                sendButton.setBackground(new Color(52, 152, 219));
            }
        });

        sendButton.addActionListener(e -> sendMessage());
        inputField.addActionListener(e -> sendMessage());

        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        chatPanel.add(headerPanel, BorderLayout.NORTH);
        chatPanel.add(scrollPane, BorderLayout.CENTER);
        chatPanel.add(inputPanel, BorderLayout.SOUTH);

        return chatPanel;
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

                javax.swing.text.Style timestampStyle = styleContext.addStyle("timestamp", null);
                timestampStyle.addAttribute(javax.swing.text.StyleConstants.FontFamily, "Segoe UI");
                timestampStyle.addAttribute(javax.swing.text.StyleConstants.FontSize, 10);
                timestampStyle.addAttribute(javax.swing.text.StyleConstants.Foreground, new Color(100, 100, 105));

                javax.swing.text.Style usernameStyle = styleContext.addStyle("username", null);
                usernameStyle.addAttribute(javax.swing.text.StyleConstants.FontFamily, "Segoe UI");
                usernameStyle.addAttribute(javax.swing.text.StyleConstants.FontSize, 13);
                usernameStyle.addAttribute(javax.swing.text.StyleConstants.Bold, true);
                usernameStyle.addAttribute(javax.swing.text.StyleConstants.Foreground,
                        username.equals(currentUsername) ? new Color(52, 152, 219) : new Color(46, 204, 113));

                javax.swing.text.Style messageStyle = styleContext.addStyle("message", null);
                messageStyle.addAttribute(javax.swing.text.StyleConstants.FontFamily, "Segoe UI");
                messageStyle.addAttribute(javax.swing.text.StyleConstants.FontSize, 13);
                messageStyle.addAttribute(javax.swing.text.StyleConstants.Foreground, Color.WHITE);

                styledDoc.insertString(styledDoc.getLength(), "[" + timestamp + "] ", timestampStyle);
                styledDoc.insertString(styledDoc.getLength(), username + ": ", usernameStyle);
                styledDoc.insertString(styledDoc.getLength(), message + "\n", messageStyle);

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
}