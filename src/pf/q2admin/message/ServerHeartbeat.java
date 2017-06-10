/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pf.q2admin.message;

/**
 *
 * @author setupuser
 */
public class ServerHeartbeat {
    
    private String map;
    private String rcon;
    private int maxplayers;
    private int port;
    private int flags;

    public ServerHeartbeat() {
    }
    
    public ServerHeartbeat(String str) {
        
        String[] regparts = str.split("\\\\");
        
        setMap(regparts[0]);
        setMaxplayers(Integer.parseInt(regparts[1]));
        setRcon(regparts[2]);
        setPort(Integer.parseInt(regparts[3]));
        setFlags(Integer.parseInt(regparts[4].trim()));
    }

    public String getMap() {
        return map;
    }

    public final void setMap(String map) {
        this.map = map;
    }

    public String getRcon() {
        return rcon;
    }

    public final void setRcon(String rcon) {
        this.rcon = rcon;
    }

    public int getMaxplayers() {
        return maxplayers;
    }

    public final void setMaxplayers(int maxplayers) {
        this.maxplayers = maxplayers;
    }

    public int getPort() {
        return port;
    }

    public final void setPort(int port) {
        this.port = port;
    }

    public int getFlags() {
        return flags;
    }

    public final void setFlags(int flags) {
        this.flags = flags;
    }
}
