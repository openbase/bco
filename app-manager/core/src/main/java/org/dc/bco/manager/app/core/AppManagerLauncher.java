/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.manager.app.core;

/**
 *
 * @author <a href="mailto:mpohling@cit-ec.uni-bielefeld.de">Divine Threepwood</a>
 */
public class AppManagerLauncher {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws InterruptedException {
        while(!Thread.interrupted()) {
            Thread.sleep(10000);
        }
    }
}
