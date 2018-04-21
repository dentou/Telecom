package com.github.dentou;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserHandler {
    private Map<String, Long> nickToId;
    private Map<Long, User> idToUser;
    private List<User> users;
    private List<IRCChannel> channels;

    public enum StatusCode {
        SUCCESS, NEW_USER, NICK_DUPLICATE, ID_DUPLICATE, USER_NOT_EXIST, UNKNOWN_OPERATION,
        INVALID_CHANNEL_NAME;
    }

    public UserHandler() {
        this.nickToId = new HashMap<String, Long>();
        this.idToUser = new HashMap<Long, User>();
        this.users = new ArrayList<User>();
        this.channels = new ArrayList<IRCChannel>();
    }

    public long getUserId(String nick) {
        return nickToId.get(nick);
    }

    public List<Long> getUserId(List<String> nicks) {
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

    public boolean containsNick(String nick) {
        return nickToId.containsKey(nick);
    }

    public boolean containsId(long id) {
        return idToUser.containsKey(id);
    }

    public StatusCode addUser(long id, String nick) {
        if (containsNick(nick)) {
            return StatusCode.NICK_DUPLICATE;
        } else if (containsId(id)) {
            return StatusCode.ID_DUPLICATE;
        }
        User user = new User(id, nick);
        this.users.add(user);
        nickToId.put(nick, id);
        idToUser.put(id, user);
        return StatusCode.SUCCESS;
    }

    public StatusCode changeUserInfo(long id, String parameter, String newValue) {
        User user = idToUser.get(id);

        if (user == null) {
            return StatusCode.USER_NOT_EXIST;
        }

        switch (parameter) {
            case "userName":
                if (user.getUserName() == null) {
                    user.setUserName(newValue);
                    return StatusCode.NEW_USER;
                } else {
                    user.setUserName(newValue);
                }
                break;
            case "userFullName":
                user.setUserFullName(newValue);
                break;
            default:
                return StatusCode.UNKNOWN_OPERATION;

        }
        return StatusCode.SUCCESS;
    }


}
