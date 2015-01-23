/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.service.rsb;

import rsb.Event;
import rsb.Factory;
import rsb.Informer;
import rsb.InitializeException;
import rsb.RSBException;
import rsb.Scope;

/**
 *
 * @author Divine <DivineThreepwood@gmail.com>
 * @param <DataType>
 */
public class RSBSingleInformer<DataType extends Object> implements RSBInformerInterface<DataType> {

	private final Informer<DataType> internalInformer;

	/**
     * Creates an informer for a specific data type with a given scope and with
     * a specified config.
     *
     * @param scope the scope
     * @param type the data type to send by this informer
     * @throws InitializeException error initializing the informer
     */
	public RSBSingleInformer(final String scope, final Class<DataType> type) throws InitializeException {
        this.internalInformer = Factory.getInstance().createInformer(scope, type);
    }

    /**
     * Creates an informer for a specific data type with a given scope and with
     * a specified config.
     *
     * @param scope the scope
     * @param type the data type to send by this informer
     * @throws InitializeException error initializing the informer
     */
    public RSBSingleInformer(final Scope scope, final Class<DataType> type) throws InitializeException {
		this.internalInformer = Factory.getInstance().createInformer(scope, type);
	}

	@Override
	public Event send(Event event) throws RSBException {
		return internalInformer.send(event);
	}

	@Override
	public Event send(DataType data) throws RSBException {
		return internalInformer.send(data);
	}

	@Override
	public Class<?> getTypeInfo() {
		return internalInformer.getTypeInfo();
	}

	@Override
	public void setTypeInfo(Class<DataType> typeInfo) {
		internalInformer.setTypeInfo(typeInfo);
	}

	@Override
	public Scope getScope() {
		return internalInformer.getScope();
	}

	@Override
	public void activate() throws RSBException {
		internalInformer.activate();
	}

	@Override
	public void deactivate() throws RSBException, InterruptedException {
		internalInformer.deactivate();
	}

	@Override
	public boolean isActive() {
		return internalInformer.isActive();
	}
}
