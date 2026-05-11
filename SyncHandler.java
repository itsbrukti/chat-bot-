import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;

public class SyncHandler implements Runnable {

    private Socket socket;

    private DatabaseManager db;

    public SyncHandler(Socket socket,
                       String dbName) {

        this.socket = socket;

        this.db = new DatabaseManager(dbName);
    }

    @Override
    public void run() {

        try {

            BufferedReader in =
                    new BufferedReader(
                            new InputStreamReader(
                                    socket.getInputStream()
                            )
                    );

            String data = in.readLine();

            String[] parts =
                    data.split("\\|");

            // REGISTER SYNC
            if (parts[0].equals("REGISTER")) {

                db.registerUser(
                        parts[1],
                        parts[2]
                );
            }

            // MESSAGE SYNC
            else if (parts[0].equals("MESSAGE")) {

                String username =
                        parts[1];

                String message =
                        parts[2];

                int userId =
                        db.getUserId(username);

                db.saveMessage(
                        userId,
                        username,
                        message
                );

                System.out.println(
                        "Message synchronized"
                );
            }

        } catch (Exception e) {

            e.printStackTrace();
        }
    }
}