package com.github.dentou.controller;

import com.github.dentou.MainApp;
import com.github.dentou.utils.ClientUtils;
import com.github.dentou.model.chat.IRCClient;
import com.github.dentou.utils.ClientConstants.Response;
import com.github.dentou.model.chat.User;
import com.github.dentou.utils.FXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.util.List;

import javafx.scene.control.Alert.AlertType;
import javafx.scene.input.MouseEvent;
import org.controlsfx.control.MaskerPane;


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

    private MaskerPane maskerPane = new MaskerPane();

    @FXML
    @Override
    protected void initialize() {
        super.initialize();
        loginButton.setDisable(true);
    }

    @Override
    protected void onWindowClosed(MouseEvent event) {
        boolean yes = getMainApp().showConfirmationDialog("Logout Confirmation", "Are you sure want to log out?", null);
        if (!yes) {
            return;
        }
        getMainApp().stop();
        super.getMainApp().showConnectionDialog();

    }

    @Override
    public void processMessage(String message) {
        enableAll();
        List<String> messageParts = ClientUtils.parseMessage(message);
        String numericCode = messageParts.get(1);
        switch (Response.getResponse(numericCode)) {
            case RPL_WELCOME:
                maskerPane.setVisible(false);
                getMainApp().setUser(new User(nickField.getText(), userNameField.getText(), fullnameField.getText()));
                getMainApp().showMainWindow();
                break;
            case ERR_NICKNAMEINUSE:
                getMainApp().showAlertDialog(AlertType.ERROR, "Error", "Nick error", "Nickname already exists. Please choose another.");
                break;
        }
    }

    @FXML
    private void onLogin() throws IOException{
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
        maskerPane.setVisible(true);
    }

    @FXML
    private void onDisconnect() {
        getMainApp().getIrcClient().sendToServer("QUIT");
        getMainApp().getIrcClient().stop();
        getMainApp().showConnectionDialog();
        //Platform.exit();
    }

    @FXML
    private void onKeyReleased() {
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
