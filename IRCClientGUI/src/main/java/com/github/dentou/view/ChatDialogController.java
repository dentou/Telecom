package com.github.dentou.view;

import com.github.dentou.model.PrivateMessage;
import com.github.dentou.utils.FXUtils;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import sun.applet.Main;


public class ChatDialogController extends Controller<PrivateMessage>{
    @FXML
    protected ScrollPane scrollPane;
    @FXML
    protected VBox vBox;
    @FXML
    protected TextArea chatBox;
    @FXML
    protected Button sendButton;

    private String chatter;

    private boolean blocked = false;

    public synchronized String getChatter() {
        return chatter;
    }

    public synchronized boolean isBlocked() {
        return blocked;
    }

    public synchronized void setBlocked(boolean blocked) {
        this.blocked = blocked;
        if (blocked) {
            disableAll();
        } else {
            enableAll();
        }
    }

    public synchronized void setChatter(String chatter) {
        this.chatter = chatter;
    }

    public void close() {
        Stage stage = (Stage) chatBox.getScene().getWindow();
        stage.close();
    }

    @FXML
    @Override
    protected void initialize() {
        //chatBox.setWrapText(true);
        chatBox.addEventFilter(KeyEvent.KEY_PRESSED, new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                if (event.getCode().equals(KeyCode.ENTER)) {
                    if (!chatBox.getText().trim().isEmpty()) {
                        onSend();
                        chatBox.clear();
                    }
                    event.consume();
                }
            }
        });
        scrollPane.vvalueProperty().bind(vBox.heightProperty());
        sendButton.setDisable(true);
    }


    @Override
    public void processMessage(PrivateMessage privateMessage) {
        if (privateMessage == null) {
            return;
        }
        String sender = privateMessage.getSender();
        String content = privateMessage.getContent();
        this.displayMessage(sender, content);
    }

    @FXML
    protected void onSend() {
        // todo send PRIVMSG to server
        getMainApp().getIrcClient().sendToServer("PRIVMSG", " ", chatter, " ", ":", chatBox.getText());
        if (chatter.charAt(0) != '#') { // Privmsg to another user is not relayed back to sender by server
            MainWindowController mainWindowController = (MainWindowController) getMainApp().getController();
            mainWindowController.updateChatHistory(chatter,
                    new PrivateMessage(getMainApp().getUser().getNick(), chatter, chatBox.getText()));
            this.displayMessage(getMainApp().getUser().getNick(), chatBox.getText());
        }
        chatBox.clear();
        sendButton.setDisable(true);
    }



    @FXML
    protected void onKeyReleased() {
        boolean disableSendButton = chatBox.getText().trim().isEmpty();
        sendButton.setDisable(disableSendButton);
    }

    public void disableAll() {
        FXUtils.setDisabled(true, sendButton, chatBox);
    }

    public void enableAll() {
        if (blocked) {
            return;
        }
        FXUtils.setDisabled(false, sendButton, chatBox);
    }

    protected void displayNotification(String message) {
        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(200);
        messageLabel.setMinHeight(Region.USE_PREF_SIZE);
        messageLabel.getStyleClass().add("notification-label");

        HBox hBox = new HBox();
        hBox.setAlignment(Pos.BASELINE_CENTER);

        hBox.getChildren().add(messageLabel);
        vBox.getChildren().add(hBox);
        vBox.getChildren().add(new Label());
    }

    protected void displayMessage(String sender, String content) {

        if (sender == "server") {
            displayNotification(content);
            return;
        }

        Label nickLabel = new Label(sender);
        nickLabel.getStyleClass().add("nick-label");

        Label messageLabel = new Label(content);
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(200);
        messageLabel.setMinHeight(Region.USE_PREF_SIZE);

        HBox hBox = new HBox();

        VBox smallVBox = new VBox();
        smallVBox.setSpacing(5);

        if (sender.equals(getMainApp().getUser().getNick())) {
            hBox.setAlignment(Pos.BASELINE_RIGHT);
            messageLabel.getStyleClass().add("send-message-label");
        } else {
            hBox.setAlignment(Pos.BASELINE_LEFT);
            smallVBox.getChildren().add(nickLabel);
            messageLabel.getStyleClass().add("receive-message-label");
        }
        smallVBox.getChildren().add(messageLabel);
        hBox.getChildren().add(smallVBox);
        vBox.getChildren().add(hBox);
        vBox.getChildren().add(new Label());
    }
}

