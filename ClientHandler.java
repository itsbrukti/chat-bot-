// ===============================
// ClientHandler.java
// FULL UPDATED VERSION
// ===============================

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

public class ClientHandler implements Runnable {

    private Socket socket;

    private BufferedReader in;

    private PrintWriter out;

    private String username;

    private String role;

    private DatabaseManager db;

    private int serverPort;

    public ClientHandler(Socket socket,
                         String dbName,
                         int serverPort) {

        this.socket = socket;

        this.db = new DatabaseManager(dbName);

        this.serverPort = serverPort;
    }

    @Override
    public void run() {

        try {

            in = new BufferedReader(
                    new InputStreamReader(
                            socket.getInputStream()
                    )
            );

            out = new PrintWriter(
                    socket.getOutputStream(),
                    true
            );

            // =========================
            // LOGIN / REGISTER
            // =========================
            while (true) {

                String choice = in.readLine();

                // =========================
                // REGISTER
                // =========================
                if ("REGISTER".equals(choice)) {

                    String user = in.readLine();

                    String pass = in.readLine();

                    boolean success =
                            db.registerUser(user, pass);

                    if (success) {

                        // SYNC TO OTHER DATABASE
                        syncRegister(user, pass);

                        out.println("REGISTER_SUCCESS");

                    } else {

                        out.println("REGISTER_FAILED");
                    }
                }

                // =========================
                // LOGIN
                // =========================
                else if ("LOGIN".equals(choice)) {

                    username = in.readLine();

                    String password = in.readLine();

                    role =
                            db.loginUser(
                                    username,
                                    password
                            );

                    if (role != null) {

                        out.println("LOGIN_SUCCESS");

                        out.println(role);

                        // LOAD ALL OLD MESSAGES
                        ArrayList<String> oldMessages =
                                db.loadMessages();

                        for (String msg : oldMessages) {

                            out.println(msg);
                        }

                        out.println("END_MESSAGES");

                        break;

                    } else {

                        out.println("LOGIN_FAILED");
                    }
                }
            }

            // =========================
            // CHAT LOOP
            // =========================
            String msg;

            while ((msg = in.readLine()) != null) {

                String fullMessage =
                        username + ": " + msg;

                int userId =
                        db.getUserId(username);

                // SAVE TO LOCAL DATABASE
                db.saveMessage(
                        userId,
                        username,
                        msg
                );

                // REPLICATE TO OTHER DATABASE
                syncMessage(username, msg);

                // BROADCAST TO USERS
                ServerNode.broadcast(
                        fullMessage,
                        this
                );
            }

        } catch (Exception e) {

            System.out.println(
                    username + " disconnected"
            );
        }
    }

    // =========================
    // SEND MESSAGE TO CLIENT
    // =========================
    public void send(String message) {

        out.println(message);
    }

    // =========================
    // REPLICATE REGISTER
    // =========================
    private void syncRegister(String username,
                              String password) {

        int targetPort;

        // SERVER1 -> SERVER2
        if (serverPort == 1234) {

            targetPort = 2235;

        }

        // SERVER2 -> SERVER1
        else {

            targetPort = 2234;
        }

        try {

            Socket s =
                    new Socket(
                            "localhost",
                            targetPort
                    );

            PrintWriter out =
                    new PrintWriter(
                            s.getOutputStream(),
                            true
                    );

            out.println(
                    "REGISTER|" +
                    username +
                    "|" +
                    password
            );

            s.close();

        } catch (Exception e) {

            System.out.println(
                    "Register Sync Failed"
            );
        }
    }

    // =========================
    // REPLICATE MESSAGE
    // =========================
    private void syncMessage(String username,
                             String message) {

        int targetPort;

        // SERVER1 -> SERVER2
        if (serverPort == 1234) {

            targetPort = 2235;

        }

        // SERVER2 -> SERVER1
        else {

            targetPort = 2234;
        }

        try {

            Socket s =
                    new Socket(
                            "localhost",
                            targetPort
                    );

            PrintWriter out =
                    new PrintWriter(
                            s.getOutputStream(),
                            true
                    );

            out.println(
                    "MESSAGE|" +
                    username +
                    "|" +
                    message
            );

            s.close();

        } catch (Exception e) {

            System.out.println(
                    "Message Sync Failed"
            );
        }
    }
}