package com.github.dentou.view;

import com.github.dentou.MainApp;
import de.jensd.fx.glyphs.fontawesome.FontAwesomeIconView;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

public abstract class Controller<M> { // M is type of message

    @FXML
    protected Node root;

    @FXML
    protected Label titleLabel;
    @FXML
    protected Label closeLabel;
    @FXML
    protected FontAwesomeIconView closeIcon;
    @FXML
    protected Label minimizeLabel;
    @FXML
    protected FontAwesomeIconView minimizeIcon;

    private MainApp mainApp;
    private final Queue<M> receiveQueue = new ArrayBlockingQueue<M>(512);

    public abstract void disableAll();
    public abstract void enableAll();

    private double x;
    private double y;


    @FXML
    protected void initialize() {
        initializeWindow();
    }


    private void initializeWindow() {

        // Set window title
        if (titleLabel.getText().isEmpty()) {
            titleLabel.setText("IRC Client");
        }
        // Set title bar buttons
        closeLabel.setOnMouseEntered(event -> {
            closeLabel.setStyle("-fx-cursor:hand; -fx-background-color: primary-color;");
            closeIcon.setStyle("-fx-text-fill: red; -fx-fill: red;");
        });

        closeLabel.setOnMouseExited(event -> {
            closeLabel.setStyle("-fx-cursor:normal; -fx-background-color: primary-dark-color;");
            closeIcon.setStyle("-fx-text-fill: white; -fx-fill: white");
        });

        minimizeLabel.setOnMouseEntered(event -> {
            minimizeLabel.setStyle("-fx-cursor:hand; -fx-background-color: primary-color;");
            minimizeIcon.setStyle("-fx-text-fill: #00E676; -fx-fill: #00E676;");
        });

        minimizeLabel.setOnMouseExited(event -> {
            minimizeLabel.setStyle("-fx-cursor:normal; -fx-background-color: primary-dark-color;");
            minimizeIcon.setStyle("-fx-text-fill: white; -fx-fill: white;");
        });

        closeLabel.setOnMouseClicked(event -> onWindowClosed(event));

        minimizeLabel.setOnMouseClicked(event -> onWindowMinimized(event));


        // Set window title drag
        root.setOnMouseDragged(event -> onWindowDragged(event));
        root.setOnMousePressed(event -> onWindowPressed(event));
    }

    protected void onWindowDragged(MouseEvent event) {
        Stage stage = (Stage) root.getScene().getWindow();
        stage.setX(event.getScreenX() + this.x);
        stage.setY(event.getScreenY() + this.y);
    }

    protected void onWindowPressed(MouseEvent event) {
        Stage stage = (Stage) root.getScene().getWindow();
        this.x = stage.getX() - event.getScreenX();
        this.y = stage.getY() - event.getScreenY();
    }

    protected void onWindowClosed(MouseEvent event) {
        Platform.exit();
    }

    protected void onWindowMinimized(MouseEvent event) {
        Stage stage = (Stage) root.getScene().getWindow();
        stage.setIconified(true);
    }



    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    public MainApp getMainApp() {
        return this.mainApp;
    }

    public void setTitle(String title) {
        this.titleLabel.setText(title);
    }

    public void displayInfo() {
        // Do nothing by default
    }

    public void refresh() {
        // Do nothing by default
    }


    public void update() {
        while (true) {
            M message = receiveQueue.poll();
            if (message == null) {
                return;
            }
            processMessage(message);
        }
    }

    public abstract void processMessage(M message);





    public void enqueue(M message) {
        this.receiveQueue.add(message);
    }
    public void enqueueAll(List<M> messages) {
        if (messages == null || messages.isEmpty()) {
            return;
        }
        for (M message : messages) {
            enqueue(message);
        }
    }


    protected Queue<M> getReceiveQueue() {
        return this.receiveQueue;
    }

}
