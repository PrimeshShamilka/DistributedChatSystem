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
    private static ArrayList<Chatroom> chatrooms= new ArrayList<Chatroom>();
    public Server(){
        chatrooms.add(new Chatroom("MainHall-1", new Owner("Adel"), this));
        chatrooms.add(new Chatroom("MainHall-2", new Owner("Maria"), this));
    }
    public void start(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        while (true)
            new ClientHandler(serverSocket.accept()).start();
    }
    public static void main(String[] args) throws IOException {
        Server server=new Server();
        server.start(6666);
    }
    public void stop() throws IOException {
        serverSocket.close();
    }

    private static class ClientHandler extends Thread {
        private Socket clientSocket;
        private PrintWriter out;
        private BufferedReader in;
        private String newClient;
        String identity;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }


        //new id
        public boolean isAlphaNumeric(String s) {
            return s != null && s.matches("^[a-zA-Z0-9]*$");
        }
    
        public boolean checkOtherServers(String identity){
            return false;
        }
        public void broadcastRoomMessage(String identity, String roomid){
    
        }

        // check alphanumeric
        // 3<lenght<16
        // check identity in other servers
        // broadcast roomchange message to chat room members
        public JSONObject newIdentify(String identity){
            JSONObject newIdentity = new JSONObject();
            newIdentity.put("type", "newidentity");
            newIdentity.put("approved", true);
            if ((isAlphaNumeric(identity)==false) || (3>identity.length() && identity.length()>16) || (checkOtherServers(identity)==true)){
                newIdentity.replace("approved", false);
            }
    
            broadcastRoomMessage(identity, "MainHall-s1");
            return newIdentity;
        }

        public void run() {
            try{
            JSONParser parser = new JSONParser(); 
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(
              new InputStreamReader(clientSocket.getInputStream()));

            newClient = in.readLine();
            JSONObject clientJsonObject;
            
            try {
                clientJsonObject = (JSONObject) parser.parse(newClient);
                String typeClient = (String) clientJsonObject.get("type");
                if (typeClient.equals("newidentity")){
                    identity = (String) clientJsonObject.get("identity");
                    //System.out.println(identity+" connected!");
                    JSONObject newIdentify = newIdentify(identity);
                    //System.out.println(newIdentify);
                    if(newIdentify.get("approved").equals(true)){
                        JSONObject message = new JSONObject();
                        message.put("type","message");
                        message.put("identity", identity);
                        message.put("content","moves to MainHall-s1");
                        out.println(message);
                    }
                    else{
                        //out.close();
                        System.out.println("Not Approved!");
                    }
                }

              } catch (ParseException e1) {
                  //
              }
            
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                System.out.println(inputLine);

                try {
                    JSONObject json = (JSONObject) parser.parse(inputLine);

                    if(json.get("type").equals("deleteroom")){
                        String roomid = (String) json.get("roomid");

                        for(Chatroom ch:chatrooms){
                            if (ch.owner.identity.equals(identity)){
                                System.out.println("same owner");
                            }
                            else{
                                System.out.println(identity + " is not owner of this chatroom");
                            }

                        }

                    }

                    if(json.get("type").equals("message")){
                        JSONObject message = new JSONObject();
                        message.put("type","message");
                        message.put("identity", "Adel");
                        message.put("content", json.get("content"));
                        out.println(message);
                    }

                    
                    if(json.get("type").equals("list")){
                        JSONObject response = new JSONObject();
                        response.put("type", "roomlist");
                        JSONArray jsonarray = new JSONArray();
                        for (int i = 0; i < chatrooms.size(); i++) {
                            jsonarray.add(chatrooms.get(i).name);
                        }
                        response.put("rooms",jsonarray);
                        out.println(response);
                        
                        System.out.println(inputLine);
                    }
                    // else{
                    //     JSONObject response = new JSONObject();
                    //     response.put("type", "message");
                    //     response.put("else", "else");
                    //     out.println(response.toString());
                    // } 
                } catch (ParseException e) {
                    e.printStackTrace();
                }  
                
            }

            in.close();
            out.close();
            clientSocket.close();
        }
        catch (IOException e) {

        }
    
        
    
    }
    
    }
}
