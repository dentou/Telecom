package com.github.dentou.chat;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;

public class SocketAcceptor implements Runnable{

    private int chatPort;
    private Queue<IRCSocket> chatSocketQueue;

    private int filePort;
    private Queue<IRCSocket> fileSocketQueue;

    private ServerSocketChannel chatServerChannel;
    private ServerSocketChannel fileServerChannel;

    private Selector chatSelector;
    private Selector fileSelector;

    public SocketAcceptor(int tcpPort, Queue<IRCSocket> chatSocketQueue, int filePort, Queue<IRCSocket> fileSocketQueue) {
        this.chatPort = tcpPort;
        this.chatSocketQueue = chatSocketQueue;

        this.filePort = filePort;
        this.fileSocketQueue = fileSocketQueue;
    }


    @Override
    public void run() {
        try {
            initializeChatServer();
            initializeFileServer();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        while (true) {
            try {
                listenToChatServer();
            } catch (IOException e) {
                System.out.println("Exceptions in chat listener: ");
                e.printStackTrace();
            }

            try {
                listenToFileServer();
            } catch (IOException e) {
                System.out.println("Exceptions in file listener: ");
                e.printStackTrace();
            }

        }

    }

    private void initializeChatServer() throws IOException {
        this.chatSelector = Selector.open();
        this.chatServerChannel = ServerSocketChannel.open();
        this.chatServerChannel.bind(new InetSocketAddress(chatPort));
        this.chatServerChannel.configureBlocking(false);
        this.chatServerChannel.register(chatSelector, SelectionKey.OP_ACCEPT);
        System.out.println("ChatServer starts at " + InetAddress.getLocalHost().getHostAddress() + ", port " + chatPort);
    }

    private void initializeFileServer() throws IOException {
        this.fileSelector = Selector.open();
        this.fileServerChannel = ServerSocketChannel.open();
        this.fileServerChannel.bind(new InetSocketAddress(filePort));
        this.fileServerChannel.configureBlocking(false);
        this.fileServerChannel.register(fileSelector, SelectionKey.OP_ACCEPT);
        System.out.println("FileServer starts at " + InetAddress.getLocalHost().getHostAddress() + ", port " + filePort);
    }

    private void listenToChatServer() throws IOException {
        int socketReady = this.chatSelector.selectNow();

        if (socketReady > 0) {
            Set<SelectionKey> selectedKeys = this.chatSelector.selectedKeys();
            Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

            while (keyIterator.hasNext()) {
                SelectionKey key = keyIterator.next();

                SocketChannel chatSocketChannel = ((ServerSocketChannel) key.channel()).accept();
                System.out.println("Chat Client connected: " + chatSocketChannel.socket().getLocalAddress());
                this.chatSocketQueue.add(new IRCSocket(chatSocketChannel, false));

                keyIterator.remove();
            }
            selectedKeys.clear();
        }
    }

    private void listenToFileServer() throws IOException {
        int socketReady = this.fileSelector.selectNow();

        if (socketReady > 0) {
            Set<SelectionKey> selectedKeys = this.fileSelector.selectedKeys();
            Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

            while (keyIterator.hasNext()) {
                SelectionKey key = keyIterator.next();

                SocketChannel fileSocketChannel = ((ServerSocketChannel) key.channel()).accept();
                System.out.println("File Client connected: " + fileSocketChannel.socket().getLocalAddress());
                this.fileSocketQueue.add(new IRCSocket(fileSocketChannel, false));

                keyIterator.remove();
            }
            selectedKeys.clear();
        }
    }
}
