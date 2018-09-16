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
import java.util.Iterator;
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
                    handleTeleport();
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
    
    /**
     * When a player uses the teleport command with no argument, they're given a list of available
     * servers to choose from or are stuffed with a connect command if they specify which
     * server to connect to.
     * 
     */
    private void handleTeleport() {
        int client_id = msg.readByte();
        String lookup = msg.readString();
        Player caller = cl.getPlayers()[client_id];
        Client q2srv;
        int srvcount = 0;
        String buffer;
        
        // list possible servers and give usage info
        if (lookup.equals("")) {
            buffer = String.format("sv !say_person CL %d Available servers:\n", client_id);
            cl.send(buffer);
            String here = "";
            while ((q2srv = parent.getClients().next()) != null) {
                if (q2srv.getAddress().equals(cl.getAddress())) {
                    here = "<--- you are here";
                } else {
                    here = "";
                }
                
                buffer = String.format("sv !say_person CL %d %s       [%s] %s\n", client_id, q2srv.getTeleportname(), q2srv.getName(), here);
                cl.send(buffer);
            }
            
            buffer = String.format("sv !say_person CL %d Usage: !teleport <name>", client_id);
            cl.send(buffer);
        } else {
            while ((q2srv = parent.getClients().next()) != null) {
                if (q2srv.getTeleportname().equalsIgnoreCase(lookup)){
                    buffer = String.format("sv !say_person CL %d Teleporting you to '%s [%s]'", 
                            client_id, 
                            q2srv.getName(), 
                            q2srv.getAddress()
                    );
                    cl.send(buffer);
                    cl.send(String.format("sv !stuff CL %d connect %s", client_id, q2srv.getAddress()));
                    return;
                }
            }
      
            buffer = String.format("sv !say_person CL %d No server matching '%s' was found", client_id, lookup);
            cl.send(buffer);
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
        cl.send("sv !remote_online");
    }
}

