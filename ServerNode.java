import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;

public class ServerNode {

    public static List<ClientHandler> clients =
            Collections.synchronizedList(new ArrayList<>());

    private int port;

    private String dbName;

    public ServerNode(int port,
                      String dbName) {

        this.port = port;
        this.dbName = dbName;
    }

    public void startServer() {

        try (ServerSocket serverSocket =
                     new ServerSocket(port)) {

            System.out.println(
                    "Server Running On Port: " + port
            );

            // START SYNC SERVER
            new Thread(
                    new SyncManager(
                            port + 1000,
                            dbName
                    )
            ).start();

            while (true) {

                Socket socket =
                        serverSocket.accept();

                System.out.println(
                        "Client Connected To Server " + port
                );

                ClientHandler handler =
                        new ClientHandler(
                                socket,
                                dbName,
                                port
                        );

                clients.add(handler);

                new Thread(handler).start();
            }

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    public static void broadcast(String message,
                                 ClientHandler sender) {

        synchronized (clients) {

            for (ClientHandler c : clients) {

                c.send(message);
            }
        }
    }

    public static void main(String[] args) {

        System.out.println(
                "Starting Distributed Servers..."
        );

        // SERVER 1
        new Thread(() ->
                new ServerNode(
                        1234,
                        "chat_system_1"
                ).startServer()
        ).start();

        // SERVER 2
        new Thread(() ->
                new ServerNode(
                        1235,
                        "chat_system_2"
                ).startServer()
        ).start();
    }
}