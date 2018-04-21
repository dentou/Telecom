package com.github.dentou;

import java.io.IOException;
import java.util.Scanner;

public class SocketSpeaker implements Runnable{
    private IRCSocket ircSocket;

    public SocketSpeaker(IRCSocket ircSocket) {
        this.ircSocket = ircSocket;
    }

    @Override
    public void run() {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("Input: ");
            String line = scanner.nextLine();

            if (line.equals("exit")) {
                try {
                    ircSocket.close();
                    break;
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
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
}
