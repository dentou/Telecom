package com.github.dentou;

import java.io.IOException;
import java.net.Socket;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.*;


public class SocketProcessor implements Runnable {
    private Queue<IRCSocket> socketQueue;
    private Map<Long, IRCSocket> socketMap = new HashMap<Long, IRCSocket>();
    private long nextSocketId = 1024; // Id frm 0 to 1023 is reserved for servers

    private Selector readSelector;
    private Selector writeSelector;


    private Queue<IRCMessage> requestQueue = new LinkedList<>();
    private Queue<IRCMessage> sendQueue = new LinkedList<>();

    private UserHandler userHandler;

    private Set<IRCSocket> emptyToNonEmptySockets = new HashSet<>();
    private Set<IRCSocket> nonEmptyToEmptySockets = new HashSet<>();

    public SocketProcessor(Queue<IRCSocket> socketQueue) throws IOException{
        this.socketQueue = socketQueue;

        this.readSelector = Selector.open();
        this.writeSelector = Selector.open();

        this.userHandler = new UserHandler();
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

    private void readFromSocket(SelectionKey key) throws IOException {
        IRCSocket socket = (IRCSocket) key.attachment();
        List<IRCMessage> requests = socket.getMessages();
        System.out.println("(readFromSocket) Requests from socket: " + requests);

        if (requests.size() > 0) {
            for (IRCMessage request : requests) {
                this.requestQueue.add(request);
            }
        }

        if (socket.isEndOfStreamReached()) {
            System.out.println("Socket closed: " + socket.getId());
            this.socketMap.remove(socket.getId());
            key.attach(null);
            key.cancel();
            key.channel().close();
        }
    }

    private void readRequests() throws IOException {
        int readReady = this.readSelector.selectNow();

        if (readReady > 0) {
            Set<SelectionKey> selectedKeys = this.readSelector.selectedKeys();
            Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

            while(keyIterator.hasNext()) {
                SelectionKey key = keyIterator.next();

                readFromSocket(key);

                keyIterator.remove();
            }
            selectedKeys.clear();
        }
    }

    private void processRequests() throws IOException{
        while (true) {
            IRCMessage request = requestQueue.poll();
            if (request == null) {
                break;
            }
            processRequest(request);
        }
        // todo write to socket
        writeResponses();

    }



    private void processRequest(IRCMessage request) { // Process command and add response to send queue

        List<String> requestParts = IRCUtils.parseRequest(request.getMessage());
        System.out.println("Request " + requestParts);
        String command = requestParts.get(0);

        UserHandler.StatusCode statusCode;
        switch (command) {
            case "NICK": // NICK <nick>
                statusCode = userHandler.addUser(request.getFromId(), requestParts.get(1));
                if (statusCode == UserHandler.StatusCode.NICK_DUPLICATE) {
                    sendQueue.add(createErrorReplies(IRCConstants.ErrorReplies.ERR_NICKNAMEINUSE, request, requestParts));
                }
                break;
            case "USER":
                statusCode = userHandler.changeUserInfo(request.getFromId(), "userName", requestParts.get(1));
                if (statusCode == UserHandler.StatusCode.NEW_USER) {
                    sendQueue.add(createCommandResponse(IRCConstants.CommandResponse.RPL_WELCOME, request, requestParts));
                    System.out.println("Welcome sent");
                }
                statusCode = userHandler.changeUserInfo(request.getFromId(), "userFullName", requestParts.get(4));
                break;
        }

    }


    private void writeResponses() throws IOException {
        pullAllRequest();

        cancelEmptySockets();

        registerNonEmptySockets();

        int writeReady = writeSelector.selectNow();

        if (writeReady > 0) {
            Set<SelectionKey> selectedKeys = writeSelector.selectedKeys();
            Iterator<SelectionKey> keyIterator   = selectedKeys.iterator();

            while(keyIterator.hasNext()){
                SelectionKey key = keyIterator.next();

                IRCSocket ircSocket = (IRCSocket) key.attachment();

                ircSocket.sendMessages();

                if(ircSocket.isWriterEmpty()){
                    this.nonEmptyToEmptySockets.add(ircSocket);
                }

                keyIterator.remove();
            }

            selectedKeys.clear();
        }
    }

    private void registerNonEmptySockets() throws ClosedChannelException {
        for(IRCSocket ircSocket : emptyToNonEmptySockets){
            SelectionKey key = ircSocket.register(this.writeSelector, SelectionKey.OP_WRITE);
            key.attach(ircSocket);
        }
        emptyToNonEmptySockets.clear();
    }

    private void cancelEmptySockets() {
        for(IRCSocket ircSocket : nonEmptyToEmptySockets){
            SelectionKey key = ircSocket.getSelectionKey(this.writeSelector);
            key.cancel();
        }
        nonEmptyToEmptySockets.clear();
    }

    private void pullAllRequest() throws IOException{
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


    private IRCMessage createCommandResponse(IRCConstants.CommandResponse responseType, IRCMessage request, List<String> requestParts) {
        String serverName = "localhost"; // todo change to real serverName
        long requesterId = request.getFromId();
        String requesterNick = userHandler.getUserNick(requesterId);
        String requesterUserName = userHandler.getUserName(requesterId);

        String serverHeader = IRCUtils.createServerHeader(serverName);
        String userHeader = IRCUtils.createUserHeader(serverName, requesterNick, requesterUserName);

        long receiverId = 0;

        StringBuilder sb = new StringBuilder();

        switch (responseType) {
            case RPL_WELCOME:
                receiverId = requesterId;
                userHeader = IRCUtils.createUserHeader(serverName, requesterNick, userHandler.getUserName(requesterId));
                System.out.println("User header: " + userHeader);
                sb.append(":");
                String responseString = IRCUtils.createResponseString(serverHeader, responseType.getNumericCode(),
                        requesterNick, "Welcome to the Internet Relay Network", userHeader);
                sb.append(responseString);
                break;
        }
        return new IRCMessage(sb.toString(), 0, receiverId);
    }

    private IRCMessage createErrorReplies(IRCConstants.ErrorReplies errorType, IRCMessage request, List<String> requestParts) {
        String serverName = "localhost"; // todo change to real serverName
        long requesterId = request.getFromId();

        String serverHeader = IRCUtils.createServerHeader(serverName);

        long receiverId = requesterId;

        StringBuilder sb = new StringBuilder();

        switch (errorType) {
            case ERR_NICKNAMEINUSE:
                sb.append(":");
                String responseString = IRCUtils.createResponseString(serverHeader, errorType.getNumericCode(),
                        requestParts.get(1), ":Nickname is already in use");
                sb.append(responseString);
                break;

        }

        return new IRCMessage(sb.toString(), 0, receiverId);
    }


}
