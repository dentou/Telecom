package com.github.dentou.model.chat;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.Objects;

public class User {
    private final StringProperty nick;
    private final StringProperty userName;
    private final StringProperty fullName;

    public User(String nick, String userName, String fullName) {
        this.nick = new SimpleStringProperty(nick);
        this.userName = new SimpleStringProperty(userName);
        this.fullName = new SimpleStringProperty(fullName);
    }

    public String getNick() {
        return nick.get();
    }

    public StringProperty nickProperty() {
        return nick;
    }

    public void setNick(String nick) {
        this.nick.set(nick);
    }

    public String getUserName() {
        return userName.get();
    }

    public StringProperty userNameProperty() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName.set(userName);
    }

    public String getFullName() {
        return fullName.get();
    }

    public StringProperty fullNameProperty() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName.set(fullName);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(nick, user.nick);
    }

    @Override
    public int hashCode() {

        return Objects.hash(nick);
    }
}
