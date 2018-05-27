package com.github.dentou.utils;

import javafx.scene.Node;

public class FXUtils {
    public static void setDisabled(boolean disable, Node... nodes) {
        for(Node node : nodes) {
            node.setDisable(disable);
        }
    }


}
