# Telecom

Project for Fundamentals of Telecommunication course @ Vietnamese-German University

Server: [IRCServer](https://github.com/dentou/Telecom/tree/master/IRCServer)\
CLI IRC Client: [IRCClient](https://github.com/dentou/Telecom/tree/master/IRCClient)\
GUI Client: [IRCClientGUI](https://github.com/dentou/Telecom/tree/master/IRCClientGUI)\
Protocol Descriptions: [ProtocolDescriptions](https://github.com/dentou/Telecom/blob/master/IRCServer/README.md)

# Installation instructions (@NguyenHuuMinhTri, @NguyenHuyThong)
1. Install [Maven](https://maven.apache.org/guides/).
2. Clone this project.
3. Open command prompt/terminal and change directory to the module you want to run (IRCServer, IRCClient or IRCClientGUI).

In Windows
```
cd <INSTALL_LOCATION>\Telecom\<MODULE>
```
In Linux
```
cd <INSTALL_LOCATION>/Telecom/<MODULE>
```
where `<INSTALL_LOCATION>` is where you clone this project, and `<MODULE>` might be `IRCServer`, `IRCClient` or `IRCClientGUI`.

4. Build the project using
```
mvn package 
```
5. Run class in JVM
```
mvn exec:java -Dexec.mainClass="com.github.dentou.Main"
```
***Note:*** Replace `com.github.dentou.Main` by `com.github.dentou.MainApp` for `IRCClientGUI`.


# License
This project is licensed under the [MIT license](https://github.com/dentou/Telecom/blob/master/LICENSE.md).
