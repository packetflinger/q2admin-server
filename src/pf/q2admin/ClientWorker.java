/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pf.q2admin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import libq2.com.packet.ByteStream;
import pf.q2admin.message.Registration;

/**
 *
 * @author joe
 */
public class ClientWorker implements Runnable {
    
    public static int PAD_RIGHT = 1;
    public static int PAD_LEFT = -1;
    public static int PAD_CENTER = 0;
    
    int cmd;
    ByteStream msg;
    Server parent;
    Connection db;
    Client cl;
    
    DateTimeFormatter dateformat;
    ZonedDateTime   zdt;
    
    public ClientWorker(int cmd, ByteStream msg, Client cl, Server parent) {
        try {
            this.cmd = cmd;
            this.cl = cl;
            this.msg = msg;
            this.parent = parent;
            db = parent.getConnection();
            
            dateformat = DateTimeFormatter
                    .ofLocalizedDateTime(FormatStyle.LONG)
                    .withLocale(Locale.US)
                    .withZone(ZoneId.systemDefault());
            zdt = ZonedDateTime.now(ZoneId.systemDefault());
                    
            
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
                
                case Server.CMD_PLAYERLIST:
                    handlePlayerlist();
                    break;
                
                case Server.CMD_PLAYERUPDATE:
                    handlePlayerUpdate();
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
                
                case Server.CMD_PLAYERS:
                    handlePlayers();
                    break;
                    
                case Server.CMD_INVITE:
                    handleInvite();
                    break;
                    
                case Server.CMD_WHOIS:
                    handleWhois();
                    break;
                    
                case Server.CMD_FRAG:
                    handleFrag();
                    break;
                
                case Server.CMD_HEARTBEAT:
                    handleHeartbeat();
                    break;
            }
            
            db.close();
        } catch (SQLException ex) {
            Logger.getLogger(ClientWorker.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void handlePrint() {
        switch (msg.readByte()) {
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
       
        String print = msg.readString();
        cl.getChats().add(print);
        
        // queue is full, write to database and clear it
        if (cl.getChats().isFull()) {
            cl.getChats().writeToDatabase(db);
            cl.getChats().clear();
        }
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
        Client q2srv;
        
        sayPlayerLow(client_id, "");
        
        // list possible servers and give usage info
        if (lookup.equals("")) {
            sayPlayerLow(client_id, "Available servers:");
            String here = "";
            while ((q2srv = parent.getClients().next()) != null) {
                if (q2srv.getAddress().equals(cl.getAddress())) {
                    here = "<--- you are here";
                } else {
                    here = "";
                }
                
                sayPlayerLow(client_id, 
                        String.format("%s \t%s\t", 
                                pad(PAD_RIGHT, 25, q2srv.getTeleportname()),
                                q2srv.getName()
                        )
                );
            }
            
            sayPlayerLow(client_id, "");
            sayPlayerLow(client_id, "Usage: !teleport <name>");
            
        } else {
            while ((q2srv = parent.getClients().next()) != null) {
                if (q2srv.getTeleportname().equalsIgnoreCase(lookup)){
                    sayPlayerLow(client_id, String.format("Sending you to %s (%s)", q2srv.getName(), q2srv.getAddress()));
                    stuffPlayer(client_id, String.format("connect %s", q2srv.getAddress()));
                    return;
                }
            }
            
            sayPlayerLow(client_id, String.format("No server matching '%s' was found", lookup));
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
        
        // check bans
        // check mutes
        // check prohibited names
        
        cl.getPlayers()[client_id] = new Player(client_id, userinfo);
        
        System.out.printf("Client Connected - %s (%d)\n", 
                cl.getPlayers()[client_id].getName(), 
                cl.getPlayers()[client_id].getClientId()
        );
    }
    
    
    /**
     * Send text to a specific client as a private message
     * 
     * @param privateMsg 
     */
    private void sayPlayer(int client_id, String privateMsg) {
        if (cl == null) 
            return;
        
        String buffer = String.format("sv !say_person CL %d %s", client_id, privateMsg);
        cl.send(buffer);
    }
    
    
    /**
     * Send low level text to a player
     * 
     * @param client_id
     * @param msg 
     */
    private void sayPlayerLow(int client_id, String msg) {
        if (cl == null) 
            return;
        
        String buffer = String.format("sv !say_person_low CL %d %s", client_id, msg);
        cl.send(buffer);
    }
    
    
    /**
     * Force a command into a player's buffer
     * 
     * @param client_id
     * @param txt 
     */
    private void stuffPlayer(int client_id, String txt) {
        if (cl == null) 
            return;
        
        String buffer = String.format("sv !stuff CL %d %s", client_id, txt);
        cl.send(buffer);
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
    
    /**
     * Record the game server's registration, let it know it's connected and request
     * a list of players in case players were already connected when RA was initialized
     * 
     */
    private void handleRegister() {
        cl.setRegistration(new Registration(msg));
        
        if (cl.getRegistration().getClientversion() < Server.MINIMUM_VERSION) {
            System.out.printf("Version too old (%d - %d < %d)\n", cl.getKey(), cl.getRegistration().getClientversion(), Server.MINIMUM_VERSION);
        } else {
            cl.setRcon(cl.getRegistration().getPassword());
            cl.send("sv !remote_online");
            cl.send("sv !remote_playerlist");
        }
    }
    
    /**
     * 
     */
    private void handlePlayerlist() {
        int client_id;
        String userinfo;
        
        for (int i=0; i<msg.readByte(); i++) {
            client_id = msg.readByte();
            userinfo = msg.readString();
            
            try {
                cl.getPlayers()[client_id] = new Player(client_id, userinfo);
                System.out.printf("\t[%d] %s (%s)\n",
                    client_id,
                    cl.getPlayers()[client_id].getName(),
                    cl.getPlayers()[client_id].getIp()
                );
            } catch (ArrayIndexOutOfBoundsException e) {
                e.printStackTrace(System.err);
            }
        }
    }
    
    
    /**
     * Called when a player userinfo changes (name, skin, fov, hand, uf, etc..)
     * 
     */
    private void handlePlayerUpdate() {
        int client_id = msg.readByte();
        String userinfo = msg.readString();
            
        try {
            cl.getPlayers()[client_id] = new Player(client_id, userinfo);
            System.out.printf("\t[%d] %s updated (%s)\n",
                client_id,
                cl.getPlayers()[client_id].getName(),
                cl.getPlayers()[client_id].getIp()
            );
        } catch (ArrayIndexOutOfBoundsException e) {
            e.printStackTrace(System.err);
        }
    }
    
    
    /**
     * Respond to a player issuing the !players <server> command
     * 
     */
    private void handlePlayers() {
        int client_id = msg.readByte();
        String srv = msg.readString();
        String buf;
        int count = 0;
        
        Client gameserver = parent.getClients().getByName(srv);
        if (gameserver == null) {
            sayPlayerLow(client_id, String.format("Unknown server '%s'", srv));
            return;
        }
        
        Player[] plist = gameserver.getPlayers();
        for (int i=0; i<gameserver.getClientnum(); i++) {
            if (plist[i] == null)
                continue;

            sayPlayerLow(client_id, String.format("%s (%s)\n",
                    pad(PAD_RIGHT, 15, plist[i].getName()),
                    plist[i].getSkin())
            );
            count++;
        }
        
        sayPlayerLow(client_id, String.format("- %d players found on %s -", count, gameserver.getTeleportname()));
    }
    
    
    /**
     * Added leading/trailing spaces to a string to make it a total size given
     * 
     * @param direction - add padding to right or left
     * @param size
     * @param txt
     * @return 
     */
    private String pad (int direction, int size, String txt) {
        String newstr = "";
        int len = txt.length();
        
        if (direction > 0) {
            newstr += txt;
            for (int i = len; i<size; i++) {
                newstr += " ";
            }
            
            return newstr;
        }
        
        if (direction < 0) {
            for (int i = len; i<size; i++) {
                newstr += " ";
            }
            newstr += txt;
            
            return newstr;
        }
        
        return txt;
    }
    
    
    /**
     * When someone uses the invite command
     * 
     */
    private void handleInvite() {
        int client_id = msg.readByte();
        String invitetxt = msg.readString();
        
        if (invitetxt.equals("")) {
            invitetxt = String.format("type '!teleport %s' to join", cl.getTeleportname());
        }
        
        String txt = String.format("%s invites you to join #%s: %s\n",
                cl.getPlayer(client_id).getName(),
                cl.getTeleportname(),
                invitetxt
        );
        
        String stuff;
        Client gameserver;
        while ((gameserver = parent.getClients().next()) != null) {
            // if flags allow it...
            stuff = String.format("say %s", txt);
            gameserver.send(stuff);
        }
    }
    
    
    private void handleWhois() {
        int client_id = msg.readByte();
        String lookup = msg.readString();
        
        // find local player, get address
        // lookup address in database to get aliases
        sayPlayerLow(client_id, "Whois functions are not allowed on this server currently...");
    }
    
    private void handleFrag() {
        byte victim = (byte) msg.readByte();
        String vname = msg.readString();
        byte attacker = (byte) msg.readByte();
        String aname = msg.readString();
        
        cl.getFrags().add(victim, attacker, vname, aname);
        
        if (cl.getFrags().isFull()) {
            cl.getFrags().writeToDatabase(db);
            cl.getFrags().clear();
        }
    }
    
    private void handleMap(String map) {
        try {
            String sql = "INSERT INTO map (server_id, map, map_date) VALUES (?,?,?)";
            PreparedStatement st = db.prepareStatement(sql);
            st.setInt(1, cl.getClientnum());
            st.setString(2, map);
            st.setString(3, Client.now());
            
        } catch (SQLException ex) {
            Logger.getLogger(ClientWorker.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void handleHeartbeat() {
        System.out.printf("Heartbeat\n");
    }
}

