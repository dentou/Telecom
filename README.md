# Telecom

## Protocol Description
### Identifiers:
##### Users:
Each user is distinguished from other users by a unique nickname
having a maximum length of nine (9) characters. \
Valid characters in a nickname (also applied to username and user full name): Characters from `a` to `z`, `A` to `Z` and/or numbers from `0` to `9` and/or `_`. \
Example: `th32ong`, `_udo82`

##### Channels:
Channels names are strings (beginning with a `#`
character) of length up to fifty (50) characters.
Valid characters in a channels name: Characters from `a` to `z`, `A` to `Z` and/or numbers from `0` to `9` and/or any characters in this sequence`!@#$%^&*()`.\
Example: `#all`, `####`
### Message Format:
The format of IRC messages is described in [RFC2812 §2.3](https://tools.ietf.org/html/rfc2812#section-2.3), and can be summarized as follow:

* The IRC protocol is a text-based protocol, meaning that messages are encoded in plain ASCII. 
* A single message is a string of characters with a maximum length of 512 characters. The end of the string is denoted by a CR-LF (Carriage Return - Line Feed) pair (i.e., “\r\n”). There is no null terminator. The 512 character limit includes this delimiter, meaning that a message only has space for 510 useful characters. In our implementation, any message with more than 510 characters (not counting the delimiter) will be truncated, with the last two characters replaced with “\r\n”.

More specific rules for each type of messages are described below.

#### Requests
* A request contains at least two parts: the command and the command parameters. There may be at most 15 parameters. The command and the parameters are all separated by a single ASCII space `  ` character. The following are examples of valid IRC messages:
``` java
NICK dentou

MODE amy +o

PRIVMSG amy :hello

JOIN #vgu

QUIT
```

* When the last parameter is prefixed with a colon character, the value of that parameter will be the remainder of the message (including space characters). The following are examples of valid IRC messages with a “long parameter”:
``` java
PRIVMSG thong :Hey Rory...

PRIVMSG #vgu :Hello everybody
```

#### Relay messages
Relay messages are messages that server forward to supposed recipient on behalf of sender.
The format of relay messages is similar to that of requests, in the sense that all parts are separated by space character ` `. The difference between these two types is that relay messages always include a prefix before the command and the command parameters (with the command and command parameters taken from original request message). The presence of a prefix is indicated with a single leading colon `:` character. The prefix is used to indicate the origin of the message. For example, when a user sends a message to a channel, the server will forward that message to all the users in the channel, and will include a prefix to specify the user that sent that message originally. The following are examples of valid IRC messages with prefixes:
``` java
:dentou!huy@​vgu.edu.vn PRIVMSG #vgu :Hello everybody

:mintori!tri@​vgu.edu.vn PART #vgu
```

#### Replies
The IRC protocol includes a special type of message called a reply. When a client sends a command to a server, the server will send a reply (except in a few special commands where a reply should not be expected). Replies are used to acknowledge that a command was processed correctly, to indicate errors, or to provide information when the command performs a server query (e.g., asking for the list of users or channels).
A reply is a message with the following characteristics:

* It always includes a prefix.
* The command will be a three-digit code. The full list of possible replies is specified in [RFC2812 §5](https://tools.ietf.org/html/rfc2812#section-5).
* The first parameter is always the target of the reply, typically a nick.

The following are examples of valid IRC replies:

``` java
:irc.example.com 001 dentou :Welcome to the Internet Relay Network dentou!huy@vgu.edu.vn

:irc.example.com 433 * dentou :Nickname is already in use.

:irc.example.org 332 dentou #eeit2015 :A channel for EEIT2015 students
```

**Remark:** Only messages (relay or response) from server can be preceeded with prefix.
### Message Details:
##### Connection registration
The commands described here are used to register a connection with an
IRC server as a user as well as to correctly disconnect.
