package com.github.dentou.view;

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

import java.awt.*;
import java.io.File;
import java.io.IOException;

import static com.github.dentou.utils.ClientUtils.readableFileSize;

public class FileTransferItem {
    private final FileTransferType fileTransferType;
    private final FileMetadata fileMetadata;

    private final FileTransferStatus fileTransferStatus;


    private final Label fileNameLabel = new Label();
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

    public FileTransferItem(FileTransferType fileTransferType, FileMetadata fileMetadata, FileTransferStatus fileTransferStatus) {
        this.fileTransferType = fileTransferType;
        this.fileMetadata = fileMetadata;
        this.fileTransferStatus = fileTransferStatus;

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
        fileNameLabel.setText(fileMetadata.getFilePath().getFileName().toString());
        fileNameLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold;");

        statusLabel.setText(fileTransferStatus.toString() + " " + readableFileSize(fileMetadata.getPosition()) + "/"
                + readableFileSize(fileMetadata.getSize()));
        statusLabel.setStyle("-fx-font-size: 12;");
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
    }

    private void initializeContextMenu() {
        ContextMenu contextMenu = new ContextMenu();
        MenuItem openFileLocation = new MenuItem("Open file location");
        MenuItem resume = new MenuItem("Resume");

        if (fileTransferType == FileTransferType.SEND) {
            resume.setDisable(true);
        } else {
            resume.setDisable(fileTransferStatus == FileTransferStatus.SUCCEEDED);
        }

        openFileLocation.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                try {
                    Desktop.getDesktop().open(fileMetadata.getFilePath().getParent().toFile());
                } catch (IOException e) {
                    e.printStackTrace();
                    // todo show error dialog
                }
            }
        });

        resume.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                // todo
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
