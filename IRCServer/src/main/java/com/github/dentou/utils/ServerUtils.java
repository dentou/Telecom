package com.github.dentou.utils;

import com.github.dentou.chat.IRCMessage;
import com.github.dentou.chat.User;
import com.github.dentou.chat.UserHandler;

import java.io.IOException;
import java.net.InetAddress;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ServerUtils {
    public static String getServerAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (IOException e) {
            e.printStackTrace();
            return "Unknown server address";
        }

    }

    public static boolean isValidChannelName(String name) {
        return name.matches("#[a-zA-Z0-9!@#$%^&*()]{1,49}");
    }
    public static boolean isValidNick(String nick) {
        return nick.matches("[a-zA-Z0-9_]{1,10}");
    }

    public static List<String> parseRequest(String request) {
        List<String> parts = new ArrayList<String>();
        Matcher m = Pattern.compile("(?<=:).+|[^ :]+").matcher(request);
        while (m.find()) {
            parts.add(m.group());
        }
        return parts;
    }

    private static StringBuilder createResponseStringBuilder(String... fields) { // Create string with fields separated by space
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            sb.append(fields[i]);
            if (i < fields.length - 1) {
                sb.append(" ");
            }
        }
        return sb;
    }

    private static String createServerHeader(String serverName) {
        StringBuilder sb = new StringBuilder();
        sb.append(serverName);
        return sb.toString();
    }

    private static String createUserHeader(String serverName, String nick, String userName) {
        StringBuilder sb = new StringBuilder();
        sb.append(nick);
        sb.append("!");
        sb.append(userName);
        sb.append("@");
        sb.append(serverName);
        return sb.toString();
    }

    public static IRCMessage createRelayMessage(IRCMessage request, UserHandler userHandler, long toId) {
        String serverName = getServerAddress(); // todo change to real serverName
        long fromId = request.getFromId();
        User fromUser = userHandler.getUser(fromId);
        String userHeader = createUserHeader(serverName, fromUser.getNick(), fromUser.getUserName());
        StringBuilder sb = new StringBuilder();
        sb.append(":");
        String responseString = createResponseStringBuilder(userHeader, request.getMessage()).toString();
        sb.append(responseString);
        sb.append("\r\n"); // End of message
        return new IRCMessage(sb.toString(), request.getFromId(), toId);
    }


    public static Queue<IRCMessage> createResponse(ServerConstants.Response responseType, IRCMessage request,
                                                   List<String> requestParts , UserHandler userHandler) {

        boolean autoQueue = true; // Flag to automatically put response into queue

        Queue<IRCMessage> messageQueue = new LinkedList<>();

        String serverName = getServerAddress(); // todo change to real serverName
        long requesterId = request.getFromId();
        User requester = userHandler.getUser(requesterId);
        String requesterNick = requester.getNick();
        if (requesterNick == null) {
            requesterNick = "*";
        }

        String serverHeader = createServerHeader(serverName);
        String userHeader = createUserHeader(serverName, requesterNick, requester.getUserName());


        StringBuilder sb = new StringBuilder();
        sb.append(":");
        StringBuilder responseStringBuilder = new StringBuilder();


        switch (responseType) {
            // Replies
            case RPL_WELCOME:
                responseStringBuilder = createResponseStringBuilder(serverHeader, responseType.getNumericCode(),
                        requesterNick, ":Welcome to the Internet Relay Network", userHeader);
                break;
            case RPL_WHOREPLY:
                autoQueue = false;
                for (User user : userHandler.getAllUsers()) {
                    if (!userHandler.isRegistered(user.getId())) {
                        continue;
                    }

                    sb = new StringBuilder();
                    sb.append(":");

                    responseStringBuilder = createResponseStringBuilder(serverHeader, responseType.getNumericCode(),
                            requesterNick, user.getNick(), user.getUserName(), "*", "*", ":" + user.getUserFullName());

                    sb.append(responseStringBuilder);
                    sb.append("\r\n");
                    messageQueue.add(new IRCMessage(sb.toString(), 0, requesterId));
                }
                break;
            case RPL_ENDOFWHO:
                responseStringBuilder = createResponseStringBuilder(serverHeader, responseType.getNumericCode(),
                        requesterNick, ":End of WHO list");
                break;
            case RPL_WHOISUSER:
                User user = userHandler.getUser(requestParts.get(1));
                responseStringBuilder = createResponseStringBuilder(serverHeader, responseType.getNumericCode(),
                        requesterNick, user.getNick(), user.getUserName(), "*", "*", ":" + user.getUserFullName());
                break;
            case RPL_ENDOFWHOIS:
                responseStringBuilder = createResponseStringBuilder(serverHeader, responseType.getNumericCode(),
                        requesterNick, ":End of WHOIS list");
                break;

            case RPL_TOPIC:
                String topic = userHandler.getChannelTopic(requestParts.get(1));
                responseStringBuilder = createResponseStringBuilder(serverHeader, responseType.getNumericCode(),
                        requesterNick, requestParts.get(1), ":" + topic);
                break;
            case RPL_NOTOPIC:
                responseStringBuilder = createResponseStringBuilder(serverHeader, responseType.getNumericCode(),
                        requesterNick, requestParts.get(1), ":No topic is set");
                break;
            case RPL_NAMEREPLY:
                Map<User, String> members = userHandler.getChannelMembers(requestParts.get(1));
                responseStringBuilder = createResponseStringBuilder(serverHeader, responseType.getNumericCode(),
                        requesterNick, "=", requestParts.get(1), ":");
                int i = 0;
                for (User member : members.keySet()) {
                    responseStringBuilder.append(members.get(member)); // Append admin or moderator sign (* or +)
                    responseStringBuilder.append(member.getNick());
                    if (i < members.size() - 1) {
                        responseStringBuilder.append(" ");
                    }
                    i++;
                }
                break;
            case RPL_ENDOFNAMES:
                String channelName = requestParts.size() < 2 ? "*" : requestParts.get(1);
                responseStringBuilder = createResponseStringBuilder(serverHeader, responseType.getNumericCode(),
                        requesterNick, "=", channelName, ":End of NAMES list");
                break;
            case RPL_LIST:
                autoQueue = false;
                Map<String, String> namesAndTopics = userHandler.getAllChannelNamesAndTopics();
                for (String name : namesAndTopics.keySet()) {
                    sb = new StringBuilder();
                    sb.append(":");

                    topic = namesAndTopics.get(name);
                    String memberNum = Long.toString(userHandler.getChannelNumberOfMembers(name));
                    responseStringBuilder = createResponseStringBuilder(serverHeader, responseType.getNumericCode(),
                            requesterNick, name, memberNum , ":" + topic);

                    sb.append(responseStringBuilder);
                    sb.append("\r\n");
                    messageQueue.add(new IRCMessage(sb.toString(), 0, requesterId));
                }
                break;
            case RPL_LISTEND:
                responseStringBuilder = createResponseStringBuilder(serverHeader, responseType.getNumericCode(),
                        requesterNick, ":End of LIST");
                break;
            case RPL_CHANNELMODEIS:
                channelName = requestParts.get(1);
                responseStringBuilder = createResponseStringBuilder(serverHeader, responseType.getNumericCode(),
                        requesterNick, channelName, userHandler.getChannelModes(channelName));
                break;



            // Errors
            case ERR_NOSUCHNICK:
                responseStringBuilder = createResponseStringBuilder(serverHeader, responseType.getNumericCode(),
                        requesterNick, requestParts.get(1), ":No such nick/channel");
                break;
            case ERR_NOSUCHCHANNEL:
                responseStringBuilder = createResponseStringBuilder(serverHeader, responseType.getNumericCode(),
                        requesterNick, requestParts.get(1), ":No such channel");
                break;
            case ERR_CANNOTSENDTOCHAN:
                responseStringBuilder = createResponseStringBuilder(serverHeader, responseType.getNumericCode(),
                        requesterNick, requestParts.get(1), ":Cannot send to channel");
                break;
            case ERR_NORECIPIENT:
                responseStringBuilder = createResponseStringBuilder(serverHeader, responseType.getNumericCode(),
                        requesterNick , ":No recipient given");
                break;
            case ERR_NOTEXTTOSEND:
                responseStringBuilder = createResponseStringBuilder(serverHeader, responseType.getNumericCode(),
                        requesterNick, ":No text to send");
                break;
            case ERR_UNKNOWNCOMMAND:
                responseStringBuilder = createResponseStringBuilder(serverHeader, responseType.getNumericCode(),
                        requesterNick, requestParts.get(0), ":Unknown command");
                break;
            case ERR_NONICKNAMEGIVEN:
                responseStringBuilder = createResponseStringBuilder(serverHeader, responseType.getNumericCode(),
                        requesterNick, ":No nickname given");
                break;
            case ERR_ERRONEOUSNICKNAME:
                responseStringBuilder = createResponseStringBuilder(serverHeader, responseType.getNumericCode(),
                        requesterNick, requestParts.get(1), ":Erroneous nickname");
                break;
            case ERR_NICKNAMEINUSE:
                responseStringBuilder = createResponseStringBuilder(serverHeader, responseType.getNumericCode(),
                        requesterNick, requestParts.get(1), ":Nickname is already in use");
                break;
            case ERR_USERNOTINCHANNEL:
                if (requestParts.get(0).equals("MODE")) {
                    responseStringBuilder = createResponseStringBuilder(serverHeader, responseType.getNumericCode(),
                            requesterNick, requestParts.get(3), requestParts.get(1), ":They aren't on that channel");
                } else {
                    responseStringBuilder = createResponseStringBuilder(serverHeader, responseType.getNumericCode(),
                            requesterNick, requestParts.get(2), requestParts.get(1), ":They aren't on that channel");
                }
                break;
            case ERR_NOTONCHANNEL:
                responseStringBuilder = createResponseStringBuilder(serverHeader, responseType.getNumericCode(),
                        requesterNick, requestParts.get(1), ":You're not on that channel");
                break;
            case ERR_NOTREGISTERED:
                responseStringBuilder = createResponseStringBuilder(serverHeader, responseType.getNumericCode(),
                        requesterNick, ":You have not registered");
                break;
            case ERR_NEEDMOREPARAMS:

                responseStringBuilder = createResponseStringBuilder(serverHeader, responseType.getNumericCode(),
                        requesterNick, requestParts.get(0), ":Not enough parameters");
                break;
            case ERR_CHANNELISFULL:
                responseStringBuilder = createResponseStringBuilder(serverHeader, responseType.getNumericCode(),
                        requesterNick, requestParts.get(1), ":Cannot join channel (+l)");
                break;
            case ERR_UNKNOWNMODE:
                responseStringBuilder = createResponseStringBuilder(serverHeader, responseType.getNumericCode(),
                        requesterNick, requestParts.get(2), ":Is unknown mode char for me for channel " + requestParts.get(1));
                break;
            case ERR_INVITEONLYCHAN:
                responseStringBuilder = createResponseStringBuilder(serverHeader, responseType.getNumericCode(),
                        requesterNick, requestParts.get(1), ":Cannot join channel (+i)");
                break;

            case ERR_BADCHANNELKEY:
                responseStringBuilder = createResponseStringBuilder(serverHeader, responseType.getNumericCode(),
                        requesterNick, requestParts.get(1), ":Cannot join channel (+k)");
                break;
            case ERR_CHANOPRIVSNEEDED:
                responseStringBuilder = createResponseStringBuilder(serverHeader, responseType.getNumericCode(),
                        requesterNick, requestParts.get(1), ":You're not channel operator");
                break;

        }
        if (autoQueue) {
            sb.append(responseStringBuilder);
            sb.append("\r\n"); // End of message
            messageQueue.add(new IRCMessage(sb.toString(), 0, requesterId));
        }

        return messageQueue;
    }


}
