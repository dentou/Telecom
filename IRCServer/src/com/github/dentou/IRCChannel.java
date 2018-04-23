package com.github.dentou;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class IRCChannel {
    private List<User> userList = new ArrayList<User>();
    private String name;
    private String topic;

    public IRCChannel(String name) {
        this(name, "");
    }

    public IRCChannel(String name, String topic) {
        this.name = name;
        this.topic = topic;
    }

    public String getName() {
        return name;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public void addUser(User user) {
        if (!userList.contains(user)) {
            userList.add(user);
        }

    }

    public void removeUser(User user) {
        if (user != null) {
            userList.remove(user);
        }

    }

    public List<Long> getAllIds() {
        List<Long> ids = new ArrayList<Long>();
        for (User user : userList) {
            ids.add(user.getId());
        }
        return ids;
    }

    public List<String> getAllNicks() {
        List<String> nicks = new ArrayList<>();
        for (User user : userList) {
            nicks.add(user.getNick());
        }
        return nicks;
    }

    public boolean containsUser(User user) {
        if (user == null) {
            return false;
        }
        return this.userList.contains(user);
    }
}
