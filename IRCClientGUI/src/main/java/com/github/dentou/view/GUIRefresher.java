package com.github.dentou.view;

import com.github.dentou.MainApp;
import javafx.application.Platform;

public class GUIRefresher implements Runnable {
    private final MainApp mainApp;

    public GUIRefresher(MainApp mainApp) {
        this.mainApp = mainApp;
    }

    public MainApp getMainApp() {
        return mainApp;
    }

    @Override
    public void run() {
        while (true) {
            if (mainApp.isAppClosed()) {
                System.out.println("Refresher closed");
                return;
            }
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    mainApp.getController().refresh();
                }
            });
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                System.out.println("Interruption in refresher thread");
                e.printStackTrace();
            }
        }


    }
}
