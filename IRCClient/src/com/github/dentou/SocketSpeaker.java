package com.github.dentou;

import java.io.IOException;
import java.util.Scanner;

public class SocketSpeaker implements Runnable{
    private IRCSocket ircSocket;
    private IRCClient client;
    private boolean isClosed = false;

    public SocketSpeaker(IRCSocket ircSocket, IRCClient client) {
        this.ircSocket = ircSocket;
        this.client = client;
    }

    @Override
    public void run() {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("Input: ");
            String line = scanner.nextLine();

            if (line.equals("exit")) {
                isClosed = true;
                client.exit();
                return;
            }

            String message = line + "\r\n";

            try {
                sendMessage(message);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    private void sendMessage(String message) throws IOException{
        synchronized (ircSocket) {
            System.out.println("Speaking...");
            ircSocket.enqueue(message);
            ircSocket.sendMessages();
        }
    }

    public void close() {
        isClosed = true;
    }
}
