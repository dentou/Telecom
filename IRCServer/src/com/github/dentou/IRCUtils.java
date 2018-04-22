package com.github.dentou;

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
        for (int i = 0; i < fields.length; i++) {
            sb.append(fields[i]);
            if (i < fields.length - 1) {
                sb.append(" ");
            }
        }
        sb.append("\r\n"); // todo check this line break
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

    public static IRCMessage createRelayMessage(IRCMessage request, UserHandler userHandler, long toId) {
        String serverName = "localhost"; // todo change to real serverName
        long fromId = request.getFromId();
        String userHeader = createUserHeader(serverName, userHandler.getUserNick(fromId), userHandler.getUserName(fromId));
        StringBuilder sb = new StringBuilder();
        sb.append(":");
        String responseString = createResponseString(userHeader, request.getMessage());
        sb.append(responseString);
        return new IRCMessage(sb.toString(), request.getFromId(), toId);
    }

    public static IRCMessage createCommandResponse(IRCConstants.CommandResponse responseType, IRCMessage request,
                                             List<String> requestParts, UserHandler userHandler) {
        String serverName = "localhost"; // todo change to real serverName
        long requesterId = request.getFromId();
        String requesterNick = userHandler.getUserNick(requesterId);

        String serverHeader = createServerHeader(serverName);
        String userHeader;

        long receiverId = 0;

        StringBuilder sb = new StringBuilder();

        switch (responseType) {
            case RPL_WELCOME:
                receiverId = requesterId;
                userHeader = createUserHeader(serverName, requesterNick, userHandler.getUserName(requesterId));
                System.out.println("User header: " + userHeader);
                sb.append(":");
                String responseString = createResponseString(serverHeader, responseType.getNumericCode(),
                        requesterNick, "Welcome to the Internet Relay Network", userHeader);
                sb.append(responseString);
                break;
        }
        return new IRCMessage(sb.toString(), 0, receiverId);
    }

    public static IRCMessage createErrorReplies(IRCConstants.ErrorReplies errorType, IRCMessage request, List<String> requestParts) {
        String serverName = "localhost"; // todo change to real serverName
        long receiverId = request.getFromId();

        String serverHeader = createServerHeader(serverName);


        StringBuilder sb = new StringBuilder();
        sb.append(":");
        String responseString = "";

        switch (errorType) {
            case ERR_NICKNAMEINUSE:
                responseString = createResponseString(serverHeader, errorType.getNumericCode(),
                        requestParts.get(1), ":Nickname is already in use");
                sb.append(responseString);
                break;
            case ERR_NONICKNAMEGIVEN:
                responseString = createResponseString(serverHeader, errorType.getNumericCode(),
                        ":No nickname given");
                sb.append(responseString);
                break;
            case ERR_NEEDMOREPARAMS:
                responseString = createResponseString(serverHeader, errorType.getNumericCode(),
                        requestParts.get(0), ":No nickname given");
                sb.append(responseString);
                break;
            case ERR_NORECIPIENT:
                responseString = createResponseString(serverHeader, errorType.getNumericCode(),
                        requestParts.get(0), ":No recipient given");
                sb.append(responseString);
                break;
            case ERR_NOSUCHNICK:
                responseString = createResponseString(serverHeader, errorType.getNumericCode(),
                        requestParts.get(1), ":No such nick/channel");
                sb.append(responseString);
                break;
            case ERR_NOTEXTTOSEND:
                responseString = createResponseString(serverHeader, errorType.getNumericCode(),
                        ":No text to send");
                sb.append(responseString);
                break;
        }

        return new IRCMessage(sb.toString(), 0, receiverId);
    }


}
