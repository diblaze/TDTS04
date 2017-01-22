//
// Created by deniii on 1/22/17.
//
#include <string>

#ifndef LAB_2_SERVERHANDLER_H
#define LAB_2_SERVERHANDLER_H


#include <netdb.h>
#include <cstring>
#include <sys/socket.h>
#include <unistd.h>
#include <errno.h>
#include <iostream>

class ServerHandler
{
public:
	struct addrinfo _host_info;
	struct addrinfo *_host_info_list;
	int _socketfd;
	std::string _addr;
	//maybe change _port to cstring?
	int _port;

	bool debug = true;

	void InitServer();

	void BindPort();

	void StartListen();

	int AcceptConnection();

	void StartConnection();

	int SendMessage(std::string message);

	std::string Receive(int bufferSize = 1000);

	ServerHandler(std::string addr, int port);

	~ServerHandler();


};


#endif //LAB_2_SERVERHANDLER_H
