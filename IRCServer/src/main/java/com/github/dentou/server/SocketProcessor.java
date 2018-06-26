package com.github.dentou.server;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.*;

/**
 *
 * @param <S> Socket type
 * @param <M> Message Type
 */

public abstract class SocketProcessor<S, M> implements Runnable{
    private final Queue<S> socketQueue;
    private final Map<Long, S> socketMap = new HashMap<Long, S>();

    private final Selector readSelector;
    private final Selector writeSelector;

    private final Queue<M> requestQueue = new LinkedList<>(); // Contains already parsed requests (no \r\n)
    private Queue<M> sendQueue = new LinkedList<>();

    protected final Set<S> emptyToNonEmptySockets = new HashSet<>();
    protected final Set<S> nonEmptyToEmptySockets = new HashSet<>();

    public SocketProcessor(Queue<S> socketQueue) throws IOException {
        this.socketQueue = socketQueue;

        this.readSelector = Selector.open();
        this.writeSelector = Selector.open();
    }

    protected Queue<S> getSocketQueue() {
        return socketQueue;
    }

    protected Map<Long, S> getSocketMap() {
        return socketMap;
    }

    protected Selector getReadSelector() {
        return readSelector;
    }

    protected Selector getWriteSelector() {
        return writeSelector;
    }

    protected Queue<M> getRequestQueue() {
        return requestQueue;
    }

    protected Queue<M> getSendQueue() {
        return sendQueue;
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

        }
    }

    protected void registerNewSockets() throws ClosedChannelException {

        while (true) {
            S newSocket = this.socketQueue.poll();

            if (newSocket == null) {
                return;
            }

            registerNewSocket(newSocket);

        }
    }



    protected void registerNewSocket(S newSocket) throws ClosedChannelException {
        subscribe(newSocket, readSelector, SelectionKey.OP_READ);
    }


    protected void readRequests() throws IOException {
        int readReady = this.readSelector.selectNow();

        if (readReady > 0) {
            Set<SelectionKey> selectedKeys = this.readSelector.selectedKeys();
            Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

            while (keyIterator.hasNext()) {
                SelectionKey key = keyIterator.next();

                try {
                    readFromSocket((S) key.attachment());
                } catch (IOException e) {
                    e.printStackTrace();
                    closeSocket((S) key.attachment());
                }

                keyIterator.remove();
            }
            selectedKeys.clear();
        }
    }

    private void processRequests() throws IOException {
        while (true) {
            M request = requestQueue.poll();
            if (request == null) {
                break;
            }
            processRequest(request);
        }
        writeResponses();
    }

    protected  void writeResponses() throws IOException {
        pullAllRequests();

        cancelEmptySockets();

        registerNonEmptySockets();

        int writeReady = writeSelector.selectNow();

        if (writeReady > 0) {
            Set<SelectionKey> selectedKeys = writeSelector.selectedKeys();
            Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

            while (keyIterator.hasNext()) {
                SelectionKey key = keyIterator.next();

                S socket = (S) key.attachment();

                sendMessages(socket);

                keyIterator.remove();
            }

            selectedKeys.clear();
        }
    }

    private void registerNonEmptySockets() throws ClosedChannelException {
        for (S socket : emptyToNonEmptySockets) {
            subscribe(socket, writeSelector, SelectionKey.OP_WRITE);
        }
        emptyToNonEmptySockets.clear();
    }

    private void cancelEmptySockets() {
        for (S socket : nonEmptyToEmptySockets) {
            unsubscribe(socket, writeSelector);
        }
        nonEmptyToEmptySockets.clear();
    }

    private void pullAllRequests() {
        while (true) {
            M outMessage = sendQueue.poll();
            if (outMessage == null) {
                return;
            }
            enqueueMessage(outMessage);
        }

    }


    protected  abstract void closeSocket(S socket) throws IOException;


    protected abstract void subscribe(S socket, Selector selector, int keyOps) throws ClosedChannelException;

    protected abstract void unsubscribe(S socket, Selector selector);



    protected abstract void readFromSocket(S socket) throws IOException;

    protected abstract void sendMessages(S socket) throws IOException;


    protected abstract void processRequest(M request) throws IOException;

    protected abstract void enqueueMessage(M outMessage);


}
