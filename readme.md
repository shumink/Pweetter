# Pweetter

This is a Pweetter software. Compile the using  then modify your peer properties in the properties file you can start playing with it.

## Usage

### Project setup
First, you need to compile the code using the following command.
```
javac *.java
```

Then in the properties file, fill in your ip and port to communicate with your peers. You must also set that for your peers. All peers in the network should have the same property file.

If you wish to add more peers, just attach more peers after the participants field and add their corresponding properties below.

### Running the code
If your port number is less than 1024, you may need to run the program with sudo or as root. Then use the following command to start.
```
java Pweetter
```
Please note that the status board will only display after you send your own status. You may not notice that whether you peer is offline or not because this is a decentrailise chatroom.
 
