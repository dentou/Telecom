package com.github.dentou;


import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

public class IRCServer {

    private SocketAcceptor socketAcceptor;
    private SocketProcessor socketProcessor;

    private final int tcpPort;

    public IRCServer(int tcpPort) {
        this.tcpPort = tcpPort;
    }

    public void start() throws IOException {
        Queue<IRCSocket> socketQueue = new ArrayBlockingQueue<IRCSocket>(1024); // todo Move 1024 to constants

        this.socketAcceptor = new SocketAcceptor(tcpPort, socketQueue);
        this.socketProcessor = new SocketProcessor(socketQueue);

        Thread accepterThread  = new Thread(this.socketAcceptor);
        Thread processorThread = new Thread(this.socketProcessor);

        accepterThread.start();
        processorThread.start();
    }


}
