/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rsb.Activatable;
import rsb.Event;
import rsb.Factory;
import rsb.Informer;
import rsb.InitializeException;
import rsb.RSBException;
import rsb.Scope;

/**
 *
 * @author mpohling
 */
public class RSBInformerPool implements Activatable {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private static RSBInformerPool instance;

    private static final Object ACTIVATION_LOCK = new Object();
    public static final int DEFAULT_POOL_SIZE = 10;
    public static final String ROOT_SCOPE = "/";

    private final ArrayList<Informer> informerList;
    private final Map<WatchDog, Informer> watchDogMap;
    private int poolPointer;
    private boolean active;
    
    public static synchronized RSBInformerPool getInstance() {
        if (instance == null) {
            instance = new RSBInformerPool();
        }
        return instance;
    }

    private RSBInformerPool() {
        this.informerList = new ArrayList<>();
        this.watchDogMap = new HashMap<>();
        this.init(DEFAULT_POOL_SIZE);
    }

    private void init(final int size) {
        
        active = false;
        Informer<Object> informer;

        for (int i = 0; i < size; i++) {
            try {
                informer = Factory.getInstance().createInformer(new Scope(ROOT_SCOPE));
                informerList.add(informer);
                watchDogMap.put(new WatchDog(informer, Informer.class.getSimpleName()+"["+i+"]"), informer);
            } catch (InitializeException ex) {
                logger.error("Could not activate core "+Informer.class.getSimpleName()+"["+i+"]"+"!", ex);
            }
        }
    }

    @Override
    public void activate() {
        synchronized (ACTIVATION_LOCK) {
            logger.debug("Activate core informer.");

            if (watchDogMap.isEmpty()) {
                logger.warn("Skip activation, informerpool is empty!");
            }

            for (WatchDog watchDog : watchDogMap.keySet()) {
                try {
                    watchDog.activate();
                } catch (RSBException ex) {
                    logger.error("Could not activate core "+watchDog.getServiceName()+"!.", ex);
                }
            }
            active = true;
        }
    }

    @Override
    public void deactivate() {
        synchronized (ACTIVATION_LOCK) {
            logger.debug("Deactivate core informer.");

            if (watchDogMap.isEmpty()) {
                logger.warn("Skip deactivation, informerpool is empty!");
            }

            for (WatchDog watchDog : watchDogMap.keySet()) {
                try {
                    watchDog.deactivate();
                } catch (RSBException | InterruptedException ex) {
                    logger.error("Could not deactivate core "+watchDog.getServiceName()+"!.", ex);
                }
            }
            active = false;
        }
    }

    @Override
    public boolean isActive() {
        return active;
    }

    private synchronized Informer getNextInformer() {

        if (poolPointer >= informerList.size()) {
            poolPointer = 0;
        }
        return informerList.get(poolPointer++);
    }

    public Event send(final Event event) throws RSBException {

        if (watchDogMap.isEmpty()) {
            logger.warn("Skip send, informerpool is empty!");
        }

        return getNextInformer().send(event);
    }
}
