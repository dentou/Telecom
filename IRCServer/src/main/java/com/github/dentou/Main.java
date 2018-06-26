package com.github.dentou;

import com.github.dentou.server.IRCServer;
import com.github.dentou.utils.ServerConstants;

import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
	    // write your code here
        new IRCServer(ServerConstants.CHAT_SERVER_PORT, ServerConstants.FILE_SERVER_PORT).start();
    }
}
