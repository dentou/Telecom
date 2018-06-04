package com.github.dentou.view;

import com.github.dentou.MainApp;
import javafx.fxml.FXML;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

public abstract class Controller<M> { // M is type of message
    private MainApp mainApp;
    private final Queue<M> receiveQueue = new ArrayBlockingQueue<M>(512);

    @FXML
    protected abstract void initialize();


    public abstract void disableAll();
    public abstract void enableAll();

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



    public void setMainApp(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    public MainApp getMainApp() {
        return this.mainApp;
    }

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
