/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.service.rsb;

import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.GeneratedMessage.Builder;
import de.citec.dal.data.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rsb.Factory;
import rsb.InitializeException;
import rsb.RSBException;
import rsb.Scope;
import rsb.patterns.LocalServer;

/**
 *
 * @author mpohling
 * @param <M>
 * @param <MB>
 */
public abstract class RSBCommunicationService<M extends GeneratedMessage, MB extends Builder> {

    public enum ConnectionState {Online, Offline};
    
    public final static String SCOPE_SUFFIX_RPC = "ctrl";
    public final static String SCOPE_SUFFIX_INFORMER = "status";

    protected final Logger logger;

    protected final MB builder;
    protected DistributedInformer<M> informer;
    protected LocalServer server;
    protected Scope scope;
    private ConnectionState state;

    public RSBCommunicationService(final String id, final Location location, final MB builder) {
        this(generateScope(id, location), builder);
    }

    public RSBCommunicationService(final Scope scope, final MB builder) {
        this.logger = LoggerFactory.getLogger(getClass());
        this.scope = new Scope(scope.toString().toLowerCase());
        this.builder = builder;
        logger.debug("Init RSBCommunicationService for component " + getClass().getSimpleName() + " on " + scope + ".");
    }

    public void init() throws RSBException {
        try {
            logger.info("Init informer service...");            
            this.informer = new DistributedInformer(scope.concat(new Scope(Location.COMPONENT_SEPERATOR + SCOPE_SUFFIX_INFORMER)), detectMessageClass());
        } catch (Exception ex) {
            throw new RSBException("Could not init informer.", ex);
        }

        try {
            logger.info("Init rpc server...");
            // Get local server object which allows to expose remotely callable methods.
            server = Factory.getInstance().createLocalServer(scope.concat(new Scope(Location.COMPONENT_SEPERATOR + SCOPE_SUFFIX_RPC)));

            // register rpc methods.
            registerMethods(server);
            

        } catch (Exception ex) {
            throw new RSBException("Could not init rpc server.", ex);
        }
    }
    
    private Class<? extends M> detectMessageClass() {
        return (Class<? extends M>) ((M) builder.clone().buildPartial()).getClass();
    } 

    public void activate() throws Exception {
        logger.debug("Activate RSBCommunicationService for: " + this);
        
        try {
            informer.activate();
        } catch (InitializeException exx) {
            throw new RSBException("Could not activate informer.", exx);
        }

        try {
            server.activate();
        } catch (RSBException ex) {
            throw new RSBException("Could not activate rpc server.", ex);
        }
        
        state = ConnectionState.Online;
    }

    public void deactivate() throws Exception {
        
        try {
            informer.deactivate();
        } catch (Exception ex) {
            throw new RSBException("Could not deactivate informer.", ex);
        }

        try {
            server.deactivate();
        } catch (Exception ex) {
            throw new RSBException("Could not deactivate rpc server.", ex);
        }
        
        state = ConnectionState.Offline;
    }

    public M getMessage() throws RSBException {
        try {
            return (M) builder.clone().build();
        } catch (Exception ex) {
            throw new RSBException("Could not build message!", ex);
        }
    }

    public MB cloneBuilder() {
        return (MB) builder.clone();
    }
    
    public MB getBuilder() {
        return builder;
    }

    public Scope getScope() {
        return scope;
    }

    public void notifyChange() {
        logger.debug("Notify change of "+this);
        try {
            informer.send(getMessage());
        } catch (Exception ex) {
            logger.error("Could not notify update", ex);
        }
    }

    protected final void setField(String name, Object value) {
        try {
            builder.setField(builder.getDescriptorForType().findFieldByName(name), value);
        } catch (Exception ex) {
            logger.warn("Could not set field [" + name + "=" + value + "] for " + this);
        }
    }

    public static Scope generateScope(final String id, final Location location) {
        return location.getScope().concat(new Scope(Location.COMPONENT_SEPERATOR + id));
    }

    public ConnectionState getState() {
        return state;
    }
    
    public abstract void registerMethods(final LocalServer server) throws RSBException;

    @Override
    public String toString() {
        return getClass().getSimpleName()+"["+scope+"]";
    }
}
