package com.github.dentou;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Queue;

public class SocketAcceptor implements Runnable{

    private int tcpPort;
    private Queue<IRCSocket> socketQueue;

    private ServerSocketChannel serverChannel;

    public SocketAcceptor(int tcpPort, Queue<IRCSocket> socketQueue) {
        this.tcpPort = tcpPort;
        this.socketQueue = socketQueue;
    }


    @Override
    public void run() {
        try {
            this.serverChannel = ServerSocketChannel.open();
            this.serverChannel.bind(new InetSocketAddress(tcpPort));
            System.out.println("Server starts at " + InetAddress.getLocalHost().getHostAddress());
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        while (true) {
            try {
                SocketChannel socketChannel = serverChannel.accept();

                System.out.println("Client connected: " + socketChannel.socket().getLocalAddress());

                this.socketQueue.add(new IRCSocket(socketChannel, false));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}
