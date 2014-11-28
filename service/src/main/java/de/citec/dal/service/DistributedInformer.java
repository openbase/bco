/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.service;

import org.slf4j.LoggerFactory;
import rsb.Activatable;
import rsb.Event;
import rsb.InitializeException;
import rsb.RSBException;
import rsb.Scope;

/**
 *
 * @author mpohling
 * @param <DataType>
 */
public class DistributedInformer<DataType extends Object> implements Activatable {

    protected final org.slf4j.Logger logger;
    
    private static final RSBInformerPool pool = RSBInformerPool.getInstance();
    
    private Scope scope;
    
    private boolean active;
    
    /**
     * The default data type for this informer.
     */
    private Class<DataType> type;

    /**
     * Creates an informer for a specific data type with a given scope and with
     * a specified config.
     *
     * @param scope the scope
     * @param type the data type to send by this informer
     * @throws InitializeException error initializing the informer
     */
    public DistributedInformer(final String scope, final Class<DataType> type) throws InitializeException {
        this(new Scope(scope), type);
    }

    /**
     * Creates an informer for a specific data type with a given scope and with
     * a specified config.
     *
     * @param scope the scope
     * @param type the data type to send by this informer
     * @throws InitializeException error initializing the informer
     */
    public DistributedInformer(final Scope scope, final Class<DataType> type) throws InitializeException {

        if (scope == null) {
            throw new IllegalArgumentException("Informer scope must not be null.");
        }
        
        if (type == null) {
            throw new IllegalArgumentException("Informer type must not be null.");
        }

        this.type = type;
        this.scope = scope;
        this.logger = LoggerFactory.getLogger(getClass());
        
        logger.debug("New distributed informer instance created: [Scope:" + scope + ", Type:" + type.getName() + "]");

    }

    @Override
    public void activate() throws InitializeException {
        active = true;
    }

    @Override
    public void deactivate() throws RSBException, InterruptedException {
        active = false;
    }

    private void validateState() throws RSBException {
        if (!active) {
            throw new RSBException("Informer not active!");
        }
    }

    /**
     * Send an {@link Event} to all subscribed participants.
     *
     * @param event the event to send
     * @return modified event with set timing information
     * @throws RSBException error sending event
     * @throws IllegalArgumentException if the event is not complete or does not
     * match the type or scope settings of the informer
     */
    public Event send(final Event event) throws RSBException {
        validateState();
        return pool.send(event);
    }

    /**
     * Send data (of type <T>) to all subscribed participants.
     *
     * @param data data to send with default setting from the informer
     * @return generated event
     * @throws RSBException error sending event
     */
    public Event send(final DataType data) throws RSBException {
        validateState();
        return pool.send(new Event(scope, type, data));
    }

    /**
     * Returns the class describing the type of data sent by this informer.
     *
     * @return class
     */
    public Class<?> getTypeInfo() {
        return this.type;
    }

    /**
     * Set the class object describing the type of data sent by this informer.
     *
     * @param typeInfo a {@link Class} instance describing the sent data
     */
    public void setTypeInfo(final Class<DataType> typeInfo) {
        this.type = typeInfo;
    }

    public Scope getScope() {
        return scope;
    }

    public void setScope(Scope scope) {
        this.scope = scope;
    }    

    @Override
    public boolean isActive() {
        return active;
    }

}
