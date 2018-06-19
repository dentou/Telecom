package com.github.dentou.file;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

public class FileReceiver {

    private final SocketChannel socketChannel;

    private final FileChannel fileChannel;
    private final FileMetaData fileMetaData;

    private boolean blocking;
    private long bytesReceived = 0l;


    public FileReceiver(SocketChannel socketChannel, FileMetaData fileMetaData) throws IOException {
        this(socketChannel, fileMetaData, true);
    }

    public FileReceiver(SocketChannel socketChannel, FileMetaData fileMetaData, boolean blocking) throws IOException {
        if (Objects.isNull(socketChannel) && Objects.isNull(fileMetaData)) {
            throw new IllegalArgumentException("Socket channel and File metadata required");
        }
        if (Files.isDirectory(fileMetaData.getFilePath())) {
            throw new IllegalArgumentException("Path is a directory, not a file");

        }

        this.socketChannel = socketChannel;
        this.fileMetaData = fileMetaData;
        this.blocking = blocking;
        // Check if file exist
        boolean fileExists = Files.exists(fileMetaData.getFilePath());
        if (fileExists) {
            if (fileMetaData.getPosition() > 0) { // If resume
                this.fileChannel = FileChannel.open(fileMetaData.getFilePath(), StandardOpenOption.WRITE, StandardOpenOption.APPEND);
            } else {
                throw new FileAlreadyExistsException("File already exists");
//                boolean success = Files.deleteIfExists(fileMetaData.getFilePath());
//                System.out.println("File exists, delete: " + success);
//                this.fileChannel = FileChannel.open(fileMetaData.getFilePath(), StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW);
            }

        } else {
            this.fileChannel = FileChannel.open(fileMetaData.getFilePath(), StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW);
        }


    }

    public long receive() throws IOException {

        System.out.println("Start receiving " + fileChannel.size() + " bytes");
        if (!blocking) {
            return transfer();
        }

        while (!done()) {
            transfer();
        }

        return bytesReceived;

    }

    private long transfer() throws IOException {
        long transferred = this.fileChannel.transferFrom(socketChannel, fileMetaData.getPosition(), FileConstants.TRANSFER_MAX_SIZE);
        System.out.println("Bytes received: " + transferred);
        bytesReceived += transferred;
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
