package com.github.dentou;

public class IRCConstants {
    public static final int SERVER_PORT = 6667;

    public static final int KB = 1024; // bytes
    public static final int MB = 1024 * KB;

    public static final int MESSAGE_BUFFER_SIZE = 10 * KB; // bytes

    public enum CommandResponse {
        RPL_WELCOME("001"),
        RPL_YOURHOST("002"),
        RPL_CREATED("003")

        ;

        private String numericCode;

        CommandResponse(String numericCode) {
            this.numericCode = numericCode;
        }

        public String getNumericCode() {
            return this.numericCode;
        }
    }


    public enum ErrorReplies {
        ERR_NICKNAMEINUSE("433")

        ;

        private String numericCode;

        ErrorReplies(String numericCode) {
            this.numericCode = numericCode;
        }

        public String getNumericCode() {
            return this.numericCode;
        }
    }

}
