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
public class PlayerHeartbeat {
    
    String userinfo;
    String[] info;  // split by "\"
    String name;
    String skin;
    String ip;
    int hand;
    int fov;
    int clientid;
    
    public PlayerHeartbeat() {
    }
    
    public PlayerHeartbeat(String info) {
        String[] tmp = info.split("\\\\\\\\");
        clientid = Integer.parseInt(tmp[0]);
        userinfo = tmp[1].trim();
        this.info = userinfo.trim().split("\\\\");
        name = get("name").trim();
        skin = get("skin").trim();
        ip = get("ip").trim();
        hand = Integer.parseInt(get("hand"));
        fov = Integer.parseInt(get("fov"));
    }

    public String getUserinfo() {
        return userinfo;
    }

    public void setUserinfo(String userinfo) {
        this.userinfo = userinfo;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSkin() {
        return skin;
    }

    public void setSkin(String skin) {
        this.skin = skin;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getHand() {
        return hand;
    }

    public void setHand(int hand) {
        this.hand = hand;
    }

    public int getFov() {
        return fov;
    }

    public void setFov(int fov) {
        this.fov = fov;
    }

    public int getClientid() {
        return clientid;
    }

    public void setClientid(int clientid) {
        this.clientid = clientid;
    }

    public final String get(String key) {
        for (int i=0; i<info.length; i+=2) {
            if (info[i].equalsIgnoreCase(key)) {
                return info[i+1];
            }
        }
        
        return null;
    }
}
