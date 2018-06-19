package com.github.dentou.chat;


import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

import com.github.dentou.chat.IRCConstants;
import com.github.dentou.file.FileTransferProcessor;

public class IRCServer {

    private SocketAcceptor socketAcceptor;
    private SocketProcessor socketProcessor;
    private FileTransferProcessor fileTransferProcessor;

    private final int chatPort;
    private final int filePort;

    public IRCServer(int chatPort, int filePort) {
        this.chatPort = chatPort;
        this.filePort = filePort;
    }

    public void start() throws IOException {
        Queue<IRCSocket> chatSocketQueue = new ArrayBlockingQueue<IRCSocket>(IRCConstants.SOCKET_BUFFER_SIZE);
        Queue<IRCSocket> fileSocketQueue = new ArrayBlockingQueue<IRCSocket>(IRCConstants.SOCKET_BUFFER_SIZE);

        this.socketAcceptor = new SocketAcceptor(chatPort, chatSocketQueue, filePort, fileSocketQueue);
        this.socketProcessor = new SocketProcessor(chatSocketQueue);
        this.fileTransferProcessor = new FileTransferProcessor(fileSocketQueue);

        Thread acceptorThread  = new Thread(this.socketAcceptor);
        Thread chatProcessorThread = new Thread(this.socketProcessor);
        Thread fileProcessorThread = new Thread(this.fileTransferProcessor);

        acceptorThread.start();
        chatProcessorThread.start();
        fileProcessorThread.start();
    }


}
