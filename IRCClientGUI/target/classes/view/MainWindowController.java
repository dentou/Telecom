package com.github.dentou.view;

import com.github.dentou.MainApp;
import com.github.dentou.model.Channel;
import com.github.dentou.model.ChatHistoryItem;
import com.github.dentou.utils.ClientUtils;
import com.github.dentou.model.IRCConstants.*;
import com.github.dentou.model.PrivateMessage;
import com.github.dentou.model.User;
import com.github.dentou.utils.FXUtils;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.WindowEvent;


import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;


public class MainWindowController extends Controller<String> {
    @FXML
    private TabPane tabPane;
    @FXML
    private Tab joinedChannelsTab;
    @FXML
    private Tab historyTab;
    @FXML
    private Tab channelsTab;
    @FXML
    private Tab usersTab;

    @FXML
    private TableView<Channel> joinedChannelsTable;
    @FXML
    private TableColumn<Channel, String> joinedChannelNameColumn;
    @FXML
    private TableColumn<Channel, String> joinedChannelTopicColumn;
    @FXML
    private TableColumn<Channel, Integer> joinedChannelMembersColumn;

    @FXML
    private TableView<ChatHistoryItem> historyTable;
    @FXML
    private TableColumn<ChatHistoryItem, String> chatterColumn;
    @FXML
    private TableColumn<ChatHistoryItem, String> lastMessageColumn;
    @FXML
    private TableColumn<ChatHistoryItem, LocalDateTime> timeColumn;

    @FXML
    private TableView<Channel> channelsTable;
    @FXML
    private TableColumn<Channel, String> channelNameColumn;
    @FXML
    private TableColumn<Channel, String> channelTopicColumn;
    @FXML
    private TableColumn<Channel, Integer> channelMembersColumn;

    @FXML
    private TableView<User> usersTable;
    @FXML
    private TableColumn<User, String> nickColumn;
    @FXML
    private TableColumn<User, String> userNamecColumn;
    @FXML
    private TableColumn<User, String> fullNameColumn;

    private ObservableList<Channel> joinedChannelsData = FXCollections.observableArrayList();
    private ObservableList<ChatHistoryItem> historyData = FXCollections.observableArrayList();
    private ObservableList<Channel> channelsData = FXCollections.observableArrayList();
    private ObservableList<User> usersData = FXCollections.observableArrayList();


    @FXML
    private Label hostNickLabel;
    @FXML
    private Label hostUserNameLabel;
    @FXML
    private Label hostFullNameLabel;
    @FXML
    private Button logoutButton;
    @FXML
    private Button refreshButton;
    @FXML
    private Button createChannelButton;

    private Map<Tab, TableView> tabTableMap = new HashMap<>();

    private Map<String, Channel> allChannelsMap = new HashMap<>();
    private Map<String, ObservableList<String>> channelMembersMap = new HashMap<>();
    private Map<String, ChatHistoryItem> chatHistoryMap = new HashMap<String, ChatHistoryItem>();
    private Map<String, ChatDialogController> activeChatDialog = new HashMap<String, ChatDialogController>();

    private boolean waitingForList = false;
    private boolean waitingForWho = false;
    private boolean waitingForNames = false;

    private boolean waitingForListEnd = false;
    private boolean waitingForWhoEnd = false;
    private boolean waitingForNamesEnd = false;


    @FXML
    @Override
    protected void initialize() {
        hostNickLabel.setText("");
        hostUserNameLabel.setText("");
        hostFullNameLabel.setText("");


        // Initialize joined channels table
        joinedChannelNameColumn.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        joinedChannelTopicColumn.setCellValueFactory(cellData -> cellData.getValue().topicProperty());
        joinedChannelMembersColumn.setCellValueFactory(cellData -> cellData.getValue().numberOfMembersProperty().asObject());

        // Initialize history table
        chatterColumn.setCellValueFactory(cellData -> cellData.getValue().chatterProperty());
        lastMessageColumn.setCellValueFactory(cellData -> cellData.getValue().lastMessageProperty());
        timeColumn.setCellValueFactory(cellData -> cellData.getValue().lastMessageTimeProperty());


        // Initialize channels table
        channelNameColumn.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
        channelTopicColumn.setCellValueFactory(cellData -> cellData.getValue().topicProperty());
        channelMembersColumn.setCellValueFactory(cellData -> cellData.getValue().numberOfMembersProperty().asObject());

        // Initialize users table
        nickColumn.setCellValueFactory(cellData -> cellData.getValue().nickProperty());
        userNamecColumn.setCellValueFactory(cellData -> cellData.getValue().userNameProperty());
        fullNameColumn.setCellValueFactory(cellData -> cellData.getValue().fullNameProperty());

        // Add observable list data to the table
        joinedChannelsTable.setItems(joinedChannelsData);
        historyTable.setItems(historyData);
        channelsTable.setItems(channelsData);
        usersTable.setItems(usersData);

        // Map tabs to tables
        tabTableMap.put(joinedChannelsTab, joinedChannelsTable);
        tabTableMap.put(historyTab, historyTable);
        tabTableMap.put(channelsTab, channelsTable);
        tabTableMap.put(usersTab, usersTable);


        // todo clear channel member list

        // todo Listen for selection changes and show the channel member list when changed.

    }

    @Override
    public void displayInfo() {
        User user = super.getMainApp().getUser();
        hostNickLabel.setText(user.getNick());
        hostUserNameLabel.setText(user.getUserName());
        hostFullNameLabel.setText(user.getFullName());


        onRefresh();

    }

    @Override
    public void processMessage(String message) {
        // todo implement this
        List<String> messageParts = ClientUtils.parseMessage(message);
        System.out.println(messageParts);
        String sender = ClientUtils.parseSender(messageParts.get(0));
        String content = messageParts.get(messageParts.size() - 1);
        if (sender.equals("server")) { // Message is server's response
            // todo check for all responses
            String numericCode = messageParts.get(1);
            switch (Response.getResponse(numericCode)) {
                case RPL_LIST:
                    processRPL_LIST(messageParts);
                    break;
                case RPL_LISTEND:
                    if (!waitingForWhoEnd) {
                        enableAll();
                    }
                    waitingForListEnd = false;
                    break;
                case RPL_WHOREPLY:
                    processRPL_WHO(messageParts);
                    break;
                case RPL_ENDOFWHO:
                    if (!waitingForListEnd) {
                        enableAll();
                    }
                    waitingForWhoEnd = false;
                    break;
                case RPL_NAMEREPLY:
                    processRPL_NAMEREPLY(messageParts);
                    break;
                case RPL_ENDOFNAMES:
                    waitingForNamesEnd = false;
                    break;

            }

        } else { // Relay message
            switch (messageParts.get(1)) {
                case "PRIVMSG":
                    String receiver = messageParts.get(2);
                    PrivateMessage privateMessage = new PrivateMessage(sender, receiver, content);
                    processPRIVMSG(privateMessage);
                    break;
                case "JOIN":
                    String channel = messageParts.get(2);
                    processJOIN(sender, channel);
                    break;
                case "PART":
                    channel = messageParts.get(2);
                    processPART(sender, channel);
                    break;

            }
        }

    }

    @Override
    public void disableAll() {
        // todo implement this
        FXUtils.setDisabled(true, refreshButton);
        FXUtils.setDisabled(true, tabPane);
    }

    @Override
    public void enableAll() {
        // todo implement this
        FXUtils.setDisabled(false, refreshButton);
        FXUtils.setDisabled(false, tabPane);
    }

    @Override
    public synchronized void refresh() {
        super.refresh();
        // Toggle flags
        waitingForList = true;
        waitingForWho = true;
        waitingForNames = true;
        waitingForListEnd = true;
        waitingForWhoEnd = true;
        waitingForNamesEnd = true;
        // Send to server LIST and WHO
        getMainApp().getIrcClient().sendToServer("LIST");
        getMainApp().getIrcClient().sendToServer("WHO");
        getMainApp().getIrcClient().sendToServer("NAMES");
    }

    @FXML
    private void onLogout() {
        // todo send quit to server
        getMainApp().getIrcClient().sendToServer("QUIT");
        getMainApp().getIrcClient().stop();
        super.getMainApp().showConnectionDialog();
    }


    @FXML
    private void onRefresh() {
        refresh();
        // Disable refresh button
        disableAll();
    }

    @FXML
    private void onCreateChannel() {
        String channelName;
        while (true) {
            channelName = getMainApp().showTextInputDialog("Channel Options", "Create channel",
                    "Please input channel name: ");
            StringBuilder error = new StringBuilder();
            if (channelName == null || channelName.isEmpty()) {
                return;
            }
            if (channelName != null) {
                if (channelName.charAt(0) != '#') {
                    error.append("\nChannel name must begin with #");
                }
                if (channelName.length() < 2) {
                    error.append("\nChannel name must contain at least 2 characters");
                }
            }
            String errorString = error.toString();
            if (errorString == null || errorString.isEmpty()) {
                break;
            }
            getMainApp().showAlertDialog(AlertType.ERROR, "Invalid input", "Please fix the errors and try again",
                    errorString);

        }

        getMainApp().getIrcClient().sendToServer("JOIN", " ", channelName);
    }


    @FXML
    private void onJoinedChannelsClicked(MouseEvent event) {
        if (event.getClickCount() == 2) {
            String result = getMainApp().showCustomActionDialog("Channel Options", "Please choose your action",
                    null, "Send message");
            if (result == "Cancel") {
                return;
            }
            if (result == "Send message") {
                Channel channel = joinedChannelsTable.getSelectionModel().getSelectedItem();
                displayMessage(channel.getName(), null);
            }
        }
    }

    @FXML
    private void onHistoryClicked(MouseEvent event) {
        if (event.getClickCount() == 2) {
            ChatHistoryItem item = historyTable.getSelectionModel().getSelectedItem();
            String chatter = item.getChatter();
            if (chatter.charAt(0) == '#') { // Chatter is a channel
                boolean blocked = !joinedChannelsData.contains(allChannelsMap.get(chatter));
                displayMessage(chatter, null, blocked);
            } else {
                displayMessage(chatter, null);
            }

        }
    }

    @FXML
    private void onChannelsClicked(MouseEvent event) {
        if (event.getClickCount() == 2) {
            Channel channel = channelsTable.getSelectionModel().getSelectedItem();
            if (joinedChannelsData.contains(channel)) {
                onJoinedChannelsClicked(event);
                return;
            }
            String result = getMainApp().showCustomActionDialog("Channel Options", "Please choose your action",
                    null, "Join", "Send message");
            if (result == "Cancel") {
                return;
            }
            if (result == "Send message") {
                displayMessage(channel.getName(), null);

            } else if (result == "Join") {
                getMainApp().getIrcClient().sendToServer("JOIN", " ",
                        channelsTable.getSelectionModel().getSelectedItem().getName());
            }
        }

    }

    @FXML
    private void onUsersClicked(MouseEvent event) {
        if (event.getClickCount() == 2) {
            User user = usersTable.getSelectionModel().getSelectedItem();
            displayMessage(user.getNick(), null);
        }
    }


//    private void handleSendMessage() {
//        Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
//        TableView table = tabTableMap.get(selectedTab);
//        Object selectedItem = table.getSelectionModel().getSelectedItem();
//        if (selectedItem == null) {
//            getMainApp().showAlertDialog(AlertType.ERROR, "Send message error", "No user/channel selected",
//                    "Please select an user or channel");
//            return;
//
//        }
//        String chatter = null;
//
//        if (selectedItem instanceof User) {
//            User user = (User) selectedItem;
//            chatter = user.getNick();
//        } else if (selectedItem instanceof Channel) {
//            Channel channel = (Channel) selectedItem;
//            chatter = channel.getName();
//        } else if (selectedItem instanceof ChatHistoryItem) {
//            ChatHistoryItem chatHistoryItem = (ChatHistoryItem) selectedItem;
//            chatter = chatHistoryItem.getChatter();
//        }
//
//        displayMessage(chatter, null);
//
//    }


    /**
     * Processing methods
     *
     * @param messageParts
     */

    private void processRPL_LIST(List<String> messageParts) {
        if (waitingForList) {
            channelsData.clear();
            allChannelsMap.clear();
            for (Channel channel : joinedChannelsData) {
                allChannelsMap.put(channel.getName(), channel);
            }
            waitingForList = false;
        }
        String channelName = messageParts.get(3);
        int numberOfMembers = Integer.parseInt(messageParts.get(4));
        String topic = "";
        if (messageParts.size() > 5) {
            topic = messageParts.get(5);
        }
        Channel channel = new Channel(channelName, numberOfMembers, topic);
        if (allChannelsMap.containsKey(channelName)) {
            Channel oldChannel = allChannelsMap.get(channelName);
            oldChannel.setName(channelName);
            oldChannel.setNumberOfMembers(numberOfMembers);
            oldChannel.setTopic(topic);
        } else {
            allChannelsMap.put(channelName, channel);
            channelsData.add(channel);
        }
    }


    private void processRPL_WHO(List<String> messageParts) {
        if (waitingForWho) {
            usersData.clear();
            waitingForWho = false;
        }

        // todo implement this
        String nick = messageParts.get(3);
        String userName = messageParts.get(4);
        String fullName = messageParts.get(7);
        User user = new User(nick, userName, fullName);
        usersData.add(user);
    }

    private void processRPL_NAMEREPLY(List<String> messageParts) {
        String channelName = messageParts.get(4);
        ObservableList<String> memberList = channelMembersMap.get(channelName);
        // todo implement this
        if (waitingForNames) {
            channelMembersMap.get(channelName).clear();
            waitingForNames = false;
        }
        String[] nicks = messageParts.get(messageParts.size() - 1).split(" ");
        for (String nick : nicks) {
            memberList.add(nick);
        }
    }


    private void processPRIVMSG(PrivateMessage privateMessage) {
        String sender = privateMessage.getSender();
        String receiver = privateMessage.getReceiver();
        String chatter;

        if (sender.equals(getMainApp().getUser().getNick())) { // Message from this user to other user or to channel
            chatter = receiver;

        } else if (receiver.equals(getMainApp().getUser().getNick())) { // Message from other users to this user
            chatter = sender;
        } else { // Message from other users to the channel of which this user is a member
            chatter = receiver;
        }
        //this.updateChatHistory(chatter, privateMessage);
        this.displayMessage(chatter, privateMessage);
    }

    private void processJOIN(String sender, String channelName) {
        // todo implement this
        if (getMainApp().getUser().getNick().equals(sender)) {
            channelMembersMap.put(channelName, FXCollections.observableArrayList());
            if (!allChannelsMap.containsKey(channelName)) {
                allChannelsMap.put(channelName, new Channel(channelName, 0, ""));
            }
            joinedChannelsData.add(allChannelsMap.get(channelName));
        } else {
            //channelMembersMap.get(channelName).add(sender);
        }
        displayMessage(channelName, new PrivateMessage("server", getMainApp().getUser().getNick(),
                sender + " has joined the channel."), false);
        //refresh();
    }

    private void processPART(String sender, String channelName) {
        // todo implement this
        if (getMainApp().getUser().getNick().equals(sender)) {
            Channel channel = allChannelsMap.get(channelName);
            joinedChannelsData.remove(channel);
            //channelsData.add(channel);
        } else {
            //channelMembersMap.get(channelName).remove(sender);

        }
        displayMessage(channelName, new PrivateMessage("server", getMainApp().getUser().getNick(),
                sender + " has left the channel."), true);
        refresh();


    }

    /**
     * Display methods
     *
     * @param chatter
     * @param privateMessage
     */

    private void displayMessage(String chatter, PrivateMessage privateMessage) {
        displayMessage(chatter, privateMessage, false);
    }

    private void displayMessage(String chatter, PrivateMessage privateMessage, boolean blocked) {
        // todo implement this
        if (chatter == null) {
            System.out.println("Null chatter");
            return;
        }

        System.out.println("Update chat history");

        if (privateMessage != null) {
            updateChatHistory(chatter, privateMessage);
        }


        System.out.println("Show chat dialog for chatter: " + chatter);


        // Show chat dialog
        ChatDialogController chatDialogController = activeChatDialog.get(chatter);
        if (chatDialogController == null) { // If chat dialog is closed (null), open it and display chat history
            System.out.println("Open new chat dialog");
            chatDialogController = this.showChatDialog(chatter);

            chatDialogController.setBlocked(blocked);

            activeChatDialog.put(chatter, chatDialogController);
            if (chatHistoryMap.containsKey(chatter)) {
                chatDialogController.enqueueAll(chatHistoryMap.get(chatter).getMessageList());
            }
        } else { // If chat dialog is already opened, add message
            if (privateMessage != null) {
                chatDialogController.enqueue(privateMessage);
            }

        }
        chatDialogController.update();

    }

    public boolean updateChatHistory(String chatter, PrivateMessage privateMessage) {
        ChatHistoryItem chatHistoryItem = chatHistoryMap.get(chatter);
        if (privateMessage == null || privateMessage.getContent() == null) {
            return false;
        }
        if (chatHistoryItem == null) {
            chatHistoryItem = new ChatHistoryItem(chatter, privateMessage);
            chatHistoryMap.put(chatter, chatHistoryItem);
            historyData.add(chatHistoryItem);
        } else {
            chatHistoryItem.addMessage(privateMessage);
        }
        Collections.sort(historyData, Collections.reverseOrder());
        return true;

    }

    private ChatDialogController showChatDialog(String chatter) {
        try {
            // Load the fxml file and create a new stage for the popup dialog.
            FXMLLoader loader = new FXMLLoader();
            if (chatter.charAt(0) == '#') { // Chatter is a channel
                loader.setLocation(MainApp.class.getResource("/view/ChannelDialog.fxml"));
            } else {
                loader.setLocation(MainApp.class.getResource("/view/ChatDialog.fxml"));
            }
            Parent page = loader.load();

            // Create the dialog Stage.
            Stage dialogStage = new Stage();
            dialogStage.setTitle("Conversation with " + chatter);
            dialogStage.initModality(Modality.NONE);
            dialogStage.initOwner(getMainApp().getPrimaryStage());
            Scene scene = new Scene(page);
            dialogStage.setScene(scene);


            // Set the person into the controller.
            ChatDialogController controller = (ChatDialogController) loader.getController();
            controller.setMainApp(getMainApp());
            controller.setChatter(chatter);

            if (chatter.charAt(0) == '#') { // If chatter is a channel
                Channel channel = allChannelsMap.get(chatter);
                ChannelDialogController channelDialogController = (ChannelDialogController) controller;
                channelDialogController.setChannel(channel, channelMembersMap.get(channel.getName()));
            }

            // Clean up after close
            dialogStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
                @Override
                public void handle(WindowEvent event) {
                    activeChatDialog.remove(chatter);
                }
            });


            // Show the dialog
            dialogStage.show();

            return controller;

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


}
