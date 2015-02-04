/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.bindings.openhab;

import com.google.protobuf.GeneratedMessage;
import de.citec.jul.rsb.RSBCommunicationService;
import de.citec.jul.rsb.ScopeProvider;
import rsb.Scope;

/**
 *
 * @author Divine <DivineThreepwood@gmail.com>
 * @param <M>
 * @param <MB>
 */
public abstract class AbstractOpenHABDeviceController<M extends GeneratedMessage, MB extends GeneratedMessage.Builder> extends RSBCommunicationService<M, MB> {

	public AbstractOpenHABDeviceController(Scope scope, MB builder) {
		super(scope, builder);
	}

	public AbstractOpenHABDeviceController(String id, ScopeProvider location, MB builder) {
		super(id, location, builder);
	}
}
