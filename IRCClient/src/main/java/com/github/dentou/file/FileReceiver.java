package com.github.dentou.file;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

public class FileReceiver {

    private final SocketChannel socketChannel;

    private final FileChannel fileChannel;
    private final FileMetadata fileMetadata;

    private boolean blocking;
    private long bytesReceived = 0l;


    public FileReceiver(SocketChannel socketChannel, FileMetadata fileMetadata) throws IOException {
        this(socketChannel, fileMetadata, true);
    }

    public FileReceiver(SocketChannel socketChannel, FileMetadata fileMetadata, boolean blocking) throws IOException {
        if (Objects.isNull(socketChannel) && Objects.isNull(fileMetadata)) {
            throw new IllegalArgumentException("Socket channel and File metadata required");
        }
        if (Files.isDirectory(fileMetadata.getFilePath())) {
            throw new IllegalArgumentException("Path is a directory, not a file");

        }

        this.socketChannel = socketChannel;
        this.fileMetadata = fileMetadata;
        this.blocking = blocking;
        // Check if file exist
        boolean fileExists = Files.exists(fileMetadata.getFilePath());
        if (fileExists) {
            if (fileMetadata.getPosition() > 0) { // If resume
                this.fileChannel = FileChannel.open(fileMetadata.getFilePath(), StandardOpenOption.WRITE, StandardOpenOption.APPEND);
            } else {
                throw new FileAlreadyExistsException("File already exists");
//                boolean success = Files.deleteIfExists(fileMetadata.getFilePath());
//                System.out.println("File exists, delete: " + success);
//                this.fileChannel = FileChannel.open(fileMetadata.getFilePath(), StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW);
            }

        } else {
            this.fileChannel = FileChannel.open(fileMetadata.getFilePath(), StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW);
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
        long transferred = this.fileChannel.transferFrom(socketChannel, fileMetadata.getPosition(), FileConstants.TRANSFER_MAX_SIZE);
        System.out.println("Bytes received: " + transferred);
        bytesReceived += transferred;
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
