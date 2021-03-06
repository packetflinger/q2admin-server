/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pf.q2admin;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;
import pf.q2admin.message.Registration;

/**
 *
 * @author joe
 */
public class Client {
    
    public static final int PRINT_LOW     = 0;    // pickups
    public static final int PRINT_MEDIUM  = 1;    // obituaries
    public static final int PRINT_HIGHT   = 2;    // critical msgs
    public static final int PRINT_CHAT    = 3;    // shit talking
    
    private boolean connected;
    
    private Registration registration;
    private ChatQueue chats;
    private FragQueue frags;
    
    private InetAddress addr;
    private int port = 27910;
    private String map;
    private String rcon;
    private int maxClients = 64;
    private Player[] players;
    private int key;
    private int clientnum;
    private int flags;
    private String name;
    private String teleportname;
    private int lastInvite = 0;
    
    private DatagramSocket socket;
    
    public Client() {
    }

    public InetAddress getAddr() {
        return addr;
    }
    
    /**
     * Full ip:port of this client as a string. 
     * Ex: 10.2.3.2:27910
     * 
     * @return 
     */
    public String getAddress() {
        return String.format("%s:%d", addr.getHostAddress(), port);
    }

    public void setAddr(InetAddress addr) {
        this.addr = addr;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getMap() {
        return map;
    }

    public void setMap(String map) {
        this.map = map;
    }

    public String getRcon() {
        return rcon;
    }

    public void setRcon(String rcon) {
        this.rcon = rcon;
    }

    public int getMaxPlayers() {
        return maxClients;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxClients = maxPlayers;
    }

    public Player[] getPlayers() {
        return players;
    }
    
    public Player getPlayer(int client_id) {
        if (players[client_id] != null) {
            return players[client_id];
        }
        
        return null;
    }

    public void setPlayers(Player[] players) {
        this.players = players;
    }

    public int getKey() {
        return key;
    }

    public void setKey(int key) {
        this.key = key;
    }
    
    public Player getPlayerByName(String name) {
        for (Player player : players) {
            if (player.getName().equalsIgnoreCase(name)) {
                return player;
            }
        }
        
        return null;
    }
    
    /**
     * Send an rcon command back to this client
     * 
     * @param txt 
     */
    public void send(String txt) {
        try {
            String tmp = String.format("rcon %s %s", rcon, txt);
            byte[] tempb = tmp.getBytes();
            
            // Out of band commands start with 4 bytes of ones
            byte[] b = new byte[tmp.length() + 5];
            b[0] = (byte) 0xff;
            b[1] = (byte) 0xff;
            b[2] = (byte) 0xff;
            b[3] = (byte) 0xff;
            
            // copy the command into the final buffer
            for (int j=4, i=0; i<tempb.length; i++, j++) {
                b[j] = tempb[i];
            }
            
            // finish with a null
            b[b.length-1] = (byte) 0x00;
            
            DatagramPacket p = new DatagramPacket(b, b.length, addr, port);
            socket = new DatagramSocket();
            socket.send(p);
            
        } catch (SocketException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }        
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public int getClientnum() {
        return clientnum;
    }

    public void setClientnum(int clientnum) {
        this.clientnum = clientnum;
        chats = new ChatQueue(clientnum);
        frags = new FragQueue(clientnum);
    }

    public int getFlags() {
        return flags;
    }

    public void removePlayers() {
        players = new Player[getMaxPlayers() + 1];
    }
    
    public void setFlags(int flags) {
        this.flags = flags;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Registration getRegistration() {
        return registration;
    }

    public void setRegistration(Registration r) {
        registration = r;
        port = r.getPort();
        map = r.getMap();
        maxClients = r.getMaxclients();
        connected = true;
        
        if (players == null) {
            players = new Player[maxClients + 10]; // leave some room
        }
    }

    public int getMaxClients() {
        return maxClients;
    }

    public void setMaxClients(int maxClients) {
        this.maxClients = maxClients;
    }

    public String getTeleportname() {
        return teleportname;
    }

    public void setTeleportname(String teleportname) {
        this.teleportname = teleportname;
    }

    public int getLastInvite() {
        return lastInvite;
    }

    public void setLastInvite(int lastInvite) {
        this.lastInvite = lastInvite;
    }

    public ChatQueue getChats() {
        return chats;
    }

    public void setChats(ChatQueue chats) {
        this.chats = chats;
    }

    public FragQueue getFrags() {
        return frags;
    }

    public void setFrags(FragQueue frags) {
        this.frags = frags;
    }
    
    
    /**
     * Get a MySQL formatted datetime string for right now
     * 
     * @return 
     */
    public static String now() {
        return DateTimeFormatter
                    .ofPattern("yyyy-MM-dd HH:mm:ss")
                    .format(ZonedDateTime.ofInstant(Instant.now(), ZoneId.systemDefault()));
    }
}

