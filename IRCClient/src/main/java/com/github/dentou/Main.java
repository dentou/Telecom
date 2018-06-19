package com.github.dentou;

import com.github.dentou.file.FileMetaData;
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
        //new IRCClient(huyServer, 6667).start();
        //new FileTransferClient().start();

//        String pathString = "C:\\Users\\trant\\Desktop\\java-file-send\\todo.txt";
//        String fileName = "todo.txt";
//        Path path = Paths.get(pathString);
//        System.out.println(path.getFileName());
//        System.out.println(path.getParent());
//        String path = "C:\\Users\\trant\\Desktop\\java-file-receive\\Grammatik-aktuell.pdf";
//        FileReceiver receiver;
//        FileMetaData fileMetaData = new FileMetaData(Paths.get(path), 101, 0l);
//        System.out.println("File already exists. Creating new dir: ");
//        Path newDir = Paths.get(fileMetaData.getFilePath().getParent().toString(), "new-duplicated", fileMetaData.getFilePath().getFileName().toString());
//        System.out.println(newDir);

//        Path path = Paths.get(".");
//        Path newDir = Paths.get(".", "temp");
//        Files.createDirectory(newDir);
//        System.out.println(newDir.toAbsolutePath());

        emptyUserData("dentou");
        List<FileMetaData> fileMetaDataList = loadUserData("dentou");
        fileMetaDataList.add(new FileMetaData(Paths.get("C:\\Users\\trant\\Desktop\\java-file-receive\\Grammatik-aktuell.pdf"),
                125, 23));
        saveUserData("dentou", fileMetaDataList);

    }

    public static List<FileMetaData> loadUserData(String nick) throws IOException {

        List<FileMetaData> fileMetaDataList = new ArrayList<>();

        Path userDataDir = Paths.get("./users-data");
        if (!Files.isDirectory(userDataDir)) {
            Files.createDirectory(userDataDir);
        }
        Path filePath = Paths.get(userDataDir.toString(), nick + ".json");
        if (Files.notExists(filePath)) {
            Files.createFile(filePath);
            return fileMetaDataList;
        }

        // File already exists, load it
        System.out.println("Loading user data");
        JsonDeserializer<FileMetaData> fileMetaDataJsonDeserializer = new JsonDeserializer<FileMetaData>() {
            @Override
            public FileMetaData deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
                JsonObject jsonObject = jsonElement.getAsJsonObject();

                Path path = Paths.get(jsonObject.get("path").getAsString());
                return new FileMetaData(path, jsonObject.get("size").getAsLong(), jsonObject.get("position").getAsLong());
            }
        };

        JsonReader reader = new JsonReader(new InputStreamReader(new FileInputStream(filePath.toString()), "UTF-8"));
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(FileMetaData.class, fileMetaDataJsonDeserializer);
        Gson gson = gsonBuilder.create();
        // Read file in stream mode
        Type fileMetaDataListType = new TypeToken<ArrayList<FileMetaData>>(){}.getType();
        BufferedReader bufferedReader = new BufferedReader(new FileReader(new File(filePath.toString())));
        List<FileMetaData> list = gson.fromJson(reader, fileMetaDataListType);
        if (list != null) {
            fileMetaDataList.addAll(list);
        }
        System.out.println("Loaded file metadata");
        System.out.println(fileMetaDataList);
        bufferedReader.close();
        return fileMetaDataList;
    }

    public static void emptyUserData(String nick) throws IOException {
        Path filePath = Paths.get("./users-data", nick + ".json");
        try (FileWriter file = new FileWriter(filePath.toString(), false)) {
            file.write("");
            System.out.println("Successfully Empty User Data");
        }
    }

    public static void saveUserData(String nick, List<FileMetaData> fileMetaDataList) throws IOException {

        JsonSerializer<FileMetaData> fileMetaDataJsonSerializer = new JsonSerializer<FileMetaData>() {
            @Override
            public JsonElement serialize(FileMetaData fileMetaData, Type type, JsonSerializationContext jsonSerializationContext) {
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("path", fileMetaData.getFilePath().toString());
                jsonObject.addProperty("size", fileMetaData.getSize());
                jsonObject.addProperty("position", fileMetaData.getPosition());
                return jsonObject;
            }
        };

        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.setPrettyPrinting();
        gsonBuilder.registerTypeAdapter(FileMetaData.class, fileMetaDataJsonSerializer);
        Gson gson = gsonBuilder.create();
        String jsonString = gson.toJson(fileMetaDataList);
        Path filePath = Paths.get("./users-data", nick + ".json");
        try (FileWriter file = new FileWriter(filePath.toString(), false)) {
            file.write(jsonString);
            System.out.println("Successfully Copied JSON Object to File...");
            System.out.println(jsonString);
        }

    }

    public static void playWithJson() throws IOException {
        Dummy dummy = new Dummy("Norman", "norman@futurestud.io", 26, true);
        Path path = Paths.get("C:\\Users\\trant\\Desktop\\java-file-receive\\Grammatik-aktuell.pdf");
        FileMetaData fileMetaData = new FileMetaData(path, 123, 12323l);

        Gson gson = new Gson();

        GsonBuilder gsonBuilder = new GsonBuilder();

        JsonSerializer<FileMetaData> fileMetaDataJsonSerializer = new JsonSerializer<FileMetaData>() {
            @Override
            public JsonElement serialize(FileMetaData fileMetaData, Type type, JsonSerializationContext jsonSerializationContext) {
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("path", fileMetaData.getFilePath().toString());
                jsonObject.addProperty("size", fileMetaData.getSize());
                jsonObject.addProperty("position", fileMetaData.getPosition());
                return jsonObject;
            }
        };

        JsonDeserializer<FileMetaData> fileMetaDataJsonDeserializer = new JsonDeserializer<FileMetaData>() {
            @Override
            public FileMetaData deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
                JsonObject jsonObject = jsonElement.getAsJsonObject();

                Path path = Paths.get(jsonObject.get("path").getAsString());
                return new FileMetaData(path, jsonObject.get("size").getAsLong(), jsonObject.get("position").getAsLong());
            }
        };


        gsonBuilder.registerTypeAdapter(FileMetaData.class, fileMetaDataJsonSerializer);
        gsonBuilder.registerTypeAdapter(FileMetaData.class, fileMetaDataJsonDeserializer);
        Gson customGson = gsonBuilder.create();


        String jsonString = customGson.toJson(fileMetaData);
        System.out.println(jsonString);
//        FileMetaData custom = customGson.fromJson(json, FileMetaData.class);
//        System.out.println(custom);

        loadUserData("dentou");
        try (FileWriter file = new FileWriter("./users-data/dentou.json")) {
            file.write(jsonString);
            System.out.println("Successfully Copied JSON Object to File...");
            System.out.println("\nJSON Object: " + jsonString);
        }

    }

}
