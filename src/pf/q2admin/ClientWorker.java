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
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;
import pf.q2admin.message.ClientMessage;
import pf.q2admin.message.ServerHeartbeat;
import pf.q2admin.message.PlayerHeartbeat;

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
                    handleServerHeartbeat();
                    break;
                
                case Server.CMD_CONNECT:
                    handlePlayerHeartbeat();
                    break;
                    
                case Server.CMD_QUIT:
                    handleServerDisconnect();
                    break;
                    
//                case Server.CMD_CONNECT:
//                    handlePlayerConnect();
//                    break;
//                    
                case Server.CMD_DISCONNECT:
                    handlePlayerDisconnect();
                    break;
                    
//                case Server.CMD_CONNECT:
//                    handlePlayerConnect();
//                    break;
//                    
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
        String[] args = msg.getData().split("\\\\");
        int level = Integer.parseInt(args[0]);

        switch (level) {
            case Client.PRINT_CHAT:
                handleChat();
                break;
            case Client.PRINT_MEDIUM:
                handleObituary();
                break;
        }
    }
    
    /**
     * Parse a registration message
     * 
     */
    private void handleServerHeartbeat() {
        try {
            ServerHeartbeat rg = new ServerHeartbeat(msg.getData());
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
            
            cl.send("sv !ra_online");
        } catch (SQLException ex) {
            Logger.getLogger(ClientWorker.class.getName()).log(Level.SEVERE, null, ex);
        }  
    }
    
    
    /**
     * 
     */
    private void handleServerDisconnect() {
        removeAllPlayers(cl);
        cl.setConnected(false);
    }
    
    
    /**
     * Called if the message sent is chat from a player
     */
    private void handleChat() {
        Client client = parent.getClient(msg.getKey());
        if (client == null) 
            return;

        String[] parts1 = msg.getData().split("\\\\");
        int level = Integer.parseInt(parts1[0]);
        String message = parts1[1];
        
        logChat(client, message);
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
            String[] args = msg.getData().split("\\\\");
            Client dest = parent.getClientFromTeleportName(args[1]);
            
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
        System.out.printf("Player size: %d\n", cl.getPlayers().length);
        String obit = msg.getData().split("\\\\")[1];
        String[] parts = obit.split(" ");
        String message = obit.substring(parts[0].length() + 1);
        
        Player deadplayer = cl.getPlayerByName(parts[0]);
        System.out.printf("Obit - dead: %s - msg: %s\n", deadplayer.getName(), message);
    }
    
//    private void handlePlayerConnect() {
//        try {
//            
//            if (cl.getPlayers() == null) {
//                System.out.printf("Players array null...\n");
//                return;
//            }
//            
//            PlayerHeartbeat ui = new PlayerHeartbeat(msg.getData());
//            
//            String sql = "INSERT INTO player (server, clientnum, name, date_joined, date_quit) VALUES (?,?,?,NOW(),'0000-00-00 00:00:00')";
//            PreparedStatement st = db.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
//            st.setInt(1, cl.getClientnum());
//            st.setInt(2, ui.getClientid());
//            st.setString(3, ui.getName());
//            st.executeUpdate();
//            
//            ResultSet r = st.getGeneratedKeys();
//            if (r.next()) {
//                Player p = new Player();
//                p.setClientId(ui.getClientid());
//                p.setName(ui.getName());
//                p.setDatabaseId(r.getInt(1));
//                p.setUserInfo(ui.getUserinfo());
//                cl.getPlayers()[p.getClientId()] = p;
//            }
//            
//            handlePlayerHeartbeat();
//            // get the insert id and store it with the player object
//        } catch (SQLException ex) {
//            Logger.getLogger(ClientWorker.class.getName()).log(Level.SEVERE, null, ex);
//        }
//    }
    
    private void handlePlayerDisconnect() {
        try {
            int id = Integer.parseInt(msg.getData().trim());
            String sql = "UPDATE player SET date_quit = NOW() WHERE id = ? LIMIT 1";
            PreparedStatement st = db.prepareStatement(sql);
            st.setInt(1, cl.getPlayers()[id].getDatabaseId());
            st.executeUpdate();
            
            cl.getPlayers()[id] = null;
            System.out.printf("Quit handled\n");
        } catch (SQLException ex) {
            Logger.getLogger(ClientWorker.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void handlePlayerHeartbeat() {
        try {
            PlayerHeartbeat ui = new PlayerHeartbeat(msg.getData());
            Player[] players = cl.getPlayers();
            if (players != null) {
                Player pl = players[ui.getClientid()];
                if (pl != null) {
                    if (!pl.getUserInfo().equals(ui.getUserinfo())) {
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
                        System.out.printf("Updating UserInfo: %s\n", ui.getUserinfo());
                    }
                }
                
                if (pl == null) {
                    String sql = "INSERT INTO player (server, clientnum, name, date_joined, date_quit) VALUES (?,?,?,NOW(),'0000-00-00 00:00:00')";
                    PreparedStatement st = db.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                    st.setInt(1, cl.getClientnum());
                    st.setInt(2, ui.getClientid());
                    st.setString(3, ui.getName());
                    st.executeUpdate();

                    ResultSet r = st.getGeneratedKeys();
                    if (r.next()) {
                        Player p = new Player();
                        p.setClientId(ui.getClientid());
                        p.setName(ui.getName());
                        p.setDatabaseId(r.getInt(1));
                        p.setUserInfo(ui.getUserinfo());
                        cl.getPlayers()[p.getClientId()] = p;
                        
                        sql = "INSERT INTO userinfo (server, clientnum, infodate, name, skin, hand, fov, ip, info) "
                            + "VALUES (?, ?, NOW(), ?, ?, ?, ?, ?, ?)";
                        PreparedStatement st2 = db.prepareStatement(sql);
                        st2.setInt(1, cl.getClientnum());
                        st2.setInt(2, ui.getClientid());
                        st2.setString(3, ui.getName());
                        st2.setString(4, ui.getSkin());
                        st2.setInt(5, ui.getHand());
                        st2.setInt(6, ui.getFov());
                        st2.setString(7, ui.getIp());
                        st2.setString(8, ui.getUserinfo());

                        st2.executeUpdate();
                        st2.close();
                    }
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(ClientWorker.class.getName()).log(Level.SEVERE, null, ex);
        }      
    }
    
    private void sendPlayer(String privateMsg) {
        Client client = parent.getClient(msg.getKey());
        if (client == null) 
            return;

        client.send(String.format("sv !say_person CL %d %s", msg.getClientid(), privateMsg));
    }
    
    private void stuffPlayer(String cmd) {
        Client client = parent.getClient(msg.getKey());
        if (client == null) 
            return;

        client.send(String.format("sv !stuff CL %d %s", msg.getClientid(), cmd));
    }
    
    
    /**
     * Mark all active players as having quit for this server
     * 
     * @param cl 
     */
    private void removeAllPlayers(Client cl) {
        try {
            String sql = "UPDATE player SET date_quit = NOW() WHERE server = ? AND date_quit = '0000-00-00 00:00:00'";
            PreparedStatement st = db.prepareStatement(sql);
            st.setInt(1, cl.getClientnum());
            st.executeUpdate();
        } catch (SQLException ex) {
            Logger.getLogger(ClientWorker.class.getName()).log(Level.SEVERE, null, ex);
        }
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
}

