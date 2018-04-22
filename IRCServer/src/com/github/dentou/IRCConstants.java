package com.github.dentou;

public class IRCConstants {
    public static final int SERVER_PORT = 6667;

    public static final int KB = 1024; // bytes
    public static final int MB = 1024 * KB;

    public static final int MESSAGE_BUFFER_SIZE = 10 * KB; // bytes

    public enum CommandResponse {
        RPL_WELCOME("001"),
        RPL_YOURHOST("002"),
        RPL_CREATED("003"),
        RPL_MYINFO("004"),
        RPL_WHOISUSER("311"),
        RPL_WHOISSERVER("312"),
        RPL_ENDOFWHOIS("318"),
        RPL_TOPIC("332"),
        RPL_NAMEREPLY("353"),
        RPL_ENDOFNAMES("366")

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
        ERR_NICKNAMEINUSE("433"),
        ERR_NONICKNAMEGIVEN("431"),
        ERR_NEEDMOREPARAMS("461"),
        ERR_NORECIPIENT("411"),
        ERR_NOTEXTTOSEND("412"),
        ERR_NOSUCHNICK("401"),
        ERR_UNKNOWNCOMMAND("421"),
        ERR_NOTREGISTERED("451")
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
