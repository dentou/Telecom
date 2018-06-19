package com.github.dentou.chat;

import jdk.nashorn.internal.ir.annotations.Immutable;

@Immutable
public class IRCMessage {
    private final String message;
    private final long fromId;
    private final long toId;

    public IRCMessage(String message, long fromId, long toId) {
        this.message = message;
        this.fromId = fromId;
        this.toId = toId;

    }

    public String getMessage() {
        return message;
    }

    public long getFromId() {
        return fromId;
    }

    public long getToId() {
        return toId;
    }


    @Override
    public String toString() {
        return "IRCMessage{" +
                "message='" + message + '\'' +
                ", fromId=" + fromId +
                ", toId=" + toId +
                '}';
    }
}
