package com.github.dentou.chat;

import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
	    // write your code here
        new IRCServer(IRCConstants.CHAT_SERVER_PORT, IRCConstants.FILE_SERVER_PORT).start();
    }
}
