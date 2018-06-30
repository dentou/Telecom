package com.github.dentou.chat;

import com.github.dentou.server.SocketProcessor;
import com.github.dentou.utils.ServerConstants;
import com.github.dentou.utils.ServerUtils;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.*;

import static com.github.dentou.utils.ServerConstants.Response;
import static com.github.dentou.utils.ServerUtils.*;


/**
 *
 */
public class ChatSocketProcessor extends SocketProcessor<IRCSocket, IRCMessage> {

    private final UserHandler userHandler = new UserHandler();

    public ChatSocketProcessor(Queue<IRCSocket> socketQueue) throws IOException {

        super(socketQueue);
    }


    @Override
    protected void registerNewSocket(IRCSocket newSocket) throws ClosedChannelException {
        getSocketMap().put(newSocket.getId(), newSocket);
        this.userHandler.addUser(newSocket.getId());
        subscribe(newSocket, getReadSelector(), SelectionKey.OP_READ);
    }

    @Override
    protected void subscribe(IRCSocket socket, Selector selector, int keyOps) throws ClosedChannelException {
        SelectionKey key = socket.register(selector, keyOps);
        key.attach(socket);
    }

    @Override
    protected void unsubscribe(IRCSocket socket, Selector selector) {
        SelectionKey key = socket.getSelectionKey(selector);
        if (key != null) {
            key.attach(null);
            key.cancel();
        }
    }


    @Override
    protected void closeSocket(IRCSocket socket) throws IOException {
        if (socket == null) {
            return;
        }
        System.out.println("Socket closed: " + socket.getId());
        getSocketMap().remove(socket.getId());
        this.userHandler.removeUserFromAllChannels(socket.getId());
        this.userHandler.removeUser(socket.getId()); // todo check this new adds
        unsubscribe(socket, getReadSelector());
        socket.getSocketChannel().close();
    }

    @Override
    protected void readFromSocket(IRCSocket socket) throws IOException {
        List<IRCMessage> requests = socket.getMessages();
        System.out.println("[Chat] Requests from socket: " + requests);

        if (requests.size() > 0) {
            for (IRCMessage request : requests) {
                if (request.getMessage() != null & !request.getMessage().trim().isEmpty()) {
                    getRequestQueue().add(request);
                }
            }
        }

        if (socket.isEndOfStreamReached()) {
            // Create a pseudo QUIT request and add to queue it
            IRCMessage quitRequest = new IRCMessage("QUIT", socket.getId(), 0);
            getRequestQueue().add(quitRequest);
        }
    }


    @Override
    protected void processRequest(IRCMessage request) throws IOException { // Process command and add response to send queue
        if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
            return;
        }
        List<String> requestParts = parseRequest(request.getMessage());
        System.out.println("Request " + requestParts);
        String command = requestParts.get(0);

        switch (command) {
            case "NICK": // NICK <nick>
                handleNickCommand(request, requestParts);
                break;
            case "USER":
                handleUserCommand(request, requestParts);
                break;
            case "QUIT":
                handleQuitCommand(request, requestParts);
                break;
            case "PRIVMSG":
                handlePrivmsgCommand(request, requestParts);
                break;
            case "PING":
                handlePingCommand(request, requestParts);
                break;
            case "WHOIS":
                handleWhoisCommand(request, requestParts);
                break;
            case "JOIN":
                handleJoinCommand(request, requestParts);//todo implements channel
                break;
            case "PART":
                handlePartCommand(request, requestParts);
                break;
            case "TOPIC":
                handleTopicCommand(request, requestParts);
                break;
            case "KICK":
                handleKickCommand(request, requestParts);
                break;
            case "LIST":
                handleListCommand(request, requestParts);
                break;
            case "WHO":
                handleWhoCommand(request, requestParts);
                break;
            case "NAMES":
                handleNamesCommmand(request, requestParts);
                break;
            case "MODE":
                handleModeCommand(request, requestParts);
                break;
            case "FILE_SEND":
            case "FILE_RECEIVE":
            case "FILE_DENY":
                handleFileCommand(request, requestParts);
                break;
            case "FILE_RESUME":
            case "FILE_RESEND":
            case "FILE_RESUME_DENY":
                handleFileResumeCommand(request, requestParts);
                break;
            default:
                getSendQueue().addAll(createResponse(Response.ERR_UNKNOWNCOMMAND, request, requestParts, userHandler));
                break;
        }

    }

    private void handleNickCommand(IRCMessage request, List<String> requestParts) {
        // Not enough params
        if (requestParts.size() < 2) {
            getSendQueue().addAll(createResponse(Response.ERR_NONICKNAMEGIVEN, request, requestParts, userHandler));
            return;
        }
        // Check if nickname is valid
        if (!isValidNick(requestParts.get(1))) {
            getSendQueue().addAll(createResponse(Response.ERR_ERRONEOUSNICKNAME, request, requestParts, userHandler));
            return;
        }
        // Check if another user has same nick
        if (userHandler.containsNick(requestParts.get(1))) {
            getSendQueue().addAll(createResponse(Response.ERR_NICKNAMEINUSE, request, requestParts, userHandler));
            return;
        }
        // Check if user was registered before this command
        boolean isRegisteredBefore = userHandler.isRegistered(request.getFromId());
        // Set user's nick
        userHandler.changeUserInfo(request.getFromId(), "nick", requestParts.get(1));
        // Check if user is registered after this command
        boolean isRegisteredAfter = userHandler.isRegistered(request.getFromId());
        // User was not registered before and is registered after -> New user -> Send welcome
        if (!isRegisteredBefore && isRegisteredAfter) {
            sendWelcome(request, requestParts);
        }
    }

    private void handleUserCommand(IRCMessage request, List<String> requestParts) {
        // Not enough params
        if (requestParts.size() < 5) {
            getSendQueue().addAll(createResponse(Response.ERR_NEEDMOREPARAMS, request, requestParts, userHandler));
            return;
        }
        // Check if user was registered before this command
        boolean isRegisteredBefore = userHandler.isRegistered(request.getFromId());
        // Set user's name and full name
        userHandler.changeUserInfo(request.getFromId(), "userName", requestParts.get(1));
        userHandler.changeUserInfo(request.getFromId(), "userFullName", requestParts.get(4));
        // Check if user is registered after this command
        boolean isRegisteredAfter = userHandler.isRegistered(request.getFromId());
        // User was not registered before and is registered after -> New user -> Send welcome
        if (!isRegisteredBefore && isRegisteredAfter) {
            sendWelcome(request, requestParts);
        }

    }

    private void sendWelcome(IRCMessage request, List<String> requestParts) {
        System.out.println("Welcome sent");
        getSendQueue().addAll(createResponse(Response.RPL_WELCOME, request, requestParts, userHandler));
    }

    private void handleQuitCommand(IRCMessage request, List<String> requestParts) throws IOException {
        // Send PART messages
        if (userHandler.isRegistered(request.getFromId())) {
            // Create a pseudo JOIN 0 request and handle it
            IRCMessage join0Request = new IRCMessage("JOIN 0", request.getFromId(), request.getToId());
            handleJoinCommand(join0Request, parseRequest(join0Request.getMessage()));
        }

        userHandler.removeUser(request.getFromId());
        IRCSocket ircSocket = getSocketMap().get(request.getFromId());
        closeSocket(ircSocket);


    }

    private void handlePrivmsgCommand(IRCMessage request, List<String> requestParts) {
        // If user has not yet registered
        if (!userHandler.isRegistered(request.getFromId())) {
            getSendQueue().addAll(createResponse(Response.ERR_NOTREGISTERED, request, requestParts, userHandler));
            return;
        }
        // Not enough params
        if (requestParts.size() <= 1) {
            getSendQueue().addAll(createResponse(Response.ERR_NEEDMOREPARAMS, request, requestParts, userHandler));
            return;
        }
        // Not enough params
        if (requestParts.size() < 3) { // Only PRIVMSG and a parameter given
            if (request.getMessage().contains(":")) { // If only text is given, no nickname
                getSendQueue().addAll(createResponse(Response.ERR_NONICKNAMEGIVEN, request, requestParts, userHandler));
            } else { // Only nickname is given, no text to send
                getSendQueue().addAll(createResponse(Response.ERR_NOTEXTTOSEND, request, requestParts, userHandler));
            }
            return;
        }
        // Check if receiver is a channel
        if (isValidChannelName(requestParts.get(1))) {
            String channelName = requestParts.get(1);
            if (!userHandler.containsChannel(channelName)) { // No channel has that name
                getSendQueue().addAll(createResponse(Response.ERR_NOSUCHNICK, request, requestParts, userHandler));
                return;
            }
            // There's a channel with the same name
            // Check if user is a member
            if (userHandler.isOnChannel(request.getFromId(), channelName)) { // User is a member of channel
                sendToChannel(channelName, request);
                return;
            } else { // User is not a member
                // todo ERR_CANNOTSENDTOCHAN
                getSendQueue().addAll(createResponse(Response.ERR_CANNOTSENDTOCHAN, request, requestParts, userHandler));
                return;
            }

        }
        // Try to get receiver
        User user = userHandler.getUser(requestParts.get(1));
        if (user == null) {
            getSendQueue().addAll(createResponse(Response.ERR_NOSUCHNICK, request, requestParts, userHandler));
            return;
        }
        // Relay message to receiver
        getSendQueue().add(createRelayMessage(request, userHandler, user.getId()));
    }

    private void handlePingCommand(IRCMessage request, List<String> requestParts) {
        getSendQueue().add(new IRCMessage("PONG\r\n", 0, request.getFromId()));
    }

    private void handleWhoisCommand(IRCMessage request, List<String> requestParts) {
        // If user has not yet registered
        if (!userHandler.isRegistered(request.getFromId())) {
            getSendQueue().addAll(createResponse(Response.ERR_NOTREGISTERED, request, requestParts, userHandler));
            return;
        }
        // If no param, ignore
        if (requestParts.size() < 2) {
            // Not sending ERR_NEEDMOREPARAMS
            return;
        }
        // If nick user asks for does not exist
        if (!userHandler.containsNick(requestParts.get(1))) {
            getSendQueue().addAll(createResponse(Response.ERR_NOSUCHNICK, request, requestParts, userHandler));
            return;
        }
        //Send info back to requester
        getSendQueue().addAll(createResponse(Response.RPL_WHOISUSER, request, requestParts, userHandler));
        getSendQueue().addAll(createResponse(Response.RPL_ENDOFWHOIS, request, requestParts, userHandler));

    }

    private void handleJoinCommand(IRCMessage request, List<String> requestParts) {
        // If user has not yet registered
        if (!userHandler.isRegistered(request.getFromId())) {
            getSendQueue().addAll(createResponse(Response.ERR_NOTREGISTERED, request, requestParts, userHandler));
            return;
        }
        // Not enough parameters
        if (requestParts.size() < 2) {
            getSendQueue().addAll(createResponse(Response.ERR_NEEDMOREPARAMS, request, requestParts, userHandler));
            return;
        }
        // Check if JOIN 0 (PART all channels)
        if (requestParts.get(1).equals("0")) { // todo send to all members
            Set<String> channelNames = userHandler.getJoinedChannelNames(request.getFromId());
            for (String channelName : channelNames) {
                // Create a pseudo PART request and handle it
                IRCMessage partRequest = new IRCMessage("PART " + channelName, request.getFromId(), 0);
                handlePartCommand(partRequest, parseRequest(partRequest.getMessage()));
            }
            return;
        }
        //Check valid channel name
        String channelName = requestParts.get(1);
        if (!isValidChannelName(channelName)) {
            getSendQueue().addAll(createResponse(Response.ERR_NOSUCHCHANNEL, request, requestParts, userHandler));
            return;
        }
        // If there exists no channel with the same name, create new one
        if (!userHandler.containsChannel(channelName)) {
            userHandler.createChannel(channelName, request.getFromId()); // Create channel and make user admin
        } else {
            // Check if channel is invite-only
            if (userHandler.getChannelModes(channelName).contains("i")) {
                getSendQueue().addAll(createResponse(Response.ERR_INVITEONLYCHAN, request, requestParts, userHandler));
                return;
            }
            // Check if channel is full
            if (userHandler.isFull(channelName)) {
                getSendQueue().addAll(createResponse(Response.ERR_CHANNELISFULL, request, requestParts, userHandler));
                return;
            }
            // Check if channel has key
            String key = userHandler.getChannelKey(channelName);
            if (key != null && !key.isEmpty()) {
                if (requestParts.size() < 3 || !key.equals(requestParts.get(2))) {
                    getSendQueue().addAll(createResponse(Response.ERR_BADCHANNELKEY, request, requestParts, userHandler));
                    return;
                }
            }
            // Add user to channel
            userHandler.addUserToChannel(request.getFromId(), channelName);
        }
        String channelTopic = userHandler.getChannelTopic(channelName);
        //Send to all channel members
        sendToChannel(channelName, request);
        //Send channel topic to requester
        if (channelTopic != null && !channelTopic.isEmpty()) {
            getSendQueue().addAll(createResponse(Response.RPL_TOPIC, request, requestParts, userHandler));
        }
        //Send channel name list
        getSendQueue().addAll(createResponse(Response.RPL_NAMEREPLY, request, requestParts, userHandler));
        getSendQueue().addAll(createResponse(Response.RPL_ENDOFNAMES, request, requestParts, userHandler));
        return;
    }

    private void handlePartCommand(IRCMessage request, List<String> requestParts) {
        // If user has not yet registered
        if (!userHandler.isRegistered(request.getFromId())) {
            getSendQueue().addAll(createResponse(Response.ERR_NOTREGISTERED, request, requestParts, userHandler));
            return;
        }
        // Check enough params
        if (requestParts.size() < 2) {
            getSendQueue().addAll(createResponse(Response.ERR_NEEDMOREPARAMS, request, requestParts, userHandler));
            return;
        }
        // Check if channel exists
        String channelName = requestParts.get(1);
        if (!userHandler.containsChannel(channelName)) {
            getSendQueue().addAll(createResponse(Response.ERR_NOSUCHNICK, request, requestParts, userHandler));
            return;
        }
        // Check if user is on channel
        if (!userHandler.isOnChannel(request.getFromId(), channelName)) {
            getSendQueue().addAll(createResponse(Response.ERR_NOTONCHANNEL, request, requestParts, userHandler));
        }
        // Send message to all channel members
        sendToChannel(channelName, request);
        // Remove user from channel
        userHandler.removeUserFromChannel(request.getFromId(), channelName);
    }

    private void handleTopicCommand(IRCMessage request, List<String> requestParts) {
        // If user has not yet registered
        if (!userHandler.isRegistered(request.getFromId())) {
            getSendQueue().addAll(createResponse(Response.ERR_NOTREGISTERED, request, requestParts, userHandler));
            return;
        }
        // Check parameters
        if (requestParts.size() < 2) {
            getSendQueue().addAll(createResponse(Response.ERR_NEEDMOREPARAMS, request, requestParts, userHandler));
            return;
        }
        // Check if channel exists
        String channelName = requestParts.get(1);
        if (!userHandler.containsChannel(channelName)) {
            getSendQueue().addAll(createResponse(Response.ERR_NOSUCHNICK, request, requestParts, userHandler));
            return;
        }
        // Check if requester is member of channel
        if (!userHandler.isOnChannel(request.getFromId(), channelName)) {
            getSendQueue().addAll(createResponse(Response.ERR_NOTONCHANNEL, request, requestParts, userHandler));
            return;
        }
        // If only channelName is given, return topic
        if (requestParts.size() < 3) {
            String channelTopic = userHandler.getChannelTopic(channelName);
            // If no topic is set for channel
            if (channelTopic == null || channelTopic.isEmpty()) {
                getSendQueue().addAll(createResponse(Response.RPL_NOTOPIC, request, requestParts, userHandler));
                return;
            }

            getSendQueue().addAll(createResponse(Response.RPL_TOPIC, request, requestParts, userHandler));
            return;
        }

        // If channelName and newTopic is given
        // Check if requester is moderator or admin (channel operator)
        if (!userHandler.isChannelOperator(request.getFromId(), channelName)) {
            getSendQueue().addAll(createResponse(Response.ERR_CHANOPRIVSNEEDED, request, requestParts, userHandler));
            return;
        }

        String newTopic = requestParts.get(2);
        userHandler.setChannelTopic(channelName, newTopic);
        sendToChannel(channelName, request);
    }

    private void handleKickCommand(IRCMessage request, List<String> requestParts) {
        // If user has not yet registered
        if (!userHandler.isRegistered(request.getFromId())) {
            getSendQueue().addAll(createResponse(Response.ERR_NOTREGISTERED, request, requestParts, userHandler));
            return;
        }
        // Check parameters
        if (requestParts.size() < 3) {
            getSendQueue().addAll(createResponse(Response.ERR_NEEDMOREPARAMS, request, requestParts, userHandler));
            return;
        }
        // Check if channel exists
        String channelName = requestParts.get(1);
        if (!userHandler.containsChannel(channelName)) {
            getSendQueue().addAll(createResponse(Response.ERR_NOSUCHNICK, request, requestParts, userHandler));
            return;
        }
        // Check if requester is member of channel
        if (!userHandler.isOnChannel(request.getFromId(), channelName)) {
            getSendQueue().addAll(createResponse(Response.ERR_NOTONCHANNEL, request, requestParts, userHandler));
            return;
        }
        // Check if requester is moderator or admin (channel operator)
        if (!userHandler.isChannelOperator(request.getFromId(), channelName)) {
            getSendQueue().addAll(createResponse(Response.ERR_CHANOPRIVSNEEDED, request, requestParts, userHandler));
            return;
        }
        // Check if user being kicked is on channel
        User kicked = userHandler.getUser(requestParts.get(2));
        if (kicked == null || !userHandler.isOnChannel(kicked.getId(), channelName)) {
            getSendQueue().addAll(createResponse(Response.ERR_USERNOTINCHANNEL, request, requestParts, userHandler));
            return;
        }
        // Send kick message
        sendToChannel(channelName, request);
        // Remove user from channel
        userHandler.removeUserFromChannel(kicked.getId(), channelName);

    }

    private void handleListCommand(IRCMessage request, List<String> requestParts) {
        getSendQueue().addAll(createResponse(Response.RPL_LIST, request, requestParts, userHandler));
        getSendQueue().addAll(createResponse(Response.RPL_LISTEND, request, requestParts, userHandler));
    }

    private void handleWhoCommand(IRCMessage request, List<String> requestParts) {
        getSendQueue().addAll(createResponse(Response.RPL_WHOREPLY, request, requestParts, userHandler));
        getSendQueue().addAll(createResponse(Response.RPL_ENDOFWHO, request, requestParts, userHandler));
    }

    private void handleNamesCommmand(IRCMessage request, List<String> requestParts) {
        if (requestParts.size() == 1) {
            Set<String> joinedChannels = userHandler.getJoinedChannelNames(request.getFromId());
            if (joinedChannels.size() == 0) {
                getSendQueue().addAll(createResponse(Response.RPL_ENDOFNAMES, request, requestParts, userHandler));
                return;
            }
            for (String channelName : joinedChannels) {
                getRequestQueue().add(new IRCMessage("NAMES " + channelName, request.getFromId(), request.getToId()));
            }
            return;
        }
        //Send channel name list
        getSendQueue().addAll(createResponse(Response.RPL_NAMEREPLY, request, requestParts, userHandler));
        getSendQueue().addAll(createResponse(Response.RPL_ENDOFNAMES, request, requestParts, userHandler));
        return;
    }

    private void handleModeCommand(IRCMessage request, List<String> requestParts) {
        // If user has not yet registered
        if (!userHandler.isRegistered(request.getFromId())) {
            getSendQueue().addAll(createResponse(Response.ERR_NOTREGISTERED, request, requestParts, userHandler));
            return;
        }
        // Check parameters
        if (requestParts.size() < 2) {
            getSendQueue().addAll(createResponse(Response.ERR_NEEDMOREPARAMS, request, requestParts, userHandler));
            return;
        }
        // Check if channel exists
        String channelName = requestParts.get(1);
        if (!userHandler.containsChannel(channelName)) {
            getSendQueue().addAll(createResponse(Response.ERR_NOSUCHNICK, request, requestParts, userHandler));
            return;
        }
        // If only MODE and channel, send channel mode back
        if (requestParts.size() < 3) {
            // todo
            getSendQueue().addAll(createResponse(Response.RPL_CHANNELMODEIS, request, requestParts, userHandler));
            return;
        }
        // If new channel mode is given
        // Check if user is operator // todo
        if (!userHandler.isChannelOperator(request.getFromId(), channelName)) {
            getSendQueue().addAll(createResponse(Response.ERR_CHANOPRIVSNEEDED, request, requestParts, userHandler));
            return;
        }
        // todo
        String modeString = requestParts.get(2);
        boolean enable = (modeString.charAt(0) == '+');
        char flag = modeString.charAt(1);
        String parameter = "";
        if (flag == 'o' || flag == 'k' || flag == 'l') {
            if (requestParts.size() < 4) { // Check parameters
                getSendQueue().addAll(createResponse(Response.ERR_NEEDMOREPARAMS, request, requestParts, userHandler));
                return;
            } else {
                parameter = requestParts.get(3);
            }
        }

        // If affected user is not a member of the channel
        if (flag == 'o') {
            User affectedUser = userHandler.getUser(parameter); // todo check if nick is valid
            if (affectedUser == null || !userHandler.isOnChannel(affectedUser.getId(), channelName)) {
                getSendQueue().addAll(createResponse(Response.ERR_USERNOTINCHANNEL, request, requestParts, userHandler));
                return;
            }
        }

        if (ServerConstants.ChannelMode.getMode(flag) == null) {
            getSendQueue().addAll(createResponse(Response.ERR_UNKNOWNMODE, request, requestParts, userHandler));
            return;
        }
        userHandler.setChannelMode(channelName, flag, parameter, enable);
        sendToChannel(channelName, request);
    }


    private void handleFileCommand(IRCMessage request, List<String> requestParts) {
        // If user has not yet registered
        if (!userHandler.isRegistered(request.getFromId())) {
            getSendQueue().addAll(createResponse(Response.ERR_NOTREGISTERED, request, requestParts, userHandler));
            return;
        }
        // Check parameters
        if (requestParts.size() < 4) {
            getSendQueue().addAll(createResponse(Response.ERR_NEEDMOREPARAMS, request, requestParts, userHandler));
            return;
        }

        // If nick user asks for does not exist
        String nick = requestParts.get(1);
        User recipient = userHandler.getUser(nick);
        if (recipient == null) {
            getSendQueue().addAll(createResponse(Response.ERR_NOSUCHNICK, request, requestParts, userHandler));
            return;
        }

        getSendQueue().add(createRelayMessage(request, userHandler, recipient.getId()));

    }

    private void handleFileResumeCommand(IRCMessage request, List<String> requestParts) {
        // If user has not yet registered
        if (!userHandler.isRegistered(request.getFromId())) {
            getSendQueue().addAll(createResponse(Response.ERR_NOTREGISTERED, request, requestParts, userHandler));
            return;
        }
        // Check parameters
        if (requestParts.size() < 5) {
            getSendQueue().addAll(createResponse(Response.ERR_NEEDMOREPARAMS, request, requestParts, userHandler));
            return;
        }

        // If nick user asks for does not exist
        String nick = requestParts.get(1);
        User recipient = userHandler.getUser(nick);
        if (recipient == null) {
            getSendQueue().addAll(createResponse(Response.ERR_NOSUCHNICK, request, requestParts, userHandler));
            return;
        }

        getSendQueue().add(createRelayMessage(request, userHandler, recipient.getId()));
    }


    private void sendToChannel(String channelName, IRCMessage request) {
        Set<User> members = userHandler.getChannelMembers(channelName).keySet();
        for (User member : members) {
            getSendQueue().add(createRelayMessage(request, userHandler, member.getId()));
        }
    }


    @Override
    protected void sendMessages(IRCSocket socket) throws IOException {
        socket.sendMessages();

        if (socket.isWriterEmpty()) {
            this.nonEmptyToEmptySockets.add(socket);
        }
    }


    @Override
    protected void enqueueMessage(IRCMessage outMessage) {
        IRCSocket ircSocket = getSocketMap().get(outMessage.getToId());

        if (ircSocket != null) {
            if (ircSocket.isWriterEmpty()) {
                nonEmptyToEmptySockets.remove(ircSocket);
                emptyToNonEmptySockets.add(ircSocket);
            }
            ircSocket.enqueue(outMessage.getMessage());

        }
    }


}
