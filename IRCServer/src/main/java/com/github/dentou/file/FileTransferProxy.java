package com.github.dentou.file;

import com.github.dentou.chat.IRCConstants;
import com.github.dentou.chat.IRCSocket;

import java.io.IOException;
import java.nio.ByteBuffer;

public class FileTransferProxy {
    private final String fileKey;
    private IRCSocket inSocket;
    private IRCSocket outSocket;

    private boolean transferEnded = false;


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

    public boolean transfer() throws IOException {

        if (transferEnded)  {
            return false;
        }

        inSocket.read(buffer);
        buffer.flip();
        outSocket.write(buffer);
        buffer.compact();
        if (inSocket.isEndOfStreamReached()) {
            transferEnded = true;
        }
        return true;
    }

    public synchronized void endTransfer(boolean ended) {
        this.transferEnded = ended;
    }

    public synchronized boolean isTransferEnded() {
        return this.transferEnded;
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
