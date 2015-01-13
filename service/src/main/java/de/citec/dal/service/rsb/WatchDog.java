/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.service.rsb;

import de.citec.dal.util.MultiException;
import de.citec.dal.util.Observable;
import de.citec.dal.util.Observer;
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

    public enum ServiceState {

        Unknown, Initializing, Running, Terminating, Finished, Failed
    };

    private final Object stateLock = new Object();

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private final Activatable service;
    private final String serviceName;
    private Minder minder;
    private ServiceState serviceState;

    private final Observable<ServiceState> serviceStateObserable;

    public WatchDog(final Activatable task, final String serviceName) {
        this.service = task;
        this.serviceName = serviceName;
        this.serviceStateObserable = new Observable<>();
        setServiceState(ServiceState.Unknown);
    }

    @Override
    public void activate() {
        synchronized (EXECUTION_LOCK) {
            if (minder != null) {
                logger.warn("Skip activation, Service[" + serviceName + "] already running!");
                return;
            }
            minder = new Minder(serviceName + "WatchDog");
            minder.start();
        }
    }

    @Override
    public void deactivate() throws InterruptedException {
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

    public void waitForActivation() throws InterruptedException {

        synchronized (stateLock) {
            if (serviceState == ServiceState.Running) {
                return;
            }

            addObserver(new Observer<ServiceState>() {

                @Override
                public void update(Observable<ServiceState> source, ServiceState data) throws Exception {
                    if (data == ServiceState.Running) {
                        synchronized (stateLock) {
                            stateLock.notify();
                        }
                    }
                }
            });
            stateLock.wait();
        }
    }

    class Minder extends Thread {

        public Minder(String name) {
            super(name);
            setServiceState(ServiceState.Initializing);
        }

        @Override
        public void run() {
            try {
                while (!isInterrupted()) {
                    setServiceState(ServiceState.Initializing);
                    if (!service.isActive()) {
                        try {
                            service.activate();
                            logger.info("Service[" + serviceName + "] is now running.");
                            setServiceState(ServiceState.Running);
                        } catch (RSBException ex) {
                            logger.error("Could not start Service[" + serviceName + "]! Try again in " + (DELAY / 1000) + " seconds...", ex);
                            setServiceState(ServiceState.Failed);
                        }
                    }
                    waitWithinDelay();
                }
            } catch (InterruptedException ex) {
                logger.debug("Catch Service[" + serviceName + "] interruption.");
            }

            while (service.isActive()) {
                setServiceState(ServiceState.Terminating);
                try {
                    service.deactivate();
                    logger.info("Service[" + serviceName + "] is now finished.");
                    setServiceState(ServiceState.Finished);
                } catch (RSBException | InterruptedException ex) {
                    logger.error("Could not shutdown Service[" + serviceName + "]! Try again in " + (DELAY / 1000) + " seconds...", ex);
                    try {
                        waitWithinDelay();
                    } catch (InterruptedException exx) {
                        logger.debug("Catch Service[" + serviceName + "] interruption during shutdown!");
                    }
                }
            }
        }

        private void waitWithinDelay() throws InterruptedException {
            Thread.sleep(DELAY);
        }
    }

    public Activatable getService() {
        return service;
    }

    private void setServiceState(final ServiceState serviceState) {
        try {
            synchronized (stateLock) {
                if (this.serviceState == serviceState) {
                    return;
                }
                this.serviceState = serviceState;
            }
            serviceStateObserable.notifyObservers(serviceState);
        } catch (MultiException ex) {
            logger.warn("Could not notify statechange to all instanzes!", ex);
        }
    }

    public ServiceState getServiceState() {
        return serviceState;
    }

    public void addObserver(Observer<ServiceState> observer) {
        serviceStateObserable.addObserver(observer);
    }

    public void removeObserver(Observer<ServiceState> observer) {
        serviceStateObserable.removeObserver(observer);
    }

}
