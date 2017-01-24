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
import java.util.logging.Level;
import java.util.logging.Logger;
import pf.q2admin.message.RegisterMessage;

/**
 *
 * @author joe
 */
public class Client {
    private boolean connected;
    
    private InetAddress addr;
    private int port = 27910;
    private String map;
    private String rcon;
    private int maxPlayers = 64;
    private Player[] players;
    private String key;
    private int clientnum;
    private int flags;
    
    private DatagramSocket socket;
    
    public Client() {
    }

    public InetAddress getAddr() {
        return addr;
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
        return maxPlayers;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public Player[] getPlayers() {
        return players;
    }

    public void setPlayers(Player[] players) {
        this.players = players;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setRegistration(RegisterMessage r) {
        rcon = r.getRcon();
        port = r.getPort();
        map = r.getMap();
        maxPlayers = r.getMaxplayers();
        connected = true;
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
    }

    public int getFlags() {
        return flags;
    }

    public void setFlags(int flags) {
        this.flags = flags;
    }
}

