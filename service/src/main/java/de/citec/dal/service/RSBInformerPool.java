/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.service;

import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class RSBInformerPool {

    protected final Logger logger;

    private static RSBInformerPool instance;

    public static final int DEFAULT_POOL_SIZE = 10;
    public static final String ROOT_SCOPE = "/";

    private final ArrayList<Informer> informerList;
    private int poolPointer;

    public static synchronized RSBInformerPool getInstance() {
        if (instance == null) {
            instance = new RSBInformerPool();
        }
        return instance;
    }

    public RSBInformerPool() {
        this.logger = LoggerFactory.getLogger(getClass());
        this.informerList = new ArrayList<>();
    }

    public RSBInformerPool(final int size) throws InitializeException {
        this();
        for (int i = 0; i < size; i++) {
            informerList.add(Factory.getInstance().createInformer(new Scope(ROOT_SCOPE)));
        }
    }

    public void activate() throws Exception {
        logger.debug("Activate core informer.");

        try {
            for (Informer informer : informerList) {
                informer.activate();
            }
        } catch (InitializeException exx) {
            throw new RSBException("Could not activate core Informer!.", exx);
        }
    }

    public void deactivate() throws Exception {
        logger.debug("Deactivate core informer.");
        try {
            for (Informer informer : informerList) {
                informer.deactivate();
            }
        } catch (InitializeException exx) {
            throw new RSBException("Could not deactivate core Informer!.", exx);
        }
    }

    private synchronized Informer getNextInformer() {
        if (poolPointer >= informerList.size()) {
            poolPointer = 0;
        }
        return informerList.get(poolPointer++);
    }

    public Event send(final Event event) throws RSBException {
        return getNextInformer().send(event);
    }
}
