package com.github.dentou;

import com.github.dentou.chat.IRCClient;
import com.github.dentou.chat.IRCConstants;
import com.github.dentou.file.FileMetadata;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Main {
    private static final String freeNodeServer = "irc.freenode.net";
    private static final String huyServer = "169.254.53.178";

    public static void main(String[] args) throws IOException {
        new IRCClient("localhost", 6667).start();
        //new FileTransferClient().start();

//        String pathString = "C:\\Users\\trant\\Desktop\\java-file-send\\todo.txt";
//        String fileName = "todo.txt";
//        Path path = Paths.get(pathString);
//        System.out.println(path.getFileName());
//        System.out.println(path.getParent());
//        String path = "C:\\Users\\trant\\Desktop\\java-file-receive\\Grammatik-aktuell.pdf";
//        FileReceiver receiver;
//        FileMetadata fileMetaData = new FileMetadata(Paths.get(path), 101, 0l);
//        System.out.println("File already exists. Creating new dir: ");
//        Path newDir = Paths.get(fileMetaData.getFilePath().getParent().toString(), "new-duplicated", fileMetaData.getFilePath().getFileName().toString());
//        System.out.println(newDir);

//        Path path = Paths.get(".");
//        Path newDir = Paths.get(".", "temp");
//        Files.createDirectory(newDir);
//        System.out.println(newDir.toAbsolutePath());

//        createUserDataFile("dentou");
//        emptyUserData("dentou");
//        List<FileMetadata> fileMetadataList = loadUserData("dentou");
//        fileMetadataList.add(new FileMetadata(Paths.get("C:\\Users\\trant\\Desktop\\java-file-receive\\Grammatik-aktuell.pdf"),
//                125, 23));
//        saveUserData("dentou", fileMetadataList);

    }

    public static boolean createUserDataFile(String nick) throws IOException {

        boolean newFileCreated = false;

        Path userDataDir = Paths.get(IRCConstants.usersDataPath);
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

    public static List<FileMetadata> loadUserData(String nick) throws IOException {

        List<FileMetadata> fileMetadataList = new ArrayList<>();
        boolean newFile = createUserDataFile(nick);
        if (newFile) {
            return fileMetadataList;
        }
        Path userDataDir = Paths.get(IRCConstants.usersDataPath);
        Path filePath = Paths.get(userDataDir.toString(), nick + ".json");


        // File already exists, load it
        System.out.println("Loading user data");
        JsonDeserializer<FileMetadata> fileMetaDataJsonDeserializer = new JsonDeserializer<FileMetadata>() {
            @Override
            public FileMetadata deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
                JsonObject jsonObject = jsonElement.getAsJsonObject();

                Path path = Paths.get(jsonObject.get("path").getAsString());
                return new FileMetadata(path, jsonObject.get("size").getAsLong(), jsonObject.get("position").getAsLong());
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
        System.out.println("Loaded file metadata");
        System.out.println(fileMetadataList);
        bufferedReader.close();
        return fileMetadataList;
    }

    public static void emptyUserData(String nick) throws IOException {
        boolean newFile = createUserDataFile(nick);
        if (newFile) {
            return;
        }
        Path filePath = Paths.get(".", "data", "users-data", nick + ".json");
        try (FileWriter file = new FileWriter(filePath.toString(), false)) {
            file.write("");
            System.out.println("Successfully Empty User Data");
        }
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
                return jsonObject;
            }
        };

        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.setPrettyPrinting();
        gsonBuilder.registerTypeAdapter(FileMetadata.class, fileMetaDataJsonSerializer);
        Gson gson = gsonBuilder.create();
        String jsonString = gson.toJson(fileMetadataList);
        Path filePath = Paths.get(".", "data", "users-data", nick + ".json");
        try (FileWriter file = new FileWriter(filePath.toString(), false)) {
            file.write(jsonString);
            System.out.println("Successfully Copied JSON Object to File...");
            System.out.println(jsonString);
        }

    }

    public static void playWithJson() throws IOException {
        Dummy dummy = new Dummy("Norman", "norman@futurestud.io", 26, true);
        Path path = Paths.get("C:\\Users\\trant\\Desktop\\java-file-receive\\Grammatik-aktuell.pdf");
        FileMetadata fileMetadata = new FileMetadata(path, 123, 12323l);

        Gson gson = new Gson();

        GsonBuilder gsonBuilder = new GsonBuilder();

        JsonSerializer<FileMetadata> fileMetaDataJsonSerializer = new JsonSerializer<FileMetadata>() {
            @Override
            public JsonElement serialize(FileMetadata fileMetaData, Type type, JsonSerializationContext jsonSerializationContext) {
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("path", fileMetaData.getFilePath().toString());
                jsonObject.addProperty("size", fileMetaData.getSize());
                jsonObject.addProperty("position", fileMetaData.getPosition());
                return jsonObject;
            }
        };

        JsonDeserializer<FileMetadata> fileMetaDataJsonDeserializer = new JsonDeserializer<FileMetadata>() {
            @Override
            public FileMetadata deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
                JsonObject jsonObject = jsonElement.getAsJsonObject();

                Path path = Paths.get(jsonObject.get("path").getAsString());
                return new FileMetadata(path, jsonObject.get("size").getAsLong(), jsonObject.get("position").getAsLong());
            }
        };


        gsonBuilder.registerTypeAdapter(FileMetadata.class, fileMetaDataJsonSerializer);
        gsonBuilder.registerTypeAdapter(FileMetadata.class, fileMetaDataJsonDeserializer);
        Gson customGson = gsonBuilder.create();


        String jsonString = customGson.toJson(fileMetadata);
        System.out.println(jsonString);
//        FileMetadata custom = customGson.fromJson(json, FileMetadata.class);
//        System.out.println(custom);

        loadUserData("dentou");
        try (FileWriter file = new FileWriter("./users-data/dentou.json")) {
            file.write(jsonString);
            System.out.println("Successfully Copied JSON Object to File...");
            System.out.println("\nJSON Object: " + jsonString);
        }

    }

}
