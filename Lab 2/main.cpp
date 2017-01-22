//
// Created by deniii on 1/22/17.
//

//http://www.linuxhowtos.org/data/6/server.c
//http://www.linuxhowtos.org/data/6/client.c

#include <cstdlib>
#include <iostream>
#include "ServerHandler.h"

#define PORT 9999

int main(int argc, char* argv[])
{

	//check command-line for port argument (default to 9999)
	int port = PORT;
	if(argc > 1)
	{
		//TODO: Check min and max port.
		port = atoi(argv[1]);
	}

	std::cout << "Server is starting on port: " << port << std::endl;

	//start server
	ServerHandler serverHandler("127.0.0.1", port);
	serverHandler.BindPort();
	serverHandler.StartListen();


	//main loop
	while(1)
	{
		int curr_fd = serverHandler.AcceptConnection();
		//check if is child
		if(!fork())
		{
			std::string response = serverHandler.Receive();
			if(response.length() > 0)
			{
				//pass filter?
				//get host
				//get URI
				//modify request?
				std::string request = "";

				ServerHandler serverToWeb();
				serverToWeb().StartConnection();
				serverToWeb().SendMessage(request);
			}
			close(curr_fd);
			exit(0);
		}
		close(curr_fd);
	}

	std::cout << "See you soon!" << std::endl;

	return 0;

}