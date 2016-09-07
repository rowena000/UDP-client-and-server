Read Me

File list:
	1. UDPClient.java - The client class
	2. UDPServer.java - The server class
	3. Other *.java files are classes that used by UDPClient.java
	4. XXX.jar file. This is used for import to Eclipse in case there is no java setup in your machine.


Steps to run:

Start the server
	1. Copy the file to a machine. 
	2. Go to the file directory, execute "make UDPServer"
	3. Execute "java UDPServer"

	When the server starts, server will print out it's ip and port as below.
		"Server listen on: 192.168.0.106:62210"

Run the client.
	The client takes 4 params in order. First is server's ip address, second is server's port, third is the packet size for this run, last one is the max window size.
	According to requirements, the packets size for our testing are 32, 512, 1400, 8192.
	For each packet size, client will run it for different MAXWIN configurations. From 1 to 256 and increase by 2x.

	1. Copy file to a different machine in the same LAN
	2. Go to the file directory, execute "make UDPClient", this will compile all the classes that client needs.
	3. Execute client using the command. 
			java UDPClient {server address} {server port} {packet size} {max win}
		For example, 
			java UDPClient 192.168.0.106 62210 32 8

	4. After running, client will print out a report on console. 

	* The client do has the ability to run all the configuration at one time. Since it's time consuming, I commented out the part. We can enabled the auto-run by uncomment the code.

	* The client has the ablity to print log. To enable that, please open UDPClient.java, change the debugEnble variable to be true. It will print a very detail log on each send/recevie packet, timeout and CONGWIN increase or drop.