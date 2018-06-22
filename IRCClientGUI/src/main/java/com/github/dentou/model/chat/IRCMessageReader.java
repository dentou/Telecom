package com.github.dentou.model.chat;

import com.github.dentou.model.constants.IRCConstants;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

public class IRCMessageReader { // todo handle partial read (for file transfer)
    private ByteBuffer buffer = ByteBuffer.allocate(IRCConstants.MESSAGE_BUFFER_SIZE);
    private IRCSocket ircSocket;
    private int bufferPosition = 0;


    public IRCMessageReader(IRCSocket ircSocket) {
        this.ircSocket = ircSocket;
    }

    public Queue<String> read() throws IOException {
        int totalBytesRead = ircSocket.read(this.buffer);
        buffer.flip();
        Queue<String> messages = parseMessages(this.buffer.array(), 0, buffer.limit());
        buffer.position(bufferPosition);
        buffer.compact();
        return messages;
    }

    public Queue<String> parseMessages(byte[] src, int startIndex, int endIndex) {
        //System.out.println(src);
        int beginOfMessage = startIndex;
        Queue<String> messages = new LinkedList<>();

        while (true) {
            //Find end of message
            int endOfMessage = findNextLineBreak(src, beginOfMessage, endIndex);
            //System.out.println("End of message: " + endOfMessage);
            if (endOfMessage == -1) {
                break;
            }
            String message = new String(Arrays.copyOfRange(src, beginOfMessage, endOfMessage - 1)); // Ignore line break
            messages.add(message);
            beginOfMessage = endOfMessage + 1;

        }
        this.bufferPosition = beginOfMessage;
        //System.out.println(messages);
        return messages;

    }

    public int findNextLineBreak(byte[] src, int startIndex, int endIndex) {
        for (int index = startIndex; index < endIndex; index++) {
            if (src[index] == '\n') {
                if (src[index - 1] == '\r') {
                    return index;
                }
            }
        }
        return -1;
    }

}
