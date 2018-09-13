/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pf.q2admin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import libq2.com.packet.ByteStream;
import pf.q2admin.message.Registration;

/**
 *
 * @author joe
 */
public class ClientWorker implements Runnable {
    
    int cmd;
    ByteStream msg;
    Server parent;
    Connection db;
    Client cl;
    
    public ClientWorker(int cmd, ByteStream msg, Client cl, Server parent) {
        try {
            this.cmd = cmd;
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
                    "(%s) %s\n",
                    Thread.currentThread().getName(),
                    parent.getCmdString(cmd)
            );
            
            switch (cmd) {
                case Server.CMD_REGISTER:
                    handleRegister();
                    break;
                
                case Server.CMD_QUIT:
                    handleServerDisconnect();
                    break;
                    
                case Server.CMD_CONNECT:
                    handlePlayerConnect();
                    break;

                case Server.CMD_DISCONNECT:
                    handlePlayerDisconnect();
                    break;
                          
                case Server.CMD_PRINT:
                    handlePrint();
                    break;
                    
                case Server.CMD_TELEPORT:
                    //handleTeleport();
                    break;
            }
            
            db.close();
        } catch (SQLException ex) {
            Logger.getLogger(ClientWorker.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void handlePrint() {
        int level = msg.readByte();
        String print = msg.readString();

        System.out.printf("Print level: %d\t '%s'\n", level, print);
        
        switch (level) {
            case Client.PRINT_CHAT:
                handleChat();
                break;
            case Client.PRINT_MEDIUM:
                //handleObituary();
                break;
        }
    }
    
    /**
     * 
     */
    private void handleServerDisconnect() {
        cl.removePlayers();
        cl.setConnected(false);
        System.out.printf("Server disconnect\n");
    }
    
    
    /**
     * Called if the message sent is chat from a player
     */
    private void handleChat() {
//        Client client = parent.getClient(msg.getKey());
//        if (client == null) 
//            return;
//
//        String[] parts1 = msg.getData().split("\\\\");
//        int level = Integer.parseInt(parts1[0]);
//        String message = parts1[1];
//        
//        logChat(client, message);
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
        try {
            Client dest = null;
            if (dest != null) {
                sendPlayer("Teleporting you to " + dest.getName());
                stuffPlayer(String.format("connect %s:%d", dest.getAddr().getHostAddress(), dest.getPort()));
                return;
            }
            
            String emptyservers = "";
            String activeservers = "";
            
            String sql = "SELECT teleportname FROM server WHERE enabled = 1 AND playercount = 0 ORDER BY teleportname ASC";
            PreparedStatement st = db.prepareStatement(sql);
            ResultSet rs = st.executeQuery();
            
            while (rs.next()) {
                emptyservers += rs.getString("teleportname") + ", ";
            }
            
            if (!emptyservers.equals("")) {
                emptyservers = emptyservers.substring(0, emptyservers.length()-2);
                sendPlayer("Empty Servers: " + emptyservers);
            }
            
            sql = "SELECT id, teleportname, map FROM server WHERE enabled = 1 AND playercount > 0 ORDER BY teleportname ASC";
            st = db.prepareStatement(sql);
            rs = st.executeQuery();
            while (rs.next()) {
                sql = "";
            } 
            
        } catch (SQLException ex) {
            Logger.getLogger(ClientWorker.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void handleObituary() {

    }
    

    
    private void handlePlayerDisconnect() {
        int client_id = msg.readByte();
        Player p = cl.getPlayers()[client_id];
        System.out.printf("Client Disconnect - %s (%d)\n", p.getName(), p.getClientId());  
        p = null;
    }
    
    private void handlePlayerConnect() {
        int client_id = msg.readByte();
        String userinfo = msg.readString();
        
        Player p = new Player();
        p.setClientId(client_id);
        p.setUserInfo(userinfo);
        
        //players[client_id] = p;
        cl.getPlayers()[client_id] = p;
        System.out.printf("Client Connected - %s (%d)\n", p.getName(), p.getClientId());
    }
    
    
    private void sendPlayer(String privateMsg) {
        if (cl == null) 
            return;

        //cl.send(String.format("sv !say_person CL %d %s", msg.readByte(), privateMsg));
    }
    
    private void stuffPlayer(String cmd) {
        if (cl == null) 
            return;

        //cl.send(String.format("sv !stuff CL %d %s", msg.readByte(), cmd));
    }
    
    
    /**
     * Mark all active players as having quit for this server
     * 
     * @param cl 
     */
    private void removeAllPlayers(Client cl) {
//        try {
//            String sql = "UPDATE player SET date_quit = NOW() WHERE server = ? AND date_quit = '0000-00-00 00:00:00'";
//            PreparedStatement st = db.prepareStatement(sql);
//            st.setInt(1, cl.getClientnum());
//            st.executeUpdate();
//        } catch (SQLException ex) {
//            Logger.getLogger(ClientWorker.class.getName()).log(Level.SEVERE, null, ex);
//        }
    }
    
    /**
     * Mark player as quit for this server
     * 
     * @param cl
     * @param playernum 
     */
    private void removePlayer(Client cl, int playernum) {
        try {
            String sql = "UPDATE player SET date_quit = NOW() WHERE server = ? AND clientnum = ? LIMIT 1";
            PreparedStatement st = db.prepareStatement(sql);
            st.setInt(1, cl.getClientnum());
            st.setInt(2, playernum);
            st.executeUpdate();
        } catch (SQLException ex) {
            Logger.getLogger(ClientWorker.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void logChat(Client cl, String message) {
        try {
            System.out.println(message.length());
            String sql = "INSERT INTO chat (server, chat_date, message) VALUES (?, NOW(), ?)";
            PreparedStatement st = db.prepareStatement(sql);
            st.setInt(1, cl.getClientnum());
            st.setString(2, message.trim());
            st.executeUpdate();
        } catch (SQLException ex) {
            Logger.getLogger(ClientWorker.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void handleRegister() {
        cl.setRegistration(new Registration(msg));
        cl.send("sv !ra_online");
    }
}

