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
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import libq2.com.packet.ByteStream;
import pf.q2admin.message.ClientMessage;

/**
 *
 * @author joe
 */
public class Server extends Thread {

    public static final int MINIMUM_VERSION = 178;
    
    public static final int CMD_REGISTER = 0,
            CMD_QUIT = 1,
            CMD_CONNECT = 2,
            CMD_DISCONNECT = 3,
            CMD_PLAYERLIST = 4,
            CMD_PLAYERUPDATE = 5,
            CMD_PRINT = 6,
            CMD_TELEPORT = 7,
            CMD_INVITE = 8,
            CMD_SEEN = 9,
            CMD_WHOIS = 10,
            CMD_PLAYERS = 11,   // 
            CMD_FRAG = 12,
            CMD_MAP = 13,
            CMD_AUTHORIZE = 14,
            CMD_HEARTBEAT = 15;

    private DatagramSocket socket = null;
    private int listen_port;
    
    private int threads;
    
    private String dbhost;
    private String dbuser;
    private String dbpass;
    private String dbschema;
    private int dbport;
    
    private int cmd;
    private int key;
    
    private ByteStream msg;
    private ServerList gameservers;
    
    private ComboPooledDataSource dbpool;

    public Server() {
        try {
            loadProperties("q2a.properties");
            socket = new DatagramSocket(listen_port);
            dbpool = new ComboPooledDataSource();

        } catch (IOException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }

        gameservers = new ServerList();
    }

    @Override
    public void run() {
        try {
            ExecutorService threadpool = Executors.newFixedThreadPool(threads);
            dbpool.setDriverClass("com.mysql.cj.jdbc.Driver");
            dbpool.setJdbcUrl(String.format("jdbc:mysql://%s:%d/%s?&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC", dbhost, dbport, dbschema));
            dbpool.setUser(dbuser);
            dbpool.setPassword(dbpass);
            
            dbpool.setMinPoolSize(3);
            dbpool.setAcquireIncrement(5);
            dbpool.setMaxPoolSize(20);
            
            byte[] dataIn = new byte[1400];

            Client cl;
            
            System.out.printf("Listening on udp/%d\n", listen_port);
            
            //startMaintenance();
            
            loadServers();
            requestRegistrations();
            
            while (true) {
                try {
                    DatagramPacket receivePacket = new DatagramPacket(dataIn, dataIn.length);
                    socket.receive(receivePacket);
                    
                    msg = new ByteStream(receivePacket.getData(), 0);
                    
                    key = msg.readLong();
                    cmd = msg.readByte();
                    
                    if (cmd == CMD_AUTHORIZE) {
                        new Thread(new AuthorizeWorker(msg, this)).start();
                    } else {
                        cl = gameservers.get(key);
                        if (cl == null) {
                            System.out.printf("unknown server, skipping\n");
                            continue;
                        }

                        new Thread(new ClientWorker(cmd, msg, cl, this)).start();
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

    public ServerList getClients() {
        return gameservers;
    }

    public Client getClient(int key) {
        return gameservers.get(key);
    }

    public Client getValidClient(ClientMessage msg) {
        Client cl = gameservers.get(msg.getKey());
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
        
        Logger.getLogger(Server.class.getName()).log(Level.INFO, "Properties loaded...");
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
    
    public void loadServers() {
        try {
            int i = 0;
            String sql = "SELECT id, serverkey, INET_NTOA(addr) AS ip, port, password, teleportname, map, name FROM server WHERE enabled = 1 AND authorized = 1";
            Connection conn = dbpool.getConnection();
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(sql);
            Client cl;
            System.out.printf("Loading servers into memory...\n");
            while (rs.next()) {
                cl = gameservers.get(rs.getInt("serverkey"));
                if (cl == null) {
                    cl = new Client();
                    cl.setKey(rs.getInt("serverkey"));
                    cl.setMap(rs.getString("map"));
                    cl.setPort(rs.getInt("port"));
                    cl.setClientnum(rs.getInt("id"));
                    cl.setName(rs.getString("name"));
                    cl.setRcon(rs.getString("password"));
                    cl.setTeleportname(rs.getString("teleportname"));
                    
                    try {
                        cl.setAddr(InetAddress.getByName(rs.getString("ip")));
                    } catch (UnknownHostException ex) {
                        Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                        continue;
                    }
                    
                    gameservers.add(cl.getKey(), cl);
                } else {
                    cl.setKey(rs.getInt("serverkey"));
                    cl.setMap(rs.getString("map"));
                    cl.setPort(rs.getInt("port"));
                    cl.setClientnum(rs.getInt("id"));
                    cl.setName(rs.getString("name"));
                    cl.setRcon(rs.getString("password"));
                    try {
                        cl.setAddr(InetAddress.getByName(rs.getString("ip")));
                    } catch (UnknownHostException ex) {
                        Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                System.out.printf("\t%s:%d (%s)\n", cl.getAddr().getHostAddress(), cl.getPort(), cl.getTeleportname());
                i++;
            }
            
            System.out.printf("Done - %d loaded from database\n", i);
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
            case Server.CMD_DISCONNECT:
                return "PDISC";
            case Server.CMD_CONNECT:
                return "PCON";
            case Server.CMD_PRINT:
                return "PRNT";
            case Server.CMD_SEEN:
                return "SEEN";
            case Server.CMD_QUIT:
                return "QUIT";
            case Server.CMD_REGISTER:
                return "REG";
            case Server.CMD_TELEPORT:
                return "TELE";
            case Server.CMD_WHOIS:
                return "WHOIS";
            case Server.CMD_PLAYERLIST:
                return "PLYLST";
            case Server.CMD_PLAYERUPDATE:
                return "PLYUPD";
            case Server.CMD_PLAYERS:
                return "PLYRS";
            case Server.CMD_FRAG:
                return "FRAG";
            case Server.CMD_MAP:
                return "MAP";
            case Server.CMD_HEARTBEAT:
                return "<3";
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
                return getClient(rs.getInt("serverkey"));
            }
            
        } catch (SQLException ex) {
            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }
    
    public void requestRegistrations() {
        Client c;
        while ((c = gameservers.next()) != null) {
            c.send("sv !remote_register");
        }
    }
    
    public Connection getDatabaseConnection() throws SQLException {
        return dbpool.getConnection();
    }
}
