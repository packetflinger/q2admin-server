/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pf.q2admin;

import pf.q2admin.message.ClientMessage;

/**
 *
 * @author joe
 */
public class ClientWorker implements Runnable {
    
    ClientMessage msg;
    Server parent;

    public ClientWorker(ClientMessage msg, Server parent) {
        this.msg = msg;
        this.parent = parent;
    }
    
    
    /**
     * Starts this thread
     */
    @Override
    public void run() {
        System.out.printf(
                "(%s) %s - %s\n", 
                Thread.currentThread().getName(), 
                msg.getOperation(), 
                msg.getData()
        );
        
        switch (msg.getOperation()) {
            case Server.CMD_REGISTER:
                handleRegister();
                break;
            
            case Server.CMD_UNREGISTER:
                handleUnregister();
                break;
            
            case Server.CMD_CONNECT:
                handlePlayerConnect();
                break;
                
            case Server.CMD_CHAT:
                handleChat();
                break;
                
            case Server.CMD_TELEPORT:
                handleTeleport();
                break;
        }
    }
    
    
    /**
     * Parse a registration message
     * 
     */
    private void handleRegister() {
        String[] parts = msg.getData().split(Client.DELIMITER);
        Client c = parent.getClient(msg.getKey());
        if (c != null) {
            c.setMap(parts[0]);
            c.setMaxPlayers(Integer.parseInt(parts[1].trim()));
            c.setRcon(parts[2]);
            c.setPort(Integer.parseInt(parts[3].trim()));
        } else {
            c = new Client();
            c.setAddr(msg.getSource());
            c.setKey(msg.getKey());
            c.setMap(parts[0]);
            c.setMaxPlayers(Integer.parseInt(parts[1].trim()));
            c.setRcon(parts[2]);
            c.setPort(Integer.parseInt(parts[3].trim()));
            parent.getClients().add(c);
        }
    }
    
    
    /**
     * 
     */
    private void handleUnregister() {
        int index = parent.getClientIndex(msg.getKey());
        parent.getClients().remove(index);
    }
    
    
    /**
     * Called if the message sent is chat from a player
     */
    private void handleChat() {
        Client cl = parent.getClient(msg.getKey());
        if (cl == null) 
            return;

        String[] parts1 = msg.getData().split(Client.DELIMITER);
        String sender = parts1[0];
        
        if (parts1[1].toLowerCase().trim().equals("teleport")) {
            cl.send("say show list of servers");
        }
        
        if (parts1[1].toLowerCase().trim().startsWith("goto ")) {
            String[] chat = parts1[1].split(" ", 2);
            cl.send(String.format("say teleporting %s to %s", sender, chat[1]));
        }
    }
    
    
    /**
     * Stuff a connection string to the supplied player for a new server
     * 
     * @param player
     * @param dest 
     */
    private void teleport(Player player, String dest) {
        
    }
    
    
    private void handleTeleport() {
        Client cl = parent.getClient(msg.getKey());
        if (cl == null) 
            return;
        
        cl.send("say show teleport list");
        cl.send(String.format("sv !say_person LIKE %s print list", null));
    }
    
    private void handlePlayerConnect() {
        Client cl = parent.getClient(msg.getKey());
        if (cl == null) 
            return;
        
        Player player = new Player();
        
        String[] parts1 = msg.getData().split("\\\\\\\\"); // that's \\
        player.setClientId(Integer.parseInt(parts1[0]));
        player.setUserInfo("\\" + parts1[2]);
        player.setName(player.getInfo("name"));
    }
}

