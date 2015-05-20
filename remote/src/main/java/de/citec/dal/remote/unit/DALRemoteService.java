/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.remote.unit;

import com.google.protobuf.GeneratedMessage;
import de.citec.dal.hal.service.Service;
import de.citec.dal.hal.service.ServiceType;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.CouldNotTransformException;
import de.citec.jul.exception.InitializationException;
import de.citec.jul.iface.Identifiable;
import de.citec.jul.extension.rsb.com.RSBRemoteService;
import de.citec.jul.extension.rsb.scope.ScopeTransformer;
import rst.homeautomation.service.ServiceConfigType;
import rst.homeautomation.unit.UnitConfigType;

/**
 *
 * @author Divine Threepwood
 * @param <M>
 */
public abstract class DALRemoteService<M extends GeneratedMessage> extends RSBRemoteService<M> implements Service, Identifiable<String> {

    @Override
    public ServiceType getServiceType() {
        return ServiceType.MULTI;
    }

    @Override
    public String getId() throws CouldNotPerformException {
        return (String) getField(FIELD_ID);
    }

    public void init(UnitConfigType.UnitConfig config) throws InitializationException {
        try {
            super.init(ScopeTransformer.transform(config.getScope()));
        } catch (CouldNotTransformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    @Override
    @Deprecated
    public ServiceConfigType.ServiceConfig getServiceConfig() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
