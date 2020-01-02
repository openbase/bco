package org.openbase.bco.dal.remote.layer.service;

/*
 * #%L
 * BCO DAL Remote
 * %%
 * Copyright (C) 2014 - 2020 openbase.org
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

import org.openbase.bco.registry.remote.Registries;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.processing.StringProcessor;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.unit.UnitConfigType.UnitConfig;

import java.util.ArrayList;
import java.util.Collection;
//import org.openbase.jul.exception.InstantiationException;

/**
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
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
    public AbstractServiceRemote<?, ?> newInitializedInstance(final ServiceType serviceType, final Collection<UnitConfig> unitConfigs) throws CouldNotPerformException, InterruptedException {
        AbstractServiceRemote<?, ?> serviceRemote = newInstance(serviceType);
        serviceRemote.init(unitConfigs);
        return serviceRemote;
    }

    @Override
    public AbstractServiceRemote<?, ?> newInitializedInstance(ServiceType serviceType, Collection<UnitConfig> unitConfigs, boolean filterInfrastructureUnits) throws CouldNotPerformException, InterruptedException {
        AbstractServiceRemote<?, ?> serviceRemote = newInstance(serviceType);
        serviceRemote.setInfrastructureFilter(filterInfrastructureUnits);
        serviceRemote.init(unitConfigs);
        return serviceRemote;
    }

    @Override
    public AbstractServiceRemote<?, ?> newInitializedInstance(final ServiceType serviceType, final UnitConfig unitConfig) throws CouldNotPerformException, InterruptedException {
        AbstractServiceRemote<?, ?> serviceRemote = newInstance(serviceType);
        serviceRemote.init(unitConfig);
        return serviceRemote;
    }

    @Override
    public AbstractServiceRemote<?, ?> newInitializedInstanceByIds(final ServiceType serviceType, final Collection<String> unitIDs) throws CouldNotPerformException, InterruptedException {
        Registries.waitForData();
        final Collection<UnitConfig> unitRemotes = new ArrayList<>();
        for (String unitId : unitIDs) {
            unitRemotes.add(Registries.getUnitRegistry().getUnitConfigById(unitId));
        }
        return newInitializedInstance(serviceType, unitRemotes);
    }

    @Override
    public AbstractServiceRemote<?, ?> newInitializedInstanceById(final ServiceType serviceType, final String unitID) throws CouldNotPerformException, InterruptedException {
        Registries.waitForData();
        return newInitializedInstance(serviceType, Registries.getUnitRegistry().getUnitConfigById(unitID));
    }

    @Override
    public AbstractServiceRemote<?, ?> newInstance(final ServiceType serviceType) throws org.openbase.jul.exception.InstantiationException {
        try {
            return instantiateServiceRemote(loadServiceRemoteClass(serviceType));
        } catch (CouldNotPerformException ex) {
            throw new org.openbase.jul.exception.InstantiationException(AbstractServiceRemote.class, serviceType.name(), ex);
        }
    }

    public static Class<? extends AbstractServiceRemote> loadServiceRemoteClass(final ServiceType serviceType) throws CouldNotPerformException {
        String remoteClassName = AbstractServiceRemote.class.getPackage().getName() + "." + StringProcessor.transformUpperCaseToPascalCase(serviceType.name()) + "Remote";
        try {
            return (Class<? extends AbstractServiceRemote>) ServiceRemoteFactoryImpl.class.getClassLoader().loadClass(remoteClassName);
        } catch (NullPointerException | ClassNotFoundException ex) {
            throw new CouldNotPerformException("Could not detect service remote class for ServiceType[" + serviceType.name() + "]!", ex);
        }
    }

    private static AbstractServiceRemote<?, ?> instantiateServiceRemote(final Class<? extends AbstractServiceRemote> serviceRemoteClass) throws org.openbase.jul.exception.InstantiationException {
        try {
            AbstractServiceRemote<?, ?> remote = serviceRemoteClass.newInstance();
            return remote;
        } catch (InstantiationException | IllegalAccessException ex) {
            throw new org.openbase.jul.exception.InstantiationException(serviceRemoteClass, ex);
        }
    }
}
