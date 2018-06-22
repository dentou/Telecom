package com.github.dentou.model.file;

import com.github.dentou.MainApp;
import com.github.dentou.model.chat.IRCSocket;
import com.github.dentou.model.constants.IRCConstants;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.Alert;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;
import java.util.List;
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
    private class FileSendTask extends Task<Void> {

        private final FileMetadata fileMetadata;
        private final String sender;
        private final String recipient;

        private IRCSocket fileSocket = null;
        private FileSender fileSender = null;

        public FileSendTask(FileMetadata fileMetadata, String sender, String recipient) {
            this.fileMetadata = fileMetadata;
            this.sender = sender;
            this.recipient = recipient;
        }

        @Override
        protected void succeeded() {
            super.succeeded();
            updateMessage("Done");
        }

        @Override
        protected void cancelled() {
            super.cancelled();
            updateMessage("Failed");
        }

        @Override
        protected void failed() {
            super.failed();
            updateMessage("Cancelled");
        }

        @Override
        protected Void call() {
            try {
                updateMessage("Initializing...");

                connectToFileServer();

                fileSender = new FileSender(fileSocket.getSocketChannel(), fileMetadata, false);

                negotiate();

                while (!fileSender.done()) {
                    long transferred = fileSender.send();
                    updateMessage("Sent " + transferred + " bytes");
                    updateProgress(transferred, fileMetadata.getSize());
                }


                System.out.println("File successfully sent: " + fileMetadata.getFilePath());
                fileSender.close();
                System.out.println("Sender closed normally");

            } catch (IOException e) {
                if (fileSender != null) {
                    try {
                        fileSender.close();
                        System.out.println("Sender closed by exception");
                    } catch (IOException ex) {
                        System.out.println("Unable to close file sender");
                    }
                }
                Platform.runLater(() -> mainApp.showExceptionDialog("File Transfer Error", "There's an error sending file", null, e));
            }

            return null;
        }



        private void connectToFileServer() throws IOException {
            SocketChannel socketChannel = SocketChannel.open(
                    new InetSocketAddress(mainApp.getServerAddress(), IRCConstants.FILE_SERVER_PORT));
            socketChannel.configureBlocking(false);
            this.fileSocket = new IRCSocket(socketChannel);

        }

        private boolean negotiate() throws IOException {
            String fileKey = fileMetadata.getFilePath().getFileName().toString() + sender + recipient;
            String message = "SEND " + fileKey + "\r\n";

            fileSocket.enqueue(message);
            fileSocket.sendMessages();

            boolean waiting = true;

            while (waiting) {
                Queue<String> responses = fileSocket.getMessages();
                for (String response : responses) {
                    if (StringUtils.isNotEmpty(response)) {
                        if (response.equals("READY")) {
                            waiting = false;
                            break;
                        } else if (response.equals("FAILED")) {
                            Platform.runLater(() -> mainApp.showAlertDialog(Alert.AlertType.ERROR, "File Transfer Error",
                                    "Transfer denied by server", null));
                            return false;
                        }
                    }
                }
                if (fileSocket.isEndOfStreamReached()) {
                    throw new ClosedChannelException();
                }
            }
            return true;
        }
    }

    private class FileReceiveTask extends Task<Void> {

        private final FileMetadata fileMetadata;
        private final String sender;
        private final String recipient;

        private IRCSocket fileSocket = null;
        private FileReceiver fileReceiver = null;

        public FileReceiveTask(FileMetadata fileMetadata, String sender, String recipient) {
            this.fileMetadata = fileMetadata;
            this.sender = sender;
            this.recipient = recipient;
        }

        @Override
        protected void succeeded() {
            super.succeeded();
            updateMessage("Done");
        }

        @Override
        protected void cancelled() {
            super.cancelled();
            updateMessage("Cancelled");
        }

        @Override
        protected void failed() {
            super.failed();
            updateMessage("Failed");
        }

        @Override
        protected Void call() {
            try {
                updateMessage("Initializing...");

                connectToFileServer();

                fileReceiver = new FileReceiver(fileSocket.getSocketChannel(), fileMetadata, false);

                negotiate();

                while (!fileReceiver.done()) {
                    long transferred = fileReceiver.receive();
                    updateMessage("Received " + transferred + " bytes");
                    updateProgress(transferred, fileMetadata.getSize());
                }


                System.out.println("File successfully received: " + fileMetadata.getFilePath());
                fileReceiver.close();
                System.out.println("Receiver closed normally");

            } catch (IOException e) {
                if (fileReceiver != null) {
                    try {
                        fileReceiver.close();
                        System.out.println("Receiver closed by exception");
                    } catch (IOException ex) {
                        System.out.println("Unable to close file receiver");
                    }
                }
                Platform.runLater(() -> mainApp.showExceptionDialog("File Transfer Error",
                        "There's an error receiving file", null, e));
            }

            return null;
        }

        private void connectToFileServer() throws IOException {
            SocketChannel socketChannel = SocketChannel.open(
                    new InetSocketAddress(mainApp.getServerAddress(), IRCConstants.FILE_SERVER_PORT));
            socketChannel.configureBlocking(false);
            this.fileSocket = new IRCSocket(socketChannel);

        }


        private boolean negotiate() throws IOException {
            String fileKey = fileMetadata.getFilePath().getFileName().toString() + sender + recipient;
            String message = "RECEIVE " + fileKey + "\r\n";

            fileSocket.enqueue(message);
            fileSocket.sendMessages();

            boolean waiting = true;

            while (waiting) {
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
                        Platform.runLater(() -> {mainApp.showAlertDialog(Alert.AlertType.ERROR, "File Transfer Error",
                                "Transfer denied by server", null);});

                        return false;
                    }
                }
                if (fileSocket.isEndOfStreamReached()) {
                    throw new ClosedChannelException();
                }
            }
            return true;
        }

    }


}
