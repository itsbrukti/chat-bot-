import java.net.*;
import java.io.*;
import java.util.*;

public class Server {

    static List<ClientHandler> clients = new ArrayList<>();

    public static void main(String[] args) {

        try (ServerSocket serverSocket = new ServerSocket(1234)) {

            System.out.println("Server started on port 1234...");

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Client connected");

                ClientHandler handler = new ClientHandler(socket);
                clients.add(handler);

                new Thread(handler).start();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void broadcast(String message, ClientHandler sender) {
        for (ClientHandler c : clients) {
            if (c != sender) {
                c.send(message);
            }
        }
    }
}