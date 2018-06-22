package com.github.dentou.chat;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.List;

public class IRCSocket {
    private final SocketChannel socketChannel;
    private long id;
    private IRCMessageReader reader;
    private IRCMessageWriter writer;

    private boolean endOfStreamReached = false;



    public IRCSocket(SocketChannel socketChannel, boolean blocking) throws IOException{
        this.socketChannel = socketChannel;
        this.socketChannel.configureBlocking(blocking);

        this.reader = new IRCMessageReader(this);
        this.writer = new IRCMessageWriter(this);
    }

    public SocketChannel getSocketChannel() {
        return this.socketChannel;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public SelectionKey getSelectionKey(Selector selector) {
        return this.socketChannel.keyFor(selector);
    }


    public boolean isEndOfStreamReached() {
        return endOfStreamReached;
    }
    public boolean isWriterEmpty() {
        return writer.isEmpty();
    }

    public List<IRCMessage> getMessages() throws IOException {
        return this.reader.read();
    }

    public void enqueue(String message) {
        writer.enqueue(message);
    }

    public void sendMessages() throws IOException {
        writer.write();
    }


    public int read(ByteBuffer buffer) throws IOException {
        int totalBytesRead = 0;
        while (true) {
            int bytesRead = this.socketChannel.read(buffer);

            if (bytesRead == -1) {
                this.endOfStreamReached = true;
                break;
            } else if (bytesRead == 0) {
                break;
            }

            totalBytesRead += bytesRead;

        }
        return totalBytesRead;
    }

    public int write(ByteBuffer buffer) throws IOException {
        int bytesWritten      = this.socketChannel.write(buffer);
        int totalBytesWritten = bytesWritten;

        while(bytesWritten > 0 && buffer.hasRemaining()){
            bytesWritten = this.socketChannel.write(buffer);
            totalBytesWritten += bytesWritten;
        }

        return totalBytesWritten;
    }

    public SelectionKey register(Selector selector, int ops) throws ClosedChannelException {
        return this.socketChannel.register(selector, ops);
    }

}
