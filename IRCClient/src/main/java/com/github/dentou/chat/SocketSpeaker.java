package com.github.dentou.chat;

import java.io.IOException;
import java.util.Scanner;

public class SocketSpeaker implements Runnable{
    private IRCSocket ircSocket;
    private IRCClient ircClient;
    private volatile boolean isClosed = false;

    public SocketSpeaker(IRCSocket ircSocket, IRCClient ircClient) {
        this.ircSocket = ircSocket;
        this.ircClient = ircClient;
    }

    @Override
    public void run() {
        Scanner scanner = new Scanner(System.in);

        while (!isClosed) {
            System.out.print("Input: ");
            String line = scanner.nextLine();

            if (line.equals("exit") || isClosed) {
                ircClient.exit();
                return;
            }

            String message = line + "\r\n";

            try {
                sendMessage(message);
            } catch (IOException e) {
                e.printStackTrace();
                return;
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
    public boolean isClosed() {
        return isClosed;
    }
}
