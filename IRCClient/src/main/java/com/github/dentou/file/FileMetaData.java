package com.github.dentou.file;

import org.apache.commons.lang3.StringUtils;

import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

public class FileMetadata {
    private final Path filePath;
    private final long size;

    private final AtomicLong position;


    public FileMetadata(final Path filePath, final long size, final long position) {
        if (Objects.isNull(filePath)) {
            throw new IllegalArgumentException("File path required");
        }

        this.filePath = filePath;
        this.size = size;
        this.position = new AtomicLong(position);
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
