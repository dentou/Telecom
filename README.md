# Telecom (draft)

## Table of Contents

* [Protocol Description](#protocol-description)
	* [Identifiers](#identifiers)
		* [Users](#users)
		* [Channels](#channels)
    * [Message Format](#message-format)
    	* [Requests](#requests)
    	* [Relay messages](#relay-messages)
    	* [Replies](#replies)
    * [Message Details](#message-details)
    	* [Connection registration](#connection-registration)
    		* [NICK](#nick)
    		* [USER](#user)
    		* [QUIT](#quit)
    	* [Channel operations](#channel-operations)
    		* [JOIN](#join)
    		* [PART](#part)
    		* [TOPIC](#topic)
 			* [KICK](#kick)
 			* [LIST](#list)
    	* [Sending message](#sending-message)
    		* [PRIVMSG](#privmsg)
    	* [User based queries](#user-based-queries)
    	 	* [WHO](#who)
    	 	* [WHOIS](#whois)
    	* [Miscellaneous messages](#miscellaneous-messages)
    		* [PING and PONG](#ping-and-pong)
    * [Reply Details](#reply-details)
    	* [General Format](#general-format)
        * [Command Responses](#command-responses)
        * [Error Replies](#error-replies)
* [Example communication](#example-communication)
	* [Connection registration](#connection-registration)



## Protocol Description
### Identifiers
##### Users:
Each user is distinguished from other users by a unique nickname
having a maximum length of nine (9) characters. \
Valid characters in a nickname (also applied to username and user full name): Characters from `a` to `z`, `A` to `Z` and/or numbers from `0` to `9` and/or `_`. \
Example: `th32ong`, `_udo82`

##### Channels
Channels names are strings (beginning with a `#`
character) of length up to fifty (50) characters.
Valid characters in a channels name: Characters from `a` to `z`, `A` to `Z` and/or numbers from `0` to `9` and/or any characters in this sequence`!@#$%^&*()`.\
Example: `#all`, `####`
### Message Format
The format of IRC messages is described in [RFC2812 §2.3](https://tools.ietf.org/html/rfc2812#section-2.3), and can be summarized as follow:

* The IRC protocol is a text-based protocol, meaning that messages are encoded in plain ASCII. 
* A single message is a string of characters with a maximum length of 512 characters. The end of the string is denoted by a CR-LF (Carriage Return - Line Feed) pair (i.e., “\r\n”). There is no null terminator. The 512 character limit includes this delimiter, meaning that a message only has space for 510 useful characters. In our implementation, any message with more than 510 characters (not counting the delimiter) will be truncated, with the last two characters replaced with “\r\n”.

More specific rules for each type of messages are described below.

#### Requests
* A request contains at least two parts: the command and the command parameters. There may be at most 15 parameters. The command and the parameters are all separated by a single ASCII space `  ` character. The following are examples of valid IRC messages:
``` 
NICK dentou

MODE amy +o

PRIVMSG amy :hello

JOIN #vgu

QUIT
```
**Remark:** From now on, unless otherwise stated, all messages end with `\r\n`.

* When the last parameter is prefixed with a colon character, the value of that parameter will be the remainder of the message (including space characters). The following are examples of valid IRC messages with a “long parameter”:
``` 
PRIVMSG thong :Hey huy...

PRIVMSG #vgu :Hello everybody
```

#### Relay messages
Relay messages are messages that server forward to supposed recipient on behalf of sender.
The format of relay messages is similar to that of requests, in the sense that all parts are separated by space character ` `. The difference between these two types is that relay messages always include a prefix before the command and the command parameters (with the command and command parameters taken from original request message). The presence of a prefix is indicated with a single leading colon `:` character. The prefix is used to indicate the origin of the message. For example, when a user sends a message to a channel, the server will forward that message to all the users in the channel, and will include a prefix to specify the user that sent that message originally. The following are examples of valid IRC messages with prefixes:
``` 
:dentou!huy@​vgu.edu.vn PRIVMSG #vgu :Hello everybody

:mintori!tri@​vgu.edu.vn PART #vgu
```

#### Replies
The IRC protocol includes a special type of message called a reply. When a client sends a command to a server, the server will send a reply (except in a few special commands where a reply should not be expected). Replies are used to acknowledge that a command was processed correctly, to indicate errors, or to provide information when the command performs a server query (e.g., asking for the list of users or channels).
A reply is a message with the following components:

* It always includes a prefix.
* The second part will be a three-digit numeric code. The full list of possible replies is specified in [RFC2812 §5](https://tools.ietf.org/html/rfc2812#section-5).
* The third part is always the target of the reply, typically a nick.

The following are examples of valid IRC replies:

``` 
:irc.example.com 001 dentou :Welcome to the Internet Relay Network dentou!huy@​vgu.edu.vn

:irc.example.com 433 * dentou :Nickname is already in use.

:irc.example.org 332 dentou #eeit2015 :A channel for EEIT2015 students
```

**Remark:** Only messages (relay or response) from server can be preceeded with prefix.
### Message Details
#### Connection registration
##### NICK
``` 
NICK <nickname>
```
`NICK` command is used at the beginning of connection to give user a nickname or change the existing
one.

Possible Replies:
``` 
001 RPL_WELCOME
431 ERR_NONICKNAMEGIVEN   
433 ERR_NICKNAMEINUSE
432 ERR_ERRONEUSNICKNAME
```
##### USER
``` 
USER <username> <mode> <unused> :<fullname>
```
The `USER` command is used at the beginning of connection to specify
the username and fullname of a new user.\
`<mode>` and `<unused>` have not been implemented, and thus should be replaced by to asterisk `* *`.\
The `<realname>` may contain space characters.

Possible Replies:
``` 
001 RPL_WELCOME
461 ERR_NEEDMOREPARAMS
```
##### QUIT
``` 
QUIT
```
A client session is terminated with a quit message. If user is on any channels, a `PART` message will be sent to these channels.
#### Channel operations
##### JOIN
``` 
JOIN <channel name>
```
The `JOIN` command is used by a user to request to start listening to
the specific channel.\
If a `JOIN` is successful, the user receives a relayed `JOIN` message as
confirmation and is then sent the channel's topic (`RPL_TOPIC`) and
the list of users who are on the channel (`RPL_NAMREPLY`) including the user joining.\
**Remark 1:** If the channel's topic is empty, `RPL_TOPIC` is ignored.\
**Remark 2:** `RPL_NAMEREPLY` always ends with `RPL_ENDOFNAMES`.

``` 
JOIN 0
```
The `JOIN` command can accept a special parameter `0`, which is
a special request to leave (`PART`) all channels the user is currently a member
of.


Possible Replies:
``` 
332 RPL_TOPIC
353 RPL_NAMEREPLY
366 RPL_ENDOFNAMES
451 ERR_NOTREGISTERED
461 ERR_NEEDMOREPARAMS
403 ERR_NOSUCHCHANNEL
```
##### PART
``` 
PART <channel name>
```
The `PART` command causes the user sending the message to be removed
from the list of members for the given channel.\
Upon leaving, the server will send a relayed `PART` message to all the members of the channel.

Possible Replies:
``` 
451 ERR_NOTREGISTERED
461 ERR_NEEDMOREPARAMS
403 ERR_NOSUCHCHANNEL
442 ERR_NOTONCHANNEL
```
##### TOPIC 
``` 
TOPIC <channel> [:<topic>]
```
**Remark:** The square brackets `[]` denote optional parameters.\
The `TOPIC` command is used to change or view the topic of a channel.
The topic for channel `<channel>` is returned if there is no `<topic>`
given. If the `<topic>` parameter is present, the topic for that
channel will be changed, if this action is allowed for the user
requesting it. If the `<topic>` parameter is an empty string, the
topic for that channel will be removed.

Possible Replies:
``` 
332 RPL_TOPIC
331 RPL_NOTOPIC
451 ERR_NOTREGISTERED
461 ERR_NEEDMOREPARAMS
403 ERR_NOSUCHCHANNEL
442 ERR_NOTONCHANNEL
482 ERR_CHANOPRIVSNEEDED
```
##### KICK
``` 
KICK <channel> <nick>  [:<comment>]
```
The KICK command can be used to request the forced removal of a user
from a channel. It causes the user with `<nick>` to `PART` from the `<channel>` by
force. If a `<comment>` is given, this will be sent along with the relayed `KICK` message.

Possible Replies:
``` 
451 ERR_NOTREGISTERED
461 ERR_NEEDMOREPARAMS
403 ERR_NOSUCHCHANNEL
442 ERR_NOTONCHANNEL
482 ERR_CHANOPRIVSNEEDED
441 ERR_USERNOTINCHANNEL
```
##### LIST
``` 
LIST
```


Possible Replies:
``` 
451 ERR_NOTREGISTERED
461 ERR_NEEDMOREPARAMS
403 ERR_NOSUCHCHANNEL
442 ERR_NOTONCHANNEL
```

#### Sending messages
##### PRIVMSG
``` 
PRIVMSG <target> :<text to be sent>
```
`PRIVMSG` is used to send private messages between users, as well as to
send messages to channels. `<msgtarget>` is usually the nickname of
the recipient of the message, or a channel name.

Possible Replies:
``` 
451 ERR_NOTREGISTERED
461 ERR_NEEDMOREPARAMS
431 ERR_NONICKNAMEGIVEN
412 ERR_NOTEXTTOSEND
401 ERR_NOSUCHNICK
404 ERR_CANNOTSENDTOCHAN
```
#### User based queries
##### WHO
``` 
WHO
```
The WHO command is used to query information about all current users in the server. 

Possible Replies:
``` 
352 RPL_WHOREPLY
:<servername> <numeric code> <nick of requester> <nick> <user name> :<fullname>

315 RPL_ENDOFWHO
:<servername> <numeric code> <nick of requester> :End of WHO list
```
Example (`C` represents message from Client to Server, `S` represents message from Server to Client):
``` 
C: WHO
S: :irc.example.com 352 thong dentou huy :huy tran
S: :irc.example.com 352 thong mintori tri :minh tri
S: :irc.example.com 315 :End of WHO list
```
##### WHOIS
``` 
WHOIS <nick>
```
This command is used to query information about particular user. If there is not enough parameters, the request is ignored.

Possible Replies:
``` 
451 ERR_NOTREGISTERED
401 ERR_NOSUCHNICK
311 RPL_WHOISUSER
318 RPL_ENDOFWHOIS
```
#### Miscellaneous messages
##### PING and PONG
``` 
PING
PONG
```
The PING command is used to test the presence of an active client or
server at the other end of the connection. Servers send a PING
message at regular intervals if no other activity detected coming
from a connection. If a connection fails to respond to a PING
message within a set amount of time, that connection is closed. **IMPLEMENT LATER**

### Reply Details
#### General Format
```
:<server name> <numeric code> <nick of requester> <contents> <contents> ...
```
#### Command Responses
| Numeric Code | Reply          | Contents                                                                                                            |
|--------------|----------------|---------------------------------------------------------------------------------------------------------------------|
| 001          | RPL_WELCOME    | `:Welcome to the Internet Relay Network <nick>!<user>@<host>`                                                       |
| 311          | RPL_WHOISUSER  | `<nick> <user name> * * :<full name>`                                                                               |
| 318          | RPL_ENDOFWHOIS | `:End of WHOIS list`                                                                                                |
| 322          | RPL_LIST       | `<channel name> <number of members> :<topic>`                                                                       |
| 323          | RPL_LISTEND    | `:End of LIST`                                                                                                      |
| 331          | RPL_NOTOPIC    | `<channel name> :No topic is set`                                                                                   |
| 332          | RPL_TOPIC      | `<channel name> :<topic>`                                                                                           |
| 352          | RPL_WHOREPLY   | `<nick> <user name> * * <full name>`                                                                                |
| 315          | RPL_ENDOFWHO   | `:End of WHO list`                                                                                                  |
| 353          | RPL_NAMEREPLY  | `= <channel name> :[@/+]<nick> [@/+/]<nick> ...` <br>**Note:** `=` denotes public channel, `@` denotes admin of the channel and `+` denotes moderator of the channel  |
| 366          | RPL_ENDOFNAMES | `<channel name>` :End of NAMES list` 

#### Error Replies
| Numeric code | Reply                 | Contents                                                                          |
|--------------|-----------------------|-----------------------------------------------------------------------------------|
| 401          | ERR_NOSUCHNICK        | `<nick/channel name> :No such nick/channel`                                       |
| 403          | ERR_NOSUCHCHANNEL     | `<channel name> :No such channel` <br>**Note:** Indicate channel name is invalid |
| 404          | ERR_CANNOTSENDTOCHAN  | `<channel name> :Cannot send to channel`                                          |
| 411          | ERR_NORECIPIENT       | `:No recipient given`                                                             |
| 412          | ERR_NOTEXTTOSEND      | `:No text to send`                                                                |
| 421          | ERR_UNKNOWNCOMMAND    | `<command> :Unknown command`                                                      |
| 431          | ERR_NONICKNAMEGIVEN   | `:No nickname given`                                                              |
| 432          | ERR_ERRONEOUSNICKNAME | `<nick> :Erroneous nickname`                                                      |
| 433          | ERR_NICKNAMEINUSE     | `<nick> :Nickname is already in use`                                              |
| 441          | ERR_USERNOTINCHANNEL  | `<nick> <channel> :They aren't on that channel`                                   |
| 442          | ERR_NOTONCHANNEL      | `<channel> :You're not on that channel`                                           |
| 451          | ERR_NOTREGISTERED     | `:You have not registered`                                                        |
| 461          | ERR_NEEDMOREPARAMS    | `<command> :Not enough parameters`                                                |
| 482          | ERR_CHANOPRIVSNEEDED  | `<channel> :You're not channel operator`                                          |


## Example communication
### Connection registration
When an IRC client connects to an IRC server, it must first register its connection. This is done by sending two messages (in any order): `NICK` and `USER`. `NICK` specifies the user’s nick, and `USER` provides additional information about the user. More specifically, `USER` specifies the user’s username and the user’s full name. For example:
``` 
NICK dentou
USER kuri * * :kuri weiss
```
In this example, user's nick is `dentou`. His/her username is `kuri` and full name is `kuri weiss`. The two asterisk `* *` are placeholders for future implementations.\
**Remark:** User nick must be mutually distinguished,  i.e. Two user cannot have the same nick (although they CAN have the same username and/or full name).

Upon success, the server will send an `RPL_WELCOME` reply (which is assigned code `001`) to the client who made the request. For example:
``` java
:irc.example.com 001 dentou :Welcome to the Internet Relay Network dentou!huy@​vgu.edu.vn
```
In the example,

* `:irc.example.com` The prefix indicating that this reply originates in server irc.example.com.
* `001`: The numeric code for `RPL_WELCOME`.
* `dentou`: The first parameter which, in reply messages, must always be the nick of the user this reply is intended for.
* `:Welcome to the Internet Relay Network dentou!huy@​vgu.edu.vn`: The second parameter. The content of this parameter is specified in [RFC2812 §5](https://tools.ietf.org/html/rfc2812#section-5). (Note that the specification in RFC2812 omits the first parameter, which is always the recipient of the reply and only lists the second and subsequent (if any) parameters.)

Other replies can be sent in case of errors.



