import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class LoadBalancer {

    static int current = 0;

    static int[] servers = {1234, 1235};

    public static void main(String[] args) {

        try (ServerSocket serverSocket =
                     new ServerSocket(5000)) {

            System.out.println(
                    "Load Balancer Running..."
            );

            while (true) {

                Socket socket =
                        serverSocket.accept();

                PrintWriter out =
                        new PrintWriter(
                                socket.getOutputStream(),
                                true
                        );

                // ROUND ROBIN
                int serverPort =
                        servers[current];

                current =
                        (current + 1)
                                % servers.length;

                out.println(serverPort);

                socket.close();
            }

        } catch (Exception e) {

            e.printStackTrace();
        }
    }
}