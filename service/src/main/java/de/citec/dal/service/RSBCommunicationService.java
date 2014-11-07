/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.service;

import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.GeneratedMessage.Builder;
import de.citec.dal.data.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rsb.Event;
import rsb.Factory;
import rsb.Informer;
import rsb.RSBException;
import rsb.Scope;
import rsb.patterns.LocalServer;

/**
 *
 * @author mpohling
 */
public abstract class RSBCommunicationService<M extends GeneratedMessage, MB extends Builder> {

    public final static String SCOPE_SUFFIX_RPC = "ctrl";
    public final static String SCOPE_SUFFIX_INFORMER = "status";

    protected final Logger logger;

    protected final MB builder;
    protected static Informer informer;

    static { //TODO mpohling: REDESIGN HACK
        Logger LOGGER = LoggerFactory.getLogger(RSBCommunicationService.class);
        try {
            LOGGER.info("Init informer service...");
            informer = Factory.getInstance().createInformer("/");
        } catch (Exception ex) {
            LOGGER.error("Could not init RSBInformer!", new RSBException("Could not init informer.", ex));
        }
    }

    protected LocalServer server;
    protected Scope scope, informerScope, serverScope;

    public RSBCommunicationService(final String id, final Location location, final MB builder) {
        this(generateScope(id, location), builder);
    }

    public RSBCommunicationService(final Scope scope, final MB builder) {
        this.logger = LoggerFactory.getLogger(getClass());
        this.scope = new Scope(scope.toString().toLowerCase());
        this.informerScope = scope.concat(new Scope(Location.COMPONENT_SEPERATOR + SCOPE_SUFFIX_INFORMER));
        this.serverScope = scope.concat(new Scope(Location.COMPONENT_SEPERATOR + SCOPE_SUFFIX_RPC));

        this.builder = builder;
        logger.debug("Init RSBCommunicationService for component " + getClass().getSimpleName() + " on " + scope + ".");
    }

    protected void init() throws RSBException {
        try {
            logger.info("Init rpc server...");
            // Get local server object which allows to expose remotely callable methods.
            server = Factory.getInstance().createLocalServer(serverScope);

            // register rpc methods.
            registerMethods(server);

        } catch (Exception ex) {
            throw new RSBException("Could not init rpc server.", ex);
        }
    }

    public void activate() throws Exception {
        logger.debug("Activate RSBCommunicationService for: " + this);
//        try {
//            informer.activate();
//        } catch (InitializeException exx) {
//            throw new RSBException("Could not activate informer.", exx);
//        }
        if(!informer.isActive()) { //TODO mpohling: REDESIGN HACK
            informer.activate();
        }
        
        try {
            server.activate();
        } catch (RSBException ex) {
            throw new RSBException("Could not activate rpc server.", ex);
        }
    }

    public void deactivate() throws Exception {
//        try {
//            informer.deactivate();
//        } catch (Exception ex) {
//            throw new RSBException("Could not deactivate informer.", ex);
//        }

        try {
            server.deactivate();
        } catch (Exception ex) {
            throw new RSBException("Could not deactivate rpc server.", ex);
        }
    }

    protected final void setField(String name, Object value) {
        try {
            builder.setField(builder.getDescriptorForType().findFieldByName("name"), value);
        } catch (Exception ex) {
            logger.warn("Could not set field [" + name + "=" + value+ "] for "+this);
        }
    }

    public M getMessage() throws RSBException {
        try {
            return (M) builder.clone().build();
        } catch (Exception ex) {
            throw new RSBException("Could not build message!", ex);
        }
    }

    public M.Builder getBuilder() {
        return (M.Builder) builder.clone();
    }

    public Scope getScope() {
        return scope;
    }

    public void notifyChange() {
        try {
            M message = getMessage();
            informer.send(new Event(informerScope, message.getClass(), message));
        } catch (Exception ex) {
            logger.error("Could not notify update", ex);
        }
    }

    public static Scope generateScope(final String id, final Location location) {
        return location.getScope().concat(new Scope(Location.COMPONENT_SEPERATOR + id));
    }

    public abstract void registerMethods(final LocalServer server) throws RSBException;
}
