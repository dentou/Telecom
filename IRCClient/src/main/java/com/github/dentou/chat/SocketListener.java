package com.github.dentou.chat;

import java.io.IOException;
import java.util.List;

public class SocketListener implements Runnable{
    private IRCSocket ircSocket;
    private IRCClient ircClient;
    private volatile boolean isClosed = false;

    public SocketListener(IRCSocket ircSocket, IRCClient ircClient) {

        this.ircSocket = ircSocket;
        this.ircClient = ircClient;
    }

    @Override
    public void run() {
        while (!isClosed) {
            try {
                List<IRCMessage> messages = readMessages();
                for (IRCMessage message : messages) {
                    System.out.println("\nFrom server: " + message.getMessage());
                }
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }


    }

    public List<IRCMessage> readMessages() throws IOException {
        List<IRCMessage> messages;
        synchronized (ircSocket) {
            //System.out.println("Listening...");
            messages = ircSocket.getMessages();

        }
        if (ircSocket.isEndOfStreamReached()) {
            System.out.println("Socket closed");
            ircClient.exit();
        }
        return messages;
    }

    public void close() {
        isClosed = true;
    }
    public boolean isClosed() {
        return isClosed;
    }
}
