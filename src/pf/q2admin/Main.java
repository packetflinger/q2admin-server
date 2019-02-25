/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pf.q2admin;

import pf.q2admin.mgmt.WebsiteManagement;

/**
 *
 * @author joe
 */
public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Server server = new Server();
        server.start();
        
        WebsiteManagement webman = new WebsiteManagement();
        webman.start();
    }
}
