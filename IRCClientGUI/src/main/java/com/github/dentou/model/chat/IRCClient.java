package com.github.dentou.model.chat;

import com.github.dentou.MainApp;
import com.github.dentou.model.chat.IRCSocket;
import com.github.dentou.model.chat.User;
import javafx.application.Platform;
import javafx.scene.control.Alert;

import java.io.File;
import java.io.IOException;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class IRCClient implements Runnable{
    private IRCSocket clientSocket;
    private User user = null;

    private AtomicBoolean stopRequested = new AtomicBoolean(false);
    private AtomicBoolean stopped = new AtomicBoolean(false);


    private MainApp mainApp;

    private Queue<String> sendQueue = new ArrayBlockingQueue<String>(512);
    private Queue<String> receiveQueue = new ArrayBlockingQueue<String>(512);

    private Set<IRCSocket> emptyToNonEmptySockets = new HashSet<>();
    private Set<IRCSocket> nonEmptyToEmptySockets = new HashSet<>();

    private Selector readSelector;
    private Selector writeSelector;


    public IRCClient(IRCSocket clientSocket, MainApp mainApp) throws IOException{

        this.clientSocket = clientSocket;
        this.mainApp = mainApp;

        this.readSelector = Selector.open();
        this.writeSelector = Selector.open();

        //SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress(serverAddress, serverPort));
        //this.clientSocket = new IRCSocket(socketChannel, false);

        SelectionKey key = clientSocket.register(this.readSelector, SelectionKey.OP_READ);
        key.attach(clientSocket);

    }



    @Override
    public void run() {
        System.out.println("Client running");
        while (true) {

            if (stopped.get()) {
                System.out.println("File executor has been shut down");
                try {
                    closeSocket(clientSocket);
                } catch (IOException e) {
                    System.out.println("Cannot close chat socket");
                    e.printStackTrace();
                }
                return;
            }

            try {
                readMessages();
                processMessages();
                writeMessages();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void stop() {
        stopRequested.set(true);
    }

    public void sendToServer(String... messageParts) {

        StringBuilder sb = new StringBuilder();
        for (String message : messageParts) {
            sb.append(message);
        }
        sb.append("\r\n");
        sendQueue.add(sb.toString());
        System.out.println("Send message added to queue");
    }


    private void closeSocket(IRCSocket socket) throws IOException {
        try {
            System.out.println("Socket closed");
            SelectionKey readKey = socket.getSelectionKey(readSelector);
            readKey.attach(null);
            readKey.cancel();
            readKey.channel().close();

        } catch (IOException e) {
            System.out.println("Cannot close socket");
            e.printStackTrace();
        }

    }

    private void processMessages() {

        if (stopRequested.get()) {
            return;
        }

        if (!receiveQueue.isEmpty()) {
            while (true) {
                String message = receiveQueue.poll();
                if (message == null) {
                    break;
                }
                mainApp.getController().enqueue(message);
            }
            Platform.runLater(() -> mainApp.getController().update());
        }

    }




    private void readMessages() throws IOException{

        if (stopRequested.get()) {
            return;
        }

        int readReady = this.readSelector.selectNow();

        if (readReady > 0) {
            Set<SelectionKey> selectedKeys = this.readSelector.selectedKeys();
            Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

            while (keyIterator.hasNext()) {
                SelectionKey key = keyIterator.next();

                try {
                    readFromSocket(key);
                } catch (IOException e) {
                    e.printStackTrace();
                    closeSocket((IRCSocket) key.attachment());
                    stop(); // New
                    Platform.runLater(() -> {
                        mainApp.showConnectionDialog();
                        mainApp.showAlertDialog(Alert.AlertType.ERROR, "Connection lost", "Connection to server lost", null);
                    });
                }


                keyIterator.remove();
            }
            selectedKeys.clear();
        }

    }

    private void readFromSocket(SelectionKey key) throws IOException {
        IRCSocket socket = (IRCSocket) key.attachment();
        Queue<String> messages = socket.getMessages();
        System.out.println("Message from server: " + messages);

        if (messages.size() > 0) {
            for (String message : messages) {
                if (message != null & !message.trim().isEmpty()) {
                    this.receiveQueue.add(message);
                }
            }
        }

        if (socket.isEndOfStreamReached()) {
            closeSocket(socket);
        }
    }


    private void writeMessages() throws IOException {

        if (stopRequested.get() && sendQueue.isEmpty()) {
            stopped.set(true);
        }

        pullAllMessages();

        cancelEmptySockets();

        registerNonEmptySockets();

        int writeReady = writeSelector.selectNow();

        if (writeReady > 0) {
            Set<SelectionKey> selectedKeys = writeSelector.selectedKeys();
            Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

            while (keyIterator.hasNext()) {
                SelectionKey key = keyIterator.next();

                IRCSocket ircSocket = (IRCSocket) key.attachment();

                ircSocket.sendMessages();

                if (ircSocket.isWriterEmpty()) {
                    this.nonEmptyToEmptySockets.add(ircSocket);
                }

                keyIterator.remove();
            }

            selectedKeys.clear();
        }
    }

    private void registerNonEmptySockets() throws ClosedChannelException {
        for (IRCSocket ircSocket : emptyToNonEmptySockets) {
            SelectionKey key = ircSocket.register(this.writeSelector, SelectionKey.OP_WRITE);
            key.attach(ircSocket);
        }
        emptyToNonEmptySockets.clear();
    }

    private void cancelEmptySockets() {
        for (IRCSocket ircSocket : nonEmptyToEmptySockets) {
            SelectionKey key = ircSocket.getSelectionKey(this.writeSelector);
            key.cancel();
        }
        nonEmptyToEmptySockets.clear();
    }

    private void pullAllMessages() {
        while (true) {
            String outMessage = sendQueue.poll();
            if (outMessage == null) {
                return;
            }
            System.out.println("Pull message " + outMessage);

            if (clientSocket.isWriterEmpty()) {
                nonEmptyToEmptySockets.remove(clientSocket);
                emptyToNonEmptySockets.add(clientSocket);
            }
            clientSocket.enqueue(outMessage);

        }

    }

}
