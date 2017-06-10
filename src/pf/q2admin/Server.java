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
import java.sql.PreparedStatement;
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

    public static final int CMD_SERVER_HEARTBEAT = 0,
            CMD_SERVER_DISCONNECT = 1,
            CMD_PLAYER_HEARTBEAT = 2,
            CMD_PLAYER_DISCONNECT = 3,
            CMD_PRINT = 4,
            CMD_TELEPORT = 5,
            CMD_INVITE = 6,
            CMD_SEEN = 7,
            CMD_WHOIS = 8;

    private DatagramSocket socket = null;
    private int listen_port;
    
    private int threads;
    
    private String dbhost;
    private String dbuser;
    private String dbpass;
    private String dbschema;
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
            dbpool.setJdbcUrl(String.format("jdbc:mysql://%s:%d/%s", dbhost, dbport, dbschema));
            dbpool.setUser(dbuser);
            dbpool.setPassword(dbpass);
            
            dbpool.setMinPoolSize(3);
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
                        System.out.printf("Invalid server: %s - %s\n", msg.getKey(), msg.getSource().getHostAddress());
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
            this.threads = Integer.parseInt(p.getProperty("threads"));
            this.dbschema = p.getProperty("db_schema");
            
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
                    Thread.sleep(60 * 1000);
                } catch (InterruptedException ex) {
                    Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
        
        t.start();
    }
    
    private void loadServers() {
        try {
            String sql = "SELECT id, serverkey, INET_NTOA(addr) AS ip, port, teleportname, map, name FROM server WHERE enabled = 1";
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
                    cl.setName(rs.getString("name"));
                    try {
                        cl.setAddr(InetAddress.getByName(rs.getString("ip")));
                    } catch (UnknownHostException ex) {
                        Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                        continue;
                    }
                    
                    clients.put(cl.getKey(), cl);
                } else {
                    cl.setKey(rs.getString("serverkey"));
                    cl.setMap(rs.getString("map"));
                    cl.setPort(rs.getInt("port"));
                    cl.setClientnum(rs.getInt("id"));
                    cl.setName(rs.getString("name"));
                    try {
                        cl.setAddr(InetAddress.getByName(rs.getString("ip")));
                    } catch (UnknownHostException ex) {
                        Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                System.out.printf("Server loaded: %s:%d\n", cl.getAddr().getHostAddress(), cl.getPort());
                
            }
        } catch (SQLException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private void updateServerObject(String key) {
        try {
            String sql = "SELECT * FROM server WHERE serverkey = ? LIMIT 1";
            Connection db = dbpool.getConnection();
            PreparedStatement st = db.prepareStatement(sql);
            st.setString(1, sql);
        } catch (SQLException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public String getCmdString(int cmd) {
        switch (cmd) {
            case Server.CMD_INVITE:
                return "INVT";
            case Server.CMD_PLAYER_DISCONNECT:
                return "PDISC";
            case Server.CMD_PLAYER_HEARTBEAT:
                return "PHB";
            case Server.CMD_PRINT:
                return "PRNT";
            case Server.CMD_SEEN:
                return "SEEN";
            case Server.CMD_SERVER_DISCONNECT:
                return "SDISC";
            case Server.CMD_SERVER_HEARTBEAT:
                return "SHB";
            case Server.CMD_TELEPORT:
                return "TELE";
            case Server.CMD_WHOIS:
                return "WHOIS";
            default:
                return "UNKN";
        }
    }
    
    public Client getClientFromTeleportName(String tp) {
        try {
            String sql = "SELECT serverkey FROM server WHERE teleportname = ? LIMIT 1";
            Connection conn = dbpool.getConnection();
            PreparedStatement st = conn.prepareStatement(sql);
            st.setString(1, tp);
            ResultSet rs = st.executeQuery();
            while (rs.next()) {
                return getClient(rs.getString("serverkey"));
            }
            
        } catch (SQLException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
}
