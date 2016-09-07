# define a makefile variable for the java compiler
#
UDPClient: UDPClient.java
	javac -d . UDPClient.java

UDPServer: UDPServer.java
	javac -d . UDPServer.java

runserver: 
	java UDPServer

clean:
	rm -f *.class 