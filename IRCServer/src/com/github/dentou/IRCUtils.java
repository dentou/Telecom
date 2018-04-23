package com.github.dentou;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IRCUtils {

    public static boolean isValidChannelName(String name) {
        return name.matches("#[a-zA-Z0-9!@#$%^&*()]{1,49}");
    }

    public static List<String> parseRequest(String request) {
        List<String> parts = new ArrayList<String>();
        Matcher m = Pattern.compile("(?<=:).+|[^ :]+").matcher(request);
        while (m.find()) {
            parts.add(m.group());
        }
        return parts;
    }

    public static StringBuilder createResponseStringBuilder(String... fields) { // Create string with fields separated by space
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            sb.append(fields[i]);
            if (i < fields.length - 1) {
                sb.append(" ");
            }
        }
        return sb;
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
        String responseString = createResponseStringBuilder(userHeader, request.getMessage()).toString();
        sb.append(responseString);
        sb.append("\r\n"); // End of message
        return new IRCMessage(sb.toString(), request.getFromId(), toId);
    }

    public static IRCMessage createCommandResponse(IRCConstants.CommandResponse responseType, IRCMessage request,
                                                   List<String> requestParts, UserHandler userHandler) {
        String serverName = "localhost"; // todo change to real serverName
        long requesterId = request.getFromId();
        String requesterNick = userHandler.getUserNick(requesterId);
        if (requesterNick == null) {
            requesterNick = "*";
        }

        String serverHeader = createServerHeader(serverName);
        String userHeader = createUserHeader(serverName, requesterNick, userHandler.getUserName(requesterId));;


        StringBuilder sb = new StringBuilder();
        sb.append(":");
        StringBuilder responseStringBuilder = new StringBuilder();



        switch (responseType) {
            case RPL_WELCOME:
                responseStringBuilder = createResponseStringBuilder(serverHeader, responseType.getNumericCode(),
                        requesterNick, ":Welcome to the Internet Relay Network", userHeader);
                break;
            case RPL_YOURHOST:
                responseStringBuilder = createResponseStringBuilder(serverHeader, responseType.getNumericCode(),
                        requesterNick, ":Your host is", userHeader);
                break;
            case RPL_WHOISUSER:
                long userId = userHandler.getUserId(requestParts.get(1));
                responseStringBuilder = createResponseStringBuilder(serverHeader, responseType.getNumericCode(),
                        requesterNick, userHandler.getUserNick(userId), userHandler.getUserName(userId), serverName,
                        "*", ":" + userHandler.getUserFullName(userId));
                break;
            case RPL_TOPIC:
                String topic = userHandler.getChannel(requestParts.get(1)).getTopic();
                responseStringBuilder = createResponseStringBuilder(serverHeader, responseType.getNumericCode(),
                        requesterNick, requestParts.get(1), ":" + topic);
                break;
            case RPL_NAMEREPLY:
                List<String> nicks = userHandler.getChannelMemberNicks(requestParts.get(1));
                responseStringBuilder = createResponseStringBuilder(serverHeader, responseType.getNumericCode(),
                        requesterNick, "=", requestParts.get(1), ":");
                for (int i = 0; i < nicks.size(); i++) {
                    responseStringBuilder.append(nicks.get(i));
                    if (i < nicks.size() - 1) {
                        responseStringBuilder.append(" ");
                    }
                }
                break;
            case RPL_ENDOFNAMES:
                responseStringBuilder = createResponseStringBuilder(serverHeader, responseType.getNumericCode(),
                        requesterNick, "=", requestParts.get(1), ":End of NAMES list");
                break;
        }
        sb.append(responseStringBuilder.toString());
        sb.append("\r\n"); // End of message
        return new IRCMessage(sb.toString(), 0, requesterId);
    }

    public static IRCMessage createErrorReplies(IRCConstants.ErrorReplies errorType, IRCMessage request,
                                                List<String> requestParts , UserHandler userHandler) {
        String serverName = "localhost"; // todo change to real serverName
        long receiverId = request.getFromId();

        String serverHeader = createServerHeader(serverName);


        StringBuilder sb = new StringBuilder();
        sb.append(":");
        StringBuilder responseStringBuilder = new StringBuilder();

        String requesterNick = userHandler.getUserNick(request.getFromId());
        if (requesterNick == null) {
            requesterNick = "*";
        }

        switch (errorType) {
            case ERR_NICKNAMEINUSE:
                responseStringBuilder = createResponseStringBuilder(serverHeader, errorType.getNumericCode(),
                        requesterNick, requestParts.get(1), ":Nickname is already in use");
                break;
            case ERR_NONICKNAMEGIVEN:
                responseStringBuilder = createResponseStringBuilder(serverHeader, errorType.getNumericCode(),
                        requesterNick, ":No nickname given");
                break;
            case ERR_NEEDMOREPARAMS:

                responseStringBuilder = createResponseStringBuilder(serverHeader, errorType.getNumericCode(),
                        requesterNick, requestParts.get(0), ":Not enough parameters");
                break;
            case ERR_NORECIPIENT:
                responseStringBuilder = createResponseStringBuilder(serverHeader, errorType.getNumericCode(),
                       requesterNick , requestParts.get(0), ":No recipient given");
                break;
            case ERR_NOSUCHNICK:
                responseStringBuilder = createResponseStringBuilder(serverHeader, errorType.getNumericCode(),
                        requesterNick, requestParts.get(1), ":No such nick/channel");
                break;
            case ERR_NOTEXTTOSEND:
                responseStringBuilder = createResponseStringBuilder(serverHeader, errorType.getNumericCode(),
                        requesterNick, ":No text to send");
                break;
            case ERR_UNKNOWNCOMMAND:
                responseStringBuilder = createResponseStringBuilder(serverHeader, errorType.getNumericCode(),
                        requesterNick, requestParts.get(0), ":Unknown command");
                break;
            case ERR_NOTREGISTERED:
                responseStringBuilder = createResponseStringBuilder(serverHeader, errorType.getNumericCode(),
                        requesterNick, ":You have not registered");
                break;
        }
        sb.append(responseStringBuilder.toString());
        sb.append("\r\n"); // End of message
        return new IRCMessage(sb.toString(), 0, receiverId);
    }


}
