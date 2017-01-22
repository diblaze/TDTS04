cmake_minimum_required(VERSION 3.6)
project(Lab_2)

set(CMAKE_CXX_STANDARD 11)

set(CLIENT_FILES main.cpp ServerHandler.cpp ServerHandler.h)
add_executable(proxy ${CLIENT_FILES})

#set(CLIENT_FILES main.cpp)
#add_executable(Client ${CLIENT_FILES})