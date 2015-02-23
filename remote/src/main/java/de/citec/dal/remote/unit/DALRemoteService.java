/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.remote.unit;

import com.google.protobuf.GeneratedMessage;
import de.citec.dal.hal.service.Service;
import de.citec.dal.hal.service.ServiceType;
import de.citec.jul.rsb.RSBRemoteService;

/**
 *
 * @author Divine <DivineThreepwood@gmail.com>
 * @param <M>
 */
public abstract class DALRemoteService<M extends GeneratedMessage> extends RSBRemoteService<M> implements Service {

	@Override
	public ServiceType getServiceType() {
		return ServiceType.MULTI;
	}

}
