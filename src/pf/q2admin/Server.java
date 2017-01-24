/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pf.q2admin;

import com.mchange.v2.c3p0.ComboPooledDataSource;
import java.beans.PropertyVetoException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Properties;
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

    public static final int CMD_REGISTER = 0,
            CMD_CONNECT = 1,
            CMD_USERINFO = 2,
            CMD_PRINT = 3,
            CMD_CHAT = 4,
            CMD_DISCONNECT = 5,
            CMD_UNREGISTER = 6,
            CMD_TELEPORT = 7,
            CMD_INVITE = 8,
            CMD_FIND = 9,
            CMD_FRAG = 10;

    private DatagramSocket socket = null;
    private int listen_port;
    
    private int threads = 4;
    
    private String dbhost;
    private String dbuser;
    private String dbpass;
    private int dbport;

    private HashMap<String, Client> clients;
    
    private ComboPooledDataSource dbpool;

    public Server() {
        try {
            loadProperties("q2a.properties");
            socket = new DatagramSocket(listen_port);
            dbpool = new ComboPooledDataSource();

        } catch (IOException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }

        clients = new HashMap();
    }

    @Override
    public void run() {
        try {
            ExecutorService threadpool = Executors.newFixedThreadPool(threads);
            
            dbpool.setDriverClass("com.mysql.jdbc.Driver");
            dbpool.setJdbcUrl("jdbc:mysql://localhost:3306/q2admin");
            dbpool.setUser("root");
            dbpool.setPassword("");
            
            dbpool.setMinPoolSize(5);
            dbpool.setAcquireIncrement(5);
            dbpool.setMaxPoolSize(20);
            
            byte[] dataIn = new byte[1400];
            ClientMessage msg;
            Client cl;
            
            System.out.printf("Listening on udp/%d\n", listen_port);
            
            startMaintenance();
            
            while (true) {
                try {
                    DatagramPacket receivePacket = new DatagramPacket(dataIn, dataIn.length);
                    socket.receive(receivePacket);
                    
                    msg = new ClientMessage(receivePacket);
                    if ((cl = getValidClient(msg)) != null) {
                        threadpool.execute(new ClientWorker(msg, cl, this));
                    } else {
                        System.out.printf("Invalid server: %s\n", msg.getKey());
                    }
                } catch (IOException e) {
                    System.out.print(e.getMessage());
                }
                dataIn = new byte[1400];
            }
        } catch (PropertyVetoException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public HashMap<String, Client> getClients() {
        return clients;
    }

    public Client getClient(String key) {
        return clients.get(key);
    }

    public Client getValidClient(ClientMessage msg) {
        Client cl = clients.get(msg.getKey());
        if (cl != null) {
            return cl.getAddr().getHostAddress().equals(msg.getSource().getHostAddress()) ? cl : null;
        }
        
        return null;
    }
    
    public Connection getConnection() throws SQLException {
        return dbpool.getConnection();
    } 
    
    public final void loadProperties(String file) {
        try {
            InputStream in = new FileInputStream(file);
            Properties p = new Properties();
            p.load(in);
            
            this.setListen_port(Integer.parseInt(p.getProperty("listen_port")));
            this.setDbhost(p.getProperty("db_host"));
            this.setDbuser(p.getProperty("db_user"));
            this.setDbpass(p.getProperty("db_pass"));
            this.setDbport(Integer.parseInt(p.getProperty("db_port")));
            
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public String getDbhost() {
        return dbhost;
    }

    public void setDbhost(String dbhost) {
        this.dbhost = dbhost;
    }

    public String getDbuser() {
        return dbuser;
    }

    public void setDbuser(String dbuser) {
        this.dbuser = dbuser;
    }

    public String getDbpass() {
        return dbpass;
    }

    public void setDbpass(String dbpass) {
        this.dbpass = dbpass;
    }

    public int getDbport() {
        return dbport;
    }

    public void setDbport(int dbport) {
        this.dbport = dbport;
    }

    public int getListen_port() {
        return listen_port;
    }

    public void setListen_port(int listen_port) {
        this.listen_port = listen_port;
    }
    
    private void startMaintenance() {
        Thread t = new Thread(() -> {
            while (true) {
                try {
                    loadServers();
                    Thread.sleep(30 * 1000);
                } catch (InterruptedException ex) {
                    Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
        
        t.start();
    }
    
    private void loadServers() {
        try {
            String sql = "SELECT id, serverkey, INET_NTOA(addr) AS IP, port, teleportname, map FROM server WHERE enabled = 1";
            Connection conn = dbpool.getConnection();
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(sql);
            Client cl;
            
            while (rs.next()) {
                cl = clients.get(rs.getString("serverkey"));
                if (cl == null) {
                    cl = new Client();
                    cl.setKey(rs.getString("serverkey"));
                    cl.setMap(rs.getString("map"));
                    cl.setPort(rs.getInt("port"));
                    cl.setClientnum(rs.getInt("id"));
                    try {
                        cl.setAddr(InetAddress.getByName(rs.getString("ip")));
                    } catch (UnknownHostException ex) {
                        Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                        continue;
                    }
                    
                    clients.put(cl.getKey(), cl);
                } else {
                    cl.setMap(rs.getString("map"));
                }
                
                System.out.printf("Server: %s:%d\n", cl.getAddr().getHostAddress(), cl.getPort());
            }
        } catch (SQLException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
