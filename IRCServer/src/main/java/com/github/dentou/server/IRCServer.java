package com.github.dentou.server;


import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

import com.github.dentou.utils.ServerConstants;
import com.github.dentou.chat.IRCSocket;
import com.github.dentou.chat.ChatSocketProcessor;
import com.github.dentou.file.FileTransferProcessor;

public class IRCServer {

    private SocketAcceptor socketAcceptor;
    private ChatSocketProcessor chatSocketProcessor;
    private FileTransferProcessor fileTransferProcessor;

    private final int chatPort;
    private final int filePort;

    public IRCServer(int chatPort, int filePort) {
        this.chatPort = chatPort;
        this.filePort = filePort;
    }

    public void start() throws IOException {
        Queue<IRCSocket> chatSocketQueue = new ArrayBlockingQueue<IRCSocket>(ServerConstants.SOCKET_BUFFER_SIZE);
        Queue<IRCSocket> fileSocketQueue = new ArrayBlockingQueue<IRCSocket>(ServerConstants.SOCKET_BUFFER_SIZE);

        this.socketAcceptor = new SocketAcceptor(chatPort, chatSocketQueue, filePort, fileSocketQueue);
        this.chatSocketProcessor = new ChatSocketProcessor(chatSocketQueue);
        this.fileTransferProcessor = new FileTransferProcessor(fileSocketQueue);

        Thread acceptorThread  = new Thread(this.socketAcceptor);
        Thread chatProcessorThread = new Thread(this.chatSocketProcessor);
        Thread fileProcessorThread = new Thread(this.fileTransferProcessor);

        acceptorThread.start();
        chatProcessorThread.start();
        fileProcessorThread.start();
    }


}
