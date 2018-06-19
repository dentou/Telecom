package com.github.dentou.chat;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IRCUtils {

    public static List<String> parseRequest(String request) {
        List<String> parts = new ArrayList<String>();
        Matcher m = Pattern.compile("(?<=:).+|[^ :]+").matcher(request);
        while (m.find()) {
            parts.add(m.group());
        }
        return parts;
    }

    public static String createResponseString(String ... fields) { // Create string with fields separated by space
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.length - 1; i++) {
            sb.append(fields[i]);
            if (i < fields.length - 1) {
                sb.append(" ");
            }
        }
        return sb.toString();
    }

    public static String createServerHeader(String serverName) {
        StringBuilder sb = new StringBuilder();
        sb.append(serverName);
        return sb.toString();
    }

    public static String createUserHeader(String serverName, String nick, String userName) {
        StringBuilder sb = new StringBuilder();
        sb.append(nick);
        sb.append("!");
        sb.append(userName);
        sb.append("@");
        sb.append(serverName);
        return sb.toString();
    }
}
