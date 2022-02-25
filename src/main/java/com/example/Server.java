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
        chatrooms.add(new Chatroom("MainHall-1", new Owner(1), this));
        chatrooms.add(new Chatroom("MainHall-2", new Owner(1), this));
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

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        public void run() {
            try{
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(
              new InputStreamReader(clientSocket.getInputStream()));
            
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                JSONParser parser = new JSONParser();  
                try {
                    JSONObject json = (JSONObject) parser.parse(inputLine);
                    if(json.get("type").equals("list")){
                        JSONObject response = new JSONObject();
                        response.put("type", "roomlist");
                        JSONArray jsonarray = new JSONArray();
                        for (int i = 0; i < chatrooms.size(); i++) {
                            jsonarray.add(chatrooms.get(i).name);
                    }
                        response.put("rooms",jsonarray);
                        out.println(response.toString());
                        System.out.println(inputLine);
                    }
                    else{
                        JSONObject response = new JSONObject();
                        response.put("type", "message");
                        response.put("else", "else");
                        out.println(response.toString());
                    } 
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
