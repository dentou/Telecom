package com.github.dentou;

public class IRCConstants {
    public static final int SERVER_PORT = 6667;

    public static final int KB = 1024; // bytes
    public static final int MB = 1024 * KB;

    public static final int MESSAGE_BUFFER_SIZE = 10 * KB; // bytes


    public enum Response {
        RPL_WELCOME("001"),
        RPL_YOURHOST("002"),
        RPL_CREATED("003"),
        RPL_MYINFO("004"),
        RPL_AWAY("301"),
        RPL_UNAWAY("305"),
        RPL_NOWAWAY("306"),
        RPL_WHOISUSER("311"),
        RPL_WHOISSERVER("312"),
        RPL_ENDOFWHOIS("318"),
        RPL_WHOISCHANNEL("319"),
        RPL_LIST("322"),
        RPL_LISTEND("323"),
        RPL_CHANNELMODEIS("324"),
        RPL_NOTOPIC("331"),
        RPL_TOPIC("332"),
        RPL_INVITING("341"),
        RPL_WHOREPLY("352"),
        RPL_ENDOFWHO("315"),
        RPL_NAMEREPLY("353"),
        RPL_ENDOFNAMES("366"),
        RPL_USERS("393"),
        RPL_ENDOFUSERS("394"),
        RPL_LUSERCLIENT("251"),
        RPL_LUSEROP("252"),
        RPL_LUSERUNKNOWN("253"),
        RPL_LUSERCHANNELS("254"),
        RPL_LUSERME("255"),
        RPL_TRYAGAIN("263"),




        // Errors
        ERR_NOSUCHNICK("401"),
        ERR_NOSUCHSERVER("402"),
        ERR_NOSUCHCHANNEL("403"),
        ERR_CANNOTSENDTOCHAN("404"),

        ERR_NORECIPIENT("411"),
        ERR_NOTEXTTOSEND("412"),
        ERR_UNKNOWNCOMMAND("421"),
        ERR_NONICKNAMEGIVEN("431"),
        ERR_ERRONEOUSNICKNAME("432"),
        ERR_NICKNAMEINUSE("433"),
        ERR_USERNOTINCHANNEL("441"),
        ERR_NOTONCHANNEL("442"),
        ERR_USERONCHANNEL("443"),

        ERR_NOTREGISTERED("451"),
        ERR_NEEDMOREPARAMS("461"),

        ERR_PASSWDMISMATCH("464"),

        ERR_INVITEONLYCHAN("473"),
        ERR_CHANOPRIVSNEEDED("482"),













        ;


        private String numericCode;

        Response(String numericCode) {
            this.numericCode = numericCode;
        }

        public String getNumericCode() {
            return this.numericCode;
        }
    }

}
