import java.net.ServerSocket;
import java.net.Socket;

public class SyncManager implements Runnable {

    private int port;

    private String dbName;

    public SyncManager(int port,
                       String dbName) {

        this.port = port;
        this.dbName = dbName;
    }

    @Override
    public void run() {

        try (ServerSocket serverSocket =
                     new ServerSocket(port)) {

            System.out.println(
                    "Sync Server Running On Port: " + port
            );

            while (true) {

                Socket socket =
                        serverSocket.accept();

                new Thread(
                        new SyncHandler(
                                socket,
                                dbName
                        )
                ).start();
            }

        } catch (Exception e) {

            e.printStackTrace();
        }
    }
}