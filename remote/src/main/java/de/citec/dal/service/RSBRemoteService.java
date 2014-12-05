/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.service;

import com.google.protobuf.GeneratedMessage;
import de.citec.dal.data.Location;
import de.citec.dal.util.NotAvailableException;
import de.citec.dal.util.Observable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rsb.Event;
import rsb.Factory;
import rsb.Handler;
import rsb.Listener;
import rsb.RSBException;
import rsb.Scope;
import rsb.patterns.RemoteServer;

/**
 *
 * @author mpohling
 * @param <M>
 * @param <MB>
 */
public abstract class RSBRemoteService<M extends GeneratedMessage> extends Observable<M> {

    protected final Logger logger;

    private Listener listener;
    private WatchDog listenerWatchDog, remoteServerWatchDog;
    private Handler mainHandler;
    private RemoteServer remoteServer;

    protected Scope scope;
    private M data;

    public RSBRemoteService(final String id, final Location location) {
        this(generateScope(id, location));
    }

    public RSBRemoteService(final Scope scope) {
        this.logger = LoggerFactory.getLogger(getClass());
        
        this.mainHandler = new InternalUpdateHandler();
    }

    public void init(final Scope scope) {
        this.scope = new Scope(scope.toString().toLowerCase());
        logger.debug("Init RSBCommunicationService for component " + getClass().getSimpleName() + " on " + this.scope + ".");
        
        initListener(this.scope);
        initRemoteServer(this.scope);

        try {
            addHandler(mainHandler, true);
        } catch (InterruptedException ex) {
            logger.warn("Could not register main handler!", ex);
        }
    }

    private void initListener(final Scope scope) {
        try {
            this.listener = Factory.getInstance().createListener(scope);
            this.listenerWatchDog = new WatchDog(listener, "RSBListener");
        } catch (Exception ex) {
            logger.error("Could not create Listener on scope [" + scope.toString() + "]!", ex);
        }
    }

    private void initRemoteServer(final Scope scope) {
        try {
            this.remoteServer = Factory.getInstance().createRemoteServer(scope);
            this.remoteServerWatchDog = new WatchDog(listener, "RSBRemoteServer");
        } catch (Exception ex) {
            logger.error("Could not create RemoteServer on scope [" + scope.toString() + "]!", ex);
        }
    }

    public void addHandler(final Handler handler, final boolean wait) throws InterruptedException {
        try {
            listener.addHandler(handler, wait);
        } catch (InterruptedException ex) {
            logger.error("Could not register Handler!", ex);
        }
    }

    public void activate() {
        activateListener();
        activateRemoteServer();
    }

    public void deactivate() {
        deacivateListener();
        deactivateRemoteServer();
    }

    private void activateListener() {
        try {
            listenerWatchDog.activate();
        } catch (RSBException ex) {
            logger.error("Could not activate Listener!", ex);
        }
    }

    private void deacivateListener() {
        try {
            listenerWatchDog.deactivate();
        } catch (InterruptedException | RSBException ex) {
            logger.error("Unable to deactivate Listener!", ex);
        }
    }

    private void activateRemoteServer() {
        try {
            remoteServerWatchDog.activate();
        } catch (RSBException ex) {
            logger.error("Could not activate RemoteServer!", ex);
        }
    }

    private void deactivateRemoteServer() {
        try {
            remoteServerWatchDog.deactivate();
        } catch (InterruptedException | RSBException ex) {
            logger.error("Unable to deactivate RemoteServer!", ex);
        }
    }

    public <T extends Object> void callMethod(String methodName, T type, boolean async) {
        try {
            System.out.println("Calling method [" + methodName + "] on scope: " + remoteServer.getScope().toString());
            if (async) {
                remoteServer.callAsync(methodName, type);
            } else {
                remoteServer.call(methodName, type);
            }
        } catch (RSBException | ExecutionException | TimeoutException ex) {
            logger.error("Could not call remote Methode[" + methodName + "] on Scope[" + remoteServer.getScope() + "].");
        }
    }
    
    @Override
    public void shutdown() {
        deactivate();
        super.shutdown();
    }

    public static Scope generateScope(final String id, final Location location) {
        return location.getScope().concat(new Scope(Location.COMPONENT_SEPERATOR + id));
    }
    
    public M getData() throws NotAvailableException {
        if(data == null) {
            throw new NotAvailableException("data");
        }
        return data;
    }

    public abstract void notifyUpdated(M data);

    private class InternalUpdateHandler implements Handler {

        @Override
        public void internalNotify(Event event) {
            logger.debug("Internal notification: " + event.toString());
            try {
                data = (M) event.getData();
                notifyUpdated(data);
                notifyObservers(data);
            } catch (Exception ex) {
                logger.error("Could not notify new data, given datatype is not valid!", ex);
            }
        }
    }
}
