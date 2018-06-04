package com.github.dentou;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class IRCChannel {
    private List<User> userList = new ArrayList<User>();
    private List<User> moderatorList = new ArrayList<>();
    private String name;
    private String topic;
    private long memberCount = 1;

    private User admin;

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

    public void addUser(User user) {
        if (!userList.contains(user)) {
            userList.add(user);
            this.memberCount++;
        }

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


}
