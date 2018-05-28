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
import sun.applet.Main;


public class ChatDialogController extends Controller<PrivateMessage>{
    @FXML
    ScrollPane scrollPane;
    @FXML
    VBox vBox;
    @FXML
    TextArea chatBox;
    @FXML
    Button sendButton;

    private String chatter;

    public synchronized String getChatter() {
        return chatter;
    }

    public synchronized void setChatter(String chatter) {
        this.chatter = chatter;
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
                        handleSend();
                        chatBox.clear();
                    }
                    event.consume();
                }
            }
        });
        scrollPane.vvalueProperty().bind(vBox.heightProperty());
        disableAll();
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
    private void handleSend() {
        // todo send PRIVMSG to server
        getMainApp().getIrcClient().sendToServer("PRIVMSG", " ", chatter, " ", ":", chatBox.getText());
        if (chatter.charAt(0) != '#') { // Privmsg to another user is not relayed back to sender by server
            MainWindowController mainWindowController = (MainWindowController) getMainApp().getController();
            mainWindowController.updateChatHistory(chatter,
                    new PrivateMessage(getMainApp().getUser().getNick(), chatter, chatBox.getText()));
            this.displayMessage(getMainApp().getUser().getNick(), chatBox.getText());
        }
        chatBox.clear();
        disableAll();
    }



    @FXML
    private void handleKeyReleased() {
        String text = chatBox.getText().trim();
        if (text.isEmpty()) {
            disableAll();
        } else {
            enableAll();
        }
    }

    public void disableAll() {
        FXUtils.setDisabled(true, sendButton);
    }

    public void enableAll() {
        FXUtils.setDisabled(false, sendButton);
    }

    private void displayMessage(String sender, String content) {
        Label nickLabel = new Label(sender);
        nickLabel.getStyleClass().add("nick-label");

        Label messageLabel = new Label(content);
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(vBox.getPrefWidth() * 0.6);
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
