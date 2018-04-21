package com.github.dentou;

import java.io.IOException;
import java.util.List;

public class SocketListener implements Runnable{
    private IRCSocket ircSocket;
    private boolean isClosed = false;

    public SocketListener(IRCSocket ircSocket) {
        this.ircSocket = ircSocket;
    }

    @Override
    public void run() {
        while (true) {
            if (isClosed) {
                try {
                    ircSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return;
            }
            try {
                List<IRCMessage> messages = readMessages();
                for (IRCMessage message : messages) {
                    System.out.println("\nFrom server: " + message.getMessage());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


    }

    public List<IRCMessage> readMessages() throws IOException {
        List<IRCMessage> messages;
        synchronized (ircSocket) {
            //System.out.println("Listening...");
            messages = ircSocket.getMessages();

        }
        return messages;
    }

    public void close() {
        isClosed = true;
    }
}
