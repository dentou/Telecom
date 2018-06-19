package com.github.dentou.chat;

import jdk.nashorn.internal.ir.annotations.Immutable;

import java.util.Objects;

@Immutable
public class User {
    private final long id;
    private final String nick;
    private final String userName;
    private final String userFullName;

    public User(long id, String nick, String userName, String userFullName) {
        this.id = id;
        this.nick = nick;
        this.userName = userName;
        this.userFullName = userFullName;
    }

    public long getId() {
        return id;
    }

    public String getNick() {
        return nick;
    }

    public String getUserName() {
        return userName;
    }

    public String getUserFullName() {
        return userFullName;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return id == user.id;
    }

    @Override
    public int hashCode() {

        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", nick='" + nick + '\'' +
                ", userName='" + userName + '\'' +
                ", userFullName='" + userFullName + '\'' +
                '}';
    }
}
