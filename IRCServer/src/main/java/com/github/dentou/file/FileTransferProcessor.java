package com.github.dentou.file;

import com.github.dentou.chat.IRCMessage;
import com.github.dentou.chat.IRCSocket;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.*;

public class FileTransferProcessor implements Runnable {

    private Queue<IRCSocket> socketQueue;
    private Map<Long, IRCSocket> socketMap = new HashMap<Long, IRCSocket>();
    private long nextSocketId = 1024; // Id frm 0 to 1023 is reserved for servers

    private Selector readSelector;
    private Selector writeSelector;

    private Selector fileSendSelector;


    private Queue<IRCMessage> requestQueue = new LinkedList<>(); // Contains already parsed requests (no \r\n)
    private Queue<IRCMessage> sendQueue = new LinkedList<>();

    private Set<IRCSocket> emptyToNonEmptySockets = new HashSet<>();
    private Set<IRCSocket> nonEmptyToEmptySockets = new HashSet<>();


    private Map<String, Long> waitingToSendMap = new HashMap<>();
    private Map<String, Long> waitingToReceiveMap = new HashMap<>();

    private Map<String, FileTransferProxy> fileTransferProxyMap = new HashMap<>();

    private Map<Long, String> receiverMap = new HashMap<>();

    public FileTransferProcessor(Queue<IRCSocket> socketQueue) throws IOException {
        this.socketQueue = socketQueue;

        this.readSelector = Selector.open();
        this.writeSelector = Selector.open();

        this.fileSendSelector = Selector.open();

    }

    @Override
    public void run() {
        while (true) {
            try {
                registerNewSockets();
                readRequests();
                processRequests(); // todo implement processRequests()
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void registerNewSockets() throws ClosedChannelException {

        while (true) {
            IRCSocket newSocket = this.socketQueue.poll();

            if (newSocket == null) {
                return;
            }

            newSocket.setId(nextSocketId++);
            this.socketMap.put(newSocket.getId(), newSocket);
            SelectionKey key = newSocket.register(this.readSelector, SelectionKey.OP_READ);
            key.attach(newSocket);
        }
    }

    private void closeSocket(IRCSocket socket) throws IOException {
        System.out.println("[FileTF] Socket closed: " + socket.getId());
        this.socketMap.remove(socket.getId());
        SelectionKey readKey = socket.getSelectionKey(readSelector);
        if (readKey != null) {
            readKey.attach(null);
            readKey.cancel();
            readKey.channel().close();
        }
    }


    private void readFromSocket(SelectionKey key) throws IOException {
        IRCSocket socket = (IRCSocket) key.attachment();
        List<IRCMessage> requests = socket.getMessages();
        System.out.println("[FileTF] (readFromSocket) Requests from socket: " + requests);

        if (requests.size() > 0) {
            for (IRCMessage request : requests) {
                if (request.getMessage() != null & !request.getMessage().trim().isEmpty()) {
                    this.requestQueue.add(request);
                }
            }
        }

        if (socket.isEndOfStreamReached()) {
            closeSocket(socket);
        }
    }

    private void readRequests() throws IOException {
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
                }


                keyIterator.remove();
            }
            selectedKeys.clear();
        }
    }


    private void processRequests() throws IOException {
        while (true) {
            IRCMessage request = requestQueue.poll();
            if (request == null) {
                break;
            }
            processRequest(request);
        }
        // todo write to socket
        writeResponses();
        transferFiles();

    }

    private void transferFiles() throws IOException {

        int fileReady = this.fileSendSelector.selectNow();

        if (fileReady > 0) {
            Set<SelectionKey> selectedKeys = this.fileSendSelector.selectedKeys();
            Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

            while (keyIterator.hasNext()) {
                SelectionKey key = keyIterator.next();

                FileTransferProxy proxy = (FileTransferProxy) key.attachment();
                try {
                    if (!proxy.transfer()) {
                        closeProxy(proxy);
                    }
                } catch (IOException e) {
                    closeProxy(proxy);
                    e.printStackTrace();
                }

                keyIterator.remove();
            }
            selectedKeys.clear();
        }

//        for (FileTransferProxy proxy : fileTransferProxyMap.values()) {
//            try {
//                if (!proxy.transfer()) {
//                    closeProxy(proxy);
//                }
//            } catch (IOException e) {
//                closeProxy(proxy);
//                e.printStackTrace();
//            }
//        }
    }

    private void closeProxy(FileTransferProxy proxy) throws IOException {
        System.out.println("Closing proxy " + proxy.toString());
        unsubscribe(proxy.getInSocket().getId(), fileSendSelector);
        fileTransferProxyMap.remove(proxy.getFileKey());
        closeSocket(proxy.getInSocket());
        closeSocket(proxy.getOutSocket());

    }


    private void processRequest(IRCMessage request) throws IOException { // Process command and add response to send queue
        if (request.getMessage() == null || request.getMessage().trim() == "") {
            return;
        }
        List<String> requestParts = Arrays.asList(request.getMessage().split(" "));
        System.out.println("[FileTF] Request " + requestParts);
        String command = requestParts.get(0);

        switch (command) {
            case "SEND":
                handleSendCommand(request, requestParts);
                break;
            case "RECEIVE":
                handleReceiveCommand(request, requestParts);
                break;
            case "QUIT":
                handleQuitCommand(request, requestParts);
                break;
            case "READY":
                handleReadyCommand(request, requestParts);
                break;
            default:
                handleQuitCommand(request, requestParts);
                break;
        }

    }

    private void interconnect(String fileKey) throws ClosedChannelException {
        Long senderId = waitingToSendMap.get(fileKey);
        Long receiverId = waitingToReceiveMap.get(fileKey);
        if (senderId != null && receiverId != null) {
            System.out.println("Interconnecting " + senderId + " " + receiverId);
            // Glue two sockets together
            IRCSocket senderSocket = socketMap.get(senderId);
            IRCSocket receiverSocket = socketMap.get(receiverId);
            FileTransferProxy proxy = new FileTransferProxy(fileKey, senderSocket, receiverSocket);
            fileTransferProxyMap.put(fileKey, proxy);
            // Remove waiting map entries
            removeKeyFromWaitingMaps(fileKey);
//            // Announce connection
//            sendQueue.add(new IRCMessage("READY\r\n", 0, senderId));
//            sendQueue.add(new IRCMessage("READY\r\n", 0, receiverId));
            // Unsubscribe
            unsubscribe(senderId, readSelector);
            unsubscribe(receiverId, readSelector);

            SelectionKey key = senderSocket.register(this.fileSendSelector, SelectionKey.OP_READ);
            key.attach(proxy);
        }

    }

    private void unsubscribe(Long id, Selector selector) {
        IRCSocket socket = socketMap.get(id);
        SelectionKey selectionKey = socket.getSelectionKey(selector);
        if (selectionKey != null) {
            selectionKey.attach(null);
            selectionKey.cancel();
        }

    }

    private void removeKeyFromWaitingMaps(String fileKey) {
        waitingToSendMap.remove(fileKey);
        waitingToReceiveMap.remove(fileKey);
    }

    private void handleSendCommand(IRCMessage request, List<String> requestParts) {
        String fileKey = requestParts.get(1);
        Long senderId = request.getFromId();
        waitingToSendMap.put(fileKey, senderId);
        //interconnect(fileKey);
        Long receiverId = waitingToReceiveMap.get(fileKey);
        if (receiverId == null) {
            return;
        }
        sendQueue.add(new IRCMessage("READY\r\n", 0, receiverId));
    }

    private void handleReceiveCommand(IRCMessage request, List<String> requestParts) {
        String fileKey = requestParts.get(1);
        Long receiverId = request.getFromId();
        waitingToReceiveMap.put(fileKey, receiverId);
        receiverMap.put(receiverId, fileKey);
        //interconnect(fileKey);
        Long senderId = waitingToSendMap.get(fileKey);
        if (senderId == null) {
            return;
        }
        sendQueue.add(new IRCMessage("READY\r\n", 0, receiverId));

    }

    private void handleQuitCommand(IRCMessage request, List<String> requestParts) throws IOException {
        Long id = request.getFromId();
        waitingToSendMap.remove(id);
        waitingToReceiveMap.remove(id);
        IRCSocket ircSocket = socketMap.get(id);
        closeSocket(ircSocket);
    }

    private void handleReadyCommand(IRCMessage request, List<String> requestParts) throws IOException {
        Long receiverId = request.getFromId();
        String fileKey = receiverMap.get(receiverId);
        Long senderId = waitingToSendMap.get(fileKey);
        if (senderId == null) {
            return;
        }

        interconnect(fileKey);
        sendQueue.add(new IRCMessage("READY\r\n", 0, senderId));
    }


    private void writeResponses() throws IOException {
        pullAllRequests();

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

    private void pullAllRequests() {
        while (true) {
            IRCMessage outMessage = sendQueue.poll();
            if (outMessage == null) {
                return;
            }
            IRCSocket ircSocket = socketMap.get(outMessage.getToId());

            if (ircSocket != null) {
                if (ircSocket.isWriterEmpty()) {
                    nonEmptyToEmptySockets.remove(ircSocket);
                    emptyToNonEmptySockets.add(ircSocket);
                }
                ircSocket.enqueue(outMessage.getMessage());

            }
        }

    }


}
