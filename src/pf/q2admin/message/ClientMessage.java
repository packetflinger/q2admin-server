/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pf.q2admin.message;

import java.net.DatagramPacket;
import java.net.InetAddress;

/**
 *
 * @author joe
 */
public class ClientMessage {
    private String raw;
    private String key;
    private int operation;
    private String data;
    private InetAddress source;
    private int clientid;

    public ClientMessage(DatagramPacket p) {
        setRaw(new String(p.getData()));
        source = p.getAddress();
        String[] msg = getRaw().split("\\\\", 3);
        setKey(msg[0]);
        setOperation(Integer.parseInt(msg[1]));
        setData(msg[2]);
    }
    
    public String getKey() {
        return key;
    }

    public final void setKey(String key) {
        this.key = key;
    }

    public int getOperation() {
        return operation;
    }

    public final void setOperation(int operation) {
        this.operation = operation;
    }

    public String getData() {
        return data;
    }

    public final void setData(String data) {
        this.data = data;
    }

    public final String getRaw() {
        return raw;
    }

    public final void setRaw(String raw) {
        this.raw = raw;
    }

    public InetAddress getSource() {
        return source;
    }

    public void setSource(InetAddress source) {
        this.source = source;
    }

    public int getClientid() {
        return clientid;
    }

    public final void setClientid(int clientid) {
        this.clientid = clientid;
    }
}

