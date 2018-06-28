package com.github.dentou.file;

import com.github.dentou.chat.IRCMessage;
import com.github.dentou.chat.IRCSocket;
import com.github.dentou.server.SocketProcessor;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.*;
import java.util.concurrent.*;

public class FileTransferProcessor extends SocketProcessor<IRCSocket, IRCMessage> {

    private final ExecutorService fileTransferExecutor = Executors.newFixedThreadPool(5);

    private final ConcurrentMap<String, Long> waitingToSendMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Long> waitingToReceiveMap = new ConcurrentHashMap<>();


    private final ConcurrentMap<Long, String> receiverMap = new ConcurrentHashMap<>();

    public FileTransferProcessor(Queue<IRCSocket> socketQueue) throws IOException {
        super(socketQueue);

    }


    @Override
    protected void registerNewSocket(IRCSocket newSocket) throws ClosedChannelException {
        getSocketMap().put(newSocket.getId(), newSocket);
        subscribe(newSocket, getReadSelector(), SelectionKey.OP_READ);
    }

    @Override
    protected void subscribe(IRCSocket socket, Selector selector, int keyOps) throws ClosedChannelException {
        SelectionKey key = socket.register(selector, keyOps);
        key.attach(socket);
    }

    @Override
    protected void unsubscribe(IRCSocket socket, Selector selector) {
        SelectionKey key = socket.getSelectionKey(selector);
        if (key != null) {
            key.attach(null);
            key.cancel();
        }
    }


    @Override
    protected void closeSocket(IRCSocket socket) throws IOException {
        if (socket == null) {
            System.out.println("Cannot close null socket");
            return;
        }
        System.out.println("[FileTF] Socket closed: " + socket.getId());
        getSocketMap().remove(socket.getId());
        unsubscribe(socket, getReadSelector());
        socket.getSocketChannel().close();
    }

    @Override
    protected void readFromSocket(IRCSocket socket) throws IOException {
        List<IRCMessage> requests = socket.getMessages();
        System.out.println("[FileTF] Requests from socket: " + requests);

        if (requests.size() > 0) {
            for (IRCMessage request : requests) {
                if (request.getMessage() != null & !request.getMessage().trim().isEmpty()) {
                    getRequestQueue().add(request);
                }
            }
        }

        if (socket.isEndOfStreamReached()) {
            closeSocket(socket);
        }
    }


    private void closeProxy(FileTransferProxy proxy) throws IOException {
        System.out.println("Closing proxy " + proxy.toString());
        receiverMap.remove(proxy.getInSocket().getId());
        closeSocket(proxy.getInSocket());
        closeSocket(proxy.getOutSocket());

    }


    protected void processRequest(IRCMessage request) throws IOException { // Process command and add response to send queue
        if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
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

    private void interconnect(String fileKey) {
        Long senderId = waitingToSendMap.get(fileKey);
        Long receiverId = waitingToReceiveMap.get(fileKey);
        if (senderId != null && receiverId != null) {
            System.out.println("Interconnecting " + senderId + " " + receiverId);
            // Glue two sockets together
            IRCSocket senderSocket = getSocketMap().get(senderId);
            IRCSocket receiverSocket = getSocketMap().get(receiverId);
            FileTransferProxy proxy = new FileTransferProxy(fileKey, senderSocket, receiverSocket);
            // Remove waiting map entries
            removeKeyFromWaitingMaps(fileKey);
            // Unsubscribe
            unsubscribe(senderSocket, getReadSelector());
            unsubscribe(receiverSocket, getReadSelector());
            // Put on executor
            fileTransferExecutor.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        while (true) {
                            proxy.transfer();
                            if (proxy.isTransferEnded()) {
                                break;
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            closeProxy(proxy);
                        } catch (IOException ex) {
                            System.out.println("Cannot close proxy " + proxy);
                            ex.printStackTrace();
                        }

                    }
                }
            });

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
        getSendQueue().add(new IRCMessage("READY\r\n", 0, receiverId));
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
        getSendQueue().add(new IRCMessage("READY\r\n", 0, receiverId));

    }

    private void handleQuitCommand(IRCMessage request, List<String> requestParts) throws IOException {
        Long id = request.getFromId();
        waitingToSendMap.remove(id);
        waitingToReceiveMap.remove(id);
        IRCSocket ircSocket = getSocketMap().get(id);
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
        getSendQueue().add(new IRCMessage("READY\r\n", 0, senderId));
    }


    @Override
    protected void sendMessages(IRCSocket socket) throws IOException {
        socket.sendMessages();

        if (socket.isWriterEmpty()) {
            this.nonEmptyToEmptySockets.add(socket);
        }
    }


    @Override
    protected void enqueueMessage(IRCMessage outMessage) {
        IRCSocket ircSocket = getSocketMap().get(outMessage.getToId());

        if (ircSocket != null) {
            if (ircSocket.isWriterEmpty()) {
                nonEmptyToEmptySockets.remove(ircSocket);
                emptyToNonEmptySockets.add(ircSocket);
            }
            ircSocket.enqueue(outMessage.getMessage());

        }
    }


}
