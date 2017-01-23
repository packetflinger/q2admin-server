/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pf.q2admin;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;
import pf.q2admin.message.ClientMessage;
import pf.q2admin.message.RegisterMessage;

/**
 *
 * @author joe
 */
public class ClientWorker implements Runnable {
    
    ClientMessage msg;
    Server parent;
    Connection db;
    Client cl;

    public ClientWorker(ClientMessage msg, Client cl, Server parent) {
        try {
            this.cl = cl;
            this.msg = msg;
            this.parent = parent;
            db = parent.getConnection();
        } catch (SQLException ex) {
            Logger.getLogger(ClientWorker.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    
    /**
     * Starts this thread
     */
    @Override
    public void run() {
        try {
            System.out.printf(
                    "(%s) %s - %s\t\t",
                    Thread.currentThread().getName(),
                    msg.getOperation(),
                    msg.getData()
            );
            
            String sql = "SELECT NOW() AS now";
            Statement st = db.createStatement();
            ResultSet r = st.executeQuery(sql);
            while (r.next()) {
                System.out.printf("Time: %s\n", r.getString("now"));
            }
            
            switch (msg.getOperation()) {
                case Server.CMD_REGISTER:
                    handleRegister();
                    break;
                    
//                case Server.CMD_UNREGISTER:
//                    handleUnregister();
//                    break;
//                    
//                case Server.CMD_CONNECT:
//                    handlePlayerConnect();
//                    break;
//                    
//                case Server.CMD_CHAT:
//                    handleChat();
//                    break;
//                    
//                case Server.CMD_TELEPORT:
//                    handleTeleport();
//                    break;
            }
            
            db.close();
        } catch (SQLException ex) {
            Logger.getLogger(ClientWorker.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    
    /**
     * Parse a registration message
     * 
     */
    private void handleRegister() {
        RegisterMessage rg = new RegisterMessage(msg.getData());
        cl.setRegistration(rg);
    }
    
    
    /**
     * 
     */
    private void handleUnregister() {
        //int index = parent.getClientIndex(msg.getKey());
        //parent.getClients().remove(index);
        cl.setConnected(false);
    }
    
    
    /**
     * Called if the message sent is chat from a player
     */
    private void handleChat() {
//        Client cl = parent.getClient(msg.getKey());
//        if (cl == null) 
//            return;
//
//        String[] parts1 = msg.getData().split(Client.DELIMITER);
//        String sender = parts1[0];
//        
//        if (parts1[1].toLowerCase().trim().equals("teleport")) {
//            cl.send("say show list of servers");
//        }
//        
//        if (parts1[1].toLowerCase().trim().startsWith("goto ")) {
//            String[] chat = parts1[1].split(" ", 2);
//            cl.send(String.format("say teleporting %s to %s", sender, chat[1]));
//        }
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

