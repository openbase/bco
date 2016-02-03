/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.dal.remote.service;

/*
 * #%L
 * DAL Remote
 * %%
 * Copyright (C) 2014 - 2016 DivineCooperation
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */

import java.util.Collection;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.processing.StringProcessor;
import rst.homeautomation.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class ServiceRemoteFactoryImpl implements ServiceRemoteFactory {

    private static ServiceRemoteFactory instance;

    private ServiceRemoteFactoryImpl() {
    }

    public synchronized static ServiceRemoteFactory getInstance() {
        if (instance == null) {
            instance = new ServiceRemoteFactoryImpl();
        }
        return instance;
    }

    @Override
    public AbstractServiceRemote createAndInitServiceRemote(ServiceType serviceType, Collection<UnitConfig> unitConfigs) throws CouldNotPerformException {
        AbstractServiceRemote serviceRemote = createServiceRemote(serviceType);
        serviceRemote.init(unitConfigs);
        return serviceRemote;
    }

    @Override
    public AbstractServiceRemote createAndInitServiceRemote(ServiceType serviceType, UnitConfig unitConfig) throws CouldNotPerformException {
        AbstractServiceRemote serviceRemote = createServiceRemote(serviceType);
        serviceRemote.init(unitConfig);
        return serviceRemote;
    }

    @Override
    public AbstractServiceRemote createServiceRemote(ServiceType serviceType) throws CouldNotPerformException {
        try {
            return instantiatServiceRemote(loadServiceRemoteClass(serviceType));
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not create service remote!", ex);
        }
    }

    public static Class<? extends AbstractServiceRemote> loadServiceRemoteClass(final ServiceType serviceType) throws CouldNotPerformException {
        String remoteClassName = AbstractServiceRemote.class.getPackage().getName() + "." + StringProcessor.transformUpperCaseToCamelCase(serviceType.name()) + "Remote";
        try {
            return (Class<? extends AbstractServiceRemote>) ServiceRemoteFactoryImpl.class.getClassLoader().loadClass(remoteClassName);
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not detect service remote class for ServiceType[" + serviceType.name() + "]!", ex);
        }
    }

    private static AbstractServiceRemote instantiatServiceRemote(final Class<? extends AbstractServiceRemote> serviceRemoteClass) throws org.dc.jul.exception.InstantiationException {
        try {
            AbstractServiceRemote remote = serviceRemoteClass.newInstance();
            return remote;
        } catch (Exception ex) {
            throw new org.dc.jul.exception.InstantiationException("Could not instantiate service remote out of Class[" + serviceRemoteClass.getName() + "]", ex);
        }
    }
}
