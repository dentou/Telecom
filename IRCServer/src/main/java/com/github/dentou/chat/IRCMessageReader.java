package com.github.dentou.chat;

import com.github.dentou.utils.ServerConstants;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class IRCMessageReader { // todo handle partial read (for file transfer)
    private ByteBuffer buffer = ByteBuffer.allocate(ServerConstants.MESSAGE_BUFFER_SIZE);
    private IRCSocket ircSocket;
    private int bufferPosition = 0;


    public IRCMessageReader(IRCSocket ircSocket) {
        this.ircSocket = ircSocket;
    }

    public List<IRCMessage> read() throws IOException {
        int totalBytesRead = ircSocket.read(this.buffer);
        buffer.flip();
        List<IRCMessage> messages = parseMessages(this.buffer.array(), 0, buffer.limit());
        buffer.position(bufferPosition);
        buffer.compact();
        return messages;
    }

    public List<IRCMessage> parseMessages(byte[] src, int startIndex, int endIndex) {
        //System.out.println(src);
        int beginOfMessage = startIndex;
        List<IRCMessage> messages = new ArrayList<IRCMessage>();

        while (true) {
            //Find end of message
            int endOfMessage = findNextLineBreak(src, beginOfMessage, endIndex);
            //System.out.println("End of message: " + endOfMessage);
            if (endOfMessage == -1) {
                break;
            }
            String content = new String(Arrays.copyOfRange(src, beginOfMessage, endOfMessage - 1)); // Ignore line break
            if (content.length() > 510) {
                content = content.substring(0, 510);
            }
            IRCMessage message = new IRCMessage(content, ircSocket.getId(), 0); // 0 is server
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
