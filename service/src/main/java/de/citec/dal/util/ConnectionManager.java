/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.util;

import de.citec.dal.hal.device.AbstractDeviceController;
import de.citec.jul.rsb.RSBInformerPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rsb.Activatable;

/**
 *
 * @author mpohling
 */
public class ConnectionManager implements Activatable {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionManager.class);

    private static ConnectionManager instance;

    private final Object SYNC_LOCK = new Object();
    private boolean active;
    private final DALRegistry registry;

    public synchronized static ConnectionManager getInstance() {
        if (instance == null) {
            instance = new ConnectionManager();
        }
        return instance;
    }

    private ConnectionManager() {
        this.registry = DALRegistry.getInstance();
    }

    @Override
    public void activate() {
        synchronized (SYNC_LOCK) {
            active = true;

            for (AbstractDeviceController hardware : registry.getHardwareCollection()) {
                hardware.activate();
            }
            RSBInformerPool.getInstance().activate();
        }
    }

    @Override
    public void deactivate() {
        synchronized (SYNC_LOCK) {
            active = false;
            for (AbstractDeviceController hardware : registry.getHardwareCollection()) {
                try {
                    hardware.deactivate();
                } catch (InterruptedException ex) {
                    logger.error("Could not deactivate: " + hardware, ex);
                }
            }
        }
    }

    @Override
    public boolean isActive() {
        synchronized (SYNC_LOCK) {
            return active;
        }
    }
}
