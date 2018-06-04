package com.github.dentou.model;

public class PrivateMessage {
    private final String sender;
    private final String receiver;
    private final String content;

    public PrivateMessage(String sender, String receiver, String content) {
        this.sender = sender;
        this.receiver = receiver;
        this.content = content;
    }

    public String getSender() {
        return sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public String getContent() {
        return content;
    }
}

