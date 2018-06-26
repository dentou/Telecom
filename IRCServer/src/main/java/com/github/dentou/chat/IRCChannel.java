package com.github.dentou.chat;

import java.util.*;
import com.github.dentou.utils.ServerConstants.ChannelMode;

public class IRCChannel {
    private String name;
    private String topic;
    private long memberCount = 1;

    private User admin;
    private List<User> moderatorList = new ArrayList<>();
    private List<User> userList = new ArrayList<User>();


    private EnumSet<ChannelMode> channelModes = EnumSet.noneOf(ChannelMode.class);
    private String key = "";
    private long memberLimit = 100;


    public IRCChannel(String name, User admin) {
        this(name, admin, "");
    }

    public IRCChannel(String name, User admin, String topic) {
        this.name = name;
        this.admin = admin;
        this.topic = topic;
    }

    public boolean isAdmin(User user) {
        if (user == null) {
            return false;
        }
        return user.equals(admin);
    }

    public boolean isModerator(User user) {
        if (user == null) {
            return false;
        }
        return moderatorList.contains(user);
    }

    public boolean isOperator(User user) {
        return isAdmin(user) || isModerator(user);
    }

    public boolean isUser(User user) {
        if (user == null) {
            return false;
        }
        return userList.contains(user);
    }

    public boolean isMember(User user) {
        return isUser(user) || isModerator(user) || isAdmin(user);
    }



    public boolean isEmpty() {
        return userList.isEmpty() && moderatorList.isEmpty() && admin == null;
    }

    public boolean isFull() {
        return memberCount >= memberLimit;
    }

    public String getName() {
        return name;
    }

    public String getTopic() {
        return topic;
    }

    public long getNumberOfMembers() {
        return this.memberCount;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public boolean addUser(User user) {
        if (memberCount >= memberLimit) {
            return false;
        }
        if (userList.contains(user)) {
            return false;
        }
        userList.add(user);
        this.memberCount++;
        return true;
    }

    public void removeUser(User user) {
        if (user != null) {
            userList.remove(user);
            moderatorList.remove(user);
            this.memberCount--;
            if (admin != null && admin.equals(user)) {
                if (!moderatorList.isEmpty()) {
                    admin = moderatorList.get(0);
                } else if (!userList.isEmpty()){
                    admin = userList.get(0);
                } else {
                    admin = null;
                }
            }

        }
    }

    public List<User> getAllUsers() {
        return new ArrayList<>(userList);
    }

    public List<User> getAllModerators() {
        return new ArrayList<>(moderatorList);
    }

    public User getAdmin() {
        return this.admin;
    }

    public List<User> getAllMembers() {
        List<User> members = new ArrayList<>();
        members.addAll(userList);
        members.addAll(moderatorList);
        if (admin != null) {
            members.add(admin);
        }
        return members;
    }

    public String getKey() {
        return this.key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void promote(User user) {
        if (user == null) {
            return;
        }
        if (!userList.contains(user)) {
            return;
        }
        userList.remove(user);
        moderatorList.add(user);
    }

    public void demote(User user) {
        if (user == null) {
            return;
        }
        if (!moderatorList.contains(user)) {
            return;
        }
        moderatorList.remove(user);
        userList.add(user);
    }

    public boolean hasMode(ChannelMode mode) {
        return this.channelModes.contains(mode);
    }

    public void setMode(ChannelMode mode, boolean enable) {
        if (mode == null) {
            return;
        }
        if (enable) {
            this.channelModes.add(mode);
        } else {
            this.channelModes.remove(mode);
        }
    }

    public String getModes() {
        StringBuilder sb = new StringBuilder();
        sb.append("+");
        for (ChannelMode mode : channelModes) {
            sb.append(mode.getFlag());
        }
        return sb.toString();
    }

    public long getMemberLimit() {
        return memberLimit;
    }

    public void setMemberLimit(long memberLimit) {
        this.memberLimit = memberLimit;
    }
}
