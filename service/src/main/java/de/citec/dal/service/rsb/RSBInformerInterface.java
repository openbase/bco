/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.service.rsb;

import rsb.Activatable;
import rsb.Event;
import rsb.RSBException;
import rsb.Scope;

/**
 *
 * @author Divine <DivineThreepwood@gmail.com>
 * @param <DataType>
 */
public interface RSBInformerInterface<DataType extends Object> extends Activatable {

	public enum InformerType {

		Distributed, Single
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
    public Event send(final Event event) throws RSBException;

    /**
     * Send data (of type <T>) to all subscribed participants.
     *
     * @param data data to send with default setting from the informer
     * @return generated event
     * @throws RSBException error sending event
     */
    public Event send(final DataType data) throws RSBException;

    /**
     * Returns the class describing the type of data sent by this informer.
     *
     * @return class
     */
    public Class<?> getTypeInfo();

    /**
     * Set the class object describing the type of data sent by this informer.
     *
     * @param typeInfo a {@link Class} instance describing the sent data
     */
    public void setTypeInfo(final Class<DataType> typeInfo);

    public Scope getScope();

}
