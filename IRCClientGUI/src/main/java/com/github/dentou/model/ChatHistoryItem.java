package com.github.dentou.model;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ChatHistoryItem implements Comparable<ChatHistoryItem>{
    private final StringProperty chatter;
    private final StringProperty lastMessage;
    private final ObjectProperty<LocalDateTime> lastMessageTime;

    private List<PrivateMessage> messageList = new ArrayList<>();


    public ChatHistoryItem(String chatter, PrivateMessage message) {
        this.chatter = new SimpleStringProperty(chatter);
        this.lastMessage = new SimpleStringProperty(message.getSender() + ": " + message.getContent());
        this.lastMessageTime = new SimpleObjectProperty<LocalDateTime>(getCurrentTime());

        messageList.add(message);
    }

    public void addMessage(PrivateMessage message) {
        if (message != null) {
            messageList.add(message);
            setLastMessage(message.getContent());
            setLastMessageTime(getCurrentTime());
        }
    }

    private LocalDateTime getCurrentTime() {
        LocalDateTime jetzt = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return LocalDateTime.parse(jetzt.format(formatter), formatter);
    }


    private void setLastMessage(String lastMessage) {
        this.lastMessage.set(lastMessage);
    }

    private void setLastMessageTime(LocalDateTime lastMessageTime) {
        this.lastMessageTime.set(lastMessageTime);
    }

    public String getChatter() {
        return chatter.get();
    }

    public StringProperty chatterProperty() {
        return chatter;
    }

    public String getLastMessage() {
        return lastMessage.get();
    }

    public StringProperty lastMessageProperty() {
        return lastMessage;
    }

    public LocalDateTime getLastMessageTime() {
        return lastMessageTime.get();
    }

    public ObjectProperty<LocalDateTime> lastMessageTimeProperty() {
        return lastMessageTime;
    }

    public List<PrivateMessage> getMessageList() {
        return Collections.unmodifiableList(messageList);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChatHistoryItem that = (ChatHistoryItem) o;
        return Objects.equals(chatter, that.chatter);
    }

    @Override
    public int hashCode() {

        return Objects.hash(chatter);
    }

    @Override
    public int compareTo(ChatHistoryItem otherItem) {
        return this.getLastMessageTime().compareTo(otherItem.getLastMessageTime());
    }
}
