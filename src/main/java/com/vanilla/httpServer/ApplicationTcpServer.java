package com.vanilla.httpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class ApplicationTcpServer {

    public static void main(String[] args) {
        int port = 8080;
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server listening on port " + port);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connectect from " + clientSocket.getRemoteSocketAddress());
                new ApplicationTcpServer().sendJsonResponse(clientSocket);
                clientSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendJsonResponse(Socket clientSocket) {
        try (OutputStream os = clientSocket.getOutputStream()) {
            String jsonResponse = "{ \"message\": \"Hello world!\" }";
            String httpHeaders = "HTTP/1.1 200 OK\r\n"
                    + "Content-Type: application/json\r\n"
                    + "Content-Length: " + jsonResponse.length() + "\r\n"
                    + "\r\n";
            os.write(httpHeaders.getBytes());
            os.write(jsonResponse.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
