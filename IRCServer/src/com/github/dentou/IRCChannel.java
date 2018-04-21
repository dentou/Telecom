package com.github.dentou;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class IRCChannel {
    private List<User> userList;
    private String name;
    private String topic;

    public IRCChannel(String name) {
        this.name = name;
        this.userList = new ArrayList<User>();
    }

    public void addUser(User user) {
        if (!userList.contains(user)) {
            userList.add(user);
        }

    }

    public void removeUser(User user) {
        userList.remove(user);
    }

    public List<Long> getAllId() {
        List<Long> ids = new ArrayList<Long>();
        for (User user : userList) {
            ids.add(user.getId());
        }
        return ids;
    }
}
