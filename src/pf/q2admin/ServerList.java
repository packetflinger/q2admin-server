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
    
    
    public void add(int index, Client cl) {
        if (gameservers == null) {
            gameservers = new HashMap();
        }
        
        gameservers.put(index, cl);
    }
    
    public void remove(int index) {
        if (gameservers == null) {
            return;
        }
        
        gameservers.remove(index);
    }
    
    public Client get(int index) {
        if (gameservers == null) {
            return null;
        }
        
        return gameservers.get(index);
    }
    
    public Iterator<Integer> getIterator() {
        return gameservers.keySet().iterator();
    }
    
    public int getSize() {
        if (gameservers == null) {
            gameservers = new HashMap();
        }
        
        return gameservers.size();
    }
    
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
     * move back to the start
     */
    public void reset() {
        index = 0;
        itr = gameservers.keySet().iterator();
    }
    
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
    
    public HashMap<Integer, Client> getMap() {
        return gameservers;
    }
}
