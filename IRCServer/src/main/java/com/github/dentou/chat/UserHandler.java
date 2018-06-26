package com.github.dentou.chat;

import java.util.*;
import com.github.dentou.utils.ServerConstants.ChannelMode;


public class UserHandler {
    private Set<User> userList = new HashSet<>();
    private Map<String, Long> nickToId = new HashMap<String, Long>();
    private Map<Long, User> idToUser = new HashMap<Long, User>();
    private Map<User, Set<IRCChannel>> userToChannels = new HashMap<>();

    private Set<IRCChannel> channels = new HashSet<>();
    private Map<String, IRCChannel> nameToChannel = new HashMap<>();


    public enum StatusCode {
        SUCCESS, NICK_DUPLICATE, ID_DUPLICATE, ID_NOT_EXIST, UNKNOWN_OPERATION,
        ;
    }

    public UserHandler() {

    }

    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>(this.userList);
        return users;
    }

    public User getUser(long id) {
        return this.idToUser.get(id);
    }

    public User getUser(String nick) {
        return this.idToUser.get(nickToId.get(nick));
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

    public boolean isFull(String channelName) {
        IRCChannel channel = nameToChannel.get(channelName);
        if (channel == null) {
           throw new IllegalArgumentException("Channel does not exist");
        }
        return channel.isFull();
    }


    public StatusCode addUser(long id) {
        if (containsId(id)) {
            return StatusCode.ID_DUPLICATE;
        }
        User user = new User(id, null, null, null);
        userList.add(user);
        idToUser.put(id, user);
        userToChannels.put(user, new HashSet<IRCChannel>());
        return StatusCode.SUCCESS;
    }

    public StatusCode removeUser(long id) {
        User user = idToUser.get(id);
        if (user == null) {
            return StatusCode.ID_NOT_EXIST;
        }
        this.userList.remove(user);
        this.idToUser.remove(id);
        if (user.getNick() != null) {
            nickToId.remove(user.getNick());
        }

        removeUserFromAllChannels(id);
        this.userToChannels.remove(user);
        return StatusCode.SUCCESS;
    }

    public StatusCode changeUserInfo(long id, String parameter, String newValue) {
        User user = idToUser.get(id);

        if (user == null) {
            return StatusCode.ID_NOT_EXIST;
        }

        Set<IRCChannel> joinedChannels = this.userToChannels.get(user);

        this.userList.remove(user);
        this.idToUser.remove(user.getId());
        this.userToChannels.remove(user);

        switch (parameter) {
            case "nick":
                if (containsNick(newValue)) {
                    return StatusCode.NICK_DUPLICATE;
                }
                nickToId.remove(user.getNick());
                user = new User(user.getId(), newValue, user.getUserName(), user.getUserFullName());
                nickToId.put(user.getNick(), user.getId());
                break;
            case "userName":
                user = new User(user.getId(), user.getNick(), newValue, user.getUserFullName());
                break;
            case "userFullName":
                user = new User(user.getId(), user.getNick(), user.getUserName(), newValue);
                break;
            default:
                return StatusCode.UNKNOWN_OPERATION;

        }
        userList.add(user);
        idToUser.put(user.getId(), user);
        userToChannels.put(user, joinedChannels);
        return StatusCode.SUCCESS;
    }

    public void createChannel(String name, long adminId) {
        createChannel(name, adminId, "");
    }

    public IRCChannel createChannel(String name, long adminId, String topic) {
        User admin = idToUser.get(adminId);
        IRCChannel channel = new IRCChannel(name, admin, topic);
        this.channels.add(channel);
        this.nameToChannel.put(name, channel);
        this.userToChannels.get(admin).add(channel);
        return channel;
    }

    public String getChannelTopic(String channelName) {
        IRCChannel channel = nameToChannel.get(channelName);
        if (channel == null) {
            return null;
        }
        return channel.getTopic();
    }

    public void setChannelTopic(String channelName, String newTopic) {
        IRCChannel channel = nameToChannel.get(channelName);
        if (channel == null) {
            return;
        }
        channel.setTopic(newTopic);
    }

    public boolean containsChannel(String name) {
        return this.nameToChannel.containsKey(name);
    }

    public boolean isOnChannel(long id, String channelName) {
        IRCChannel channel = nameToChannel.get(channelName);
        if (channel == null) {
            return false;
        }
        return channel.isMember(idToUser.get(id));
    }

    public boolean isChannelOperator(long id, String channelName) {
        User user = idToUser.get(id);
        IRCChannel channel = nameToChannel.get(channelName);
        if (channel == null) {
            return false;
        }
        return channel.isOperator(user);
    }

    public Set<String> getJoinedChannelNames(long id) {
        Set<String> channelNames = new HashSet<>();
        Set<IRCChannel> joinedChannels = this.userToChannels.get(idToUser.get(id));
        for (IRCChannel channel : joinedChannels) {
            channelNames.add(channel.getName());
        }
        return channelNames;
    }

    public void addUserToChannel(long id, String channelName) {
        IRCChannel channel = nameToChannel.get(channelName);
        User user = idToUser.get(id);
        if (channel == null || user == null) {
            return;
        }
        channel.addUser(idToUser.get(id));
        userToChannels.get(user).add(channel);
    }

    public void removeUserFromChannel(long id, String channelName) {
        IRCChannel channel = nameToChannel.get(channelName);
        User user = idToUser.get(id);
        if (channel == null || user == null) {
            return;
        }
        channel.removeUser(idToUser.get(id));
        userToChannels.get(user).remove(channel);
        if (channel.isEmpty()) {
            channels.remove(channel);
            nameToChannel.remove(channelName);
        }

    }

    public void removeUserFromAllChannels(long id) {
        User user = idToUser.get(id);
        Set<IRCChannel> joinedChannels = userToChannels.get(user);
        if (user == null || joinedChannels == null || joinedChannels.isEmpty()) {
            return;
        }
        for (IRCChannel channel : joinedChannels) {
            removeUserFromChannel(id, channel.getName());
        }
    }

    public Map<String, String> getAllChannelNamesAndTopics() {
        Map<String, String> namesAndTopics = new HashMap<>();
        for (IRCChannel channel : this.channels) {
            namesAndTopics.put(channel.getName(), channel.getTopic());
        }
        return namesAndTopics;
    }

    public long getChannelNumberOfMembers(String channelName) {
        IRCChannel channel = nameToChannel.get(channelName);
        if (channel == null) {
            return 0;
        }
        return channel.getNumberOfMembers();
    }

    public Map<User, String> getChannelMembers(String channelName) {
        IRCChannel channel = nameToChannel.get(channelName);
        Map<User, String> users = new HashMap<>();
        if (channel == null) {
            return users;
        }
        for (User user : channel.getAllMembers()) {
            if (channel.isAdmin(user)) {
                users.put(user, "@");
            } else if (channel.isModerator(user)) {
                users.put(user, "+");
            } else {
                users.put(user, "");
            }
        }
        return users;
    }

    public String getChannelModes(String channelName) {
        IRCChannel channel = nameToChannel.get(channelName);
        if (channel == null) {
            throw new IllegalArgumentException("Channel not exist");
        }
        return channel.getModes();
    }

    public void setChannelMode(String channelName, char flag, String parameter, boolean enable) {
        IRCChannel channel = nameToChannel.get(channelName);
        if (channel == null) {
            throw new IllegalArgumentException("Channel does not exist");
        }
        switch (flag) {
            case 'i':
                channel.setMode(ChannelMode.INVITE_ONLY, enable);
                break;
            case 'l':
                channel.setMode(ChannelMode.LIMITED, enable);
                channel.setMemberLimit(Long.parseLong(parameter));
                break;
            case 'k':
                channel.setMode(ChannelMode.KEY, enable);
                if (enable) {
                    channel.setKey(parameter);
                } else {
                    channel.setKey("");
                }
                break;
            case 'o':
                User user = idToUser.get(nickToId.get(parameter));
                if (user == null) {
                    throw new IllegalArgumentException("User is not on channel to set +-o mode");
                }
                if (enable) {
                    channel.promote(user);
                } else {
                    channel.demote(user);
                }

                break;

        }
    }
     public String getChannelKey(String channelName) {
         IRCChannel channel = nameToChannel.get(channelName);
         if (channel == null) {
             return "";
         }
         return channel.getKey();

     }

}
