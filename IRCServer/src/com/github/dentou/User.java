package com.github.dentou;

import jdk.nashorn.internal.ir.annotations.Immutable;

import java.util.Objects;

public class User {
    private final long id;
    private String nick = null;
    private String userName = null;
    private String userFullName = null;

    public User(long id) {
        this.id = id;
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


    public void setNick(String nick) {
        this.nick = nick;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setUserFullName(String userFullName) {
        this.userFullName = userFullName;
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
}
