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
    
    public Registration(ByteStream msg) {   
        setPort(msg.readShort());
        setMaxclients(msg.readByte());
        setMap(msg.readString());
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
    
    @Override
    public String toString() {
        return String.format("Registration:\nport = %d\nmap = %s\nmaxclients = %d\n", getPort(), getMap(), getMaxclients());
    }
}
