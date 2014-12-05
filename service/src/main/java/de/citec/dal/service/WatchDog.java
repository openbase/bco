/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rsb.Activatable;
import rsb.RSBException;

/**
 *
 * @author mpohling
 */
public class WatchDog implements Activatable {

    private final Object EXECUTION_LOCK = new Object();
    private static final long DELAY = 10000;

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private final Activatable service;
    private final String serviceName;
    private Minder minder;

    public WatchDog(final Activatable task, final String serviceName) {
        this.service = task;
        this.serviceName = serviceName;
    }

    @Override
    public void activate() throws RSBException {
        synchronized (EXECUTION_LOCK) {
            if (minder != null) {
                logger.warn("Skip activation, Service[" + serviceName + "] already running!");
                return;
            }
            minder = new Minder();
            minder.start();
        }
    }

    @Override
    public void deactivate() throws RSBException, InterruptedException {
        synchronized (EXECUTION_LOCK) {
            if (minder == null) {
                logger.warn("Skip deactivation, Service[" + serviceName + "] not running!");
                return;
            }

            minder.interrupt();
            minder.join();
            minder = null;
        }
    }

    @Override
    public boolean isActive() {
        return minder != null;
    }

    public String getServiceName() {
        return serviceName;
    }    

    class Minder extends Thread {

        @Override
        public void run() {
            try {
                while (!isInterrupted()) {
                    if (!service.isActive()) {
                        try {
                            service.activate();
                            logger.info("Service[" + serviceName + "] is now running.");
                        } catch (RSBException ex) {
                            logger.error("Could not start Service[" + serviceName + "]! Try again in " + (DELAY / 10) + " secunds...", ex);
                        }
                    }
                    waitWithinDelay();
                }
            } catch (InterruptedException ex) {
                logger.debug("Catch Service[" + serviceName + "] inerruption.");
            }

            while (service.isActive()) {
                try {
                    service.deactivate();
                    logger.info("Service[" + serviceName + "] is now finished.");
                } catch (RSBException | InterruptedException ex) {
                    logger.error("Could not shutdown Service[" + serviceName + "]! Try again in " + (DELAY / 10) + " secunds...", ex);
                }
                try {
                    waitWithinDelay();
                } catch (InterruptedException ex) {
                    logger.debug("Catch Service[" + serviceName + "] inerruption during shutdown!");
                }
            }
        }

        private void waitWithinDelay() throws InterruptedException {
            Thread.sleep(DELAY);
        }
    }
}
