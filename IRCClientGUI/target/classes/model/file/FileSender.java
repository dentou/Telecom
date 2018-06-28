package com.github.dentou.model.file;

import com.github.dentou.utils.ClientConstants;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

public class FileSender {

    private final SocketChannel socketChannel;
    private final FileChannel fileChannel;

    private final FileMetadata fileMetadata;

    private boolean blocking = true;

    private long bytesSent = 0l;

    public FileSender(SocketChannel socketChannel, FileMetadata fileMetadata) throws IOException {
        this(socketChannel, fileMetadata, true);
    }

    public FileSender(SocketChannel socketChannel, FileMetadata fileMetadata, boolean blocking) throws IOException {
        if (Objects.isNull(socketChannel) || Objects.isNull(fileMetadata)) {
            throw new IllegalArgumentException("socketChannel and fileMetadata required");
        }

        this.socketChannel = socketChannel;
        this.fileMetadata = fileMetadata;
        this.fileChannel = FileChannel.open(fileMetadata.getFilePath(), StandardOpenOption.READ);
        this.blocking = blocking;
    }

    public long send() throws IOException {

        if (!blocking) {
            return transfer();
        }

        while (!done()) {
            transfer();
        }

        return bytesSent;

    }

    private long transfer() throws IOException {
        long transferred = fileChannel.transferTo(fileMetadata.getPosition(), ClientConstants.TRANSFER_MAX_SIZE, socketChannel);
        bytesSent += transferred;
        this.fileMetadata.addToPosition(transferred);
        return transferred;
    }

    public boolean done() {
        return fileMetadata.done();
    }

    public void close() throws IOException {
        this.socketChannel.close();
        this.fileChannel.close();
    }


}
