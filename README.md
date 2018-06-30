# Telecom

Project for Fundamentals of Telecommunication course @ Vietnamese-German University

[IRCServer](https://github.com/dentou/IRCServer/tree/aa0082f01279c5cae5e2138e16fd52bd8c11d8a0)\
[IRCClient](https://github.com/dentou/IRCClient/tree/a4dd358c9c10c91f23ed54d3afde76cba52fc193)\
[IRCClientGUI](https://github.com/dentou/IRCClientGUI/tree/dd616c35ae93c4fdab254966af73c9d01e5ca043)\
[Protocol Descriptions](https://github.com/dentou/Telecom/blob/master/IRCServer/README.md)

# Installation instructions
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
mvn clean package 
```
5. Run class in JVM
```
mvn exec:java -Dexec.mainClass="com.github.dentou.Main"
```
***Note:*** Replace `com.github.dentou.Main` by `com.github.dentou.MainApp` for `IRCClientGUI`.


# License
This project is licensed under the [MIT license](https://github.com/dentou/Telecom/blob/master/LICENSE.md).
