package com.github.dentou.file;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

public class FileSender {

    private final SocketChannel socketChannel;
    private final FileChannel fileChannel;

    private final FileMetaData fileMetaData;

    private boolean blocking = true;

    private long bytesSent = 0l;

    public FileSender(SocketChannel socketChannel, String path, long initialPosition) throws IOException {
        this(socketChannel, path, initialPosition, true);
    }

    public FileSender(SocketChannel socketChannel, String path, long initialPosition, boolean blocking) throws IOException {
        if (Objects.isNull(socketChannel) || StringUtils.isEmpty(path)) {
            throw new IllegalArgumentException("socketChannel and fileMetaData required");
        }

        this.socketChannel = socketChannel;
        Path filePath = Paths.get(path);
        this.fileChannel = FileChannel.open(filePath, StandardOpenOption.READ);
        this.fileMetaData = new FileMetaData(filePath, fileChannel.size(), initialPosition);
        this.blocking = blocking;
    }

    public long send() throws IOException {

        System.out.println("Start sending " + fileChannel.size() + " bytes");
        if (!blocking) {
            return transfer();
        }

        while (!done()) {
            transfer();
        }

        return bytesSent;

    }

    private long transfer() throws IOException {
        long transferred = fileChannel.transferTo(fileMetaData.getPosition(), FileConstants.TRANSFER_MAX_SIZE, socketChannel);
        System.out.println("Bytes sent: " + transferred);
        bytesSent += transferred;
        this.fileMetaData.addToPosition(transferred);
        return transferred;
    }

    public boolean done() {
        return fileMetaData.done();
    }

    public void close() throws IOException {
        this.socketChannel.close();
        this.fileChannel.close();
    }


}
