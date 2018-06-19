package com.github.dentou.file;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

public class FileWriter {
    private final FileChannel fileChannel;

    public FileWriter(final String path) throws IOException {
        if (StringUtils.isEmpty(path)) {
            throw new IllegalArgumentException("path required");
        }

        this.fileChannel = FileChannel.open(Paths.get(path), StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW);
    }

    void transfer(final SocketChannel channel, final long bytes) throws IOException {
        assert !Objects.isNull(channel);

        long position = 0l;
        while (position < bytes) {
            position += this.fileChannel.transferFrom(channel, position, FileConstants.TRANSFER_MAX_SIZE);
        }
    }

    public int write(ByteBuffer buffer, long position) throws IOException {
        assert(Objects.isNull(buffer));

        int bytesWritten = 0;
        while (buffer.hasRemaining()) {
            bytesWritten += this.fileChannel.write(buffer, position + bytesWritten);

        }
        return bytesWritten;
    }

    public void close() throws IOException {
        this.fileChannel.close();
    }
}
