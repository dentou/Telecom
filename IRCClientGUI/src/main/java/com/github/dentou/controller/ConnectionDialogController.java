package com.github.dentou.controller;


import com.github.dentou.model.chat.IRCClient;
import com.github.dentou.utils.ClientConstants;
import com.github.dentou.model.chat.IRCSocket;
import com.github.dentou.utils.FXUtils;
import com.github.dentou.view.WorkIndicatorDialog;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.MenuButton;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert.AlertType;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.function.Function;
import java.util.regex.Pattern;

public class ConnectionDialogController extends Controller<String>{

    @FXML
    private TextField serverAddressField;
    @FXML
    private Button connectButton;
    @FXML
    private Button resetButton;

    @FXML
    private MenuButton serverMenuButton;

    @Override
    protected void initialize() {
        super.initialize();
        connectButton.setDisable(true);
        resetButton.setDisable(true);

        onLocalHostChosen();

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
    private void onReset() {
        serverAddressField.setText("");
        serverAddressField.requestFocus();
        connectButton.setDisable(true);
        resetButton.setDisable(true);
    }

    @FXML
    private void onConnect() {
        String serverAddress = serverAddressField.getText().trim();
        if (!isValidIpAddress(serverAddress) && !serverAddress.equals("localhost")) {
            super.getMainApp().showAlertDialog(AlertType.ERROR, "Invalid Fields", "Please correct invalid fields", "Invalid IP Address");
            return;
        }


        InetSocketAddress inetAddress = new InetSocketAddress(serverAddress, ClientConstants.CHAT_SERVER_PORT);
        WorkIndicatorDialog<InetSocketAddress, SocketChannel> wd = new WorkIndicatorDialog<>(super.getMainApp().getPrimaryStage(), "Connecting to server");
        wd.addTaskEndNotification(result -> {
            enableAll();
            onLocalHostChosen();
            if (result == null) {
                getMainApp().showAlertDialog(AlertType.ERROR,"Connection Error", "Connection Error", "Cannot connect to server. Please try again.");
                return;
            }
            getMainApp().setServerAddress(serverAddress);
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

        wd.execute(inetAddress, new Function<InetSocketAddress, SocketChannel>() {
            @Override
            public SocketChannel apply(InetSocketAddress inetSocketAddress) {
                try {
                    disableAll();
                    SocketChannel channel = SocketChannel.open(inetSocketAddress);
                    channel.configureBlocking(false);
                    return channel;
                } catch (IOException e) {
                    //e.printStackTrace();
                    return null;
                }
            }
        });
    }

    @FXML
    private void onKeyReleased() {
        String serverAddress = serverAddressField.getText().trim();
        boolean disableButtons = serverAddress.isEmpty();
        connectButton.setDisable(disableButtons);
        resetButton.setDisable(disableButtons);
    }

    @FXML
    private void onLocalHostChosen() {
        serverMenuButton.setText("Local Host");
        serverAddressField.setText("localhost");
        connectButton.setDisable(false);
        resetButton.setDisable(false);
        serverAddressField.setDisable(true);
    }

    @FXML
    private void onAWSServerChosen() {
        serverMenuButton.setText("AWS Server");
        serverAddressField.setText(ClientConstants.AWS_SERVER_IP);
        connectButton.setDisable(false);
        resetButton.setDisable(false);
        serverAddressField.setDisable(true);
    }

    @FXML
    private void onOtherChosen() {
        serverMenuButton.setText("Other");
        serverAddressField.setText("");
        connectButton.setDisable(true);
        resetButton.setDisable(true);
        serverAddressField.setDisable(false);
        serverAddressField.requestFocus();
    }

    private boolean isValidIpAddress(String ip) {
        Pattern PATTERN = Pattern.compile(
                "^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");
        return PATTERN.matcher(ip).matches();
    }



}
