package com.github.dentou.view;

import com.github.dentou.model.chat.PrivateMessage;
import com.github.dentou.model.file.FileMetadata;
import com.github.dentou.utils.FXUtils;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;

import java.io.File;
import java.nio.file.Paths;


public class ChatDialogController extends Controller<PrivateMessage>{
    @FXML
    protected ScrollPane scrollPane;
    @FXML
    protected VBox vBox;
    @FXML
    protected TextField chatBox;
    @FXML
    protected FontAwesomeIconView sendButton;
    @FXML
    protected FontAwesomeIconView fileButton;

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

    @Override
    protected void onWindowClosed(MouseEvent event) {
        close();
    }

    @Override
    public void update() {
        Stage stage = (Stage) scrollPane.getScene().getWindow();
        stage.toFront();
        super.update();
    }


    public synchronized void setChatter(String chatter) {
        this.chatter = chatter;
    }

    public void close() {
        Stage stage = (Stage) chatBox.getScene().getWindow();
        stage.fireEvent(new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST));
    }

    @FXML
    @Override
    protected void initialize() {
        super.initialize();
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
        chatBox.requestFocus();
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
        if(chatBox.getText().trim().isEmpty()) {
            sendButton.setDisable(true);
            return;
        }

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
    protected void onFile(MouseEvent mouseEvent) {
        Node source = (Node) mouseEvent.getSource();
        Window chatStage = source.getScene().getWindow();

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Open File for transfer");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Text Files", "*.txt"),
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.gif"),
                new FileChooser.ExtensionFilter("Audio Files", "*.wav", "*.mp3", "*.aac"),
                new FileChooser.ExtensionFilter("All Files", "*.*"));
        fileChooser.setInitialDirectory(new File("."));

        File selectedFile = fileChooser.showOpenDialog(chatStage);
        if (selectedFile == null) {
            return;
        }

        String fileName = selectedFile.getName();
        long fileSize = selectedFile.length();

        System.out.println("File selected for send: " + selectedFile.getAbsolutePath() + ", size in bytes: " + fileSize);
        FileMetadata fileMetadata = new FileMetadata(Paths.get(selectedFile.getAbsolutePath()), fileSize, 0l,
                getMainApp().getUser().getNick(), chatter);

        MainWindowController mainWindowController = (MainWindowController) getMainApp().getController();
        mainWindowController.addFileSend(fileMetadata);

        getMainApp().getIrcClient().sendToServer("FILE_SEND", " ",
                chatter, " ",
                "" + fileSize, " ",
                ":", fileName);


    }



    @FXML
    protected void onKeyReleased() {
        boolean disableSendButton = chatBox.getText().trim().isEmpty();
        sendButton.setDisable(disableSendButton);
    }

    public void disableAll() {
        FXUtils.setDisabled(true, sendButton, fileButton, chatBox);
    }

    public void enableAll() {
        if (blocked) {
            return;
        }
        FXUtils.setDisabled(false, sendButton, fileButton, chatBox);
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

