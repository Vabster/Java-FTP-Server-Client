# Java-FTP-Server-Client
An implementation of an FTP Server and Client

After compiling both files run the server. Then run the client
Example:
java myftpserver 8000 8001
java myftp [ipaddress] 8000 8001

8000 in each corresponds to the normal port number and 8001 corresponds to the terminate port number.

This program allows users to get files from the server and put files on to the server. It also allows other commands as well such as ls, cd, mkdir, delete, and termiante.

It uses a multithreaded approach in order to allow multiple clients to be connected to the same server at once. A client can add an & symbol after either a get or put command  in order to run the command in the background. If this is done the server will send a terminate ID which would allow the user to stop the server from performing the action.
