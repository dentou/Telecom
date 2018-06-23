package com.github.dentou.model.file;

import com.github.dentou.MainApp;
import com.github.dentou.model.chat.IRCSocket;
import com.github.dentou.model.constants.IRCConstants;
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

        FileSendTask fileSendTask = new FileSendTask(fileMetadata, sender, recipient);
        fileTransferExecutor.submit(fileSendTask);
        return fileSendTask;
    }

    public FileReceiveTask receiveFile(FileMetadata fileMetadata, String sender, String recipient) {
        if (Objects.isNull(fileMetadata) || StringUtils.isEmpty(sender) || StringUtils.isEmpty(recipient)) {
            throw new IllegalArgumentException("File metadata, sender and recipient required");
        }

        FileReceiveTask fileReceiveTask = new FileReceiveTask(fileMetadata, sender, recipient);
        fileTransferExecutor.submit(fileReceiveTask);
        return fileReceiveTask;

    }


    /**
     * Transfer Tasks
     */
    public class FileSendTask extends Task<Void> {

        private final FileMetadata fileMetadata;
        private final String sender;
        private final String recipient;

        private IRCSocket fileSocket = null;
        private FileSender fileSender = null;

        private ByteBuffer checkBuffer = ByteBuffer.allocate(100);

        public FileSendTask(FileMetadata fileMetadata, String sender, String recipient) {
            this.fileMetadata = fileMetadata;
            this.sender = sender;
            this.recipient = recipient;
        }

        private void closeSender() throws IOException {
            if (fileSender == null) {
                return;
            }
            fileSender.close();
        }

        @Override
        protected void succeeded() {
            super.succeeded();
            try {
                closeSender();
                System.out.println("Sender closed");
            } catch (IOException ex) {
                System.out.println("Unable to close file sender");
            }
        }

        @Override
        protected void cancelled() {
            super.cancelled();
            try {
                closeSender();
                System.out.println("Sender closed");
            } catch (IOException ex) {
                System.out.println("Unable to close file sender");
            }
        }

        @Override
        protected void failed() {
            super.failed();
            try {
                closeSender();
                System.out.println("Sender closed");
            } catch (IOException ex) {
                System.out.println("Unable to close file sender");
            }
        }


        @Override
        protected Void call() throws IOException {
            updateTitle(fileMetadata.getFilePath().getFileName().toString());

            updateMessage("Initializing...");

            connectToFileServer();

            fileSender = new FileSender(fileSocket.getSocketChannel(), fileMetadata, false);

            negotiate();

            while (!fileSender.done()) {
                if (isCancelled()) {
                    return null;
                }
                long transferred = fileSender.send();
                double percent = (double) fileMetadata.getPosition() / fileMetadata.getSize() * 100;
                String percentString = String.format("%3.2f", percent);
                updateMessage("Sent " + percentString + "%");
                updateProgress(percent, 100);

                int bytes = fileSocket.getSocketChannel().read(checkBuffer);
                if (bytes == -1) {
                    updateMessage("Cancelled");
                    cancel();
                }

            }

            System.out.println("File successfully sent: " + fileMetadata.getFilePath());
            return null;
        }


        private void connectToFileServer() throws IOException {
            SocketChannel socketChannel = SocketChannel.open(
                    new InetSocketAddress(mainApp.getServerAddress(), IRCConstants.FILE_SERVER_PORT));
            socketChannel.configureBlocking(false);
            this.fileSocket = new IRCSocket(socketChannel);

        }

        private void negotiate() throws IOException {
            String fileKey = fileMetadata.getFilePath().getFileName().toString() + sender + recipient;
            String message = "SEND " + fileKey + "\r\n";

            fileSocket.enqueue(message);
            fileSocket.sendMessages();

            boolean waiting = true;

            while (waiting) {
                if (isCancelled()) {
                    return;
                }
                Queue<String> responses = fileSocket.getMessages();
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
                if (fileSocket.isEndOfStreamReached()) {
                    throw new ClosedChannelException();
                }
            }
        }
    }

    public class FileReceiveTask extends Task<Void> {

        private final FileMetadata fileMetadata;
        private final String sender;
        private final String recipient;

        private IRCSocket fileSocket = null;
        private FileReceiver fileReceiver = null;

        ByteBuffer checkBuffer = ByteBuffer.allocate(100);

        public FileReceiveTask(FileMetadata fileMetadata, String sender, String recipient) {
            this.fileMetadata = fileMetadata;
            this.sender = sender;
            this.recipient = recipient;
        }

        private void closeReceiver() throws IOException {
            if (fileReceiver == null) {
                return;
            }
            fileReceiver.close();
        }

        @Override
        protected void succeeded() {
            super.succeeded();
            try {
                closeReceiver();
                System.out.println("Sender closed");
            } catch (IOException ex) {
                System.out.println("Unable to close file sender");
            }
        }

        @Override
        protected void cancelled() {
            super.cancelled();
            try {
                closeReceiver();
                System.out.println("Sender closed");
            } catch (IOException ex) {
                System.out.println("Unable to close file sender");
            }
        }

        @Override
        protected void failed() {
            super.failed();
            try {
                closeReceiver();
                System.out.println("Sender closed");
            } catch (IOException ex) {
                System.out.println("Unable to close file sender");
            }
        }


        @Override
        protected Void call() throws IOException {
            updateTitle(fileMetadata.getFilePath().getFileName().toString());

            updateMessage("Initializing...");

            connectToFileServer();

            fileReceiver = new FileReceiver(fileSocket.getSocketChannel(), fileMetadata, false);

            negotiate();

            while (!fileReceiver.done()) {
                if (isCancelled()) {
                    return null;
                }
                long received = fileReceiver.receive();

                if (fileReceiver.isEndOfStreamReached()) {
                    failed();
                }

                double percent = (double) fileMetadata.getPosition() / fileMetadata.getSize() * 100;
                String percentString = String.format("%3.2f", percent);
                updateMessage("Received " + percentString + "%");
                updateProgress(fileMetadata.getPosition(), fileMetadata.getSize());
            }


            System.out.println("File successfully received: " + fileMetadata.getFilePath());

            return null;
        }

        private void connectToFileServer() throws IOException {
            SocketChannel socketChannel = SocketChannel.open(
                    new InetSocketAddress(mainApp.getServerAddress(), IRCConstants.FILE_SERVER_PORT));
            socketChannel.configureBlocking(false);
            this.fileSocket = new IRCSocket(socketChannel);

        }


        private void negotiate() throws IOException {
            String fileKey = fileMetadata.getFilePath().getFileName().toString() + sender + recipient;
            String message = "RECEIVE " + fileKey + "\r\n";

            fileSocket.enqueue(message);
            fileSocket.sendMessages();

            boolean waiting = true;

            while (waiting) {
                if (isCancelled()) {
                    return;
                }
                Queue<String> responses = fileSocket.getMessages();
                while (true) {
                    String response = responses.poll();
                    if (response == null) {
                        break;
                    }
                    if (response.equals("READY")) {
                        waiting = false;
                        fileSocket.enqueue("READY\r\n");
                        fileSocket.sendMessages();
                        break;
                    } else if (response.equals("FAILED")) {
                        throw new IOException("File transfer denied by server");
                    }
                }
                if (fileSocket.isEndOfStreamReached()) {
                    throw new ClosedChannelException();
                }
            }
        }

    }


}
