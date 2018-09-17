/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pf.q2admin;

/**
 *
 * @author joe
 */
public final class Player {
    private String name;
    private int clientId;
    private String userInfo;
    private int databaseId;
    private String skin;
    private String gender;
    private int hand;
    private String ip;
    
    private int frags       = 0;
    private int deaths      = 0;
    private int suicides    = 0;

    public Player() {
    }
    
    public Player(int id, String ui) {
        setClientId(id);
        setUserInfo(ui);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getClientId() {
        return clientId;
    }

    public void setClientId(int clientId) {
        this.clientId = clientId;
    }

    public String getUserInfo() {
        return userInfo;
    }

    public void setUserInfo(String userInfo) {
        this.userInfo = userInfo;
        setName(getInfo("name"));
        setSkin(getInfo("skin"));
        setIp(getInfo("ip"));
        setHand(Integer.parseInt(getInfo("hand")));
        setGender(getInfo("gender"));
    }

    public int getFrags() {
        return frags;
    }

    public void setFrags(int frags) {
        this.frags = frags;
    }

    public int getSuicides() {
        return suicides;
    }

    public void setSuicides(int suicides) {
        this.suicides = suicides;
    }
    
    public String getInfo(String key) {
        String[] ui = userInfo.split("\\\\");
        
        if (ui.length == 0) {
            return null;
        }
        
        for (int i=0; i<ui.length; i++) {
            if (ui[i].toLowerCase().equals(key)) {
                return ui[i+1];
            }
        }
        
        return null;
    }

    public int getDatabaseId() {
        return databaseId;
    }

    public void setDatabaseId(int databaseId) {
        this.databaseId = databaseId;
    }

    public int getDeaths() {
        return deaths;
    }

    public void setDeaths(int deaths) {
        this.deaths = deaths;
    }

    public String getSkin() {
        return skin;
    }

    public void setSkin(String skin) {
        this.skin = skin;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public int getHand() {
        return hand;
    }

    public void setHand(int hand) {
        this.hand = hand;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }
}

