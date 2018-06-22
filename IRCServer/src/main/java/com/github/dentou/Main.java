package com.github.dentou;

import com.github.dentou.chat.IRCConstants;
import com.github.dentou.chat.IRCServer;

import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
	    // write your code here
        new IRCServer(IRCConstants.CHAT_SERVER_PORT, IRCConstants.FILE_SERVER_PORT).start();
    }
}
