package com.github.dentou.view;

import com.github.dentou.MainApp;
import com.github.dentou.model.Channel;
import com.github.dentou.model.ChatHistoryItem;
import com.github.dentou.utils.ClientUtils;
import com.github.dentou.model.IRCConstants.*;
import com.github.dentou.model.PrivateMessage;
import com.github.dentou.model.User;
import com.github.dentou.utils.FXUtils;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
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
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import org.controlsfx.control.Notifications;


import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Predicate;


public class MainWindowController extends Controller<String> {


    @FXML
    private TextField searchField;
    @FXML
    private Label nickLabel;
    @FXML
    private FontAwesomeIconView nickIcon;

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
    private TableColumn<User, String> userNameColumn;
    @FXML
    private TableColumn<User, String> fullNameColumn;

    @FXML
    private ProgressBar progressBar;

    private ObservableList<Channel> joinedChannelsData = FXCollections.observableArrayList();
    private ObservableList<ChatHistoryItem> historyData = FXCollections.observableArrayList();
    private ObservableList<Channel> channelsData = FXCollections.observableArrayList();
    private ObservableList<User> usersData = FXCollections.observableArrayList();

    @FXML
    private Button refreshButton;
    @FXML
    private Button createButton;

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
        super.initialize();
        initializeTables();
        initializeSearchField();
        initializeNickLabel();
        // todo clear channel member list
        // todo Listen for selection changes and show the channel member list when changed.
    }

    private void initializeNickLabel() {
        // Set title bar buttons
        nickLabel.setOnMouseEntered(event -> {
            nickLabel.setStyle("-fx-cursor:hand; -fx-text-fill: secondary-color;");
            nickIcon.setStyle("-fx-text-fill: secondary-color; -fx-fill: secondary-color;");
        });

        nickLabel.setOnMouseExited(event -> {
            nickLabel.setStyle("-fx-cursor:normal; -fx-text-fill: white;");
            nickIcon.setStyle("-fx-text-fill: white; -fx-fill: white");
        });
    }



    private void initializeTables() {
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
        userNameColumn.setCellValueFactory(cellData -> cellData.getValue().userNameProperty());
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
    }

    private void initializeSearchField() {
        // Wrap the ObservableList in a FilteredList (initially display all data).
        FilteredList<Channel> filteredJoinedChannelsData = new FilteredList<>(joinedChannelsData, p -> true);
        FilteredList<ChatHistoryItem> filteredHistoryData = new FilteredList<>(historyData, p -> true);
        FilteredList<Channel> filteredChannelsData = new FilteredList<>(channelsData, p -> true);
        FilteredList<User> filteredUsersData = new FilteredList<>(usersData, p -> true);

        // Set the filter Predicate whenever the filter changes.
        searchField.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                filteredJoinedChannelsData.setPredicate(new Predicate<Channel>() {
                    @Override
                    public boolean test(Channel channel) {
                        return false;
                    }
                });
            }
        });
        searchField.textProperty().addListener((observable, oldValue, newValue) ->
                filteredJoinedChannelsData.setPredicate(channel ->
                        ClientUtils.predicate(newValue, channel.getName(), channel.getTopic())));

        searchField.textProperty().addListener((observable, oldValue, newValue) ->
                filteredHistoryData.setPredicate(item ->
                        ClientUtils.predicate(newValue, item.getChatter())));
        searchField.textProperty().addListener((observable, oldValue, newValue) ->
                filteredChannelsData.setPredicate(channel ->
                        ClientUtils.predicate(newValue, channel.getName(), channel.getTopic())));
        searchField.textProperty().addListener((observable, oldValue, newValue) ->
                filteredUsersData.setPredicate(user ->
                        ClientUtils.predicate(newValue, user.getNick(), user.getUserName(), user.getFullName())));

        // Wrap the FilteredList in a SortedList.
        SortedList<Channel> sortedJoinedChannelsData = new SortedList<>(filteredJoinedChannelsData);
        SortedList<ChatHistoryItem> sortedHistoryData = new SortedList<>(filteredHistoryData);
        SortedList<Channel> sortedChannelsData = new SortedList<>(filteredChannelsData);
        SortedList<User> sortedUsersData = new SortedList<>(filteredUsersData);

        // Bind the SortedList comparator to the TableView comparator.
        sortedJoinedChannelsData.comparatorProperty().bind(joinedChannelsTable.comparatorProperty());
        sortedHistoryData.comparatorProperty().bind(historyTable.comparatorProperty());
        sortedChannelsData.comparatorProperty().bind(channelsTable.comparatorProperty());
        sortedUsersData.comparatorProperty().bind(usersTable.comparatorProperty());

        // Add sorted (and filtered) data to the table.
        joinedChannelsTable.setItems(sortedJoinedChannelsData);
        historyTable.setItems(sortedHistoryData);
        channelsTable.setItems(sortedChannelsData);
        usersTable.setItems(sortedUsersData);

    }

    /**
     * GUI methods
     */

    @Override
    public void displayInfo() {
        super.displayInfo();

        User user = super.getMainApp().getUser();
        setTitle("IRCClient: " + user.getNick());
        nickLabel.setText(user.getNick());

        progressBar.setProgress(0);

        onRefresh();

    }

    @Override
    protected void onWindowClosed(MouseEvent event) {
        onLogout();
    }

    @Override
    public void disableAll() {
        // todo implement this
        FXUtils.setDisabled(true, refreshButton, createButton, searchField);
        //FXUtils.setDisabled(true, tabPane);
    }

    @Override
    public void enableAll() {
        // todo implement this
        FXUtils.setDisabled(false, refreshButton, createButton, searchField);
        //FXUtils.setDisabled(false, tabPane);
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
        // Show progress bar
        showProgressBar(true);

    }

    private boolean isRefreshing() {
        return waitingForListEnd || waitingForWhoEnd || waitingForNamesEnd;
    }

    @FXML
    private void onLogout() {
        // Confirm
        boolean yes = getMainApp().showConfirmationDialog("Logout Confirmation", "Are you sure want to log out?", null);
        if (!yes) {
            return;
        }

        for (ChatDialogController controller : activeChatDialog.values()) {
            controller.close();
        }
        // todo send quit to server
        getMainApp().stop();
//        getMainApp().getIrcClient().sendToServer("QUIT");
//        getMainApp().getIrcClient().stop();
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





    /**
     * Processing methods
     *
     */

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
                    waitingForListEnd = false;
                    if (!isRefreshing()) {
                        showProgressBar(false);
                        enableAll();
                    }
                    break;
                case RPL_WHOREPLY:
                    processRPL_WHO(messageParts);
                    break;
                case RPL_ENDOFWHO:
                    waitingForWhoEnd = false;
                    if (!isRefreshing()) {
                        showProgressBar(false);
                        enableAll();
                    }
                    break;
                case RPL_NAMEREPLY:
                    processRPL_NAMEREPLY(messageParts);
                    break;
                case RPL_ENDOFNAMES:
                    waitingForNamesEnd = false;
                    if (!isRefreshing()) {
                        showProgressBar(false);
                        enableAll();
                    }
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
        if (privateMessage != null) {
            getMainApp().showNotifications("Notification for " + chatter, privateMessage.getContent());
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
            dialogStage.initStyle(StageStyle.TRANSPARENT);
            //dialogStage.initOwner(getMainApp().getPrimaryStage());
            dialogStage.setResizable(false);
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

    private void showProgressBar(boolean show) {
        if (show) {
            progressBar.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        } else {
            progressBar.setProgress(0);
        }
    }






}
