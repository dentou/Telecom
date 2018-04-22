package com.github.dentou;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserHandler {
    private Map<String, Long> nickToId;
    private Map<Long, User> idToUser;
    private Map<String, IRCChannel> nameToChannel = new HashMap<>();
    private List<User> users;
    private List<IRCChannel> channels;

    public enum StatusCode {
        SUCCESS, NICK_DUPLICATE, ID_DUPLICATE, ID_NOT_EXIST, UNKNOWN_OPERATION,
        INVALID_CHANNEL_NAME;
    }

    public UserHandler() {
        this.nickToId = new HashMap<String, Long>();
        this.idToUser = new HashMap<Long, User>();
        this.users = new ArrayList<User>();
        this.channels = new ArrayList<IRCChannel>();
    }

    public long getUserId(String nick) {
        Long longId = nickToId.get(nick);
        if (longId == null) {
            return -1;
        }
        return longId.longValue();
    }

    public List<Long> getUserIds(List<String> nicks) {
        List<Long> ids = new ArrayList<Long>();
        for (String nick : nicks) {
            ids.add(getUserId(nick));
        }
        return ids;
    }

    public String getUserNick(long id) {
        return idToUser.get(id).getNick();
    }
    public String getUserName(long id) { return idToUser.get(id).getUserName(); }
    public String getUserFullName(long id) {
        return idToUser.get(id).getUserFullName();
    }

    public boolean containsNick(String nick) {
        return nickToId.containsKey(nick);
    }

    public boolean containsId(long id) {
        return idToUser.containsKey(id);
    }

    public boolean isRegistered(long id) {
        User user = idToUser.get(id);
        if (user == null) {
            return false;
        }
        if (user.getNick() == null || user.getUserName() == null) {
            return false;
        }
        return true;
    }

    public StatusCode addUser(long id) {
        if (containsId(id)) {
            return StatusCode.ID_DUPLICATE;
        }
        User user = new User(id);
        this.users.add(user);
        idToUser.put(id, user);
        return StatusCode.SUCCESS;
    }

    public StatusCode removeUser(long id) {
        User user = idToUser.get(id);
        if (user == null) {
            return StatusCode.ID_NOT_EXIST;
        }
        users.remove(user);
        idToUser.remove(id);
        if (user.getNick() != null) {
            nickToId.remove(user.getNick());
        }
        return StatusCode.SUCCESS;
    }

    public StatusCode changeUserInfo(long id, String parameter, String newValue) {
        User user = idToUser.get(id);

        if (user == null) {
            return StatusCode.ID_NOT_EXIST;
        }

        switch (parameter) {
            case "nick":
                if (containsNick(newValue)) {
                    return StatusCode.NICK_DUPLICATE;
                }
                user.setNick(newValue);
                break;
            case "userName":
                user.setUserName(newValue);
                break;
            case "userFullName":
                user.setUserFullName(newValue);
                break;
            default:
                return StatusCode.UNKNOWN_OPERATION;

        }
        return StatusCode.SUCCESS;
    }

    public void createChannel(String name) {
        createChannel(name, "");
    }

    public void createChannel(String name, String topic) {
        IRCChannel channel = new IRCChannel(name, topic);
        this.channels.add(channel);
        this.nameToChannel.put(name, channel);
    }

    public boolean containsChannel(String name) {
        return this.nameToChannel.containsKey(name);
    }

    public IRCChannel getChannel(String name) {
        return this.nameToChannel.get(name);
    }

    public void addUserToChannel(long id, String channelName) {
        IRCChannel channel = nameToChannel.get(channelName);
        if (channel == null) {
            return;
        }
        channel.addUser(idToUser.get(id));

    }

    public List<String> getChannelMemberNicks(String channelName) {
        IRCChannel channel = nameToChannel.get(channelName);
        if (channel == null) {
            return null;
        }
        return channel.getAllNicks();
    }


}
