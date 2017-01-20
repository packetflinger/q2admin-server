/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pf.q2admin;


import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import pf.q2admin.message.ClientMessage;

/**
 *
 * @author joe
 */
public class Server extends Thread {
    
    public static final int CMD_REGISTER    = 0;
    public static final int CMD_CONNECT     = 1;
    public static final int CMD_USERINFO    = 2;
    public static final int CMD_PRINT       = 3;
    public static final int CMD_CHAT        = 4;
    public static final int CMD_DISCONNECT  = 5;
    public static final int CMD_UNREGISTER  = 6;
    public static final int CMD_TELEPORT    = 7;
    public static final int CMD_INVITE      = 8;
    public static final int CMD_FIND        = 9;
    
    
    private DatagramSocket socket = null;
    public static int SOCKET_PORT = 9999;
    private int threads = 4;
    
    private List<Client> clients;
    
    public Server() {  
        try {
            socket = new DatagramSocket(SOCKET_PORT);
            
        } catch (IOException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        clients = new ArrayList();
    }
    
    @Override
    public void run()
    {
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        
        byte[] dataIn = new byte[1400];
        ClientMessage msg;
        
        System.out.printf("Listening on udp/%d\n", Server.SOCKET_PORT);
        
        while (true) {
            try {   
                DatagramPacket receivePacket = new DatagramPacket(dataIn, dataIn.length);
                socket.receive(receivePacket);
                
                msg = new ClientMessage(receivePacket);
                if (isValidKey(msg)) {
                    executor.execute(new ClientWorker(msg, this));
                }
            }
            catch (IOException e) {
                System.out.print(e.getMessage());
            }
            dataIn = new byte[1400];
        }
    }

    public List<Client> getClients() {
        return clients;
    }

    public void setClients(List<Client> clients) {
        this.clients = clients;
    }
    
    public Client getClient(String key) {
        if (clients == null) {
            return null;
        }
        
        for (Client c : clients) {
            if (c.getKey().equals(key))
                return c;
        }
        
        return null;
    }
    
    public int getClientIndex(String key) {
        if (clients == null) {
            return 0;
        }
        
        for (int i=0; i<clients.size(); i++) {
            if (clients.get(i).getKey().equals(key))
                return i;
        }
        
        return 0;
    }
    
    public boolean isValidKey(ClientMessage msg) {
        return true;    // do this later
    }
}
