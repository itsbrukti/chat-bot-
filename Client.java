import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;

public class Client {

    static BufferedReader in;
    static PrintWriter out;

    static JTextArea chatArea;

    public static void main(String[] args) {

        try {

            // CONNECT TO LOAD BALANCER
            Socket lbSocket =
                    new Socket("localhost", 5000);

            BufferedReader lbIn =
                    new BufferedReader(
                            new InputStreamReader(
                                    lbSocket.getInputStream()
                            )
                    );

            int serverPort =
                    Integer.parseInt(lbIn.readLine());

            lbSocket.close();

            // CONNECT TO SERVER
            Socket socket =
                    new Socket("localhost", serverPort);

            in = new BufferedReader(
                    new InputStreamReader(
                            socket.getInputStream()
                    )
            );

            out = new PrintWriter(
                    socket.getOutputStream(),
                    true
            );

            // LOGIN OR REGISTER
            String[] options = {
                    "Login",
                    "Register"
            };

            int choice =
                    JOptionPane.showOptionDialog(
                            null,
                            "Choose Option",
                            "Authentication",
                            JOptionPane.DEFAULT_OPTION,
                            JOptionPane.INFORMATION_MESSAGE,
                            null,
                            options,
                            options[0]
                    );

            String username =
                    JOptionPane.showInputDialog(
                            "Enter Username:"
                    );

            String password =
                    JOptionPane.showInputDialog(
                            "Enter Password:"
                    );

            // REGISTER
            if (choice == 1) {

                out.println("REGISTER");
                out.println(username);
                out.println(password);

                String response = in.readLine();

                if (response.equals("REGISTER_SUCCESS")) {

                    JOptionPane.showMessageDialog(
                            null,
                            "Registration Successful"
                    );

                } else {

                    JOptionPane.showMessageDialog(
                            null,
                            "Username Already Exists"
                    );

                    return;
                }
            }

            // LOGIN
            out.println("LOGIN");
            out.println(username);
            out.println(password);

            String response = in.readLine();

            if (!response.equals("LOGIN_SUCCESS")) {

                JOptionPane.showMessageDialog(
                        null,
                        "Login Failed"
                );

                return;
            }

            String role = in.readLine();

            JFrame frame = new JFrame();

            chatArea = new JTextArea();
            chatArea.setEditable(false);

            JTextField input =
                    new JTextField();

            JButton sendBtn =
                    new JButton("Send");

            JPanel bottom =
                    new JPanel(new BorderLayout());

            bottom.add(input, BorderLayout.CENTER);
            bottom.add(sendBtn, BorderLayout.EAST);

            frame.setLayout(new BorderLayout());

            frame.add(
                    new JScrollPane(chatArea),
                    BorderLayout.CENTER
            );

            frame.add(bottom, BorderLayout.SOUTH);

            frame.setSize(500, 600);

            frame.setTitle(username + " - " + role);

            frame.setDefaultCloseOperation(
                    JFrame.EXIT_ON_CLOSE
            );

            frame.setVisible(true);

            // LOAD OLD MESSAGES
            String msg;

            while (!(msg = in.readLine())
                    .equals("END_MESSAGES")) {

                chatArea.append(msg + "\n");
            }

            // RECEIVE THREAD
            new Thread(() -> {

                try {

                    String message;

                    while ((message =
                                    in.readLine()) != null) {

                        chatArea.append(
                                message + "\n"
                        );
                    }

                } catch (Exception e) {

                    chatArea.append(
                            "Disconnected\n"
                    );
                }

            }).start();

            // SEND MESSAGE
            sendBtn.addActionListener(e -> {

                String text = input.getText();

                if (!text.isEmpty()) {

                    out.println(text);

                    input.setText("");
                }
            });

            input.addActionListener(
                    e -> sendBtn.doClick()
            );

            // ADMIN PANEL
            if (role.equals("admin")) {

                SwingUtilities.invokeLater(() -> {
                    new AdminPanel();
                });
            }

        } catch (Exception e) {

            e.printStackTrace();
        }
    }
}