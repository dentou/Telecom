package com.github.dentou.model.file;

import com.github.dentou.MainApp;
import com.github.dentou.model.chat.IRCSocket;
import com.github.dentou.utils.ClientConstants;
import com.sun.security.ntlm.Client;
import javafx.concurrent.Task;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import static com.github.dentou.utils.ClientUtils.readableFileSize;

public class FileTransferClient {

    private MainApp mainApp;

    private ExecutorService fileTransferExecutor = Executors.newFixedThreadPool(10);

    public FileTransferClient(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    public void close() {
        fileTransferExecutor.shutdown();
    }

    public void forcedClose() {
        fileTransferExecutor.shutdownNow();
    }


    public FileSendTask sendFile(FileMetadata fileMetadata, String sender, String recipient) {
        if (Objects.isNull(fileMetadata) || StringUtils.isEmpty(sender) || StringUtils.isEmpty(recipient)) {
            throw new IllegalArgumentException("File metadata, sender and recipient required");
        }

        FileSendTask fileSendTask = new FileSendTask(fileMetadata);
        fileTransferExecutor.submit(fileSendTask);
        return fileSendTask;
    }

    public FileReceiveTask receiveFile(FileMetadata fileMetadata, String sender, String recipient) {
        if (Objects.isNull(fileMetadata) || StringUtils.isEmpty(sender) || StringUtils.isEmpty(recipient)) {
            throw new IllegalArgumentException("File metadata, sender and recipient required");
        }

        FileReceiveTask fileReceiveTask = new FileReceiveTask(fileMetadata);
        fileTransferExecutor.submit(fileReceiveTask);
        return fileReceiveTask;

    }


    /**
     * Transfer Tasks
     */

    public abstract class FileTransferTask extends Task<Void> {
        private final FileMetadata fileMetadata;

        private IRCSocket fileSocket = null;

        private AtomicLong lastTransferTime = new AtomicLong(0);

        public FileTransferTask(FileMetadata fileMetadata) {
            this.fileMetadata = fileMetadata;
        }

        protected FileMetadata getFileMetadata() {
            return fileMetadata;
        }

        protected IRCSocket getFileSocket() {
            return fileSocket;
        }

        protected long updateAndGetTransferDuration() { // In nanoseconds
            if (lastTransferTime.get() == 0) {
                lastTransferTime.set(System.nanoTime());
                return 0;
            }
            long currentTime = System.nanoTime();
            long duration = currentTime - lastTransferTime.get();
            lastTransferTime.set(currentTime);
            return duration;
        }

        /**
         *
         * @param bytesTransferred in bytes
         * @param duration in nanoseconds
         * @return rate in bytes/seconds
         */
        protected long computeTransferRate(long bytesTransferred, long duration) {
            if (duration == 0) {
                return 0;
            }
            return bytesTransferred * ClientConstants.NANOS_PER_SECOND / duration;
        }

        protected abstract void closeTransfer();

        protected void connectToFileServer() throws IOException {
            SocketChannel socketChannel = SocketChannel.open(
                    new InetSocketAddress(mainApp.getServerAddress(), ClientConstants.FILE_SERVER_PORT));
            socketChannel.configureBlocking(false);
            this.fileSocket = new IRCSocket(socketChannel);

        }

        @Override
        protected void succeeded() {
            super.succeeded();
            closeTransfer();
        }

        @Override
        protected void cancelled() {
            super.cancelled();
            closeTransfer();
        }

        @Override
        protected void failed() {
            super.failed();
            closeTransfer();
        }
    }

    public class FileSendTask extends FileTransferTask {

        private FileSender fileSender = null;

        private ByteBuffer checkBuffer = ByteBuffer.allocate(100);

        public FileSendTask(FileMetadata fileMetadata) {
            super(fileMetadata);
        }

        @Override
        protected void closeTransfer() {
            if (fileSender == null) {
                return;
            }
            try {
                fileSender.close();
                System.out.println("File sender closed");
            } catch (IOException e) {
                System.out.println("Unable to close file sender");

            }
        }


        @Override
        protected Void call() throws IOException {
            updateTitle(getFileMetadata().getFilePath().getFileName().toString() + " - to: " + getFileMetadata().getReceiver());

            updateMessage("Initializing...");

            connectToFileServer();

            fileSender = new FileSender(getFileSocket().getSocketChannel(), getFileMetadata(), false);

            negotiate();

            while (!fileSender.done()) {
                if (isCancelled()) {
                    return null;
                }
                long transferred = fileSender.send();
//                double percent = (double) fileMetadata.getPosition() / fileMetadata.getSize() * 100;
//                String percentString = String.format("%3.2f", percent);
                updateMessage("Sent " + readableFileSize(getFileMetadata().getPosition()) + "/" +
                        readableFileSize(getFileMetadata().getSize()));
                updateProgress(getFileMetadata().getPosition(), getFileMetadata().getSize());

                int bytes = getFileSocket().getSocketChannel().read(checkBuffer);
                if (bytes == -1) {
                    updateMessage("Cancelled");
                    cancel();
                }

            }

            System.out.println("File successfully sent: " + getFileMetadata().getFilePath());
            return null;
        }


        private void negotiate() throws IOException {
            String fileKey = getFileMetadata().getFilePath().getFileName().toString() + getFileMetadata().getSender() +
                    getFileMetadata().getReceiver();
            String message = "SEND " + fileKey + "\r\n";

            getFileSocket().enqueue(message);
            getFileSocket().sendMessages();

            boolean waiting = true;

            while (waiting) {
                if (isCancelled()) {
                    return;
                }
                Queue<String> responses = getFileSocket().getMessages();
                for (String response : responses) {
                    if (StringUtils.isNotEmpty(response)) {
                        if (response.equals("READY")) {
                            waiting = false;
                            break;
                        } else if (response.equals("FAILED")) {
//                            Platform.runLater(() -> mainApp.showAlertDialog(Alert.AlertType.ERROR, "File Transfer Error",
//                                    "Transfer denied by server", null));
                            throw new IOException("File transfer denied by server");
                        }
                    }
                }
                if (getFileSocket().isEndOfStreamReached()) {
                    throw new ClosedChannelException();
                }
            }
        }
    }

    public class FileReceiveTask extends FileTransferTask {

        private FileReceiver fileReceiver = null;


        public FileReceiveTask(FileMetadata fileMetadata) {
            super(fileMetadata);
        }


        @Override
        protected void closeTransfer() {
            if (fileReceiver == null) {
                return;
            }
            try {
                fileReceiver.close();
                System.out.println("File receiver closed");
            } catch (IOException e) {
                System.out.println("Unable to close file receiver");

            }
        }



        @Override
        protected Void call() throws IOException {
            updateTitle(getFileMetadata().getFilePath().getFileName().toString() + " - from " + getFileMetadata().getSender());

            updateMessage("Initializing...");

            connectToFileServer();

            fileReceiver = new FileReceiver(getFileSocket().getSocketChannel(), getFileMetadata(), false);

            negotiate();

            while (!fileReceiver.done()) {
                if (isCancelled()) {
                    return null;
                }
                long received = fileReceiver.receive();

                if (fileReceiver.isEndOfStreamReached()) {
                    failed();
                }

                updateMessage("Received " + readableFileSize(getFileMetadata().getPosition()) + "/" +
                        readableFileSize(getFileMetadata().getSize()));
                updateProgress(getFileMetadata().getPosition(), getFileMetadata().getSize());
            }


            System.out.println("File successfully received: " + getFileMetadata().getFilePath());

            return null;
        }



        private void negotiate() throws IOException {
            String fileKey = getFileMetadata().getFilePath().getFileName().toString()
                    + getFileMetadata().getSender()
                    + getFileMetadata().getReceiver();
            String message = "RECEIVE " + fileKey + "\r\n";

            getFileSocket().enqueue(message);
            getFileSocket().sendMessages();

            boolean waiting = true;

            while (waiting) {
                if (isCancelled()) {
                    return;
                }
                Queue<String> responses = getFileSocket().getMessages();
                while (true) {
                    String response = responses.poll();
                    if (response == null) {
                        break;
                    }
                    if (response.equals("READY")) {
                        waiting = false;
                        getFileSocket().enqueue("READY\r\n");
                        getFileSocket().sendMessages();
                        break;
                    } else if (response.equals("FAILED")) {
                        throw new IOException("File transfer denied by server");
                    }
                }
                if (getFileSocket().isEndOfStreamReached()) {
                    throw new ClosedChannelException();
                }
            }
        }

    }


}
