package com.github.dentou.view;

import com.github.dentou.MainApp;
import com.github.dentou.model.file.FileMetadata;
import com.github.dentou.utils.ClientUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import org.apache.commons.lang3.StringUtils;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Objects;

import static com.github.dentou.utils.ClientUtils.readableFileSize;

public class FileTransferItem {

    private final MainApp mainApp;

    private final FileTransferType fileTransferType;
    private final FileMetadata fileMetadata;

    private final File file;

    private final FileTransferStatus fileTransferStatus;


    private final Label fileNameLabel = new Label();
    private final Label errorLabel = new Label();
    private final Label statusLabel = new Label();

    private final GridPane gridPane = new GridPane();


    public enum FileTransferType {
        SEND, RECEIVE
    }

    public enum FileTransferStatus {
        SUCCEEDED("Succeeded"),
        FAILED("Failed"),
        CANCELLED("Cancelled");

        private final String statusText;

        FileTransferStatus(String statusText) {
            this.statusText = statusText;
        }

        @Override
        public String toString() {
            return statusText;
        }
    }

    public FileTransferItem(FileTransferType fileTransferType, FileMetadata fileMetadata, FileTransferStatus fileTransferStatus, MainApp mainApp) {
        this.fileTransferType = fileTransferType;
        this.fileMetadata = fileMetadata;
        this.fileTransferStatus = fileTransferStatus;
        this.mainApp = mainApp;

        this.file = fileMetadata.getFilePath().toFile();

        initializeLabels();
        initializeGridPane();
        initializeContextMenu();
    }

    public Node getGUINode() {
        return this.gridPane;
    }

    public FileTransferType getFileTransferType() {
        return fileTransferType;
    }

    private void initializeLabels() {
        String sendOrReceive = "";
        if (fileTransferType == FileTransferType.SEND) {
            sendOrReceive += " - to " + fileMetadata.getReceiver();
        } else {
            sendOrReceive += " - from " + fileMetadata.getSender();
        }
        fileNameLabel.setText(fileMetadata.getFilePath().getFileName().toString() + sendOrReceive);
        fileNameLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold;");

        statusLabel.setText(fileTransferStatus.toString() + " " + readableFileSize(fileMetadata.getPosition()) + "/"
                + readableFileSize(fileMetadata.getSize()));
        statusLabel.setStyle("-fx-font-size: 12;");

        errorLabel.setStyle("-fx-text-fill: red;");
        if (!file.exists()) {
            errorLabel.setText("File has been moved to other directory");
        }

    }

    private void initializeGridPane() {


        gridPane.setVgap(5);
        gridPane.setHgap(5);
        gridPane.setPadding(new Insets(15));

        gridPane.setOnMouseEntered(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                gridPane.setStyle("-fx-background-color: #ffdc65;");
            }
        });

        gridPane.setOnMouseExited(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                gridPane.setStyle("-fx-background-color: white");
            }
        });

        gridPane.add(fileNameLabel, 0, 0);
        gridPane.add(statusLabel, 0, 1);

        if (StringUtils.isNotEmpty(errorLabel.getText())) {
            gridPane.add(errorLabel, 0, 2);
            gridPane.setColumnSpan(errorLabel, GridPane.REMAINING);
        }

    }

    private void initializeContextMenu() {
        ContextMenu contextMenu = new ContextMenu();
        MenuItem openFileLocation = new MenuItem("Open file location");
        MenuItem resume = new MenuItem("Resume");

        if (!file.exists()) {
            openFileLocation.setDisable(true);
            resume.setDisable(true);
        } else {
            if (fileTransferType == FileTransferType.SEND) {
                resume.setDisable(true);
            } else {
                resume.setDisable(fileTransferStatus == FileTransferStatus.SUCCEEDED);
            }
        }


        openFileLocation.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                try {
                    if (!file.exists()) {
                        openFileLocation.setDisable(true);
                        resume.setDisable(true);
                    }
                    Desktop.getDesktop().open(file);
                } catch (IOException e) {
                    e.printStackTrace();
                    // todo show error dialog
                }
            }
        });

        resume.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if (!file.exists()) {
                    openFileLocation.setDisable(true);
                    resume.setDisable(true);
                }
                // todo
                if (!Objects.isNull(mainApp)) {
                    mainApp.getIrcClient().sendToServer("FILE_RESUME", " ",
                            fileMetadata.getSender(), " ",
                            "" + fileMetadata.getSize(), " ",
                            "" + fileMetadata.getPosition(), " ",
                            ":" + fileMetadata.getFilePath().getFileName());

                }
            }
        });

        contextMenu.getItems().addAll(openFileLocation, resume);

        gridPane.setOnContextMenuRequested(new EventHandler<ContextMenuEvent>() {
            @Override
            public void handle(ContextMenuEvent event) {
                contextMenu.show(gridPane, event.getScreenX(), event.getScreenY());
            }
        });
    }

}
