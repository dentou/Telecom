package com.github.dentou;

import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
        // write your code here
        new IRCClient("localhost", 6667).start();

    }
}
