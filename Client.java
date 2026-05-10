import javax.swing.*;
import java.awt.*;
import java.net.*;
import java.io.*;

public class Client {

    static BufferedReader in;
    static PrintWriter out;
    static String username;

    public static void main(String[] args) {

        JFrame frame = new JFrame();
        JTextArea chatArea = new JTextArea();
        JTextField input = new JTextField();
        JButton sendBtn = new JButton("Send");

        chatArea.setEditable(false);

        frame.setLayout(new BorderLayout());
        frame.add(new JScrollPane(chatArea), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(input, BorderLayout.CENTER);
        bottom.add(sendBtn, BorderLayout.EAST);

        frame.add(bottom, BorderLayout.SOUTH);

        frame.setSize(400, 500);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        try {
            Socket socket = new Socket("localhost", 1234);

            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            // 🟢 Start receive thread FIRST (fixes first-message bug)
            new Thread(() -> {
                try {
                    String msg;
                    while ((msg = in.readLine()) != null) {

                        // remove server prompt noise
                        if (msg.equals("Enter username:")) continue;

                        chatArea.append(msg + "\n");
                        chatArea.setCaretPosition(chatArea.getDocument().getLength());
                    }
                } catch (Exception e) {
                    chatArea.append("Disconnected\n");
                }
            }).start();

            // 🟢 Username input
            username = JOptionPane.showInputDialog("Enter username:");
            frame.setTitle("Chat - " + username);

            // send username to server
            out.println(username);

            frame.setVisible(true);

        } catch (Exception e) {
            e.printStackTrace();
        }

        // 🟢 SEND MESSAGE
        sendBtn.addActionListener(e -> {
            String msg = input.getText();

            if (!msg.isEmpty()) {

                // ✔ show immediately with username
                chatArea.append(username + ": " + msg + "\n");

                // send to server
                out.println(msg);

                input.setText("");
            }
        });

        input.addActionListener(e -> sendBtn.doClick());
    }
}