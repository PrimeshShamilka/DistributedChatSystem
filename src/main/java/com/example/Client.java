package com.example;

public class Client {
    int id = 0;
    Server server;
    Chatroom chatroom;
    
    public Client(int id, Server server, Chatroom chatroom) {
        this.id = id;
        this.server = server;
        this.chatroom = chatroom;
    }
}
