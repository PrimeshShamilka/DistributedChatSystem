package com.example;

import java.util.ArrayList;

public class Chatroom {
    String name;
    Owner owner;
    Server server;
    ArrayList<Client> clients;

    public Chatroom(String name, Owner owner, Server server) {
        this.name = name;
        this.owner = owner;
        this.server = server;
        this.clients = new ArrayList<Client>();
    }
        
}
