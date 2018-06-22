package com.github.dentou.file;

import com.github.dentou.chat.IRCMessage;
import com.github.dentou.chat.IRCSocket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;

public class FileTransferClient {


    public void start() throws IOException {
        Scanner scanner = new Scanner(System.in);

        displayOptions(scanner);

        scanner = new Scanner(System.in);
        String choice = scanner.nextLine();
        if ("1".equals(choice)) { // Send
            sendFile(scanner);
        } else if ("2".equals(choice)) {
            receiveFile(scanner);
        }

    }

    private void displayOptions(Scanner scanner) {
        System.out.println("File Transfer Client");
        System.out.println(
                "1. Send\n" +
                        "2. Receive");

    }

    public static void sendFile(Scanner scanner) throws IOException {
        IRCSocket socket = new IRCSocket(SocketChannel.open(new InetSocketAddress("localhost", 6668)), false);

        String path = "C:\\Users\\trant\\Desktop\\java-file-send\\Grammatik-aktuell.pdf";
        FileSender sender = new FileSender(socket.getSocketChannel(), path, 0l);

        String sendMessage = new String("SEND huy\r\n");
        socket.enqueue(sendMessage);
        socket.sendMessages();

        boolean waiting = true;

        while (waiting) {
            List<IRCMessage> messages = socket.getMessages();
            for (IRCMessage message : messages) {
                System.out.println("From server");
                if (message.getMessage().contains("READY")) {
                    waiting = false;
                    break;
                }
            }
            if (socket.isEndOfStreamReached()) {
                socket.close();
                return;
            }
        }


        sender.send();

        System.out.println("Socket closed");
        sender.close();

    }

    public static void receiveFile(Scanner scanner) throws IOException {

        IRCSocket socket = new IRCSocket(SocketChannel.open(new InetSocketAddress("localhost", 6668)), false);

        String path = "C:\\Users\\trant\\Desktop\\java-file-receive\\Grammatik-aktuell.pdf";
        long bytes = 185951582;

        System.out.print("Enter file position (0 for new file): ");
        long position = Long.parseLong(scanner.nextLine());


        socket.enqueue("RECEIVE huy\r\n");
        socket.sendMessages();

        boolean waiting = true;

        while (waiting) {
            List<IRCMessage> messages = socket.getMessages();
            for (IRCMessage message : messages) {
                System.out.println("From server: " + message);
                if (message.getMessage().contains("READY")) {
                    waiting = false;
                    break;
                }
            }
            if (socket.isEndOfStreamReached()) {
                System.out.println("Socket closed");
                socket.close();
                return;
            }
        }

        FileReceiver receiver;
        FileMetadata fileMetadata = new FileMetadata(Paths.get(path), bytes, position);
        try {
            receiver = new FileReceiver(socket.getSocketChannel(), fileMetadata);
        } catch (FileAlreadyExistsException e) {
            System.out.println("File already exists. Creating new dir: ");
            Path newDir = Paths.get(fileMetadata.getFilePath().getParent().toString(), "new-duplicated");
            if (!Files.isDirectory(newDir)) {
                Files.createDirectory(newDir);
            }
            Path newFilePath = Paths.get(newDir.toString(), fileMetadata.getFilePath().getFileName().toString());
            fileMetadata = new FileMetadata(newFilePath, fileMetadata.getSize(), fileMetadata.getPosition());
            receiver = new FileReceiver(socket.getSocketChannel(), fileMetadata);
            System.out.println(newFilePath);
        }


        socket.enqueue("READY\r\n");
        socket.sendMessages();


        receiver.receive();

        System.out.println("Socket closed");
        receiver.close();
    }

}
