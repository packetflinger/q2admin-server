/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pf.q2admin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import pf.q2admin.message.ClientMessage;
import pf.q2admin.message.RegisterMessage;
import pf.q2admin.message.UserinfoMessage;

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
                    "(%s) %s - %s\n",
                    Thread.currentThread().getName(),
                    parent.getCmdString(msg.getOperation()),
                    msg.getData()
            );
            
            switch (msg.getOperation()) {
                case Server.CMD_REGISTER:
                    handleRegister();
                    break;
                
                case Server.CMD_USERINFO:
                    handleUserinfo();
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
        try {
            RegisterMessage rg = new RegisterMessage(msg.getData());
            cl.setRegistration(rg);
            
            String sql = "UPDATE server SET "
                    + "map = ?, "
                    + "maxclients = ?, "
                    + "flags = ?, "
                    + "date_lastcontact = NOW() "
                    + "WHERE serverkey = ? LIMIT 1";
            PreparedStatement st = db.prepareStatement(sql);
            st.setString(1, rg.getMap());
            st.setInt(2, rg.getMaxplayers());
            st.setInt(3, rg.getFlags());
            st.setString(4, cl.getKey());
            st.executeUpdate();
            
        } catch (SQLException ex) {
            Logger.getLogger(ClientWorker.class.getName()).log(Level.SEVERE, null, ex);
        }
        
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
        cl.send(String.format("sv !say_person LIKE %s print list", "blah"));
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
    
    private void handleUserinfo() {
        try {
            UserinfoMessage ui = new UserinfoMessage(msg.getData());
            System.out.printf("userinfo len: %d - '%s'",ui.getUserinfo().length(), ui.getUserinfo());
            String sql = "INSERT INTO userinfo (server, clientnum, infodate, name, skin, hand, fov, ip, info) "
                    + "VALUES (?, ?, NOW(), ?, ?, ?, ?, ?, ?)";
            PreparedStatement st = db.prepareStatement(sql);
            st.setInt(1, cl.getClientnum());
            st.setInt(2, ui.getClientid());
            st.setString(3, ui.getName());
            st.setString(4, ui.getSkin());
            st.setInt(5, ui.getHand());
            st.setInt(6, ui.getFov());
            st.setString(7, ui.getIp());
            st.setString(8, ui.getUserinfo());
            
            st.executeUpdate();
            st.close();
            db.close();
        } catch (SQLException ex) {
            Logger.getLogger(ClientWorker.class.getName()).log(Level.SEVERE, null, ex);
        }
                
    }
}

