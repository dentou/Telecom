package com.github.dentou.file;

import com.github.dentou.chat.IRCConstants;
import com.github.dentou.chat.IRCSocket;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

public class FileTransferProxy {
    private final String fileKey;
    private final IRCSocket inSocket;
    private final IRCSocket outSocket;

    private AtomicBoolean transferEnded = new AtomicBoolean(false);


    private final ByteBuffer checkBuffer = ByteBuffer.allocate(100);


    private ByteBuffer buffer = ByteBuffer.allocate(IRCConstants.FILE_BUFFER_SIZE);

    public FileTransferProxy(String fileKey, IRCSocket inSocket, IRCSocket outSocket) {
        this.fileKey = fileKey;
        this.inSocket = inSocket;
        this.outSocket = outSocket;
    }

    public String getFileKey() {
        return fileKey;
    }

    public IRCSocket getInSocket() {
        return inSocket;
    }

    public IRCSocket getOutSocket() {
        return outSocket;
    }

    public void transfer() throws IOException {

        if (transferEnded.get()) {
            return;
        }

//        inSocket.read(buffer);
//        buffer.flip();
//        outSocket.write(buffer);
//        buffer.compact();
//        if (inSocket.isEndOfStreamReached()) {
//            transferEnded = true;
//        }
//        return true;


        inSocket.read(buffer);
        if (inSocket.isEndOfStreamReached()) {
            transferEnded.set(true);
        }
        buffer.flip();
        outSocket.write(buffer);
        int check = outSocket.getSocketChannel().read(checkBuffer);
        if (check == -1) {
            transferEnded.set(true);
        }

//        SocketChannel writeChannel = outSocket.getSocketChannel();
//        while (buffer.hasRemaining()) {
//            writeChannel.write(buffer);
//        }
//        int check = writeChannel.read(checkBuffer);
//        if (check == -1) {
//            transferEnded.set(true);
//        }
        if (transferEnded.get()) {
            while (buffer.hasRemaining()) {
                outSocket.write(buffer);
            }
        }
        buffer.compact();

    }


    public void endTransfer(boolean ended) {
        this.transferEnded.set(ended);
    }

    public boolean isTransferEnded() {
        return this.transferEnded.get();
    }

    @Override
    public String toString() {
        return "FileTransferProxy{" +
                "fileKey='" + fileKey + '\'' +
                ", inSocket=" + inSocket.getId() +
                ", outSocket=" + outSocket.getId() +
                '}';
    }
}
