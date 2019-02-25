/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pf.q2admin.message;

import libq2.com.packet.ByteStream;

/**
 *
 * @author joe
 */
public final class Registration {
    private int port;
    private int maxclients;
    private String map;
    private String password;
    private int clientversion;
    
    public Registration(ByteStream msg) {   
        setClientversion(msg.readLong());
        setPort(msg.readShort());
        setMaxclients(msg.readByte());
        setPassword(msg.readString());
        setMap(msg.readString());
    }

    public int getClientversion() {
        return clientversion;
    }

    public void setClientversion(int clientversion) {
        this.clientversion = clientversion;
    }
    
    
    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getMaxclients() {
        return maxclients;
    }

    public void setMaxclients(int maxclients) {
        this.maxclients = maxclients;
    }

    public String getMap() {
        return map;
    }

    public void setMap(String map) {
        this.map = map;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
    
    
    @Override
    public String toString() {
        return String.format("Registration:\nport = %d\nmap = %s\nmaxclients = %d\n", getPort(), getMap(), getMaxclients());
    }
}
