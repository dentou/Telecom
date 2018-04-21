package com.github.dentou;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.Scanner;

public class IRCClient {
    private IRCSocket ircSocket;

    private SocketListener socketListener;
    private SocketSpeaker socketSpeaker;

    public IRCClient(String serverAddress, int serverPort) throws IOException {
        SocketChannel socketChannel = SocketChannel.open(new InetSocketAddress(serverAddress, serverPort));
        this.ircSocket = new IRCSocket(socketChannel, false);
    }

    public void start() throws IOException{
        this.socketListener = new SocketListener(ircSocket);
        this.socketSpeaker = new SocketSpeaker(ircSocket);

        Thread listenerThread = new Thread(socketListener);
        Thread speakerThread = new Thread(socketSpeaker);

        listenerThread.start();
        speakerThread.start();

    }
}
