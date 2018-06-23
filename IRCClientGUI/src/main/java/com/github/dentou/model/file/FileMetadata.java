package com.github.dentou.model.file;

import jdk.nashorn.internal.ir.annotations.Immutable;

import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

public class FileMetadata {
    @Immutable
    private final Path filePath;
    @Immutable
    private final long size;

    private final AtomicLong position;
    @Immutable
    private final String sender;
    @Immutable
    private final String receiver;


    public FileMetadata(final Path filePath, final long size, final long position, final String sender, final String receiver) {
        if (Objects.isNull(filePath)) {
            throw new IllegalArgumentException("File path required");
        }

        this.filePath = filePath;
        this.size = size;
        this.position = new AtomicLong(position);

        this.sender = sender;
        this.receiver = receiver;
    }


    public Path getFilePath() {
        return this.filePath;
    }


    public long getSize() {
        return this.size;
    }

    public long getPosition() {
        return position.get();
    }

    public void setPosition(long position) {
        this.position.set(position);
    }

    public void addToPosition(long bytes) {
        this.position.addAndGet(bytes);
    }

    public String getSender() {
        return sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public boolean done() {
        return position.get() >= size;
    }

    @Override
    public String toString() {
        return "FileMetadata{" +
                "filePath=" + filePath +
                ", size=" + size +
                ", position=" + position +
                '}';
    }
}
