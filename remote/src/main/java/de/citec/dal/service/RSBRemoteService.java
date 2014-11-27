/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.service;

import com.google.protobuf.GeneratedMessage;
import de.citec.dal.data.Location;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
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
 */
public class RSBRemoteService<M extends GeneratedMessage, MB extends GeneratedMessage.Builder> {

    protected final Logger logger;

    private Listener listener;
    private Handler handler;
    private RemoteServer remoteServer;

//    protected final MB builder;
    protected Scope scope;

    public RSBRemoteService(final String id, final Location location, final MB builder) {
        this(generateScope(id, location), builder);
    }

    public RSBRemoteService(final Scope scope, final MB builder) {
        this.logger = LoggerFactory.getLogger(getClass());
        this.scope = new Scope(scope.toString().toLowerCase());
        this.handler = new InternalHandler();
//        this.builder = builder;

        logger.debug("Init RSBCommunicationService for component " + getClass().getSimpleName() + " on " + scope + ".");

        init(scope);
    }

    private void init(final Scope scope) {
        initListener(scope);
        initRemoteServer(scope);
        registerHandler();
    }
    
    private void initListener(final Scope scope) {
        try {
            this.listener = Factory.getInstance().createListener(scope);
        } catch (Exception ex) {
            logger.error("Could not create Listener on scope [" + scope.toString() + "]!", ex);
        }
    }

    private void initRemoteServer(final Scope scope) {
        try {
            this.remoteServer = Factory.getInstance().createRemoteServer(scope);
        } catch (Exception ex) {
            logger.error("Could not create RemoteServer on scope [" + scope.toString() + "]!", ex);
        }
    }

    private void registerHandler() {
        try {
            listener.addHandler(handler, true);
        } catch (InterruptedException ex) {
            java.util.logging.Logger.getLogger(RSBRemoteService.class.getName()).log(Level.SEVERE, null, ex);
            logger.error("Could not register Handler!", ex);
        }
    }
    
    private void activate() {
        
    }

    private void activateListener() {
        try {
            listener.activate();
        } catch (RSBException ex) {
            logger.error("Could not activate Listener!", ex);
        }
    }

    private void deacivateListener() {
        try {
            listener.deactivate();
        } catch (InterruptedException | RSBException ex) {
            logger.error("Unable to deactivate Listener!", ex);
        }
    }

    private void activateRemoteServer() {
        try {
            remoteServer.activate();
        } catch (RSBException ex) {
            logger.error("Could not activate RemoteServer!", ex);
        }
    }

    private void deactivateRemoteServer() {
        try {
            remoteServer.deactivate();
        } catch (InterruptedException | RSBException ex) {
            logger.error("Unable to deactivate RemoteServer!", ex);
        }
    }

    public <T extends Object> void callMethod(String methodName, T type, final Scope scope, boolean async) {
        try {
            System.out.println("Calling method [" + methodName + "] on scope: " + scope.toString());
            if (async) {
                remoteServer.callAsync(methodName, type);
            } else {
                remoteServer.call(methodName, type);
            }
        } catch (RSBException | ExecutionException | TimeoutException ex) {
            logger.error("Could not call remote Methode["+methodName+"] on Scope["+scope+"].");
        }
    }

    public static Scope generateScope(final String id, final Location location) {
        return location.getScope().concat(new Scope(Location.COMPONENT_SEPERATOR + id));
    }

//    protected abstract void addConverter();
//
//    @Override
//    public abstract void internalNotify(Event event);
//    
    private class InternalHandler implements Handler {

        @Override
        public void internalNotify(Event event) {
            logger.debug("Internal notification: "+event.toString());
        }
        
    }
}
