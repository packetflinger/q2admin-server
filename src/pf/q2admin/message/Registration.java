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
    private String password;
    
    public Registration(ByteStream msg) {
        
        setPort(msg.readShort());
        setPassword(msg.readString());
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    } 
    
    @Override
    public String toString() {
        return String.format("Registration:\nport = %d\npassword = %s\n", getPort(), getPassword());
    }
}
