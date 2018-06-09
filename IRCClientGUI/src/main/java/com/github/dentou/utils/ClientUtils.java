package com.github.dentou.utils;

import com.github.dentou.model.Channel;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClientUtils {
    public static List<String> parseMessage(String message) {
        List<String> parts = new ArrayList<String>();
        Matcher m = Pattern.compile("(?<=:).+|[^ :]+").matcher(message.substring(1));
        while (m.find()) {
            parts.add(m.group());
        }
        return parts;
    }


    public static String parseSender(String header) {
        if (!header.contains("!")) {
            return "server";
        }
        return header.split("!")[0];
    }

    public static boolean predicate(String key, String... values) {
        if (key == null || key.isEmpty()) {
            return true;
        }

        // Compare first name and last name of every person with filter text.
        String lowerCaseKey = key.toLowerCase();

        for (String value : values) {
            if (value.toLowerCase().contains(lowerCaseKey)) {
                return true;
            }
        }

        return false;

    }
}
