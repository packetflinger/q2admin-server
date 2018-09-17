/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pf.q2admin;

import java.util.HashMap;
import java.util.Iterator;

/**
 *
 * @author joe
 */
public class ServerList {
    private HashMap<Integer, Client> gameservers;
    private int index = 0; // for easy traversal
    private Iterator<Integer> itr;
    
    public ServerList() {
        gameservers = new HashMap();
    }
    
    
    /**
     * Add a gameserver to the list
     * 
     * @param index
     * @param cl 
     */
    public void add(int index, Client cl) {
        if (gameservers == null) {
            gameservers = new HashMap();
        }
        
        gameservers.put(index, cl);
    }
    
    
    /**
     * Remove a gameserver from the list
     * 
     * @param index 
     */
    public void remove(int index) {
        if (gameservers == null) {
            return;
        }
        
        gameservers.remove(index);
    }
    
    
    /**
     * Get a particular gameserver from it's key
     * 
     * @param index
     * @return 
     */
    public Client get(int index) {
        if (gameservers == null) {
            return null;
        }
        
        return gameservers.get(index);
    }
    
    
    /**
     * Get the iterator for the list
     * @return 
     */
    public Iterator<Integer> getIterator() {
        return gameservers.keySet().iterator();
    }
    
    
    /**
     * Get the number of servers in the list
     * 
     * @return 
     */
    public int getSize() {
        if (gameservers == null) {
            gameservers = new HashMap();
        }
        
        return gameservers.size();
    }
    
    
    /**
     * Get the first server in the list
     * 
     * @return 
     */
    public Client getFirst() {
        if (gameservers == null) {
            gameservers = new HashMap();
            return null;
        }
        
        index = 0;
        reset();
        
        return gameservers.get(itr.next());
    }
    
    /**
     * Set the internal pointer back to the start of the list
     * 
     */
    public void reset() {
        index = 0;
        itr = gameservers.keySet().iterator();
    }
    
    
    /**
     * Get the next server in the list based on the iterator position
     * 
     * @return 
     */
    public Client next() {
        if (itr == null) {
            reset();
        }
        
        if (itr.hasNext()) {
            return gameservers.get(itr.next());
        } else {
            itr = null;
            return null;
        }
    }
    
    
    /**
     * Get the map of client gameservers
     * 
     * @return 
     */
    public HashMap<Integer, Client> getMap() {
        return gameservers;
    }
    
    
    /**
     * Return the client of a certain name, or null if not found
     * 
     * @param name
     * @return 
     */
    public Client getByName(String name) {
        Client cl;
        reset();
        while ((cl = next()) != null) {
            if (cl.getTeleportname().equalsIgnoreCase(name)) {
                return cl;
            }
        }
        reset();
        
        return null;
    }
}
