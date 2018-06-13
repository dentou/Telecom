# Server Descriptions

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
having a maximum length of 20 characters. \
Valid characters in a nickname (also applied to username and user full name): Characters from `a` to `z`, `A` to `Z` and/or numbers from `0` to `9` and/or `_`. \
Example: `th32ong`, `_udo82`

##### Channels

Channels names are strings (beginning with a `#`
character) of length up to fifty (50) characters.
Valid characters in a channels name: Characters from `a` to `z`, `A` to `Z` and/or numbers from `0` to `9` and/or any characters in this sequence`!@#$%^&*()`.\
Example: `#all`, `#vgu`

### Message Format

The format of IRC messages is described in [RFC2812 §2.3](https://tools.ietf.org/html/rfc2812#section-2.3), and can be summarized as follow:

* The IRC protocol is a text-based protocol, meaning that messages are encoded in plain ASCII. 
* The end of a message is denoted by CR-LF (Carriage Return - Line Feed) pair (i.e. `\r\n`)
* A single message is a string of characters with a maximum length of 512 characters, including the CR-LF, meaning that a message only has space for 510 useful characters. In our implementation, any message with more than 510 characters (not counting the delimiter) will be truncated, with the last two characters replaced with `\r\n`.

**Remark:** For the rest of this document, unless otherwise stated, all messages implicitly end with CR-LF (`\r\n`).

More specific rules for each type of messages are described below.

#### Requests

A request contains at least two parts: the command and the command parameters. The command and the parameters are all separated by a single ASCII space  ` ` character. The following are examples of valid IRC messages:

```
NICK dentou

PRIVMSG amy :hello

JOIN #vgu

QUIT
```

When the last parameter is prefixed with a colon character, the value of that parameter will be the remainder of the message (including space characters). The following are examples of valid IRC messages with a “long parameter”:

``` 
PRIVMSG thong :Hey huy...

PRIVMSG #vgu :Hello everybody
```

#### Relay messages

Relay messages are messages that server forward to supposed recipient(s) on behalf of the sender.\
The format of relay messages is similar to that of requests, in the sense that all parts are separated by space ` ` character.\
The difference between these two types is that relay messages always include a prefix before the command and the command parameters (with the command and command parameters taken from original request message). The presence of a prefix is indicated with a single leading colon `:` character. The prefix is used to indicate the origin of the message. For example, when a user sends a message to a channel, the server will forward that message to all the users in the channel, and will include a prefix to specify the user that sent that message originally. The following are examples of valid IRC messages with prefixes:

```
:dentou!huy@​vgu.edu.vn PRIVMSG #vgu :Hello everybody

:mintori!tri@​vgu.edu.vn PART #vgu
```

#### Replies

The IRC protocol includes a special type of message called a reply. When a client sends a command to a server, the server will send a reply (except in a few special commands where a reply should not be expected). Replies are used to acknowledge that a command was processed correctly, to indicate errors, or to provide information when the command performs a server query (e.g., asking for the list of users or channels).
A reply is a message with the following components:

* The first part is always a prefix indicating the server.
* The second part will be a three-digit numeric code corresponding to the type of reply. The full list of possible replies is specified in [RFC2812 §5](https://tools.ietf.org/html/rfc2812#section-5).
* The third part is always the nick of user who made the request (if the user has not registered, the nick is replaced by an asterisk `*`)

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

* Request accepted

``` 
001 RPL_WELCOME (only when both NICK and USER requests have been received and accepted)
```

* Errors

```
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
`<mode>` and `<unused>` have not been implemented, and thus should be replaced by to asterisks `* *`.\
`<fullname>` may contain space characters.

Possible Replies:

* Request accepted

``` 
001 RPL_WELCOME (only when both NICK and USER requests have been received and accepted)
```

* Errors

```
461 ERR_NEEDMOREPARAMS
```

##### QUIT

``` 
QUIT
```

A client session is terminated with a quit message. If user is on any channels, a `PART` message will be sent to all the member in each of these channels (including the requester).

#### Channel operations

> A channel is a collection of users under a common channel name and topic.\
When a message is sent to a channel, it will be delivered to all members of that channel. Only member can send messages to his/her channel.\
Every channel has 1 admin and severals or no moderators (together they are called channel operators). Only the admin or moderators have privilege to change the channel topic, to invite or kick channel members (see details in next few sections).\
The creator of a channel will automatically be the first admin of that channel. When an admin leave (`PART`) his/her channel, another moderator (or a member if the channel has no moderator) will be appointed as the admin of that channel.\
When the last member of the channel leaves, the channel will be destroyed.

##### JOIN

``` 
JOIN <channel name>
```

The `JOIN` command is used by a user to request to join a specific channel.\
If a `JOIN` request is successful, the user receives a relayed `JOIN` message as confirmation and is then sent the channel's topic (`RPL_TOPIC`) and the list of users who are on the channel (`RPL_NAMREPLY`) including the user joining. Relayed `JOIN` message is also sent to all members of the channel.\
**Remark 1:** If the channel's topic is empty, `RPL_TOPIC` is ignored.\
**Remark 2:** `RPL_NAMEREPLY`s always ends with `RPL_ENDOFNAMES`.

``` 
JOIN 0
```

The `JOIN` command can accept a special parameter `0`, which is
a special request to leave (`PART`) all channels the user is currently a member of (See [PART]($part) section for more information).

Possible Replies:

*  Request is accepted

```
Relay message (sent to all members of the channel)
332 RPL_TOPIC
353 RPL_NAMEREPLY
366 RPL_ENDOFNAMES
```

* Errors

```
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

* Request is accepted

``` 
Relay message (sent to all members of the channel, including requester)
```

* Errors

```
451 ERR_NOTREGISTERED
461 ERR_NEEDMOREPARAMS
403 ERR_NOSUCHCHANNEL
442 ERR_NOTONCHANNEL
```

##### TOPIC

```
TOPIC <channel> [:<new topic>]
```

**Remark:** The square brackets `[]` denote optional parameter.

The `TOPIC` command is used to change or view the topic of a channel.
The topic for channel `<channel>` is returned if there is no `<new topic>`
given.\
If the `<new topic>` parameter is present, the topic for that
channel will be changed, if this action is allowed for the user
requesting it (the user must be an admin or a moderator of the channel in order to change the topic).\
If the `<new topic>` parameter is an empty string, the topic for that channel will be removed.\
When the topic of a channel changed (either replaced or removed), the relayed `TOPIC` message is sent to all members of the channel.

Possible Replies:

* Request is accepted

``` 
Relay message (sent to all members of the channel)
332 RPL_TOPIC
331 RPL_NOTOPIC
```

* Errors

```
451 ERR_NOTREGISTERED
461 ERR_NEEDMOREPARAMS
403 ERR_NOSUCHCHANNEL
442 ERR_NOTONCHANNEL
482 ERR_CHANOPRIVSNEEDED
```

##### NAMES

```
NAMES
```

The `NAMES` command is used to list all channels that user has joined and their members. If user has not joined any channel, only the `RPL_ENDOFNAMES` will be sent back.

Replies:

```
353 RPL_NAMEREPLY
366 RPL_ENDOFNAMES
```

##### LIST

``` 
LIST
```

The `LIST` command is used to request information about all channels on the server.

Replies (always):

``` 
322 RPL_LIST
323 RPL_LISTEND
```

##### INVITE /TODO

```
INVITE <nick> <channel>
```

The `INVITE` command is used to invite a user to a channel.\
`<nick>` is the nickname of the person to be invited to the target channel with name `<channel>`.\
Only members of the channel are allowed to invite other users.\
When the channel is in "invite-only" mode, only channel operators (admin or moderators) may issue an `INVITE` command.


##### KICK

``` 
KICK <channel> <nick>  [:<comment>]
```

The KICK command can be used to request the forced removal of a user
from a channel. It causes the user with `<nick>` to `PART` from the `<channel>` by
force. If a `<comment>` is given, this will be sent along with the relayed `KICK` message.\
A channel member cannot kick a member who with higher level, which means the following

* An admin can kick anyone in the channel.
* A moderator can kick any members except for admin and other moderators.
* A member who is not an admin or moderator does not have privilege to kick anyone.

Possible Replies:

* Request accepted

``` 
Relay message (sent to all members of the channel)
```

* Errors

```
451 ERR_NOTREGISTERED
461 ERR_NEEDMOREPARAMS
403 ERR_NOSUCHCHANNEL
442 ERR_NOTONCHANNEL
482 ERR_CHANOPRIVSNEEDED
441 ERR_USERNOTINCHANNEL
```

#### Sending messages

##### PRIVMSG

``` 
PRIVMSG <target> :<text to be sent>
```

`PRIVMSG` is used to send private messages between users, as well as to send messages to channels. `<target>` is usually the nickname of the recipient of the message, or a channel name.\
In case the recipient is a channel, the sender must be a member of that channel.

Possible Replies:

* Request accepted

```
Relay message (sent to recipient)
```

* Errors

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

Replies:

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

* Request accepted

```
311 RPL_WHOISUSER
318 RPL_ENDOFWHOIS
```

* Errors

```
451 ERR_NOTREGISTERED
401 ERR_NOSUCHNICK
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
| 366          | RPL_ENDOFNAMES | `<channel name> :End of NAMES list`

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