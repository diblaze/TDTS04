//
// Created by deniii on 1/22/17.
//

#include "ServerHandler.h"

ServerHandler::ServerHandler(std::string addr, int port)
{
	_addr = addr;
	_port = port;

	memset(&_host_info, 0, sizeof(_host_info)); // Allocated memory might not be empty.

	InitServer();
}

ServerHandler::~ServerHandler()
{
	freeaddrinfo(_host_info_list);
	close(_socketfd);
}

void ServerHandler::InitServer()
{
	if(debug)
	{
		std::cout << "Initilizing the structs..." << std::endl;
	}

	_host_info.ai_family = AF_UNSPEC; //doesn't matter if IPv4 or IPv6
	_host_info.ai_socktype = SOCK_STREAM; //TCP


	_host_info.ai_flags = AI_PASSIVE; //accepts any connection on localhost


	int statusInfo = getaddrinfo(_addr.c_str(), (const char *) _port, &_host_info, &_host_info_list);

	if(statusInfo != 0)
	{
		std::cout << "getaddrinfo() failed" << std::endl;
		std::cout << gai_strerror(statusInfo) << std::endl;
		std::cout << _addr << "\n" << _port << std::endl;
		exit(1);
	}

	if(debug)
	{
		std::cout << "Creating socket to listen on..." << std::endl;
	}

	_socketfd = socket(_host_info_list->ai_family, _host_info_list->ai_socktype, _host_info_list->ai_protocol);

	if(_socketfd == -1)
	{
		std::cout << "Socket couldn't be created" << std::endl;
		std::cout << strerror(errno) << std::endl;
		exit(0);
	}
}

void ServerHandler::BindPort()
{
	if(debug)
	{
		std::cout << "Binding socket..." << std::endl;
	}

	//TODO: Check if port is in use.

	int status = bind(_socketfd, _host_info_list->ai_addr, _host_info_list->ai_addrlen);

	if(status == -1)
	{
		std::cout << "Couldn't bind port!" << std::endl;
		std::cout << strerror(errno) << std::endl;
		exit(1);
	}
}

void ServerHandler::StartListen()
{
	if(debug)
	{
		std::cout << "Listening for connections" << std::endl;
	}

	//BACKLOG == 5
	int status = listen(_socketfd, 5);

	if(status == -1)
	{
		std::cout << "Somethings wrong with listen()" << std::endl;
		std::cout << strerror(errno) << std::endl;
		exit(1);
	}
}

int ServerHandler::AcceptConnection()
{
	if(debug)
	{
		std::cout << "Waiting for client..." << std::endl;
	}

	struct sockaddr_storage their_addr;
	socklen_t addr_size;

	return accept(_socketfd, (struct sockaddr*) &their_addr, &addr_size);

}

std::string ServerHandler::Receive(int bufferSize)
{
	if(debug)
	{
		std::cout << "Waiting to recieve data..." << std::endl;
	}

	//char array to store data buffer
	char inc_data_buff[bufferSize];
	//response from connection
	std::string response;

	//while not received all bytes, add bytes to data buffer.
	ssize_t received_bytes;
	while((received_bytes = recv(_socketfd, inc_data_buff, sizeof(bufferSize), 0)) > 0)
	{
		for(int x = 0; x < received_bytes; ++x)
		{
			response.push_back(inc_data_buff[x]);
		}
		memset(&inc_data_buff[0], 0, sizeof(inc_data_buff)); //clear buffer

	}

	return response;

}

int ServerHandler::SendMessage(std::string message)
{
	return send(_socketfd, message.c_str(), message.size(), 0);

}

void ServerHandler::StartConnection()
{
	if(debug)
	{
		std::cout << "Connecting..." << std::endl;

	}

	int status = connect(_socketfd, _host_info_list->ai_addr, _host_info_list->ai_addrlen);

	if(status == -1)
	{
		std::cout << "connection error" << std::endl;
	}

}
