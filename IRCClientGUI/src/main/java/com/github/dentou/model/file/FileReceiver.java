package com.github.dentou.model.file;

import com.github.dentou.model.constants.IRCConstants;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class FileReceiver {

    public static final int BUFFER_SIZE = 1 * IRCConstants.MB;

    private final SocketChannel socketChannel;

    private final FileChannel fileChannel;
    private final FileMetadata fileMetadata;

    private boolean blocking;
    private long bytesReceived = 0l;

    private final ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);

    private AtomicBoolean endOfStreamReached = new AtomicBoolean(false);


    public FileReceiver(SocketChannel socketChannel, FileMetadata fileMetadata) throws IOException {
        this(socketChannel, fileMetadata, true);
    }

    public boolean isEndOfStreamReached() {
        return this.endOfStreamReached.get();
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

        if (!blocking) {
            return transfer();
        }

        while (!done()) {
            transfer();
        }

        return bytesReceived;

    }

    private long transfer() throws IOException {
        long transferred = this.fileChannel.transferFrom(socketChannel, fileMetadata.getPosition(), IRCConstants.TRANSFER_MAX_SIZE);
        bytesReceived += transferred;
        this.fileMetadata.addToPosition(transferred);
        return transferred;

//        long totalBytesRead = 0;
//
//        while (true) {
//            int bytesRead = this.socketChannel.read(buffer);
//
//            if (bytesRead == -1) {
//                endOfStreamReached.set(true);
//                break;
//            } else if (bytesRead == 0) {
//                break;
//            }
//
//            bytesReceived += bytesRead;
//            totalBytesRead += bytesRead;
//            this.fileMetadata.addToPosition(bytesRead);
//        }
//
//        buffer.flip();
//        if (endOfStreamReached.get()) {
//            while (buffer.hasRemaining()) {
//                fileChannel.write(buffer);
//            }
//        } else {
//            while (buffer.remaining() > BUFFER_SIZE / 2) {
//                fileChannel.write(buffer);
//            }
//        }
//        buffer.compact();
//        return totalBytesRead;


//        buffer.clear();
//        while (buffer.hasRemaining()) {
//            long transferred = this.socketChannel.read(buffer);
//
//            if (transferred == -1) {
//                endOfStreamReached.set(true);
//
//                buffer.flip();
//                while (buffer.hasRemaining()) {
//                    fileChannel.write(buffer);
//                }
//
//                return 0;
//            }
//
//            bytesReceived += transferred;
//            totalTransferred += transferred;
//            this.fileMetadata.addToPosition(transferred);
//        }
//
//        buffer.flip();
//
//        while (buffer.hasRemaining()) {
//            fileChannel.write(buffer);
//        }
//
//        return totalTransferred;

    }

    public boolean done() {
        return fileMetadata.done();
    }

    public void close() throws IOException {
        this.socketChannel.close();
        this.fileChannel.close();
    }


}
