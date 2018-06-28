package com.github.dentou.utils;

import com.github.dentou.model.file.FileMetadata;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ClientUtils {

    public static String readableFileSize(long size) {
        if(size <= 0) return "0B";
        final String[] units = new String[] { "B", "kB", "MB", "GB", "TB", "PB", "EB"};
        int digitGroups = (int) (Math.log10(size)/Math.log10(1024));
        return new DecimalFormat("#,##0.#").format(size/Math.pow(1024, Math.min(digitGroups, units.length)))
                + " " + units[digitGroups];
    }


    public static boolean createUserDataFile(String nick) throws IOException {

        boolean newFileCreated = false;

        Path userDataDir = Paths.get(ClientConstants.userDataPath);
        if (!Files.isDirectory(userDataDir)) {
            Files.createDirectories(userDataDir);
        }
        Path filePath = Paths.get(userDataDir.toString(), nick + ".json");
        if (Files.notExists(filePath)) {
            Files.createFile(filePath);
            newFileCreated = true;
        }

        return newFileCreated;
    }

    public static void saveUserData(String nick, FileMetadata fileMetadata) throws IOException {
        List<FileMetadata> fileMetadataList = new ArrayList<>();
        fileMetadataList.add(fileMetadata);
        saveUserData(nick, fileMetadataList);
    }

    public static void saveUserData(String nick, List<FileMetadata> fileMetadataList) throws IOException {

        createUserDataFile(nick);

        JsonSerializer<FileMetadata> fileMetaDataJsonSerializer = new JsonSerializer<FileMetadata>() {
            @Override
            public JsonElement serialize(FileMetadata fileMetaData, Type type, JsonSerializationContext jsonSerializationContext) {
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("path", fileMetaData.getFilePath().toString());
                jsonObject.addProperty("size", fileMetaData.getSize());
                jsonObject.addProperty("position", fileMetaData.getPosition());
                jsonObject.addProperty("sender", fileMetaData.getSender());
                jsonObject.addProperty("receiver", fileMetaData.getReceiver());
                return jsonObject;
            }
        };

        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.setPrettyPrinting();
        gsonBuilder.registerTypeAdapter(FileMetadata.class, fileMetaDataJsonSerializer);
        Gson gson = gsonBuilder.create();
        String jsonString = gson.toJson(fileMetadataList);

        Path userDataDir = Paths.get(ClientConstants.userDataPath);
        Path filePath = Paths.get(userDataDir.toString(), nick + ".json");

        try (FileWriter file = new FileWriter(filePath.toString(), false)) {
            file.write(jsonString);
            System.out.println("Successfully Saved JSON Object to File");
            System.out.println(jsonString);
        }

    }

    public static List<FileMetadata> loadUserData(String nick) throws IOException {
        List<FileMetadata> fileMetadataList = new ArrayList<>();
        boolean newFile = createUserDataFile(nick);
        if (newFile) {
            return fileMetadataList;
        }

        Path userDataDir = Paths.get(ClientConstants.userDataPath);
        Path filePath = Paths.get(userDataDir.toString(), nick + ".json");

        // File already exists, load it
        System.out.println("Loading user data");
        JsonDeserializer<FileMetadata> fileMetaDataJsonDeserializer = new JsonDeserializer<FileMetadata>() {
            @Override
            public FileMetadata deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
                JsonObject jsonObject = jsonElement.getAsJsonObject();

                Path path = Paths.get(jsonObject.get("path").getAsString());
                return new FileMetadata(path, jsonObject.get("size").getAsLong(), jsonObject.get("position").getAsLong(),
                        jsonObject.get("sender").getAsString(), jsonObject.get("receiver").getAsString());
            }
        };

        JsonReader reader = new JsonReader(new InputStreamReader(new FileInputStream(filePath.toString()), "UTF-8"));
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(FileMetadata.class, fileMetaDataJsonDeserializer);
        Gson gson = gsonBuilder.create();
        // Read file in stream mode
        Type fileMetaDataListType = new TypeToken<ArrayList<FileMetadata>>(){}.getType();
        BufferedReader bufferedReader = new BufferedReader(new FileReader(new File(filePath.toString())));
        List<FileMetadata> list = gson.fromJson(reader, fileMetaDataListType);
        if (list != null) {
            fileMetadataList.addAll(list);
        }
        System.out.println("File metadata for user " + nick + " loaded");
        System.out.println(fileMetadataList);
        bufferedReader.close();
        return fileMetadataList;
    }

    public static void emptyUserData(String nick) throws IOException {
        boolean newFile = createUserDataFile(nick);
        if (newFile) {
            return;
        }
        Path userDataDir = Paths.get(ClientConstants.userDataPath);
        Path filePath = Paths.get(userDataDir.toString(), nick + ".json");
        try (FileWriter file = new FileWriter(filePath.toString(), false)) {
            file.write("");
            System.out.println("Successfully Empty User Data");
        }
    }


    public static List<String> parseMessage(String message) {
        List<String> parts = new ArrayList<>();
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
