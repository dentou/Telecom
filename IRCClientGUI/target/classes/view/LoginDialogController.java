package com.github.dentou.view;

import com.github.dentou.MainApp;
import com.github.dentou.utils.ClientUtils;
import com.github.dentou.model.IRCClient;
import com.github.dentou.model.IRCConstants.Response;
import com.github.dentou.model.User;
import com.github.dentou.utils.FXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.util.List;

import javafx.scene.control.Alert.AlertType;


public class LoginDialogController extends Controller<String>{
    @FXML
    private TextField nickField;
    @FXML
    private TextField userNameField;
    @FXML
    private TextField fullnameField;

    @FXML
    private Button loginButton;
    @FXML
    private Button disconnectButton;

    @FXML
    @Override
    protected void initialize() {
        loginButton.setDisable(true);
    }

    @Override
    public void processMessage(String message) {
        enableAll();
        List<String> messageParts = ClientUtils.parseMessage(message);
        String numericCode = messageParts.get(1);
        switch (Response.getResponse(numericCode)) {
            case RPL_WELCOME:
                getMainApp().setUser(new User(nickField.getText(), userNameField.getText(), fullnameField.getText()));
                getMainApp().showMainWindow();
                break;
            case ERR_NICKNAMEINUSE:
                getMainApp().showAlertDialog(AlertType.ERROR, "Error", "Nick error", "Nickname already exists. Please choose another.");
                break;
        }
    }

    @FXML
    private void handleLogin() throws IOException{ // todo send NICK and USER request to server
        if (!isInputValid()) {
            return;
        }

        String nick = nickField.getText().trim();
        String userName = userNameField.getText().trim();
        String fullName = fullnameField.getText().trim();

        MainApp mainApp = super.getMainApp();
        IRCClient ircClient = mainApp.getIrcClient();

        ircClient.sendToServer("NICK", " " , nick);
        ircClient.sendToServer("USER", " ", userName, " ", "* *", " ", ":", fullName);

        disableAll();
    }

    @FXML
    private void handleDisconnect() {
        getMainApp().getIrcClient().sendToServer("QUIT");
        getMainApp().getIrcClient().stop();
        getMainApp().showConnectionDialog();
        //Platform.exit();
    }

    @FXML
    private void handleKeyReleased() {
        String nick = nickField.getText().trim();
        String userName = userNameField.getText().trim();
        String fullName = fullnameField.getText().trim();


        boolean disableLoginButton = nick.isEmpty() || userName.isEmpty() || fullName.isEmpty();
        loginButton.setDisable(disableLoginButton);
    }


    private boolean isInputValid() {
        String errorMessage = "";

        if (errorMessage.length() == 0) {
            return true;
        } else {
            // Show the error message.
            super.getMainApp().showAlertDialog(AlertType.ERROR, "Invalid Fields", "Please correct invalid fields", errorMessage);

            return false;
        }
    }





    public void disableAll() {
        FXUtils.setDisabled(true, nickField, userNameField, fullnameField, loginButton, disconnectButton);
    }

    public void enableAll() {
        FXUtils.setDisabled(false, nickField, userNameField, fullnameField, loginButton, disconnectButton);
    }



}
