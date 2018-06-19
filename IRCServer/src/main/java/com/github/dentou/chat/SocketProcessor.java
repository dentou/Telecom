package com.github.dentou.chat;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.*;

import static com.github.dentou.chat.IRCConstants.Response;
import static com.github.dentou.chat.IRCUtils.*;


/**
 *
 */
public class SocketProcessor implements Runnable {
    private String serverName = getServerAddress();
    private Queue<IRCSocket> socketQueue;
    private Map<Long, IRCSocket> socketMap = new HashMap<Long, IRCSocket>();
    private long nextSocketId = 1024; // Id frm 0 to 1023 is reserved for servers

    private Selector readSelector;
    private Selector writeSelector;


    private Queue<IRCMessage> requestQueue = new LinkedList<>(); // Contains already parsed requests (no \r\n)
    private Queue<IRCMessage> sendQueue = new LinkedList<>();

    private UserHandler userHandler;

    private Set<IRCSocket> emptyToNonEmptySockets = new HashSet<>();
    private Set<IRCSocket> nonEmptyToEmptySockets = new HashSet<>();

    public SocketProcessor(Queue<IRCSocket> socketQueue) throws IOException {
        this.socketQueue = socketQueue;

        this.readSelector = Selector.open();
        this.writeSelector = Selector.open();

        this.userHandler = new UserHandler();
    }

    @Override
    public void run() {
        while (true) {
            try {
                registerNewSockets();
                readRequests();
                processRequests(); // todo implement processRequests()
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


    private void registerNewSockets() throws ClosedChannelException {

        while (true) {
            IRCSocket newSocket = this.socketQueue.poll();

            if (newSocket == null) {
                return;
            }

            newSocket.setId(nextSocketId++);
            this.socketMap.put(newSocket.getId(), newSocket);
            this.userHandler.addUser(newSocket.getId());
            SelectionKey key = newSocket.register(this.readSelector, SelectionKey.OP_READ);
            key.attach(newSocket);
        }
    }

    private void closeSocket(IRCSocket socket) throws IOException {
        System.out.println("Socket closed: " + socket.getId());
        this.socketMap.remove(socket.getId());
        this.userHandler.removeUser(socket.getId()); // todo check this new adds
        SelectionKey readKey = socket.getSelectionKey(readSelector);
        readKey.attach(null);
        readKey.cancel();
        readKey.channel().close();
    }

    private void readFromSocket(SelectionKey key) throws IOException {
        IRCSocket socket = (IRCSocket) key.attachment();
        List<IRCMessage> requests = socket.getMessages();
        System.out.println("(readFromSocket) Requests from socket: " + requests);

        if (requests.size() > 0) {
            for (IRCMessage request : requests) {
                if (request.getMessage() != null & !request.getMessage().trim().isEmpty()) {
                    this.requestQueue.add(request);
                }
            }
        }

        if (socket.isEndOfStreamReached()) {
            closeSocket(socket);
        }
    }

    private void readRequests() throws IOException {
        int readReady = this.readSelector.selectNow();

        if (readReady > 0) {
            Set<SelectionKey> selectedKeys = this.readSelector.selectedKeys();
            Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

            while (keyIterator.hasNext()) {
                SelectionKey key = keyIterator.next();

                try {
                    readFromSocket(key);
                } catch (IOException e) {
                    e.printStackTrace();
                    closeSocket((IRCSocket) key.attachment());
                }


                keyIterator.remove();
            }
            selectedKeys.clear();
        }
    }


    private void processRequests() throws IOException {
        while (true) {
            IRCMessage request = requestQueue.poll();
            if (request == null) {
                break;
            }
            processRequest(request);
        }
        // todo write to socket
        writeResponses();

    }


    private void processRequest(IRCMessage request) throws IOException { // Process command and add response to send queue
        if (request.getMessage() == null || request.getMessage().trim() == "") {
            return;
        }
        List<String> requestParts = IRCUtils.parseRequest(request.getMessage());
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
                sendQueue.addAll(createResponse(Response.ERR_UNKNOWNCOMMAND, request, requestParts, userHandler));
                break;
        }

    }

    private void handleNickCommand(IRCMessage request, List<String> requestParts) {
        // Not enough params
        if (requestParts.size() < 2) {
            sendQueue.addAll(createResponse(Response.ERR_NONICKNAMEGIVEN, request, requestParts, userHandler));
            return;
        }
        // Check if nickname is valid
        if (!isValidNick(requestParts.get(1))) {
            sendQueue.addAll(createResponse(Response.ERR_ERRONEOUSNICKNAME, request, requestParts, userHandler));
            return;
        }
        // Check if another user has same nick
        if (userHandler.containsNick(requestParts.get(1))) {
            sendQueue.addAll(createResponse(Response.ERR_NICKNAMEINUSE, request, requestParts, userHandler));
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
            sendQueue.addAll(createResponse(Response.ERR_NEEDMOREPARAMS, request, requestParts, userHandler));
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
        sendQueue.addAll(createResponse(Response.RPL_WELCOME, request, requestParts, userHandler));
    }

    private void handleQuitCommand(IRCMessage request, List<String> requestParts) throws IOException {
        // Send PART messages
        if (userHandler.isRegistered(request.getFromId())) {
            Set<String> channelNames = userHandler.getJoinedChannelNames(request.getFromId());
            if (channelNames.isEmpty()) {
                userHandler.removeUser(request.getFromId());
                IRCSocket ircSocket = socketMap.get(request.getFromId());
                closeSocket(ircSocket);
                return;
            } else {
                for (String channelName : channelNames) {
                    requestQueue.add(new IRCMessage("PART " + channelName, request.getFromId(), 0));
                }
                requestQueue.add(request);
            }
        } else {
            userHandler.removeUser(request.getFromId());
            IRCSocket ircSocket = socketMap.get(request.getFromId());
            closeSocket(ircSocket);
        }

    }

    private void handlePrivmsgCommand(IRCMessage request, List<String> requestParts) {
        // If user has not yet registered
        if (!userHandler.isRegistered(request.getFromId())) {
            sendQueue.addAll(createResponse(Response.ERR_NOTREGISTERED, request, requestParts, userHandler));
            return;
        }
        // Not enough params
        if (requestParts.size() <= 1) {
            sendQueue.addAll(createResponse(Response.ERR_NEEDMOREPARAMS, request, requestParts, userHandler));
            return;
        }
        // Not enough params
        if (requestParts.size() < 3) { // Only PRIVMSG and a parameter given
            if (request.getMessage().contains(":")) { // If only text is given, no nickname
                sendQueue.addAll(createResponse(Response.ERR_NONICKNAMEGIVEN, request, requestParts, userHandler));
            } else { // Only nickname is given, no text to send
                sendQueue.addAll(createResponse(Response.ERR_NOTEXTTOSEND, request, requestParts, userHandler));
            }
            return;
        }
        // Check if receiver is a channel
        if (isValidChannelName(requestParts.get(1))) {
            String channelName = requestParts.get(1);
            if (!userHandler.containsChannel(channelName)) { // No channel has that name
                sendQueue.addAll(createResponse(Response.ERR_NOSUCHNICK, request, requestParts, userHandler));
                return;
            }
            // There's a channel with the same name
            // Check if user is a member
            if (userHandler.isOnChannel(request.getFromId(), channelName)) { // User is a member of channel
                sendToChannel(channelName, request);
                return;
            } else { // User is not a member
                // todo ERR_CANNOTSENDTOCHAN
                sendQueue.addAll(createResponse(Response.ERR_CANNOTSENDTOCHAN, request, requestParts, userHandler));
                return;
            }

        }
        // Try to get receiver
        User user = userHandler.getUser(requestParts.get(1));
        if (user == null) {
            sendQueue.addAll(createResponse(Response.ERR_NOSUCHNICK, request, requestParts, userHandler));
            return;
        }
        // Relay message to receiver
        sendQueue.add(createRelayMessage(request, userHandler, user.getId()));
    }

    private void handlePingCommand(IRCMessage request, List<String> requestParts) {
        sendQueue.add(new IRCMessage("PONG\r\n", 0, request.getFromId()));
    }

    private void handleWhoisCommand(IRCMessage request, List<String> requestParts) {
        // If user has not yet registered
        if (!userHandler.isRegistered(request.getFromId())) {
            sendQueue.addAll(createResponse(Response.ERR_NOTREGISTERED, request, requestParts, userHandler));
            return;
        }
        // If no param, ignore
        if (requestParts.size() < 2) {
            // Not sending ERR_NEEDMOREPARAMS
            return;
        }
        // If nick user asks for does not exist
        if (!userHandler.containsNick(requestParts.get(1))) {
            sendQueue.addAll(createResponse(Response.ERR_NOSUCHNICK, request, requestParts, userHandler));
            return;
        }
        //Send info back to requester
        sendQueue.addAll(createResponse(Response.RPL_WHOISUSER, request, requestParts, userHandler));
        sendQueue.addAll(createResponse(Response.RPL_ENDOFWHOIS, request, requestParts, userHandler));

    }

    private void handleJoinCommand(IRCMessage request, List<String> requestParts) {
        // If user has not yet registered
        if (!userHandler.isRegistered(request.getFromId())) {
            sendQueue.addAll(createResponse(Response.ERR_NOTREGISTERED, request, requestParts, userHandler));
            return;
        }
        // Not enough parameters
        if (requestParts.size() < 2) {
            sendQueue.addAll(createResponse(Response.ERR_NEEDMOREPARAMS, request, requestParts, userHandler));
            return;
        }
        // Check if JOIN 0 (PART all channels)
        if (requestParts.get(1).equals("0")) { // todo send to all members
            Set<String> channelNames = userHandler.getJoinedChannelNames(request.getFromId());
            for (String channelName : channelNames) {
                requestQueue.add(new IRCMessage("PART " + channelName, request.getFromId(), 0));
            }
            return;
        }
        //Check valid channel name
        String channelName = requestParts.get(1);
        if (!isValidChannelName(channelName)) {
            sendQueue.addAll(createResponse(Response.ERR_NOSUCHCHANNEL, request, requestParts, userHandler));
            return;
        }
        // If there exists no channel with the same name, create new one
        if (!userHandler.containsChannel(channelName)) {
            userHandler.createChannel(channelName, request.getFromId()); // Create channel and make user admin
        } else {
            // Check if channel is invite-only
            if (userHandler.getChannelModes(channelName).contains("i")) {
                sendQueue.addAll(createResponse(Response.ERR_INVITEONLYCHAN, request, requestParts, userHandler));
                return;
            }
            // Check if channel is full
            if (userHandler.isFull(channelName)) {
                sendQueue.addAll(createResponse(Response.ERR_CHANNELISFULL, request, requestParts, userHandler));
                return;
            }
            // Check if channel has key
            String key = userHandler.getChannelKey(channelName);
            if (key != null && !key.isEmpty()) {
                if (requestParts.size() < 3 || !key.equals(requestParts.get(2))) {
                    sendQueue.addAll(createResponse(Response.ERR_BADCHANNELKEY, request, requestParts, userHandler));
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
            sendQueue.addAll(createResponse(Response.RPL_TOPIC, request, requestParts, userHandler));
        }
        //Send channel name list
        sendQueue.addAll(createResponse(Response.RPL_NAMEREPLY, request, requestParts, userHandler));
        sendQueue.addAll(createResponse(Response.RPL_ENDOFNAMES, request, requestParts, userHandler));
        return;
    }

    private void handlePartCommand(IRCMessage request, List<String> requestParts) {
        // If user has not yet registered
        if (!userHandler.isRegistered(request.getFromId())) {
            sendQueue.addAll(createResponse(Response.ERR_NOTREGISTERED, request, requestParts, userHandler));
            return;
        }
        // Check enough params
        if (requestParts.size() < 2) {
            sendQueue.addAll(createResponse(Response.ERR_NEEDMOREPARAMS, request, requestParts, userHandler));
            return;
        }
        // Check if channel exists
        String channelName = requestParts.get(1);
        if (!userHandler.containsChannel(channelName)) {
            sendQueue.addAll(createResponse(Response.ERR_NOSUCHNICK, request, requestParts, userHandler));
            return;
        }
        // Check if user is on channel
        if (!userHandler.isOnChannel(request.getFromId(), channelName)) {
            sendQueue.addAll(createResponse(Response.ERR_NOTONCHANNEL, request, requestParts, userHandler));
        }
        // Send message to all channel members
        sendToChannel(channelName, request);
        // Remove user from channel
        userHandler.removeUserFromChannel(request.getFromId(), channelName);
    }

    private void handleTopicCommand(IRCMessage request, List<String> requestParts) {
        // If user has not yet registered
        if (!userHandler.isRegistered(request.getFromId())) {
            sendQueue.addAll(createResponse(Response.ERR_NOTREGISTERED, request, requestParts, userHandler));
            return;
        }
        // Check parameters
        if (requestParts.size() < 2) {
            sendQueue.addAll(createResponse(Response.ERR_NEEDMOREPARAMS, request, requestParts, userHandler));
            return;
        }
        // Check if channel exists
        String channelName = requestParts.get(1);
        if (!userHandler.containsChannel(channelName)) {
            sendQueue.addAll(createResponse(Response.ERR_NOSUCHNICK, request, requestParts, userHandler));
            return;
        }
        // Check if requester is member of channel
        if (!userHandler.isOnChannel(request.getFromId(), channelName)) {
            sendQueue.addAll(createResponse(Response.ERR_NOTONCHANNEL, request, requestParts, userHandler));
            return;
        }
        // If only channelName is given, return topic
        if (requestParts.size() < 3) {
            String channelTopic = userHandler.getChannelTopic(channelName);
            // If no topic is set for channel
            if (channelTopic == null || channelTopic.isEmpty()) {
                sendQueue.addAll(createResponse(Response.RPL_NOTOPIC, request, requestParts, userHandler));
                return;
            }

            sendQueue.addAll(createResponse(Response.RPL_TOPIC, request, requestParts, userHandler));
            return;
        }

        // If channelName and newTopic is given
        // Check if requester is moderator or admin (channel operator)
        if (!userHandler.isChannelOperator(request.getFromId(), channelName)) {
            sendQueue.addAll(createResponse(Response.ERR_CHANOPRIVSNEEDED, request, requestParts, userHandler));
            return;
        }

        String newTopic = requestParts.get(2);
        userHandler.setChannelTopic(channelName, newTopic);
        sendToChannel(channelName, request);
    }

    private void handleKickCommand(IRCMessage request, List<String> requestParts) {
        // If user has not yet registered
        if (!userHandler.isRegistered(request.getFromId())) {
            sendQueue.addAll(createResponse(Response.ERR_NOTREGISTERED, request, requestParts, userHandler));
            return;
        }
        // Check parameters
        if (requestParts.size() < 3) {
            sendQueue.addAll(createResponse(Response.ERR_NEEDMOREPARAMS, request, requestParts, userHandler));
            return;
        }
        // Check if channel exists
        String channelName = requestParts.get(1);
        if (!userHandler.containsChannel(channelName)) {
            sendQueue.addAll(createResponse(Response.ERR_NOSUCHNICK, request, requestParts, userHandler));
            return;
        }
        // Check if requester is member of channel
        if (!userHandler.isOnChannel(request.getFromId(), channelName)) {
            sendQueue.addAll(createResponse(Response.ERR_NOTONCHANNEL, request, requestParts, userHandler));
            return;
        }
        // Check if requester is moderator or admin (channel operator)
        if (!userHandler.isChannelOperator(request.getFromId(), channelName)) {
            sendQueue.addAll(createResponse(Response.ERR_CHANOPRIVSNEEDED, request, requestParts, userHandler));
            return;
        }
        // Check if user being kicked is on channel
        User kicked = userHandler.getUser(requestParts.get(2));
        if (kicked == null || !userHandler.isOnChannel(kicked.getId(), channelName)) {
            sendQueue.addAll(createResponse(Response.ERR_USERNOTINCHANNEL, request, requestParts, userHandler));
            return;
        }
        // Send kick message
        sendToChannel(channelName, request);
        // Remove user from channel
        userHandler.removeUserFromChannel(kicked.getId(), channelName);

    }

    private void handleListCommand(IRCMessage request, List<String> requestParts) {
        sendQueue.addAll(createResponse(Response.RPL_LIST, request, requestParts, userHandler));
        sendQueue.addAll(createResponse(Response.RPL_LISTEND, request, requestParts, userHandler));
    }

    private void handleWhoCommand(IRCMessage request,  List<String> requestParts) {
        sendQueue.addAll(createResponse(Response.RPL_WHOREPLY, request, requestParts, userHandler));
        sendQueue.addAll(createResponse(Response.RPL_ENDOFWHO, request, requestParts, userHandler));
    }

    private void handleNamesCommmand(IRCMessage request, List<String> requestParts) {
        if (requestParts.size() == 1) {
            Set<String> joinedChannels = userHandler.getJoinedChannelNames(request.getFromId());
            if (joinedChannels.size() == 0) {
                sendQueue.addAll(createResponse(Response.RPL_ENDOFNAMES, request, requestParts, userHandler));
                return;
            }
            for (String channelName :  joinedChannels){
                requestQueue.add(new IRCMessage("NAMES " + channelName, request.getFromId(), request.getToId()));
            }
            return;
        }
        //Send channel name list
        sendQueue.addAll(createResponse(Response.RPL_NAMEREPLY, request, requestParts, userHandler));
        sendQueue.addAll(createResponse(Response.RPL_ENDOFNAMES, request, requestParts, userHandler));
        return;
    }

    private void handleModeCommand(IRCMessage request, List<String> requestParts) {
        // If user has not yet registered
        if (!userHandler.isRegistered(request.getFromId())) {
            sendQueue.addAll(createResponse(Response.ERR_NOTREGISTERED, request, requestParts, userHandler));
            return;
        }
        // Check parameters
        if (requestParts.size() < 2) {
            sendQueue.addAll(createResponse(Response.ERR_NEEDMOREPARAMS, request, requestParts, userHandler));
            return;
        }
        // Check if channel exists
        String channelName = requestParts.get(1);
        if (!userHandler.containsChannel(channelName)) {
            sendQueue.addAll(createResponse(Response.ERR_NOSUCHNICK, request, requestParts, userHandler));
            return;
        }
        // If only MODE and channel, send channel mode back
        if (requestParts.size() < 3) {
            // todo
            sendQueue.addAll(createResponse(Response.RPL_CHANNELMODEIS, request, requestParts, userHandler));
            return;
        }
        // If new channel mode is given
        // Check if user is operator // todo
        if (!userHandler.isChannelOperator(request.getFromId(), channelName)) {
            sendQueue.addAll(createResponse(Response.ERR_CHANOPRIVSNEEDED, request, requestParts, userHandler));
            return;
        }
        // todo
        String modeString = requestParts.get(2);
        boolean enable = (modeString.charAt(0) == '+');
        char flag = modeString.charAt(1);
        String parameter = "";
        if (flag == 'o' || flag == 'k' || flag == 'l') {
            if (requestParts.size() < 4) { // Check parameters
                sendQueue.addAll(createResponse(Response.ERR_NEEDMOREPARAMS, request, requestParts, userHandler));
                return;
            } else {
                parameter = requestParts.get(3);
            }
        }

        // If affected user is not a member of the channel
        if (flag == 'o') {
            User affectedUser = userHandler.getUser(parameter); // todo check if nick is valid
            if (affectedUser == null || !userHandler.isOnChannel(affectedUser.getId(), channelName)) {
                sendQueue.addAll(createResponse(Response.ERR_USERNOTINCHANNEL, request, requestParts, userHandler));
                return;
            }
        }

        if (IRCConstants.ChannelMode.getMode(flag) == null) {
            sendQueue.addAll(createResponse(Response.ERR_UNKNOWNMODE, request, requestParts, userHandler));
            return;
        }
        userHandler.setChannelMode(channelName, flag, parameter, enable);
        sendToChannel(channelName, request);
    }




    private void handleFileCommand(IRCMessage request, List<String> requestParts) {
        // If user has not yet registered
        if (!userHandler.isRegistered(request.getFromId())) {
            sendQueue.addAll(createResponse(Response.ERR_NOTREGISTERED, request, requestParts, userHandler));
            return;
        }
        // Check parameters
        if (requestParts.size() < 4) {
            sendQueue.addAll(createResponse(Response.ERR_NEEDMOREPARAMS, request, requestParts, userHandler));
            return;
        }

        // If nick user asks for does not exist
        String nick = requestParts.get(1);
        User recipient = userHandler.getUser(nick);
        if (recipient == null) {
            sendQueue.addAll(createResponse(Response.ERR_NOSUCHNICK, request, requestParts, userHandler));
            return;
        }

        sendQueue.add(createRelayMessage(request, userHandler, recipient.getId()));

    }

    private void handleFileResumeCommand(IRCMessage request, List<String> requestParts) {
        // If user has not yet registered
        if (!userHandler.isRegistered(request.getFromId())) {
            sendQueue.addAll(createResponse(Response.ERR_NOTREGISTERED, request, requestParts, userHandler));
            return;
        }
        // Check parameters
        if (requestParts.size() < 5) {
            sendQueue.addAll(createResponse(Response.ERR_NEEDMOREPARAMS, request, requestParts, userHandler));
            return;
        }

        // If nick user asks for does not exist
        String nick = requestParts.get(1);
        User recipient = userHandler.getUser(nick);
        if (recipient == null) {
            sendQueue.addAll(createResponse(Response.ERR_NOSUCHNICK, request, requestParts, userHandler));
            return;
        }

        sendQueue.add(createRelayMessage(request, userHandler, recipient.getId()));
    }




    private void sendToChannel(String channelName, IRCMessage request) {
        Set<User> members = userHandler.getChannelMembers(channelName).keySet();
        for (User member : members) {
            sendQueue.add(createRelayMessage(request, userHandler, member.getId()));
        }
    }


    private void writeResponses() throws IOException {
        pullAllRequests();

        cancelEmptySockets();

        registerNonEmptySockets();

        int writeReady = writeSelector.selectNow();

        if (writeReady > 0) {
            Set<SelectionKey> selectedKeys = writeSelector.selectedKeys();
            Iterator<SelectionKey> keyIterator = selectedKeys.iterator();

            while (keyIterator.hasNext()) {
                SelectionKey key = keyIterator.next();

                IRCSocket ircSocket = (IRCSocket) key.attachment();

                ircSocket.sendMessages();

                if (ircSocket.isWriterEmpty()) {
                    this.nonEmptyToEmptySockets.add(ircSocket);
                }

                keyIterator.remove();
            }

            selectedKeys.clear();
        }
    }

    private void registerNonEmptySockets() throws ClosedChannelException {
        for (IRCSocket ircSocket : emptyToNonEmptySockets) {
            SelectionKey key = ircSocket.register(this.writeSelector, SelectionKey.OP_WRITE);
            key.attach(ircSocket);
        }
        emptyToNonEmptySockets.clear();
    }

    private void cancelEmptySockets() {
        for (IRCSocket ircSocket : nonEmptyToEmptySockets) {
            SelectionKey key = ircSocket.getSelectionKey(this.writeSelector);
            key.cancel();
        }
        nonEmptyToEmptySockets.clear();
    }

    private void pullAllRequests() {
        while (true) {
            IRCMessage outMessage = sendQueue.poll();
            if (outMessage == null) {
                return;
            }
            IRCSocket ircSocket = socketMap.get(outMessage.getToId());

            if (ircSocket != null) {
                if (ircSocket.isWriterEmpty()) {
                    nonEmptyToEmptySockets.remove(ircSocket);
                    emptyToNonEmptySockets.add(ircSocket);
                }
                ircSocket.enqueue(outMessage.getMessage());

            }
        }

    }


}
