package com.github.dentou.controller;

import com.github.dentou.MainApp;
import com.github.dentou.model.chat.Channel;
import com.github.dentou.model.chat.ChatHistoryItem;
import com.github.dentou.model.file.FileMetadata;
import com.github.dentou.model.file.FileTransferClient.FileSendTask;
import com.github.dentou.model.file.FileTransferClient.FileReceiveTask;

import com.github.dentou.utils.ClientConstants.*;
import com.github.dentou.model.chat.PrivateMessage;
import com.github.dentou.model.chat.User;
import com.github.dentou.utils.FXUtils;
import com.github.dentou.view.FileTransferItem;
import com.github.dentou.view.FileTransferItem.FileTransferStatus;
import com.github.dentou.view.FileTransferItem.FileTransferType;
import com.github.dentou.view.WorkIndicatorDialog;
import com.jfoenix.controls.JFXTabPane;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.concurrent.Task;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.*;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.FileChooser.ExtensionFilter;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.controlsfx.control.PopOver;
import org.controlsfx.control.TaskProgressView;


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

import static com.github.dentou.utils.ClientUtils.*;
import static com.github.dentou.utils.ClientUtils.readableFileSize;

public class MainWindowController extends Controller<String> {


    @FXML
    private TextField searchField;
    @FXML
    private FontAwesomeIconView nickIcon;
    @FXML
    private Label nickLabel;


    @FXML
    private Button refreshButton;
    @FXML
    private Button createButton;

    @FXML
    private ProgressBar progressBar;
    @FXML
    private Label transferCountLabel;

    private IntegerProperty transferCount = new SimpleIntegerProperty(0);

    private AnchorPane fileTransferView = new AnchorPane();

    private TaskProgressView fileProgressView = new TaskProgressView();

    private VBox fileFinishedVBox = new VBox();

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


    private ObservableList<Channel> joinedChannelsData = FXCollections.observableArrayList();
    private ObservableList<ChatHistoryItem> historyData = FXCollections.observableArrayList();
    private ObservableList<Channel> channelsData = FXCollections.observableArrayList();
    private ObservableList<User> usersData = FXCollections.observableArrayList();

    private Map<String, FileMetadata> fileSendMap = new ConcurrentHashMap<>();
    private Map<String, FileMetadata> fileReceiveMap = new ConcurrentHashMap<>();


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
        initializeTransferCountLabel();
        initializeFileTransferView();
        // todo clear channel member list
        // todo Listen for selection changes and show the channel member list when changed.
    }

    private void initializeNickLabel() {
        // Set nick
        nickLabel.setOnMouseEntered(event -> {
            nickLabel.setStyle("-fx-cursor:hand; -fx-text-fill: secondary-color;");
            nickIcon.setStyle("-fx-text-fill: secondary-color; -fx-fill: secondary-color;");
        });

        nickLabel.setOnMouseExited(event -> {
            nickLabel.setStyle("-fx-cursor:normal; -fx-text-fill: white;");
            nickIcon.setStyle("-fx-text-fill: white; -fx-fill: white");
        });

        nickLabel.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {

                VBox vBox = new VBox();
                vBox.setSpacing(15);
                vBox.setPadding(new Insets(15));

                HBox userNameHBox = new HBox();
                userNameHBox.setSpacing(10);
                userNameHBox.getChildren().add(new Label("User Name:"));
                userNameHBox.getChildren().add(new Label(getMainApp().getUser().getUserName()));

                HBox fullNameHBox = new HBox();
                fullNameHBox.setSpacing(10);
                fullNameHBox.getChildren().add(new Label("Full Name:"));
                fullNameHBox.getChildren().add(new Label(getMainApp().getUser().getFullName()));

                vBox.getChildren().addAll(userNameHBox, fullNameHBox);

                PopOver popOver = new PopOver(vBox);
                popOver.setCloseButtonEnabled(true);
                popOver.setDetachable(false);

                popOver.setHeaderAlwaysVisible(true);
                popOver.setArrowLocation(PopOver.ArrowLocation.TOP_CENTER);
                popOver.setTitle("User Info");
                popOver.show(nickLabel);


            }
        });
    }

    private void initializeTransferCountLabel() {

        transferCountLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold;");

        transferCountLabel.textProperty().bind(transferCount.asString());

        transferCountLabel.setOnMouseEntered(event -> {
            transferCountLabel.setStyle("-fx-cursor:hand; -fx-background-color: #7793a1; -fx-font-size: 14; -fx-font-weight: bold");
        });

        transferCountLabel.setOnMouseExited(event -> {
            transferCountLabel.setStyle("-fx-cursor:normal; -fx-background-color: primary-light-color; -fx-font-size: 14; -fx-font-weight: bold");
        });


    }

    private void initializeFileTransferView() {

        int viewWidth = 350;
        int viewHeight = 300;

        fileProgressView.setPrefHeight(viewHeight);
        fileProgressView.setPrefWidth(viewWidth);

        fileFinishedVBox.setPrefWidth(viewWidth);
        fileFinishedVBox.setPrefHeight(viewHeight);

        VBox progressVBox = new VBox(fileProgressView);
        Tab progressTab = new Tab("Progress");
        progressTab.setContent(progressVBox);

        ScrollPane scrollPane = new ScrollPane(fileFinishedVBox);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        Tab finishedTab = new Tab("Finished");
        finishedTab.setContent(scrollPane);

        JFXTabPane tabPane = new JFXTabPane();
        tabPane.getTabs().addAll(progressTab, finishedTab);

        fileTransferView.getChildren().add(tabPane);

        //AnchorPane anchorPane = new AnchorPane(tabPane);
        fileTransferView.getStylesheets().add("/style/FileTransferView.css");
        fileTransferView.setPrefWidth(viewWidth);
        fileTransferView.setPrefHeight(viewHeight);
        fileTransferView.setLeftAnchor(tabPane, 0d);
        fileTransferView.setRightAnchor(tabPane, 0d);


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
                        predicate(newValue, channel.getName(), channel.getTopic())));

        searchField.textProperty().addListener((observable, oldValue, newValue) ->
                filteredHistoryData.setPredicate(item ->
                        predicate(newValue, item.getChatter())));
        searchField.textProperty().addListener((observable, oldValue, newValue) ->
                filteredChannelsData.setPredicate(channel ->
                        predicate(newValue, channel.getName(), channel.getTopic())));
        searchField.textProperty().addListener((observable, oldValue, newValue) ->
                filteredUsersData.setPredicate(user ->
                        predicate(newValue, user.getNick(), user.getUserName(), user.getFullName())));

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

    @Override
    public void displayInfo() { // WARNING: Never call this inside initialize method, cause exception
        super.displayInfo();


        User user = super.getMainApp().getUser();
        setTitle("IRCClient: " + user.getNick());
        nickLabel.setText(user.getNick());

//        try {
//            List<FileMetadata> fileMetadataList = loadUserData(user.getNick());
//            for (FileMetadata fileMetadata : fileMetadataList) {
//                this.fileReceiveMap.put(fileMetadata.getFilePath().getFileName().toString(), fileMetadata);
//            }
//        } catch (IOException e) {
//            getMainApp().showExceptionDialog("File Loading Error",
//                    "Cannot load user metadata (interrupted downloads cannot be resumed)", null, e);
//        }
        WorkIndicatorDialog<String, List<FileMetadata>> wd = new WorkIndicatorDialog<>(getMainApp().getPrimaryStage(),
                "Loading user data");

        wd.addTaskEndNotification(fileMetadataList -> {
            if (Objects.isNull(fileMetadataList)) {
                getMainApp().showAlertDialog(AlertType.ERROR,
                        "File Loading Error",
                        "Cannot load user metadata (interrupted downloads cannot be resumed)",
                        null);
                return;
            }
            for (FileMetadata fileMetadata : fileMetadataList) {
                File file = fileMetadata.getFilePath().toFile();
                FileMetadata updated = new FileMetadata(fileMetadata.getFilePath(), fileMetadata.getSize(),
                        file.length(), fileMetadata.getSender(), fileMetadata.getReceiver());
                fileReceiveMap.put(updated.getFilePath().getFileName().toString(), updated);

                FileTransferItem fileTransferItem = new FileTransferItem(FileTransferType.RECEIVE,
                        updated, updated.done() ? FileTransferStatus.SUCCEEDED : FileTransferStatus.FAILED, getMainApp());
                fileFinishedVBox.getChildren().add(fileTransferItem.getGUINode());
            }
            if (!fileMetadataList.isEmpty()) {
                getMainApp().showAlertDialog(AlertType.ERROR, "File transfer error",
                        "Your file transfer didn't finish correctly",
                        "Please review by clicking the number on bottom right corner");
                transferCount.setValue(fileMetadataList.size());
            }
        });

        wd.execute(user.getNick(), new Function<String, List<FileMetadata>>() {
            @Override
            public List<FileMetadata> apply(String nick) {
                try {
                    List<FileMetadata> fileMetadataList = loadUserData(nick);
                    return fileMetadataList;
                } catch (IOException e) {
                    return null;
                }
            }
        });



        progressBar.setProgress(0);

        onRefresh();
    }


    /**
     * File transfer
     */
    public synchronized void addFileSend(FileMetadata fileMetadata) {
        if (fileMetadata != null) {
            this.fileSendMap.put(fileMetadata.getFilePath().getFileName().toString(), fileMetadata);
        }
    }

    public synchronized FileMetadata getFileSend(String fileName) {
        return this.fileSendMap.get(fileName);
    }


    /**
     * GUI methods
     */

    @Override
    protected void onWindowClosed(MouseEvent event) {
        onLogout();
    }

    @Override
    public void disableAll() {
        FXUtils.setDisabled(true, refreshButton, createButton, searchField);
        //FXUtils.setDisabled(true, tabPane);
    }

    @Override
    public void enableAll() {
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
        boolean yes = getMainApp().showConfirmationDialog("Logout Confirmation",
                "Are you sure want to log out?",
                "You will need to register again");
        if (!yes) {
            return;
        }

        for (ChatDialogController controller : activeChatDialog.values()) {
            controller.close();
        }
        getMainApp().stop();
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
            if (errorString.isEmpty()) {
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
            if ("Cancel".equals(result)) {
                return;
            }
            if ("Send message".equals(result)) {
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
            if ("Cancel".equals(result)) {
                return;
            }
            if ("Send message".equals(result)) {
                displayMessage(channel.getName(), null);

            } else if ("Join".equals(result)) {
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

    @FXML
    private void onTransferCountLabelClicked() {


        PopOver popOver = new PopOver(fileTransferView);
        popOver.setCloseButtonEnabled(true);
        popOver.setDetachable(false);
        popOver.setHeaderAlwaysVisible(true);
        popOver.setArrowLocation(PopOver.ArrowLocation.BOTTOM_RIGHT);
        popOver.setTitle("File Transfer");
        popOver.show(transferCountLabel);

    }


    /**
     * Processing methods
     */

    @Override
    public void processMessage(String message) {
        List<String> messageParts = parseMessage(message);
        System.out.println(messageParts);
        String sender = parseSender(messageParts.get(0));
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
                case "FILE_SEND":
                    processFILE_SEND(sender, messageParts);
                    break;
                case "FILE_RECEIVE":
                    processFILE_RECEIVE(sender, messageParts);
                    break;
                case "FILE_DENY":
                    processFILE_DENY(sender, messageParts);
                    break;
                case "FILE_RESUME":
                    processFILE_RESUME(sender, messageParts);
                    break;
                case "FILE_RESEND":
                    processFILE_RESEND(sender, messageParts);
                    break;
                case "FILE_RESUME_DENY":
                    processFILE_RESUME_DENY(sender, messageParts);
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

    private void processFILE_SEND(String sender, List<String> messageParts) {
        String fileName = messageParts.get(4);

        long fileSize = Long.parseLong(messageParts.get(3));


        // todo
        boolean yes = getMainApp().showConfirmationDialog("File Transfer Confirmation",
                "User " + sender + " wants to send you a file. Would you like to receive?",
                "File Name: " + fileName + "\n" +
                        "File size: " + readableFileSize(fileSize) + "\n");

        if (!yes) {
            getMainApp().getIrcClient().sendToServer("FILE_DENY", " ", sender, " ", "" + fileSize, " ", ":" + fileName);
            return;
        }

        DirectoryChooser directoryChooser = new DirectoryChooser();
        File selectedDirectory = directoryChooser.showDialog(getMainApp().getPrimaryStage());

        if (selectedDirectory == null) {
            //No Directory selected
            getMainApp().getIrcClient().sendToServer("FILE_DENY", " ", sender, " ", "" + fileSize, " ", ":" + fileName);
            return;
        }
        Path dir = Paths.get(selectedDirectory.getAbsolutePath());
        Path filePath = Paths.get(dir.toString(), fileName);

        while (Files.exists(filePath) && !Files.isDirectory(filePath)) {

            System.out.println("File already exists. Creating new dir.");
            dir = Paths.get(dir.toString(), "new-duplicate");
            filePath = Paths.get(dir.toString(), fileName);
            getMainApp().showAlertDialog(AlertType.WARNING, "File duplicate", "There's another file with the same name",
                    "The new file will be put in: " + dir);
        }

        try {
            if (!Files.isDirectory(dir)) {
                Files.createDirectories(dir);
            }
        } catch (IOException e) {
            getMainApp().getIrcClient().sendToServer("FILE_DENY", " ", sender, " ", "" + fileSize, " ", ":" + fileName);
            getMainApp().showAlertDialog(AlertType.ERROR, "File error", "Cannot create folder",
                    dir.toString());
            return;
        }


        // Send FILE_RECEIVE
        getMainApp().getIrcClient().sendToServer("FILE_RECEIVE", " ", sender, " ", "" + fileSize, " ", ":" + fileName);

        FileMetadata fileMetadata = new FileMetadata(filePath, fileSize, 0L, sender, getMainApp().getUser().getNick());
        this.fileReceiveMap.put(fileName, fileMetadata);

        // Save file metadata
        try {
            List<FileMetadata> fileMetadataList = new ArrayList<>(this.fileReceiveMap.values());
            saveUserData(getMainApp().getUser().getNick(), fileMetadataList);
        } catch (IOException e) {
            getMainApp().showExceptionDialog("File Saving Error",
                    "Cannot save user metadata (interrupted transfers can't be resumed)", null, e);
        }

        FileReceiveTask task = getMainApp().getFileTransferClient().receiveFile(fileMetadata, sender, getMainApp().getUser().getNick());

        setupTransferTask(FileTransferType.RECEIVE, fileMetadata, task);

        fileProgressView.getTasks().add(task);
        transferCount.setValue(transferCount.getValue() + 1);
        onTransferCountLabelClicked();
    }

    private void processFILE_RECEIVE(String recipient, List<String> messageParts) {
        String fileName = messageParts.get(4);
        FileMetadata fileMetadata = this.fileSendMap.get(fileName);
        if (fileMetadata == null) {
            System.out.println("File metadata not exist:" + messageParts);
            return;
        }
        // todo
        FileSendTask task = getMainApp().getFileTransferClient().sendFile(fileMetadata, getMainApp().getUser().getNick(), recipient);

        setupTransferTask(FileTransferType.SEND, fileMetadata, task);

        fileProgressView.getTasks().add(task);
        transferCount.setValue(transferCount.getValue() + 1);
        onTransferCountLabelClicked();
    }

    private void processFILE_DENY(String denier, List<String> messageParts) {
        getMainApp().showAlertDialog(AlertType.ERROR, "File Transfer Error", "File Transfer Error",
                "Your transfer request has been denied by " + denier);
        this.fileSendMap.remove(messageParts.get(4));
    }


    private void processFILE_RESUME(String requester, List<String> messageParts) {
        // todo
        String fileName = messageParts.get(5);
        long position = Long.parseLong(messageParts.get(4));
        long fileSize = Long.parseLong(messageParts.get(3));

        // User confirm resume request
        boolean yes = getMainApp().showConfirmationDialog("File Transfer Confirmation",
                "User " + requester + " wants you to resend a file",
                "File Name: " + fileName + "\n" +
                        "File size: " + readableFileSize(fileSize) + "\n" +
                        "Remaining to send: " + readableFileSize(fileSize - position));
        if (!yes) {
            yes = getMainApp().showConfirmationDialog("File Transfer Confirmation",
                    "You chose to cancel their request", "Are you sure?");
        }

        if (!yes) {
            getMainApp().getIrcClient().sendToServer("FILE_RESUME_DENY", " ",
                    requester, " ",
                    "" + fileSize, " ",
                    "" + position, " ",
                    ":" + fileName);
            return;
        }

        // Get file extension from file name
        // todo
        String extension = FilenameUtils.getExtension(fileName);

        // User choose file to resend (only the requested file is valid
        File selectedFile = null;
        while (true) {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Open file to resend");
            if (StringUtils.isNotEmpty(extension)) {
                fileChooser.getExtensionFilters().add(new ExtensionFilter("Requested file", "*." + extension));
            }
            fileChooser.setInitialDirectory(new File("."));

            selectedFile = fileChooser.showOpenDialog(getMainApp().getPrimaryStage());
            if (selectedFile == null) {
                break;
            }

            if (fileName.equals(selectedFile.getName().toString())) {
                break;
            }

            boolean continued = getMainApp().showConfirmationDialog("You selected the wrong file.",
                    "Do you want to select again?", null);
            if (!continued) {
                break;
            }
        }

        // If no file selected
        if (Objects.isNull(selectedFile)) {
            getMainApp().getIrcClient().sendToServer("FILE_RESUME_DENY", " ",
                    requester, " ",
                    "" + fileSize, " ",
                    "" + position, " ",
                    ":" + fileName);
            return;
        }

        // Accept chosen file
        System.out.println("File selected for send: " + selectedFile.getAbsolutePath() + ", size in bytes: " + fileSize);
        FileMetadata fileMetadata = new FileMetadata(Paths.get(selectedFile.getAbsolutePath()),
                fileSize, position, getMainApp().getUser().getNick(), requester);
        this.addFileSend(fileMetadata);

        getMainApp().getIrcClient().sendToServer("FILE_RESEND", " ",
                requester, " ",
                "" + fileSize, " ",
                "" + position, " ",
                ":", fileName);

        // Open file send task
        FileSendTask task = getMainApp().getFileTransferClient().sendFile(fileMetadata, getMainApp().getUser().getNick(), requester);

        setupTransferTask(FileTransferType.SEND, fileMetadata, task);

        fileProgressView.getTasks().add(task);
        transferCount.setValue(transferCount.getValue() + 1);
        onTransferCountLabelClicked();

    }

    private void processFILE_RESEND(String sender, List<String> messageParts) {
        // todo
        String fileName = messageParts.get(5);
        FileMetadata fileMetadata = fileReceiveMap.get(fileName);

        if (fileMetadata == null) {
            System.out.println("File metadata not exist:" + messageParts);
            return;
        }

        // Setup file receiver task
        FileReceiveTask task = getMainApp().getFileTransferClient().receiveFile(fileMetadata, sender, getMainApp().getUser().getNick());

        setupTransferTask(FileTransferType.RECEIVE, fileMetadata, task);

        fileProgressView.getTasks().add(task);
        transferCount.setValue(transferCount.getValue() + 1);
        onTransferCountLabelClicked();

    }

    private void processFILE_RESUME_DENY(String denier, List<String> messageParts) {
        // todo
        getMainApp().showAlertDialog(AlertType.ERROR, "File Transfer Error", "File Transfer Error",
                "Your resume request has been denied by " + denier);
        String fileName = messageParts.get(5);
        this.fileReceiveMap.remove(fileName);
    }

    /**
     * File transfer helpers
     */
    private void setupTransferTask(FileTransferType fileTransferType, FileMetadata fileMetadata, Task task) {
        task.setOnSucceeded(event -> {
            FileTransferItem fileTransferItem = new FileTransferItem(fileTransferType, fileMetadata,
                    FileTransferStatus.SUCCEEDED, getMainApp());
            fileFinishedVBox.getChildren().add(fileTransferItem.getGUINode());
            transferCount.set(transferCount.get() - 1);
            Platform.runLater(() -> getMainApp().showAlertDialog(AlertType.INFORMATION,
                    "File Transfer",
                    "Transfer succeeded",
                    "File: " + fileMetadata.getFilePath() + "\n" +
                            "Size: " + readableFileSize(fileMetadata.getSize()) + "\n" +
                            "From: " + fileMetadata.getSender() + "\n" +
                            "To: " + fileMetadata.getReceiver() + "\n"));
            if (fileTransferType == FileTransferType.SEND) {
                fileSendMap.remove(fileMetadata.getFilePath().getFileName().toString());
            } else if (fileTransferType == FileTransferType.RECEIVE) {
                fileReceiveMap.remove(fileMetadata.getFilePath().getFileName().toString());
                try {
                    emptyUserData(getMainApp().getUser().getNick());
                    if (!fileReceiveMap.isEmpty()) {
                        List<FileMetadata> fileMetadataList = new ArrayList<>(this.fileReceiveMap.values());
                        saveUserData(getMainApp().getUser().getNick(), fileMetadataList);
                    }
                } catch (IOException e) {
                    getMainApp().showExceptionDialog("File Saving Error",
                            "Cannot update file metadata", null, e);
                }

            }
        });

        task.setOnFailed(event -> {
            FileTransferItem fileTransferItem = new FileTransferItem(fileTransferType, fileMetadata, FileTransferStatus.FAILED, getMainApp());
            fileFinishedVBox.getChildren().add(fileTransferItem.getGUINode());
            transferCount.set(transferCount.get() - 1);
            Platform.runLater(() -> getMainApp().showAlertDialog(AlertType.ERROR, "File Transfer", "Transfer failed",
                    "File: " + fileMetadata.getFilePath() + "\n" +
                            "Size: " + readableFileSize(fileMetadata.getSize()) + "\n" +
                            "From: " + fileMetadata.getSender() + "\n" +
                            "To: " + fileMetadata.getReceiver() + "\n"));
        });

        task.setOnCancelled(event -> {
            FileTransferItem fileTransferItem = new FileTransferItem(fileTransferType, fileMetadata, FileTransferStatus.CANCELLED, getMainApp());
            fileFinishedVBox.getChildren().add(fileTransferItem.getGUINode());
            transferCount.set(transferCount.get() - 1);
            Platform.runLater(() -> getMainApp().showAlertDialog(AlertType.INFORMATION, "File Transfer", "Transfer cancelled",
                    "File: " + fileMetadata.getFilePath() + "\n" +
                            "Size: " + readableFileSize(fileMetadata.getSize()) + "\n" +
                            "From: " + fileMetadata.getSender() + "\n" +
                            "To: " + fileMetadata.getReceiver() + "\n"));

        });
    }


    /**
     * Display methods
     */

    private void displayMessage(String chatter, PrivateMessage privateMessage) {
        displayMessage(chatter, privateMessage, false);
    }

    private void displayMessage(String chatter, PrivateMessage privateMessage, boolean blocked) {
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
            controller.setTitle("Conversation with " + chatter);

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
                    dialogStage.close();
                }
            });


            // Show the dialog
            dialogStage.show();
            dialogStage.toFront();

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
