package com.github.dentou.model;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.util.Objects;

public class Channel {
    private final StringProperty name;
    private final IntegerProperty numberOfMembers;
    private final StringProperty topic;

    public Channel(String name, int numberOfMembers, String topic) {
        this.name = new SimpleStringProperty(name);
        this.numberOfMembers = new SimpleIntegerProperty(numberOfMembers);
        this.topic = new SimpleStringProperty(topic);
    }

    public String getName() {
        return name.get();
    }

    public StringProperty nameProperty() {
        return name;
    }

    public void setName(String name) {
        this.name.set(name);
    }

    public int getNumberOfMembers() {
        return numberOfMembers.get();
    }

    public IntegerProperty numberOfMembersProperty() {
        return numberOfMembers;
    }

    public void setNumberOfMembers(int numberOfMembers) {
        this.numberOfMembers.set(numberOfMembers);
    }

    public String getTopic() {
        return topic.get();
    }

    public StringProperty topicProperty() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic.set(topic);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Channel channel = (Channel) o;
        return Objects.equals(name, channel.name);
    }

    @Override
    public int hashCode() {

        return Objects.hash(name);
    }
}
