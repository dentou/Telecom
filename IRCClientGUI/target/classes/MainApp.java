package com.github.dentou;

import com.github.dentou.model.chat.IRCClient;
import com.github.dentou.model.chat.User;
import com.github.dentou.model.file.FileTransferClient;
import com.github.dentou.view.Controller;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.controlsfx.control.Notifications;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;


public class MainApp extends Application {

    private Stage primaryStage;
    private Controller controller;
    private User user = null;

    private IRCClient ircClient;

    private FileTransferClient fileTransferClient;

    private String serverAddress;


    private ScheduledExecutorService refresherExecutor;



    public synchronized Stage getPrimaryStage() {
        return primaryStage;
    }

    public synchronized User getUser() {
        return this.user;
    }

    public synchronized void setUser(User user) {
        this.user = user;
    }

    public synchronized String getServerAddress() {
        return serverAddress;
    }

    public synchronized void setServerAddress(String serverAddress) {
        this.serverAddress = serverAddress;
    }

    public FileTransferClient getFileTransferClient() {
        return fileTransferClient;
    }

    public synchronized IRCClient getIrcClient() {
        return ircClient;
    }

    public synchronized void setIrcClient(IRCClient ircClient) {
        this.ircClient = ircClient;
    }

    public void startClient() {
        new Thread(ircClient).start();
    }

    public synchronized Controller getController() {
        return controller;
    }

    public synchronized void setController(Controller controller) {
        this.controller = controller;
    }

    public static void main(String[] args) {
        launch(args);
    }


    @Override
    public void start(Stage primaryStage) {

        this.primaryStage = primaryStage;
        this.primaryStage.setTitle("IRC Chat Client");
        this.primaryStage.setResizable(false);
        primaryStage.initStyle(StageStyle.TRANSPARENT);

        initializeFileTransferClient();

        showConnectionDialog();

        this.primaryStage.show();
        initializeRefresher();

    }

    @Override
    public void stop() {
        System.out.println("App is closing");
        if (ircClient != null) {
            ircClient.sendToServer("QUIT");
            ircClient.stop();
        }
        refresherExecutor.shutdown();
        System.out.println("Refresher closed");
        fileTransferClient.forcedClose();
        System.out.println("File transfer client closed");
    }

    public void initializeRefresher() {
        refresherExecutor = Executors.newSingleThreadScheduledExecutor();
        refresherExecutor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                // todo uncomment the following line when done debugging
                //Platform.runLater(() -> getController().refresh());
            }
        },
        1,
        5,
        TimeUnit.SECONDS);
    }

    public void initializeFileTransferClient() {
        this.fileTransferClient = new FileTransferClient(this);
    }


    public void showConnectionDialog() {
        showScene("/view/ConnectionDialog.fxml" , "");
    }

    public void showLoginDialog() {
        showScene("/view/LoginDialog.fxml", "");
    }

    public void showMainWindow() {
        showScene("/view/MainWindow.fxml", "");
    }



    private void showScene(String fxmlFileName, String title) {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(MainApp.class.getResource(fxmlFileName));
            Parent page = loader.load();

            Controller controller = loader.getController();
            controller.setMainApp(this);
            controller.setTitle(title);
            this.controller = controller;

            controller.displayInfo();

            primaryStage.setScene(new Scene(page));

        } catch (Exception e) {
            //e.printStackTrace();
            showExceptionDialog("Loading error", "Failed to load next screen",
                    "The application will exit", e);
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, e);

            Platform.exit();
        }
    }

    public String showTextInputDialog(String title, String headerText, String contentText) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle(title);
        dialog.setHeaderText(headerText);
        dialog.setContentText(contentText);

        dialog.getDialogPane().lookupButton(ButtonType.OK).setDisable(true);

        dialog.getEditor().setOnKeyReleased(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                boolean disableOKButton = dialog.getEditor().getText().trim().isEmpty();
                dialog.getDialogPane().lookupButton(ButtonType.OK).setDisable(disableOKButton);

            }
        });

        // Traditional way to get the response value.
        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()){
            return result.get();
        }
        return null;
    }

    public boolean showConfirmationDialog(String title, String headerText, String contentText) {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
        stage.setAlwaysOnTop(true);
        alert.setTitle(title);
        alert.setHeaderText(headerText);
        alert.setContentText(contentText);

        Optional<ButtonType> result = alert.showAndWait();
        return result.get() == ButtonType.OK;
    }

    public String showCustomActionDialog(String title, String headerText, String contentText, String... buttonNames) {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
        stage.setAlwaysOnTop(true);
        alert.initOwner(this.primaryStage);
        alert.setTitle(title);
        alert.setHeaderText(headerText);
        alert.setContentText(contentText);

        List<ButtonType> buttonTypes = new ArrayList<>();
        for (String name : buttonNames) {
            buttonTypes.add(new ButtonType(name));
        }
        buttonTypes.add(new ButtonType("Cancel", ButtonData.CANCEL_CLOSE));
        alert.getButtonTypes().setAll(buttonTypes);
        Optional<ButtonType> result = alert.showAndWait();

        return result.get().getText();
    }

    public void showAlertDialog(AlertType alertType, String title, String headerText, String contentText) {
        Alert alert = new Alert(alertType);
        Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
        stage.setAlwaysOnTop(true);
        alert.initOwner(this.primaryStage);
        alert.setTitle(title);
        alert.setHeaderText(headerText);
        alert.setContentText(contentText);

        alert.showAndWait();
    }

    public void showExceptionDialog(String title, String headerText, String contentText, Exception ex) {
        Alert alert = new Alert(AlertType.ERROR);
        Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
        stage.setAlwaysOnTop(true);
        alert.initOwner(this.primaryStage);
        alert.setTitle(title);
        alert.setHeaderText(headerText);
        alert.setContentText(contentText);

        // Create expandable Exception.
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        ex.printStackTrace(pw);
        String exceptionText = sw.toString();

        Label label = new Label("Exception stacktrace:");

        TextArea textArea = new TextArea(exceptionText);
        textArea.setEditable(false);
        textArea.setWrapText(true);

        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea, Priority.ALWAYS);

        GridPane expContent = new GridPane();
        expContent.setMaxWidth(Double.MAX_VALUE);
        expContent.add(label, 0, 0);
        expContent.add(textArea, 0, 1);

        // Set expandable Exception into the dialog pane.
        alert.getDialogPane().setExpandableContent(expContent);

        alert.showAndWait();
    }

    public void showNotifications(String title, String text) {
        Notifications noti = Notifications.create();
        noti.title(title);
        noti.text(text);
        noti.show();

    }


}
