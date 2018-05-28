package com.github.dentou.view;


import com.github.dentou.model.IRCClient;
import com.github.dentou.model.IRCConstants;
import com.github.dentou.model.IRCSocket;
import com.github.dentou.utils.FXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert.AlertType;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.regex.Pattern;

public class ConnectionDialogController extends Controller<String>{

    @FXML
    private TextField serverAddressField;
    @FXML
    private Button connectButton;
    @FXML
    private Button resetButton;

    @Override
    protected void initialize() {
        connectButton.setDisable(true);
    }

    @Override
    public void disableAll() {
        FXUtils.setDisabled(true, serverAddressField, connectButton, resetButton);
    }

    @Override
    public void enableAll() {
        FXUtils.setDisabled(false, serverAddressField, connectButton, resetButton);
    }

    @Override
    public void processMessage(String message) {
        // do nothing by default
    }

    @FXML
    private void handleReset() {
        serverAddressField.setText("");
        serverAddressField.requestFocus();
        connectButton.setDisable(true);
    }

    @FXML
    private void handleConnect() {
        String serverAddress = serverAddressField.getText().trim();
        if (!isValidIpAddress(serverAddress)) {
            super.getMainApp().showAlertDialog(AlertType.ERROR, "Invalid Fields", "Please correct invalid fields", "Invalid IP Address");
            return;
        }


        InetSocketAddress inetAddress = new InetSocketAddress(serverAddress, IRCConstants.SERVER_PORT);
        WorkIndicatorDialog wd = new WorkIndicatorDialog(super.getMainApp().getPrimaryStage(), "Connecting to server");
        wd.addTaskEndNotification(result -> {
            enableAll();
            if (result == null) {
                getMainApp().showAlertDialog(AlertType.ERROR,"Connection Error", "Connection Error", "Cannot connect to server. Please try again.");
                return;
            }
            SocketChannel socketChannel = (SocketChannel) result;
            System.out.println("Server addr: " + socketChannel.socket().getInetAddress().toString());
            IRCSocket ircSocket = new IRCSocket(socketChannel);
            try {
                IRCClient ircClient = new IRCClient(ircSocket, getMainApp());
                getMainApp().setIrcClient(ircClient);
                getMainApp().startClient();
                getMainApp().showLoginDialog();

            } catch (IOException e) {
                getMainApp().showAlertDialog(AlertType.ERROR, "Client error", "Client error", "Cannot initialize client. Please try again.");
            }

        });

        wd.execute(inetAddress, inputParam -> {
            try {
                disableAll();
                SocketChannel channel = SocketChannel.open(inetAddress);
                channel.configureBlocking(false);
                return channel;
            } catch (IOException e) {
                //e.printStackTrace();
                return null;
            }

        });
    }

    @FXML
    private void handleKeyReleased() {
        String serverAddress = serverAddressField.getText().trim();
        boolean disableConnectButton = serverAddress.isEmpty();
        connectButton.setDisable(disableConnectButton);
    }

    private boolean isValidIpAddress(String ip) {
        Pattern PATTERN = Pattern.compile(
                "^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");
        return PATTERN.matcher(ip).matches();
    }



}
