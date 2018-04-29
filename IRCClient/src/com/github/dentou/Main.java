package com.github.dentou;

import java.io.IOException;

public class Main {
    private static final String freeNodeServer = "irc.freenode.net";
    private static final String huyServer = "169.254.53.178";

    public static void main(String[] args) throws IOException {
        // write your code here
        new IRCClient(huyServer, 6667).start();

    }
}
