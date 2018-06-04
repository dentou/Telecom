package com.github.dentou.view;

import com.github.dentou.model.Channel;
import com.github.dentou.model.PrivateMessage;
import com.github.dentou.utils.FXUtils;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.util.List;

public class ChannelDialogController extends ChatDialogController {

    @FXML
    private Button leaveButton;

    @FXML
    private ListView<String> channelListView;

    @FXML
    private Label blockedLabel;
    @FXML
    private Label channelNameLabel;
    @FXML
    private Label channelTopicLabel;

    private Channel channel;


    @Override
    public synchronized void setBlocked(boolean blocked) {
        super.setBlocked(blocked);
        blockedLabel.setText(blocked ? "You are not a member of this channel" : "");

    }

    @FXML
    @Override
    protected void initialize() {
        super.initialize();
        chatBox.requestFocus();
        blockedLabel.setText("");
    }

    @Override
    public void disableAll() {
        super.disableAll();
        FXUtils.setDisabled(true, leaveButton);
    }

    @Override
    public void enableAll() {
        super.enableAll();
        FXUtils.setDisabled(false, leaveButton);
    }

    public synchronized void setChannel(Channel channel, ObservableList<String> memberList) {
        this.channel = channel;
        this.channelListView.setItems(memberList);
        channelNameLabel.setText(channel.getName());
        channelTopicLabel.setText(channel.getTopic());
    }

    @FXML
    private void onLeave() {
        getMainApp().getIrcClient().sendToServer("PART", " ", channel.getName());
        disableAll();
        this.setBlocked(true);
    }

}
