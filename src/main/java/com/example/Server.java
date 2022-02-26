package com.example;

import java.io.*;
import java.net.*;
import java.util.ArrayList;

import org.json.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Server {
    private ServerSocket serverSocket;
    private static ArrayList<Chatroom> chatrooms = new ArrayList<Chatroom>();

    public Server() {
        chatrooms.add(new Chatroom("MainHall-s1", new Owner(""), this));
        chatrooms.add(new Chatroom("MainHall-2", new Owner("Maria"), this));
    }

    public void start(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        while (true)
            new ClientHandler(serverSocket.accept(), this).start();
    }

    public static void main(String[] args) throws IOException {
        Server server = new Server();
        server.start(4444);
    }

    public void stop() throws IOException {
        serverSocket.close();
    }

    public static class ClientHandler extends Thread {
        private Server server;
        private Socket clientSocket;
        private PrintWriter out;
        private BufferedReader in;
        private String newClient;
        Client client;

        public ClientHandler(Socket socket, Server server) {
            this.server = server;
            this.clientSocket = socket;
        }

        // new id
        public boolean isAlphaNumeric(String s) {
            return s != null && s.matches("^[a-zA-Z0-9]*$");
        }

        public boolean checkOtherServers(String identity) {
            return false;
        }

        public boolean checkRoomIdentity(String roomid) {
            return false;
        }

        // check alphanumeric
        // 3<length<16
        // check identity in other servers
        // broadcast roomchange message to chat room members
        public JSONObject newIdentify(String identity) {
            JSONObject newIdentity = new JSONObject();
            newIdentity.put("type", "newidentity");
            newIdentity.put("approved", "true");
            if ((isAlphaNumeric(identity) == false) || 3 > identity.length() || identity.length() > 16
                    || (checkOtherServers(identity) == true)) {
                System.out.println("New connection refused");
                newIdentity.replace("approved", "false");
            }

            return newIdentity;
        }

        public void run() {
            try {
                JSONParser parser = new JSONParser();
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                in = new BufferedReader(
                        new InputStreamReader(clientSocket.getInputStream()));

                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    System.out.println(inputLine);

                    try {
                        JSONObject json = (JSONObject) parser.parse(inputLine);

                        if (json.get("type").equals("newidentity")) {
                            String identity = (String) json.get("identity");
                            JSONObject newIdentify = newIdentify(identity);
                            out.println(newIdentify);

                            if (newIdentify.get("approved").equals("true")) {
                                client = new Client(identity, server, chatrooms.get(0), this);
                                chatrooms.get(0).addClient(client);
                                broadcastRoomMessage(identity, "", "MainHall-s1", chatrooms.get(0));
                            }
                        } else if (json.get("type").equals("deleteroom")) {
                            String roomid = (String) json.get("roomid");
                            deleteroom(client, roomid);
                        } else if (json.get("type").equals("message")) {
                            String identity = client.id;
                            Chatroom room = client.chatroom;

                            JSONObject message = new JSONObject();
                            message.put("type", "message");
                            message.put("identity", identity);
                            message.put("content", json.get("content"));

                            for (int i = 0; i < room.clients.size(); i++) {
                                Client receiver = chatrooms.get(chatrooms.indexOf(room)).clients.get(i);
                                if (client.id.equals(receiver.id)) {
                                    continue;
                                }
                                receiver.clientHandler
                                        .sendResponse(message);
                            }
                        } else if (json.get("type").equals("list")) {
                            JSONObject response = new JSONObject();
                            response.put("type", "roomlist");
                            JSONArray jsonarray = new JSONArray();
                            for (int i = 0; i < chatrooms.size(); i++) {
                                jsonarray.add(chatrooms.get(i).name);
                            }
                            response.put("rooms", jsonarray);
                            out.println(response);
                        } else if (json.get("type").equals("who")) {
                            JSONObject response = new JSONObject();
                            response.put("type", "roomcontents");
                            response.put("roomid", client.chatroom.name);

                            // roomid, identities, owner
                            JSONArray jsonarray = new JSONArray();
                            for (int i = 0; i < client.chatroom.clients.size(); i++) {
                                jsonarray.add(client.chatroom.clients.get(i).id);
                            }
                            response.put("identities", jsonarray);
                            response.put("owner", client.chatroom.owner.id);
                            out.println(response);
                        } else if (json.get("type").equals("createroom")) {
                            String roomid = (String) json.get("roomid");
                            if (!checkRoomIdentity(roomid)) {
                                JSONObject response = new JSONObject();
                                response.put("type", "createroom");
                                response.put("roomid", roomid);
                                response.put("approved", "true");
                                out.println(response);

                                chatrooms.add(new Chatroom(roomid, new Owner(client.id), server));
                                joinRoom(client, roomid);
                                // broadcastRoomMessage(client.id, client.chatroom.name, roomid, client);

                            } else {
                                JSONObject response = new JSONObject();
                                response.put("type", "createroom");
                                response.put("roomid", roomid);
                                response.put("approved", "false");
                                out.println(response);
                            }
                        } else if (json.get("type").equals("joinroom")) {
                            joinRoom(client, (String) json.get("roomid"));
                        } else if (json.get("type").equals("quit")) {
                            quit();
                        }

                        // else{
                        // JSONObject response = new JSONObject();
                        // response.put("type", "message");
                        // response.put("else", "else");
                        // out.println(response.toString());
                        // }
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }

                }

                System.out.println("Client disconnected abruptly");

                quit();
            } catch (IOException e) {

            }

        }

        public void sendResponse(JSONObject res) {
            out.println(res);
        }

        void quit(){
            Chatroom current = chatrooms.get(chatrooms.indexOf(client.chatroom));
                            if (current.owner.id.equals(client.id)) {
                                deleteroom(client, client.chatroom.name);
                            }

                            broadcastRoomMessage(client.id, client.chatroom.name, "", client.chatroom);

                            client.chatroom.removeClient(client);

                            try {
                                in.close();
                                out.close();
                                clientSocket.close();
                            } catch (IOException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                            
                            
        }

    }

    public static int getChatroomByProperty(String roomid) {
        for (int i = 0; i < chatrooms.size(); i++) {
            if (chatrooms.get(i) != null && chatrooms.get(i).name.equals(roomid)) {
                return i;
            }
        }
        return -1;// not there is list
    }

    public static void joinRoom(Client client, String roomid) {
        // need to implement not successfull
        String former = client.chatroom.name;
        client.chatroom.removeClient(client);
        broadcastRoomMessage(client.id, former, roomid, client.chatroom);
        client.chatroom = chatrooms.get(getChatroomByProperty(roomid));
        client.chatroom.addClient(client);
        broadcastRoomMessage(client.id, former, roomid, client.chatroom);
    }

    public static void deleteroom(Client client, String roomid) {
        int index = getChatroomByProperty(roomid);

        Chatroom current = chatrooms.get(index);

        if (current.owner.id.equals(client.id)) {
            JSONObject response = new JSONObject();
            response.put("type", "deleteroom");
            response.put("roomid", roomid);
            response.put("approved", "true");
            client.clientHandler.out.println(response);

            for (int i = 1; i < current.clients.size(); i++) {
                joinRoom(current.clients.get(i), chatrooms.get(0).name);
            }
            joinRoom(client, chatrooms.get(0).name);
        } else {
            JSONObject response = new JSONObject();
            response.put("type", "deleteroom");
            response.put("roomid", roomid);
            response.put("approved", "false");
            client.clientHandler.out.println(response);
        }
    }

    public static void broadcastRoomMessage(String identity, String former, String roomid, Chatroom room) {
        for (int i = 0; i < room.clients.size(); i++) {
            JSONObject res = new JSONObject();
            res.put("type", "roomchange");
            res.put("identity", identity);
            res.put("former", former);
            res.put("roomid", roomid);
            chatrooms.get(chatrooms.indexOf(room)).clients.get(i).clientHandler.sendResponse(res);
        }
    }
}
