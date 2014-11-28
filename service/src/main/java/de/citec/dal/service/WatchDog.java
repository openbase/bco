/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.service;

import rsb.Activatable;
import rsb.RSBException;

/**
 *
 * @author mpohling
 */
public class WatchDog implements Activatable {
    
    private Activatable task;
    private final Minder minder;
    
    public WatchDog(Activatable task) {
        this.task = task;
        this.minder = new Minder();
    }

    @Override
    public void activate() throws RSBException {
        task.activate();
    }

    @Override
    public void deactivate() throws RSBException, InterruptedException {
  //      if(minder.interrupt()) {
            
    //    }
        task.deactivate();
    }

    @Override
    public boolean isActive() {
        return minder != null;
    }
    
    class Minder extends Thread {

        @Override
        public void run() {
            while(!isInterrupted()) {
//                task.
            }
        }
    }
}
