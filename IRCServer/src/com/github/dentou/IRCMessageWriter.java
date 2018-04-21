package com.github.dentou;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Queue;


public class IRCMessageWriter { // todo handle partial write (for file transfer)
    private Queue<String> writeQueue = new LinkedList<String>();
    private String currentMessage = null;
    private ByteBuffer buffer = ByteBuffer.allocate(IRCConstants.MESSAGE_BUFFER_SIZE);
    private IRCSocket ircSocket;

    public IRCMessageWriter(IRCSocket ircSocket) {
        this.ircSocket = ircSocket;
    }

    public void write() throws IOException {
        if (isEmpty()) {
            System.out.println("Writer is empty");
            return;
        }
        if (currentMessage == null) {
            currentMessage = writeQueue.poll();
            String message = currentMessage;
            System.out.println("Message before sent: " + message);
            buffer.clear();
            buffer.put(message.getBytes());
            buffer.flip();
            ircSocket.write(buffer);
            if (!buffer.hasRemaining()) {
                currentMessage = null;
            }
        } else {
            System.out.println("Message before sent: " + currentMessage);
            ircSocket.write(buffer);
            if (!buffer.hasRemaining()) {
                currentMessage = null;
            }
        }
    }

    public void enqueue(String message) {
        this.writeQueue.add(message);
    }

    public boolean isEmpty() {
        return this.writeQueue.isEmpty() && this.currentMessage == null;
    }
}
